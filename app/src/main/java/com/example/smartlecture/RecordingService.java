package com.example.smartlecture;

import android.app.*;
import android.content.Context;
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
import java.util.HashMap;
import java.util.Map;

public class RecordingService extends Service {
    private MediaRecorder recorder;
    private String eventId, userId, filePath, teacherName, lessonTitle;
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

        if (lessonTitle == null || lessonTitle.isEmpty()) {
            lessonTitle = "שיעור ללא כותרת";
        }

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
            Log.d("STEP", "--- Recording Started ---");
        } catch (Exception e) { Log.e("Service", "Failed to start recording", e); }
    }

    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                Log.e("Service", "Recording too short", e);
            }
            recorder.release();
            recorder = null;
            Log.d("STEP", "--- Recording Stopped ---");
            uploadAndAnalyze();
        }
    }

    private void uploadAndAnalyze() {
        File file = new File(filePath);
        if (!file.exists()) return;

        // העלאה ל-Storage לפי המבנה שציינת: recordings/user_id/event_id.mp4
        FirebaseStorage.getInstance().getReference("recordings/" + userId + "/" + eventId + ".mp4")
                .putFile(android.net.Uri.fromFile(file))
                .addOnSuccessListener(task -> {
                    task.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                        analyzeWithGemini(file, uri.toString());
                    });
                })
                .addOnFailureListener(e -> stopSelf());
    }

    private void analyzeWithGemini(File file, String url) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());

            // ה-Prompt המעודכן לבקשת סיכום וקישורים
            String prompt = "סכם את השיעור הבא בעברית בצורה תמציתית וברורה. " +
                    "בסוף הסיכום, הוסף כותרת בשם 'קישורים רלוונטיים:' ומתחתיה הבא 3 קישורים " +
                    "מאתרים לימודיים או יוטיוב שמרחיבים על הנושאים שדובר עליהם.";

            GeminiManager.getInstance().sendTextWithFilePrompt(prompt, bytes, "audio/mp4", new GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    String summaryPart = result;
                    String linksPart = "";

                    // חיפוש גמיש יותר שמתעלם מכוכביות של Markdown
                    if (result.contains("קישורים רלוונטיים")) {
                        // פיצול לפי מילת המפתח, לא משנה מה יש סביבה
                        String[] parts = result.split("קישורים רלוונטיים:?");
                        summaryPart = parts[0].replace("*", "").trim(); // ניקוי כוכביות מיותרות
                        if (parts.length > 1) {
                            linksPart = parts[1].trim();
                        }
                    }

                    saveToDb(summaryPart, linksPart, url);
                }

                @Override
                public void onFailure(Throwable e) {
                    saveToDb("לא הצלחתי לסכם את השיעור, אך ההקלטה נשמרה.", "", url);
                }
            });
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void saveToDb(String summary, String links, String url) {
        String visibilityKey = isPublic ? "pub_true" : "pub_false";
        String userName = "User";

        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            userName = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (userName == null || userName.isEmpty()) userName = "Unknown_User";
        }

        // בניית הנתיב לפי המבנה שלך: Lectures/pub_xxx/user_id/user_name/event_id
        String path = "Lectures/" + visibilityKey + "/" + userId + "/" + userName + "/" + eventId;

        Map<String, Object> updates = new HashMap<>();
        updates.put("summaryText", summary);
        updates.put("relevantLinks", links); // שדה הקישורים החדש
        updates.put("audioURL", url);
        updates.put("status", isPublic ? "ready" : "draft");
        updates.put("userID", userId);
        updates.put("title", lessonTitle);
        updates.put("lecturer", (teacherName != null && !teacherName.isEmpty()) ? teacherName : "Unknown");
        updates.put("timestamp", System.currentTimeMillis());

        FirebaseDatabase.getInstance().getReference(path).setValue(updates)
                .addOnSuccessListener(aVoid -> {
                    // הצגת התראה כשהסיכום מוכן
                    showFinishedNotification(lessonTitle);


                    Intent intent = new Intent("RECORDING_FINISHED");
                    intent.putExtra("summaryText", summary);
                    intent.putExtra("relevantLinks", links);
                    sendBroadcast(intent);
                    stopSelf();
                });
    }

    private void showFinishedNotification(String title) {
        String channelId = "finish_chan";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(channelId, "Lesson Finished", NotificationManager.IMPORTANCE_HIGH);
            if (manager != null) manager.createNotificationChannel(chan);
        }

        Intent intent = new Intent(this, RecordLesson.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("הסיכום מוכן! ✨")
                .setContentText("השיעור '" + title + "' סוכם בהצלחה")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        if (manager != null) manager.notify(2, notification);
    }

    private Notification getNotification(String text) {
        String channelId = "record_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(channelId, "Record", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(chan);
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Smart Lecture")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}