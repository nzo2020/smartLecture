package com.example.smartlecture;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.*;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.example.smartlecture.Gemini.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class RecordingService extends Service {
    private MediaRecorder recorder;
    private String eventId, userId, filePath, teacherName, lessonTitle, locationName;
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
        teacherName = intent.getStringExtra("TEACHER");
        lessonTitle = intent.getStringExtra("LESSON_TITLE");
        locationName = intent.getStringExtra("LOCATION"); // מקבל את המיקום (אוטומטי או ידני)
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
        } catch (Exception e) { Log.e("Service", "Error", e); }
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
        File file = new File(filePath);
        FirebaseStorage.getInstance().getReference("recordings/" + userId + "/" + eventId + ".mp4")
                .putFile(android.net.Uri.fromFile(file))
                .addOnSuccessListener(task -> task.getStorage().getDownloadUrl().addOnSuccessListener(uri -> analyzeWithGemini(file, uri.toString())));
    }

    private void analyzeWithGemini(File file, String url) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String prompt = "סכם את השיעור בעברית. בסוף הוסף 'קישורים רלוונטיים:' ו-3 קישורים.";
            GeminiManager.getInstance().sendTextWithFilePrompt(prompt, bytes, "audio/mp4", new GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    String summary = result, links = "";
                    if (result.contains("קישורים רלוונטיים")) {
                        String[] parts = result.split("קישורים רלוונטיים:?");
                        summary = parts[0].trim();
                        if (parts.length > 1) links = parts[1].trim();
                    }
                    saveToDb(summary, links, url);
                }
                @Override public void onFailure(Throwable e) { saveToDb("Error", "", url); }
            });
        } catch (Exception e) { stopSelf(); }
    }

    private void saveToDb(String summary, String links, String url) {
        String visibility = isPublic ? "pub_true" : "pub_false";
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (userName == null || userName.isEmpty()) userName = "User";

        // מבנה ה-DB שלך
        String path = isPublic ?
                "Lectures/pub_true/" + userName + "/" + eventId :
                "Lectures/pub_false/" + userId + "/" + userName + "/" + eventId;

        Map<String, Object> updates = new HashMap<>();
        updates.put("summaryText", summary);
        updates.put("relevantLinks", links);
        updates.put("audioURL", url);
        updates.put("location", locationName); // נשמר כאן!
        updates.put("status", isPublic ? "ready" : "draft");
        updates.put("userID", userId);
        updates.put("title", lessonTitle);
        updates.put("lecturer", teacherName);
        updates.put("timestamp", System.currentTimeMillis());

        FirebaseDatabase.getInstance().getReference(path).setValue(updates).addOnSuccessListener(aVoid -> {
            sendBroadcast(new Intent("RECORDING_FINISHED").putExtra("summaryText", summary).putExtra("relevantLinks", links));
            stopSelf();
        });
    }

    private Notification getNotification(String text) {
        String channelId = "record_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(channelId, "Record", NotificationManager.IMPORTANCE_LOW));
        }
        return new NotificationCompat.Builder(this, channelId).setContentTitle("Smart Lecture").setContentText(text).setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}