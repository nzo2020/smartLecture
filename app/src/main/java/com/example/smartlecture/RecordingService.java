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

/**
 *A Foreground Service responsible for recording audio lessons, uploading them to Firebase,
 * analyzing content with AI (Gemini), and managing smart calendar events.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class RecordingService extends Service {
    /** MediaRecorder instance for capturing audio */
    private MediaRecorder recorder; // רכיב המערכת לביצוע הקלטת קול
    /** Meta-data for the current recording session */
    private String eventId, userId, filePath, teacherName, lessonTitle, locationName;
    /** Flag determining if the recording should be shared publicly */
    private boolean isPublic;
    /** The start time of the recording in milliseconds */
    private long recordingStartTime;

    /**
     * Handles service commands based on Intent actions (Start, Stop, or Update).
     * @param intent The intent containing parameters and actions.
     * @param flags Additional data about the start request.
     * @param startId A unique integer representing this specific request to start.
     * @return Service sticky status.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();

        // זיהוי הפעולה הנדרשת מתוך ה-Intent
        if ("START_RECORDING".equals(action)) {
            setupParameters(intent); // חילוץ נתוני ההרצאה
            // הפעלת שירות קדמי (Foreground) עם התראה כדי שהמערכת לא תסגור אותו ברקע
            startForeground(1, getNotification("Recording lesson in progress..."));
            startRecording();
        } else if ("STOP_RECORDING".equals(action)) {
            stopRecording();
        }
        else if ("UPDATE_EVENTS_FROM_SUMMARY".equals(action)) {
            // פעולה ייעודית לעיבוד חוזר של סיכום לצורך שליפת אירועים חכמים
            String newSummary = intent.getStringExtra("NEW_SUMMARY");
            userId = intent.getStringExtra("USER_ID");
            if (newSummary != null) {
                processSmartEvents(newSummary);
            }
        }
        return START_STICKY; // מבטיח שהשירות ינסה לאתחל את עצמו אם הוא נסגר בגלל מחסור בזיכרון
    }

    /**
     * Extracts parameters from the starting intent and prepares the local file path.
     * @param intent The intent containing event data.
     */
    private void setupParameters(Intent intent) {
        // משיכת כל המידע שנשלח מה-Activity (הקלטה, משתמש, מיקום וכו')
        eventId = intent.getStringExtra("EVENT_ID");
        userId = intent.getStringExtra("USER_ID");
        isPublic = intent.getBooleanExtra("IS_PUBLIC", false);
        teacherName = intent.getStringExtra("TEACHER");
        lessonTitle = intent.getStringExtra("LESSON_TITLE");
        locationName = intent.getStringExtra("LOCATION");
        recordingStartTime = intent.getLongExtra("START_TIME", System.currentTimeMillis());
        // קביעת נתיב זמני לשמירת קובץ ה-mp4 במכשיר
        filePath = getExternalCacheDir().getAbsolutePath() + "/" + eventId + ".mp4";
    }

    /**
     * Configures and starts the MediaRecorder using the device microphone.
     */
    private void startRecording() {
        try {
            // הגדרת רכיב ההקלטה: מקור (מיקרופון), פורמט (MPEG_4) וקידוד (AAC)
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

    /**
     * Stops the recording, releases resources, and initiates the cloud upload process.
     */
    private void stopRecording() {
        if (recorder != null) {
            try {
                recorder.stop(); // הפסקת פעולת המיקרופון
            } catch (RuntimeException e) {
                Log.e("SmartLecture", "Stop failed");
            }
            recorder.release();
            recorder = null;

            // עדכון ההתראה שההקלטה הסתיימה וכעת מתבצע עיבוד
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(1, getNotification("Processing and analyzing lesson..."));
            uploadFileToStorage(); // מעבר לשלב הבא: העלאה לענן
        }
    }

    /**
     * Uploads the recorded MP4 file to Firebase Cloud Storage.
     */
    private void uploadFileToStorage() {
        // העלאת קובץ ההקלטה ל-Firebase Storage תחת תיקיית recordings
        File file = new File(filePath);
        StorageReference stRef = FirebaseStorage.getInstance().getReference()
                .child("recordings").child(userId).child(eventId + ".mp4");

        stRef.putFile(android.net.Uri.fromFile(file))
                .addOnSuccessListener(task -> stRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> analyzeWithGemini(file, uri.toString())))
                .addOnFailureListener(e -> stopSelf());
    }

    /**
     * Sends the recorded audio file to the Gemini AI model for summarization
     * and extraction of smart calendar events.
     * @param file The local recording file.
     * @param audioUrl The URL of the uploaded audio file in Firebase.
     */
    private void analyzeWithGemini(File file, String audioUrl) {
        try {
            // המרת קובץ ההקלטה למערך בייטים (Bytes) עבור ה-AI
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

            // שליחת הקובץ והפקודה ל-API של Gemini
            GeminiManager.getInstance().sendTextWithFilePrompt(prompt, bytes, "audio/mp4", new GeminiCallback() {
                @Override
                public void onSuccess(String result) {
                    // הפרדת התוצאה לסיכום ולקישורים
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

    /**
     * Determines the appropriate Firebase Realtime Database path and saves the lesson data.
     * If public, it performs a check to see if an existing summary from another user is higher quality.
     * @param summary The analyzed summary text.
     * @param links Related educational links.
     * @param url The audio file URL.
     */
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

                            // בדיקה אם ההרצאה קיימת (סטייה של עד 5 דקות ומיקום זהה)
                            if (exStart != null && locationName != null && locationName.equals(exLoc) &&
                                    Math.abs(recordingStartTime - exStart) < 300000) {

                                String exSum = lectureNode.child("summaryText").getValue(String.class);
                                if (exSum != null && exSum.length() > bestSummary.length()) {
                                    bestSummary = exSum; // לוקחים את הסיכום האיכותי יותר
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

    /**
     * Saves lecture data using a transaction to ensure data integrity and updates user statistics.
     * @param summaryToSave Final summary text.
     * @param linksToSave Final links list.
     * @param url Audio URL.
     */
    private void executeFirebaseSaveWithTransaction(final String summaryToSave, final String linksToSave, String url) {
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (userName == null || userName.isEmpty()) userName = "Student";

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        // קביעת נתיב השמירה ב-Database לפי סוג ההרצאה (ציבורי/פרטי)
        String dbPath = isPublic ? "Lectures/pub_true/" + userName + "/" + eventId
                : "Lectures/pub_false/" + userId + "/" + userName + "/" + eventId;

        // שימוש ב-Transaction כדי להבטיח שהנתונים נכתבים בצורה בטוחה
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
                    // 1. עיבוד "אירועים חכמים" מתוך הסיכום (הפיכת טקסט לתזכורות ביומן)
                    processSmartEvents(summaryToSave);

                    // 2. עדכון מונה ההרצאות הכולל של המשתמש (לצורך הסטטיסטיקה ב-Dashboard)
                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(userId);

                    userRef.child("totalLectures").setValue(ServerValue.increment(1));

                    // 3. שליחת שידור (Broadcast) ל-Activity כדי לעדכן שהכל הסתיים
                    sendBroadcast(new Intent("RECORDING_FINISHED")
                            .putExtra("EVENT_ID", eventId)
                            .putExtra("summaryText", summaryToSave)
                            .putExtra("relevantLinks", linksToSave));

                    // הצגת התראה סופית שהסיכום מוכן
                    NotificationHelper.showSummaryReadyNotification(getApplicationContext(), teacherName);
                    stopSelf(); // סגירת השירות
                }
            }
        });
    }


    /**
     * Parses the summary text to find the special SMART_EVENTS_LIST header
     * and extracts individual event lines.
     * @param fullText The entire summary text analyzed by the AI.
     */
    private void processSmartEvents(String fullText) {
        if (!fullText.contains("SMART_EVENTS_LIST:")) return;
        try {
            String listPart = fullText.split("SMART_EVENTS_LIST:")[1].split("Relevant Links")[0].trim();
            String[] lines = listPart.split("\n");
            for (String line : lines) {
                if (line.contains("|") && line.contains("[") && line.contains("]")) {
                    parseAndSaveSingleEvent(line); // ניתוח כל שורה בנפרד
                }
            }
        } catch (Exception e) { Log.e("SmartLecture", "Error processing events", e); }
    }

    /**
     * Parses a single line representing a smart event (e.g., "[Exam | 12/12/2026 | 10:00 | Lab]")
     * and schedules a reminder if the date is in the future.
     * @param eventLine The raw text line from the AI summary.
     */
    private void parseAndSaveSingleEvent(String eventLine) {
        try {
            // ניקוי הסוגריים והפרדה לפי הסימן "|"
            String cleanLine = eventLine.replace("[", "").replace("]", "");
            String[] details = cleanLine.split("\\|");
            if (details.length >= 3) {
                String title = details[0].trim();
                String dateStr = details[1].trim();
                String timeStr = details[2].trim();
                String loc = (details.length >= 4) ? details[3].trim() : "No updated location";

                // המרת הטקסט של התאריך והשעה לאובייקט מסוג Date
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                java.util.Date parsedDate = sdf.parse(dateStr + " " + timeStr);

                // שמירה רק אם מדובר באירוע עתידי
                if (parsedDate != null && parsedDate.after(new java.util.Date())) {
                    saveSingleReminderToFirebase(title, parsedDate.getTime(), loc);
                }
            }
        } catch (Exception e) { Log.e("SmartLecture", "Parse failed", e); }
    }

    /**
     * Saves an extracted smart event to Firebase and schedules a system alarm via ReminderManager.
     * @param title Event title.
     * @param timestamp Event time.
     * @param loc Event location.
     */
    private void saveSingleReminderToFirebase(String title, long timestamp, String loc) {
        // יצירת ID ייחודי לכל תזכורת
        String uniqueId = "task_" + Math.abs((title + timestamp).hashCode());
        DatabaseReference remRef = FirebaseDatabase.getInstance().getReference("reminders").child(userId).child(uniqueId);

        Map<String, Object> rData = new HashMap<>();
        rData.put("eventID", uniqueId);
        rData.put("title", title);
        rData.put("remindAt", timestamp);
        rData.put("location", loc);

        remRef.setValue(rData).addOnSuccessListener(aVoid -> {
            // שליחת התזכורת ל-ReminderManager כדי לקבוע התראה פיזית בטלפון (AlarmManager)
            Task task = new Task(uniqueId, title, timestamp, userId, loc);
            new ReminderManager(getApplicationContext(), new ArrayList<>()).addTask(task);
        });
    }

    /**
     * Builds the required Foreground notification for the service.
     * @param text The current status text to display.
     * @return A notification object.
     */
    private Notification getNotification(String text) {
        // בניית התראה לשירות (חובה עבור Foreground Service)
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

    /**
     * Utility function providing a default date (one week from now) for AI events.
     * @return Formatted date string.
     */
    private String getOneWeekFromNow() {
        // פונקציית עזר למקרה ש-Gemini לא זיהה תאריך - קובעת שבוע מהיום כברירת מחדל
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_YEAR, 7);
        return new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.getTime());
    }

    @Override public IBinder onBind(Intent i) { return null; }
}