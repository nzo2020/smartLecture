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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.nio.file.Files;
import java.util.Locale; // הוסף את השורה הזו
import java.util.ArrayList;
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
            // Foreground notification during recording
            startForeground(1, getNotification("Recording lesson in progress..."));
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
        locationName = intent.getStringExtra("LOCATION");
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
        } catch (Exception e) {
            Log.e("SmartLecture", "Recording failed", e);
        }
    }

    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException e) {
                Log.e("SmartLecture", "Stop failed (recording might be too short)", e);
            }
            recorder.release();
            recorder = null;
            // Update notification to show processing
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(1, getNotification("Processing and analyzing lesson..."));
            uploadFileToStorage();
        }
    }

    private void uploadFileToStorage() {
        File file = new File(filePath);
        StorageReference stRef = FirebaseStorage.getInstance().getReference()
                .child("recordings").child(userId).child(eventId + ".mp4");

        stRef.putFile(android.net.Uri.fromFile(file))
                .addOnSuccessListener(task -> stRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> analyzeWithGemini(file, uri.toString())))
                .addOnFailureListener(e -> stopSelf());
    }

    private void analyzeWithGemini(File file, String audioUrl) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String todayDate = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new java.util.Date());
            String defaultFutureDate = getOneWeekFromNow();

            String prompt = "Summarize this lesson professionally. " +
                    "Task: Identify ALL future deadlines, exams, or assignments mentioned. " +
                    "For each one, create a concise, smart title. " +
                    "CRITICAL RULES FOR MISSING INFO: " +
                    "1. If no specific date is mentioned, use: " + defaultFutureDate + ". " +
                    "2. If no specific time is mentioned, use 09:00. " +
                    "3. If no specific location is mentioned, use exactly: 'No updated location'. " +
                    "Today's date is: " + todayDate + ". " +
                    "List them under 'SMART_EVENTS_LIST:' " +
                    "Format: [Smart Title | DD/MM/YYYY | HH:mm | Location] " +
                    "Example 1 (With Location): [History Exam | 12/05/2026 | 10:30 | Room 201]. " + // הכותרת היא היסטוריה, המיקום הוא חדר 201
                    "Example 2 (No Location): [Math Assignment | 19/05/2026 | 09:00 | No updated location]. " + // הכותרת היא מתמטיקה, המיקום מעודכן שאין
                    "Finally, add 'Relevant Links:' and provide 3 links.";
            GeminiManager.getInstance().sendTextWithFilePrompt(prompt, bytes, "audio/mp4", new GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    String summary = result, links = "";

                    if (result.contains("Relevant Links")) {
                        String[] parts = result.split("Relevant Links:?");
                        summary = parts[0].trim();
                        if (parts.length > 1) links = parts[1].trim();
                    }

                    // קריאה לאלגוריתם המעודכן שמטפל במספר אירועים
                    processSmartEvents(result);

                    saveDataToFirebase(summary, links, audioUrl);
                }
                @Override public void onFailure(Throwable e) {
                    saveDataToFirebase("AI analysis failed", "", audioUrl);
                }
            });
        } catch (Exception e) {
            stopSelf();
        }
    }

    private String getOneWeekFromNow() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, 7);
        return new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
    }

    private void saveDataToFirebase(String summary, String links, String url) {
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (userName == null || userName.isEmpty()) userName = "Student";

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        if (isPublic) {
            // שלב 1: חיפוש רוחבי כדי למצוא אם יש כבר סיכום קיים במיקום ובזמן הזה
            dbRef.child("Lectures/pub_true").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    String bestSummaryFound = summary;
                    String bestLinksFound = links;

                    for (com.google.firebase.database.DataSnapshot userNode : snapshot.getChildren()) {
                        for (com.google.firebase.database.DataSnapshot lectureNode : userNode.getChildren()) {
                            String existingLoc = lectureNode.child("location").getValue(String.class);
                            Long existingTime = lectureNode.child("timestamp").getValue(Long.class);

                            // בדיקת מיקום וזמן (טווח של 5 דקות)
                            if (locationName != null && locationName.equals(existingLoc) &&
                                    existingTime != null && Math.abs(System.currentTimeMillis() - existingTime) < 300000) {

                                String existingSum = lectureNode.child("summaryText").getValue(String.class);
                                if (existingSum != null && existingSum.length() > bestSummaryFound.length()) {
                                    bestSummaryFound = existingSum;
                                    bestLinksFound = lectureNode.child("relevantLinks").getValue(String.class);
                                }
                            }
                        }
                    }
                    // שלב 2: שמירה באמצעות טרנזקציה כדי למנוע התנגשות במילישנייה האחרונה
                    executeFirebaseSaveWithTransaction(bestSummaryFound, bestLinksFound, url);
                }

                @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    executeFirebaseSaveWithTransaction(summary, links, url);
                }
            });
        } else {
            executeFirebaseSaveWithTransaction(summary, links, url);
        }
    }

    private void executeFirebaseSaveWithTransaction(final String summaryToSave, final String linksToSave, String url) {
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (userName == null || userName.isEmpty()) userName = "Student";

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        String dbPath = isPublic ? "Lectures/pub_true/" + userName + "/" + eventId
                : "Lectures/pub_false/" + userId + "/" + userName + "/" + eventId;

        dbRef.child(dbPath).runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData mutableData) {
                // יצירת מפת הנתונים לשמירה
                Map<String, Object> data = new HashMap<>();
                data.put("title", lessonTitle);
                data.put("lecturer", teacherName);
                data.put("location", locationName);
                data.put("summaryText", summaryToSave);
                data.put("relevantLinks", linksToSave);
                data.put("audioURL", url);
                data.put("timestamp", ServerValue.TIMESTAMP);
                data.put("userID", userId);
                data.put("status", isPublic ? "ready" : "draft");

                mutableData.setValue(data);
                return com.google.firebase.database.Transaction.success(mutableData);
            }

            @Override
            public void onComplete(com.google.firebase.database.DatabaseError error, boolean committed, com.google.firebase.database.DataSnapshot snapshot) {
                if (committed) {
                    sendBroadcast(new Intent("RECORDING_FINISHED")
                            .putExtra("summaryText", summaryToSave)
                            .putExtra("relevantLinks", linksToSave));
                    NotificationHelper.showSummaryReadyNotification(getApplicationContext(), teacherName);
                    stopSelf();
                }
            }
        });
    }

    private Notification getNotification(String text) {
        String channelId = "record_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(channelId) == null) {
                nm.createNotificationChannel(new NotificationChannel(channelId, "Recording Service", NotificationManager.IMPORTANCE_LOW));
            }
        }
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Smart Lecture")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    private void processSmartEvents(String fullText) {
        if (!fullText.contains("SMART_EVENTS_LIST:")) return;

        try {
            // חיתוך הטקסט כך שנקבל רק את רשימת האירועים
            String listPart = fullText.split("SMART_EVENTS_LIST:")[1].split("Relevant Links")[0].trim();
            String[] lines = listPart.split("\n");

            for (String line : lines) {
                if (line.contains("|") && line.contains("[") && line.contains("]")) {
                    parseAndSaveSingleEvent(line);
                }
            }
        } catch (Exception e) {
            Log.e("SmartLecture", "Error processing events list", e);
        }
    }

    private void parseAndSaveSingleEvent(String eventLine) {
        try {
            String cleanLine = eventLine.replace("[", "").replace("]", "");
            String[] details = cleanLine.split("\\|");

            if (details.length >= 3) {
                String smartTitle = details[0].trim(); // כאן נשמר "History Exam"
                String dateStr = details[1].trim();
                String timeStr = details[2].trim();

                // בדיקה אם יש מיקום (חלק רביעי), אם לא - שמים את הודעת המחדל
                String aiLocation = (details.length >= 4) ? details[3].trim() : "No updated location";

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                java.util.Date parsedDate = sdf.parse(dateStr + " " + timeStr);

                if (parsedDate != null && parsedDate.after(new java.util.Date())) {
                    String subEventId = "task_" + System.currentTimeMillis() + "_" + Math.abs(smartTitle.hashCode());

                    // שמירה ל-Firebase: הכותרת תהיה "History Exam" והמיקום יהיה "No updated location"
                    saveSingleReminderToFirebase(subEventId, smartTitle, parsedDate.getTime(), aiLocation);
                }
            }
        } catch (Exception e) {
            Log.e("SmartLecture", "Failed to parse: " + eventLine, e);
        }
    }

    private void saveSingleReminderToFirebase(String id, String title, long timestamp, String specificLocation) {
        // התייחסות לנתיב ב-Firebase: reminders -> userId -> eventId
        DatabaseReference remRef = FirebaseDatabase.getInstance().getReference("reminders")
                .child(userId)
                .child(id);

        Map<String, Object> reminderData = new HashMap<>();
        reminderData.put("eventID", id);
        reminderData.put("title", title);
        reminderData.put("remindAt", timestamp);

        // כאן אנחנו שומרים את המיקום הספציפי (או את הכתובת שנאמרה, או "No updated location")
        reminderData.put("location", specificLocation);

        remRef.setValue(reminderData).addOnSuccessListener(aVoid -> {
            // יצירת אובייקט Task עם המיקום המעודכן לצורך הפעלת ההתראה בטלפון
            Task task = new Task(id, title, timestamp, userId, specificLocation);

            // שליחת המשימה לניהול ההתראות במערכת
            new ReminderManager(getApplicationContext(), new ArrayList<>()).addTask(task);

            Log.d("SmartLecture", "Smart Event Saved & Scheduled: " + title + " at " + specificLocation);
        }).addOnFailureListener(e -> {
            Log.e("SmartLecture", "Failed to save reminder to Firebase", e);
        });
    }

    @Override public IBinder onBind(Intent i) { return null; }
}