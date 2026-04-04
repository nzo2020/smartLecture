package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordingService extends Service {
    private static final String CHANNEL_ID = "RecordingChannel";
    private MediaRecorder mediaRecorder;
    private String outputFilePath, eventID, teacherName;
    private boolean isPublic;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        outputFilePath = intent.getStringExtra("filePath");
        eventID = intent.getStringExtra("eventID");
        teacherName = intent.getStringExtra("teacher");
        isPublic = intent.getBooleanExtra("isPublic", false);

        startForeground(1, createNotification("מקליט הרצאה: " + teacherName));
        startRecording();

        return START_NOT_STICKY;
    }

    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(outputFilePath);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            // כאן מפעילים את ה-Threads לעיבוד (Gemini + Storage)
            processAndUpload();
        }
        super.onDestroy();
    }

    private void processAndUpload() {
        executorService.execute(() -> {
            // כאן תבוא הקריאה ל-GeminiManager ול-Storage שכתבנו
            // בסיום התהליך, שלחי התראה:
            NotificationHelper.showSummaryReadyNotification(getApplicationContext(), teacherName);
        });
    }

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SmartLecture")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Recording Service Channel",
                    NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}