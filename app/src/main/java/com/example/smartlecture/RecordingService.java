package com.example.smartlecture;

import android.Manifest;
import android.app.*;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.*;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import com.example.smartlecture.Gemini.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class RecordingService extends Service {
    private MediaRecorder recorder;
    private String eventId, userId, filePath, teacherName, lessonTitle, locationName;
    private boolean isPublic;
    private long recordingStartTime;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();

        if ("START_RECORDING".equals(action)) {
            setupParameters(intent);
            startForeground(1, getNotification("Recording lesson in progress..."));
            startRecording();
        } else if ("STOP_RECORDING".equals(action)) {
            stopRecording();
        }
        // --- הוסף את החלק הזה ---
        else if ("UPDATE_EVENTS_FROM_SUMMARY".equals(action)) {
            String newSummary = intent.getStringExtra("NEW_SUMMARY");
            userId = intent.getStringExtra("USER_ID"); // נדרש לשמירת התזכורות
            if (newSummary != null) {
                processSmartEvents(newSummary);
            }
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
        // מקבלים את זמן תחילת ההקלטה המדויק מה-Activity לצורך סנכרון
        recordingStartTime = intent.getLongExtra("START_TIME", System.currentTimeMillis());
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
                Log.e("SmartLecture", "Stop failed");
            }
            recorder.release();
            recorder = null;

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
            String defaultDate = getOneWeekFromNow();

            String prompt = "Summarize this lesson professionally and concisely. \n" +
                    "Today's date is: " + todayDate + ". \n\n" +
                    "Task 1: Provide a clear and organized summary of the main topics discussed.\n\n" +
                    "Task 2: Identify ALL future deadlines, exams, or assignments mentioned in the audio. \n" +
                    "CRITICAL: List these events under the header 'SMART_EVENTS_LIST:' using the following exact format:\n" +
                    "[Event Title | DD/MM/YYYY | HH:mm | Location]\n\n" +
                    "Guidelines for Task 2:\n" +
                    "- If no date is mentioned, use " + defaultDate + ".\n" +
                    "- If no time is mentioned, use 09:00.\n" +
                    "- If no location is mentioned, use 'Classroom'.\n" +
                    "- Example: [Math Exam | 15/05/2026 | 10:00 | Hall A]\n\n" +
                    "Task 3: Finally, add a section called 'Relevant Links:' and provide 3 educational links related to the lesson's topic.";

            GeminiManager.getInstance().sendTextWithFilePrompt(prompt, bytes, "audio/mp4", new GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    String summary = result, links = "";
                    if (result.contains("Relevant Links")) {
                        String[] parts = result.split("Relevant Links:?");
                        summary = parts[0].trim();
                        if (parts.length > 1) links = parts[1].trim();
                    }
                    saveDataToFirebase(summary, links, audioUrl);
                }
                @Override public void onFailure(Throwable e) {
                    saveDataToFirebase("AI analysis failed", "", audioUrl);
                }
            });
        } catch (Exception e) { stopSelf(); }
    }

    private void saveDataToFirebase(String summary, String links, String url) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        if (isPublic) {
            dbRef.child("Lectures/pub_true").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    String bestSummary = summary;
                    String bestLinks = links;

                    for (DataSnapshot userNode : snapshot.getChildren()) {
                        for (DataSnapshot lectureNode : userNode.getChildren()) {
                            String exLoc = lectureNode.child("location").getValue(String.class);
                            Long exStart = lectureNode.child("recordingStartTime").getValue(Long.class);

                            if (exStart != null && locationName != null && locationName.equals(exLoc) &&
                                    Math.abs(recordingStartTime - exStart) < 300000) {

                                String exSum = lectureNode.child("summaryText").getValue(String.class);
                                if (exSum != null && exSum.length() > bestSummary.length()) {
                                    bestSummary = exSum;
                                    bestLinks = lectureNode.child("relevantLinks").getValue(String.class);
                                }
                            }
                        }
                    }
                    executeFirebaseSaveWithTransaction(bestSummary, bestLinks, url);
                }
                @Override public void onCancelled(DatabaseError error) { executeFirebaseSaveWithTransaction(summary, links, url); }
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

        dbRef.child(dbPath).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Map<String, Object> data = new HashMap<>();
                data.put("title", lessonTitle);
                data.put("lecturer", teacherName);
                data.put("location", locationName);
                data.put("summaryText", summaryToSave);
                data.put("relevantLinks", linksToSave);
                data.put("audioURL", url);
                data.put("timestamp", ServerValue.TIMESTAMP);
                data.put("recordingStartTime", recordingStartTime);
                data.put("userID", userId);
                data.put("status", isPublic ? "ready" : "draft");

                mutableData.setValue(data);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (committed) {
                    // 1. קודם כל מעבדים את האירועים החכמים (מה שכבר יש לך)
                    processSmartEvents(summaryToSave);

                    // 2. עדכון המונה בנתיב המדויק לפי המבנה שלך
                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("users") // שימי לב שזה users באותיות קטנות לפי המבנה שלך
                            .child(userId);

                    userRef.child("totalLectures").setValue(ServerValue.increment(1));

                    // 3. שליחת ה-Broadcast וסיום ה-Service
                    sendBroadcast(new Intent("RECORDING_FINISHED")
                            .putExtra("EVENT_ID", eventId)
                            .putExtra("summaryText", summaryToSave)
                            .putExtra("relevantLinks", linksToSave));

                    NotificationHelper.showSummaryReadyNotification(getApplicationContext(), teacherName);
                    stopSelf();
                }
            }
        });
    }

    private void processSmartEvents(String fullText) {
        if (!fullText.contains("SMART_EVENTS_LIST:")) return;
        try {
            String listPart = fullText.split("SMART_EVENTS_LIST:")[1].split("Relevant Links")[0].trim();
            String[] lines = listPart.split("\n");
            for (String line : lines) {
                if (line.contains("|") && line.contains("[") && line.contains("]")) {
                    parseAndSaveSingleEvent(line);
                }
            }
        } catch (Exception e) { Log.e("SmartLecture", "Error processing events", e); }
    }

    private void parseAndSaveSingleEvent(String eventLine) {
        try {
            String cleanLine = eventLine.replace("[", "").replace("]", "");
            String[] details = cleanLine.split("\\|");
            if (details.length >= 3) {
                String title = details[0].trim();
                String dateStr = details[1].trim();
                String timeStr = details[2].trim();
                String loc = (details.length >= 4) ? details[3].trim() : "No updated location";

                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                java.util.Date parsedDate = sdf.parse(dateStr + " " + timeStr);

                if (parsedDate != null && parsedDate.after(new java.util.Date())) {
                    saveSingleReminderToFirebase(title, parsedDate.getTime(), loc);
                }
            }
        } catch (Exception e) { Log.e("SmartLecture", "Parse failed", e); }
    }

    private void saveSingleReminderToFirebase(String title, long timestamp, String loc) {
        String uniqueId = "task_" + Math.abs((title + timestamp).hashCode());
        DatabaseReference remRef = FirebaseDatabase.getInstance().getReference("reminders").child(userId).child(uniqueId);

        Map<String, Object> rData = new HashMap<>();
        rData.put("eventID", uniqueId);
        rData.put("title", title);
        rData.put("remindAt", timestamp);
        rData.put("location", loc);

        remRef.setValue(rData).addOnSuccessListener(aVoid -> {
            // 1. התראה לנייד
            Task task = new Task(uniqueId, title, timestamp, userId, loc);
            new ReminderManager(getApplicationContext(), new ArrayList<>()).addTask(task);

            // 2. גוגל קלנדר
            addEventToGoogleCalendar(title, timestamp, loc);
        });
    }

    private void addEventToGoogleCalendar(String title, long startMillis, String loc) {
        try {
            ContentResolver cr = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, startMillis);
            values.put(CalendarContract.Events.DTEND, startMillis + 3600000); // שעה אחת
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.EVENT_LOCATION, loc);
            values.put(CalendarContract.Events.CALENDAR_ID, 1); // בד"כ יומן ברירת המחדל
            values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

            // בדיקת הרשאות לפני הכתיבה
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                cr.insert(CalendarContract.Events.CONTENT_URI, values);
                Log.d("Calendar", "Event added automatically: " + title);
            }
        } catch (Exception e) {
            Log.e("Calendar", "Failed to add event automatically", e);
        }
    }

    private Notification getNotification(String text) {
        String cid = "record_chan";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(cid) == null) {
                nm.createNotificationChannel(new NotificationChannel(cid, "Recording", NotificationManager.IMPORTANCE_LOW));
            }
        }
        return new NotificationCompat.Builder(this, cid)
                .setContentTitle("Smart Lecture").setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build();
    }

    private String getOneWeekFromNow() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, 7);
        return new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
    }

    @Override public IBinder onBind(Intent i) { return null; }
}