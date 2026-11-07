
package com.inkvoice.ai;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
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
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    private static final int REQ_AUDIO = 77;

    private final int BG = Color.rgb(20, 25, 29);
    private final int CARD = Color.rgb(34, 42, 46);
    private final int GOLD = Color.rgb(220, 178, 91);
    private final int TEAL = Color.rgb(34, 190, 160);
    private final int WHITE = Color.rgb(245, 244, 238);
    private final int GREY = Color.rgb(165, 174, 176);

    private String mode = "General Notes";
    private String page = "Dashboard";

    private EditText topicInput, notesInput;
    private TextView status, pen;
    private LinearLayout content;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private SpeechRecognizer speechRecognizer;
    private boolean recording = false;
    private boolean lightTheme = false;
    private File currentAudio;
    private android.content.SharedPreferences prefs;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("inkvoice_notes", MODE_PRIVATE);
        showDashboard();

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        }
    }

    private int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    private int background() {
        return lightTheme ? Color.rgb(246, 243, 235) : BG;
    }

    private int cardColor() {
        return lightTheme ? Color.WHITE : CARD;
    }

    private int mainText() {
        return lightTheme ? Color.rgb(38, 43, 43) : WHITE;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(dp(2), dp(5), dp(2), dp(5));
        return t;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.rgb(25, 31, 32));
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(shape(GOLD, 14));
        b.setPadding(dp(8), dp(8), dp(8), dp(8));
        return b;
    }

    private void addButton(LinearLayout box, String label, Runnable action) {
        Button b = button(label);
        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(0, dp(5), 0, dp(5));
        box.addView(b, p);
        b.setOnClickListener(v -> action.run());
    }

    private void addCard(LinearLayout box, String title,
                         String description, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(12), dp(15), dp(12));
        card.setBackground(shape(cardColor(), 18));

        TextView heading = text(title, 18, GOLD, true);
        card.addView(heading);
        card.addView(text(description, 13, mainText(), false));

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(7), 0, dp(7));
        box.addView(card, p);
        card.setOnClickListener(v -> action.run());
    }

    private void startPage(String title) {
        stopSpeech();

        page = title;

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(15), dp(18), dp(25));
        root.setBackgroundColor(background());

        scroll.addView(root);

        TextView brand = text("✒  INKVOICE AI", 14, GOLD, true);
        root.addView(brand);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button back = button("‹ Back");
        header.addView(back);
        back.setOnClickListener(v -> showDashboard());

        TextView heading = text(title, 23, mainText(), true);
        heading.setPadding(dp(12), dp(5), 0, dp(5));
        header.addView(heading);
        root.addView(header);

        View line = new View(this);
        line.setBackgroundColor(GOLD);
        root.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(12), 0, 0);
        root.addView(content);

        setContentView(scroll);
    }

    private void showDashboard() {
        startPage("Dashboard");

        content.addView(text("Your voice. Your words.", 24,
                mainText(), true));

        content.addView(text(
            "A smarter notebook for every moment.",
            14, GREY, false));

        content.addView(space(12));

        addCard(content, "✎  General Notes",
            "Meetings, lectures, ideas and everyday notes.",
            () -> openNotebook("General Notes"));

        addCard(content, "✦  Church Mode",
            "Sermons, preaching and church messages.",
            () -> openNotebook("Church Mode"));

        content.addView(space(10));

        content.addView(text("YOUR WORKSPACE", 13, GOLD, true));

        addCard(content, "▤  My Notebooks",
            "Open and organise your saved notes.",
            () -> showNotebooks());

        addCard(content, "♫  Recordings",
            "Listen to your saved audio recordings.",
            () -> showRecordings());

        addCard(content, "◷  History",
            "Review your previous saved notes.",
            () -> showHistory());

        addCard(content, "✧  AI Writing Assistant",
            "Your space for improving and organising writing.",
            () -> showAssistant());

        addCard(content, "⚙  Settings",
            "Appearance and app preferences.",
            () -> showSettings());

        content.addView(space(12));

        TextView footer = text("INKVOICE AI  •  YOUR INTELLIGENT NOTEBOOK",
                11, GOLD, true);
        footer.setGravity(Gravity.CENTER);
        content.addView(footer);
    }

    private View space(int height) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return v;
    }

    private void openNotebook(String selectedMode) {
        mode = selectedMode;
        startPage(selectedMode);

        content.addView(text(
            selectedMode.equals("Church Mode")
                ? "Your Sermon Notebook"
                : "Your Personal Notebook",
            19, GOLD, true));

        topicInput = new EditText(this);
        topicInput.setSingleLine(true);
        topicInput.setHint("Enter notebook topic");
        topicInput.setTextColor(mainText());
        topicInput.setHintTextColor(GREY);
        topicInput.setBackground(shape(cardColor(), 12));
        topicInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        content.addView(topicInput);

        addButton(content, "+ New topic", () -> {
            if (notesInput != null) {
                notesInput.append("\n\n— NEW TOPIC —\n");
            }
            topicInput.setText("");
            topicInput.requestFocus();
        });

        notesInput = new EditText(this);
        notesInput.setMinLines(9);
        notesInput.setGravity(Gravity.TOP);
        notesInput.setTextColor(mainText());
        notesInput.setHintTextColor(GREY);
        notesInput.setHint("Your notes and recognised speech will appear here...");
        notesInput.setBackground(shape(cardColor(), 14));
        notesInput.setPadding(dp(13), dp(13), dp(13), dp(13));
        content.addView(notesInput,
            new LinearLayout.LayoutParams(-1, dp(210)));

        pen = text("✒  AI pen ready", 16, TEAL, true);
        content.addView(pen);

        status = text("Ready • " + mode, 13, TEAL, true);
        content.addView(status);

        addButton(content, "Start speech-to-text",
            () -> startSpeech());

        addButton(content, "Stop transcription",
            () -> stopSpeech());

        addButton(content, "Save notebook",
            () -> saveNote());

        content.addView(space(15));
        content.addView(text("AUDIO RECORDING", 16, GOLD, true));

        addButton(content, "● Start audio recording",
            () -> startRecording());

        addButton(content, "■ Stop and save recording",
            () -> stopRecording());

        addButton(content, "View saved recordings",
            () -> showRecordings());
    }

    private void showNotebooks() {
        startPage("My Notebooks");

        content.addView(text(
            "Your saved notebook entries are stored on this phone.",
            14, GREY, false));

        showSavedNotes(content);
    }

    private void showHistory() {
        startPage("History");
        content.addView(text(
            "Your previous saved notes.",
            14, GREY, false));
        showSavedNotes(content);
    }

    private void showSavedNotes(LinearLayout box) {
        Map<String, ?> all = prefs.getAll();
        boolean found = false;

        for (String key : all.keySet()) {
            if (key.startsWith("note_")) {
                found = true;
                String value = prefs.getString(key, "");
                addCard(box, "Saved notebook", value,
                    () -> {
                        startPage("Saved Note");
                        TextView saved = text(value, 15, mainText(), false);
                        content.addView(saved);
                    });
            }
        }

        if (!found) {
            box.addView(text("No saved notebooks yet.",
                14, GREY, false));
        }
    }

    private void showRecordings() {
        startPage("Recordings");
        content.addView(text(
            "Your saved audio files.",
            14, GREY, false));

        File dir = new File(getExternalFilesDir(null), "recordings");
        File[] files = dir.listFiles();

        if (files == null || files.length == 0) {
            content.addView(text(
                "No recordings saved yet.",
                14, GREY, false));
            return;
        }

        Arrays.sort(files,
            (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".m4a")) {
                addCard(content, f.getName(),
                    "Saved audio recording",
                    () -> playFile(f));
            }
        }
    }

    private void showAssistant() {
        startPage("AI Writing Assistant");

        content.addView(text(
            "Writing workspace",
            19, GOLD, true));

        EditText writing = new EditText(this);
        writing.setMinLines(8);
        writing.setGravity(Gravity.TOP);
        writing.setHint("Paste or write text here...");
        writing.setTextColor(mainText());
        writing.setHintTextColor(GREY);
        writing.setBackground(shape(cardColor(), 14));
        writing.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(writing);

        addButton(content, "Copy text",
            () -> {
                android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText(
                        "InkVoice text", writing.getText().toString()));
                toast("Text copied");
            });

        content.addView(text(
            "AI rewriting and summarising will be added in a later development stage.",
            13, GREY, false));
    }

    private void showSettings() {
        startPage("Settings");

        content.addView(text("Appearance", 18, GOLD, true));

        addButton(content,
            lightTheme ? "Switch to Dark Theme" : "Switch to Light Theme",
            () -> {
                lightTheme = !lightTheme;
                showSettings();
            });

        content.addView(text(
            "Audio files are stored locally in the app.",
            14, GREY, false));

        content.addView(text(
            "Account registration, cloud backup and Premium subscriptions "
            + "will be developed separately.",
            13, GREY, false));
    }

    private void startSpeech() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Speech recognition unavailable");
            return;
        }

        stopSpeech();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(
            new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) {
                    status.setText("Listening • " + mode);
                    animatePen();
                }

                public void onBeginningOfSpeech() {
                    pen.setText("✒  Writing as you speak...");
                }

                public void onRmsChanged(float r) {}
                public void onBufferReceived(byte[] b) {}

                public void onEndOfSpeech() {
                    status.setText("Processing speech...");
                }

                public void onError(int e) {
                    status.setText("Speech service error " + e
                        + ". Check your speech service and try again.");
                    pen.clearAnimation();
                    pen.setText("✒  AI pen ready");
                }

                public void onResults(Bundle b) {
                    appendResults(b);
                    status.setText("Transcription received");
                    pen.clearAnimation();
                    pen.setText("✒  AI pen ready");
                }

                public void onPartialResults(Bundle b) {
                    appendResults(b);
                }

                public void onEvent(int t, Bundle b) {}
            });

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        // Do not force offline recognition.
        // Let Android use its available speech recognition service.
        speechRecognizer.startListening(i);
    }

    private void appendResults(Bundle b) {
        ArrayList<String> results =
            b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (results == null || results.isEmpty() || notesInput == null)
            return;

        String phrase = results.get(0);

        if (phrase.equalsIgnoreCase("new topic")) {
            topicInput.setText("");
            notesInput.append("\n\n— NEW TOPIC —\n");
            status.setText("New topic detected");
            return;
        }

        // Partial results replace the previous partial phrase.
        String existing = notesInput.getText().toString();
        String separator = existing.isEmpty() ? "" : " ";
        notesInput.setText(existing + separator + phrase);
        notesInput.setSelection(notesInput.getText().length());
    }

    private void animatePen() {
        if (pen == null) return;

        RotateAnimation a = new RotateAnimation(
            -8, 8,
            Animation.RELATIVE_TO_SELF, .5f,
            Animation.RELATIVE_TO_SELF, .5f);

        a.setDuration(280);
        a.setRepeatMode(Animation.REVERSE);
        a.setRepeatCount(Animation.INFINITE);
        pen.startAnimation(a);
    }

    private void stopSpeech() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
                speechRecognizer.destroy();
            } catch (Exception ignored) {}
            speechRecognizer = null;
        }

        if (pen != null) {
            pen.clearAnimation();
            pen.setText("✒  AI pen ready");
        }
    }

    private void saveNote() {
        if (notesInput == null) return;

        String body = notesInput.getText().toString().trim();

        if (body.isEmpty()) {
            toast("There are no notes to save yet");
            return;
        }

        String topic = topicInput.getText().toString().trim();
        if (topic.isEmpty()) topic = "Untitled topic";

        String key = "note_" + System.currentTimeMillis();

        prefs.edit().putString(key,
            "MODE: " + mode + "\nTOPIC: " + topic
            + "\n\n" + body).apply();

        toast("Notebook saved on this phone");
    }

    private void startRecording() {
        if (recording) {
            toast("Recording is already running");
            return;
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }

        try {
            File dir = new File(getExternalFilesDir(null), "recordings");
            if (!dir.exists()) dir.mkdirs();

            String stamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.US).format(new Date());

            currentAudio = new File(dir,
                mode.replace(" ", "_") + "_" + stamp + ".m4a");

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(currentAudio.getAbsolutePath());

            recorder.prepare();
            recorder.start();
            recording = true;

            if (status != null)
                status.setText("Recording audio • " + mode);

            toast("Recording started");

        } catch (Exception e) {
            toast("Could not start recording: " + e.getMessage());
            releaseRecorder();
        }
    }

    private void stopRecording() {
        if (!recording) {
            toast("No recording is running");
            return;
        }

        try {
            recorder.stop();
            toast("Recording saved");
        } catch (Exception e) {
            toast("Recording was too short to save");
            if (currentAudio != null) currentAudio.delete();
        }

        releaseRecorder();
        recording = false;

        if (status != null)
            status.setText("Ready • " + mode);
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.reset();
                recorder.release();
            } catch (Exception ignored) {}
            recorder = null;
        }
    }

    private void playFile(File f) {
        try {
            if (player != null) {
                player.release();
                player = null;
            }

            player = new MediaPlayer();
            player.setDataSource(f.getAbsolutePath());

            player.setOnPreparedListener(mp -> {
                mp.start();
                toast("Playing recording");
            });

            player.setOnCompletionListener(mp -> {
                mp.release();
                player = null;
            });

            player.prepareAsync();

        } catch (Exception e) {
            toast("Could not play recording");
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        if (!page.equals("Dashboard")) {
            showDashboard();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        stopSpeech();

        if (recording) {
            stopRecording();
        }

        if (player != null) {
            player.release();
            player = null;
        }

        super.onDestroy();
    }
}
