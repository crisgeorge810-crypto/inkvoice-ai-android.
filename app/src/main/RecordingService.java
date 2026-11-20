
package com.inkvoice.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {

    public static final String ACTION_START =
            "com.inkvoice.ai.RECORD_START";

    public static final String ACTION_STOP =
            "com.inkvoice.ai.RECORD_STOP";

    private static final String CHANNEL_ID = "inkvoice_recording";
    private static final int NOTIFICATION_ID = 2001;

    private static final int MAX_RECORDING_MS =
            6 * 60 * 60 * 1000;

    private MediaRecorder recorder;
    private File recordingFile;
    private boolean recording = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();

        if (ACTION_START.equals(action)) {
            if (!recording) {
                startRecording();
            }
        } else if (ACTION_STOP.equals(action)) {
            stopRecording();
        }

        return START_NOT_STICKY;
    }

    private void startRecording() {

        try {
            Notification notification = buildNotification();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                );
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }

            File folder = new File(
                    getExternalFilesDir(null),
                    "recordings"
            );

            if (!folder.exists() && !folder.mkdirs()) {
                throw new Exception("Could not create recordings folder");
            }

            String timestamp = new SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()
            ).format(new Date());

            recordingFile = new File(
                    folder,
                    "InkVoice_" + timestamp + ".m4a"
            );

            recorder = new MediaRecorder();

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setOutputFile(recordingFile.getAbsolutePath());

            recorder.setMaxDuration(MAX_RECORDING_MS);

            recorder.setOnInfoListener((mediaRecorder, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRecording();
                }
            });

            recorder.prepare();
            recorder.start();

            recording = true;

        } catch (Exception e) {
            releaseRecorder();

            if (recordingFile != null && recordingFile.exists()
                    && recordingFile.length() == 0) {
                recordingFile.delete();
            }

            recording = false;
            stopForeground(true);
            stopSelf();
        }
    }

    private void stopRecording() {

        if (recorder != null) {
            try {
                if (recording) {
                    recorder.stop();
                }
            } catch (RuntimeException ignored) {
                // Remove an incomplete recording below.
            }

            releaseRecorder();
        }

        recording = false;

        if (recordingFile != null && recordingFile.exists()
                && recordingFile.length() == 0) {
            recordingFile.delete();
        }

        stopForeground(true);
        stopSelf();
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try {
                recorder.reset();
            } catch (Exception ignored) {
            }

            try {
                recorder.release();
            } catch (Exception ignored) {
            }

            recorder = null;
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "InkVoice AI Recording",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription(
                    "Shows when InkVoice AI is recording audio"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {

        Intent openApp = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("InkVoice AI is recording")
                .setContentText("Your audio is being saved.")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        if (recorder != null) {
            stopRecording();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
