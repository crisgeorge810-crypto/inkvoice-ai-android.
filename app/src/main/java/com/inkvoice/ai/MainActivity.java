package com.inkvoice.ai;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.Context;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_AUDIO = 77;
    private String mode = "General Notes";
    private EditText topicInput, notesInput;
    private TextView status, pen;
    private LinearLayout recordingsList;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private SpeechRecognizer speechRecognizer;
    private boolean recording = false;
    private File currentAudio;
    private final List<File> audioFiles = new ArrayList<>();
    private android.content.SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("inkvoice_notes", MODE_PRIVATE);
        buildScreen();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        }
        refreshRecordings();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density); }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button button(String label) {
        Button b = new Button(this); b.setText(label); return b;
    }
    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(Color.rgb(250,248,242));
        scroll.addView(root);
        TextView title = text("✒  InkVoice AI", 27, Color.rgb(35,54,82), true);
        root.addView(title);
        TextView tagline = text("Your voice. Your words. Your intelligent notebook.", 13, Color.DKGRAY, false);
        root.addView(tagline);
        root.addView(space(14));
        root.addView(text("Choose your notebook", 17, Color.rgb(35,54,82), true));
        LinearLayout modes = new LinearLayout(this); modes.setOrientation(LinearLayout.HORIZONTAL);
        Button general = button("General Notes");
        Button church = button("Church Mode");
        modes.addView(general, new LinearLayout.LayoutParams(0, dp(52), 1));
        modes.addView(church, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(modes);
        general.setOnClickListener(v -> setMode("General Notes"));
        church.setOnClickListener(v -> setMode("Church Mode"));
        status = text("Ready • General Notes", 14, Color.rgb(40,120,75), true);
        root.addView(status);
        root.addView(space(10));
        topicInput = new EditText(this); topicInput.setSingleLine(true);
        topicInput.setHint("Topic (e.g. Meeting, Sunday sermon)");
        root.addView(topicInput);
        Button newTopic = button("+ New topic");
        newTopic.setOnClickListener(v -> { topicInput.setText(""); topicInput.requestFocus(); status.setText("New topic started"); });
        root.addView(newTopic);
        root.addView(space(8));
        root.addView(text("Notebook", 18, Color.rgb(35,54,82), true));
        notesInput = new EditText(this); notesInput.setMinLines(7); notesInput.setGravity(Gravity.TOP);
        notesInput.setHint("Your notes and transcription will appear here...");
        root.addView(notesInput, new LinearLayout.LayoutParams(-1, dp(180)));
        pen = text("✒  AI pen ready", 15, Color.rgb(40,90,150), true);
        root.addView(pen);
        Button listen = button("Start speech-to-text");
        listen.setOnClickListener(v -> startSpeech());
        root.addView(listen);
        Button stopListen = button("Stop transcription");
        stopListen.setOnClickListener(v -> stopSpeech());
        root.addView(stopListen);
        Button saveNote = button("Save note on this phone");
        saveNote.setOnClickListener(v -> saveNote());
        root.addView(saveNote);
        root.addView(space(14));
        root.addView(text("Audio recording", 18, Color.rgb(35,54,82), true));
        TextView recHint = text("Record audio separately from speech-to-text in this starter build. Simultaneous capture is a later engineering stage.", 12, Color.DKGRAY, false);
        root.addView(recHint);
        Button startRec = button("Start audio recording");
        startRec.setOnClickListener(v -> startRecording());
        root.addView(startRec);
        Button stopRec = button("Stop and save recording");
        stopRec.setOnClickListener(v -> stopRecording());
        root.addView(stopRec);
        root.addView(space(10));
        root.addView(text("Saved recordings", 18, Color.rgb(35,54,82), true));
        recordingsList = new LinearLayout(this); recordingsList.setOrientation(LinearLayout.VERTICAL);
        root.addView(recordingsList);
        setContentView(scroll);
    }
    private View space(int h) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h))); return v; }
    private void setMode(String m) { mode=m; status.setText("Ready • "+mode); }
    private void startSpeech() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO); return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { status.setText("Speech recognition is not available on this phone"); return; }
        stopSpeech();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b) { status.setText("Listening • "+mode); animatePen(); }
            public void onBeginningOfSpeech() { pen.setText("✒  Writing as you speak…"); }
            public void onRmsChanged(float r) {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() { status.setText("Processing speech…"); }
            public void onError(int e) { status.setText("Speech service stopped (code "+e+"). Tap Start speech-to-text to try again."); pen.setText("✒  AI pen ready"); }
            public void onResults(Bundle b) { appendResults(b); status.setText("Transcription received"); pen.setText("✒  AI pen ready"); }
            public void onPartialResults(Bundle b) { appendResults(b); }
            public void onEvent(int t, Bundle b) {}
        });
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.startListening(i);
    }
    private void appendResults(Bundle b) {
        ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (r!=null && !r.isEmpty()) {
            String existing=notesInput.getText().toString();
            String phrase=r.get(0);
            if (phrase.equalsIgnoreCase("new topic")) {
                topicInput.setText(""); notesInput.append("\n\n— New topic —\n"); status.setText("New topic command detected"); return;
            }
            if (TextUtils.isEmpty(existing) || !existing.endsWith(phrase)) notesInput.setText(existing + (existing.isEmpty()?"":" ") + phrase);
            notesInput.setSelection(notesInput.getText().length());
        }
    }
    private void animatePen() {
        RotateAnimation a=new RotateAnimation(-8,8,Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
        a.setDuration(280); a.setRepeatMode(Animation.REVERSE); a.setRepeatCount(Animation.INFINITE); pen.startAnimation(a);
    }
    private void stopSpeech() {
        if (speechRecognizer!=null) { try { speechRecognizer.stopListening(); speechRecognizer.destroy(); } catch(Exception ignored){} speechRecognizer=null; }
        if (pen!=null) { pen.clearAnimation(); pen.setText("✒  AI pen ready"); }
    }
    private void saveNote() {
        String body=notesInput.getText().toString().trim();
        if (body.isEmpty()) { toast("There are no notes to save yet"); return; }
        String topic=topicInput.getText().toString().trim();
        if (topic.isEmpty()) topic="Untitled topic";
        String key="note_"+System.currentTimeMillis();
        prefs.edit().putString(key, "MODE: "+mode+"\nTOPIC: "+topic+"\n\n"+body).apply();
        toast("Note saved on this phone");
    }
    private void startRecording() {
        if (recording) return;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO); return; }
        try {
            File dir=new File(getExternalFilesDir(null),"recordings"); if(!dir.exists())dir.mkdirs();
            String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
            currentAudio=new File(dir, mode.replace(" ","_")+"_"+stamp+".m4a");
            recorder=new MediaRecorder(); recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000); recorder.setAudioSamplingRate(44100); recorder.setOutputFile(currentAudio.getAbsolutePath());
            recorder.prepare(); recorder.start(); recording=true; status.setText("Recording audio • "+mode);
        } catch(Exception e) { toast("Could not start recording: "+e.getMessage()); releaseRecorder(); }
    }
    private void stopRecording() {
        if(!recording) { toast("No recording is running"); return; }
        try { recorder.stop(); toast("Recording saved"); } catch(Exception e) { toast("Recording was too short to save"); if(currentAudio!=null)currentAudio.delete(); }
        releaseRecorder(); recording=false; refreshRecordings(); status.setText("Ready • "+mode);
    }
    private void releaseRecorder() { if(recorder!=null){try{recorder.reset();recorder.release();}catch(Exception ignored){} recorder=null;} }
    private void refreshRecordings() {
        if(recordingsList==null)return;
        recordingsList.removeAllViews(); audioFiles.clear();
        File dir=new File(getExternalFilesDir(null),"recordings");
        File[] fs=dir.listFiles(); if(fs==null||fs.length==0){recordingsList.addView(text("No recordings saved yet.",13,Color.DKGRAY,false));return;}
        Arrays.sort(fs,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
        for(File f:fs) if(f.isFile()&&f.getName().endsWith(".m4a")) {
            audioFiles.add(f); LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            TextView name=text(f.getName(),12,Color.DKGRAY,false); name.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(name,new LinearLayout.LayoutParams(0,dp(48),1));
            Button play=button("Play"); play.setOnClickListener(v->playFile(f)); row.addView(play);
            recordingsList.addView(row);
        }
    }
    private void playFile(File f) {
        try { if(player!=null){player.release();player=null;} player=new MediaPlayer(); player.setDataSource(f.getAbsolutePath()); player.setOnPreparedListener(mp->{mp.start();toast("Playing "+f.getName());}); player.setOnCompletionListener(mp->{mp.release();player=null;}); player.prepareAsync(); }
        catch(Exception e){toast("Could not play recording");}
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){stopSpeech(); if(recording)stopRecording(); if(player!=null)player.release(); super.onDestroy();}
}
