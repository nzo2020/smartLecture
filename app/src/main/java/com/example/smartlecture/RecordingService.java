package com.example.smartlecture;

import android.app.*;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.smartlecture.Gemini.*;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import java.io.File;
import java.nio.file.Files;

public class RecordingService extends Service {
    private MediaRecorder recorder;
    private String eventId, userId, filePath;
    private boolean isPublic;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if ("START_RECORDING".equals(action)) {
            setupParameters(intent);
            startForeground(1, getNotification("Recording lesson..."));
            startRecording();
        } else if ("STOP_RECORDING".equals(action)) {
            stopRecording();
        }
        return START_STICKY;
    }

    private void setupParameters(Intent intent) {
        eventId = intent.getStringExtra("EVENT_ID");
        userId = intent.getStringExtra("USER_ID");
        isPublic = intent.getBooleanExtra("IS_PUBLIC", false);
        filePath = getExternalCacheDir().getAbsolutePath() + "/" + eventId + ".mp4";
    }

    private void startRecording() {
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            recorder.setOutputFile(filePath);
            recorder.prepare();
            recorder.start();
        } catch (Exception e) { Log.e("Service", "Failed", e); }
    }

    private void stopRecording() {
        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            uploadAndAnalyze();
        }
    }

    private void uploadAndAnalyze() {
        Log.d("STEP", "--- Starting Upload to Firebase ---");
        File file = new File(filePath);

        FirebaseStorage.getInstance().getReference("recordings/" + userId + "/" + eventId + ".mp4")
                .putFile(android.net.Uri.fromFile(file))
                .addOnSuccessListener(task -> {
                    Log.d("STEP", "--- Upload Successful! Getting URL ---");
                    task.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                        Log.d("STEP", "--- URL Received: " + uri.toString() + " ---");
                        analyzeWithGemini(file, uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("STEP", "--- Upload Failed: " + e.getMessage() + " ---");
                });
    }

    private void analyzeWithGemini(File file, String url) {
        try {
            Log.d("STEP", "--- Preparing Bytes for Gemini ---");
            byte[] bytes = Files.readAllBytes(file.toPath());

            Log.d("STEP", "--- Starting Gemini AI Request (Calling SDK) ---");

            GeminiManager.getInstance().sendTextWithFilePrompt("סכם את השיעור בעברית", bytes, "audio/mp4", new GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    Log.d("STEP", "--- Got result from Gemini! ---");
                    Log.d("STEP", "Summary Length: " + result.length() + " chars");

                    saveToDb(result, url);

                    Intent intent = new Intent("RECORDING_FINISHED");
                    intent.putExtra("summary", result);
                    sendBroadcast(intent);
                    stopSelf();
                }

                @Override
                public void onFailure(Throwable e) {
                    Log.e("STEP", "--- Gemini Failed: " + e.getMessage() + " ---");
                    stopSelf();
                }
            });
        } catch (Exception e) {
            Log.e("STEP", "--- Error in analyzeWithGemini: " + e.getMessage() + " ---");
            stopSelf();
        }
    }

    private void saveToDb(String summary, String url) {
        String path = isPublic ? "Lectures/pub_true/נועה זוהר/" + eventId : "Lectures/pub_false/" + userId + "/נועה זוהר/" + eventId;
        FirebaseDatabase.getInstance().getReference(path).child("summaryText").setValue(summary);
        FirebaseDatabase.getInstance().getReference(path).child("audioURL").setValue(url);
        FirebaseDatabase.getInstance().getReference(path).child("status").setValue("ready");
    }

    private Notification getNotification(String text) {
        String channelId = "record_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(channelId, "Record", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(chan);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Smart Lecture").setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}