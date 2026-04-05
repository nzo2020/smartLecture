package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class RecordLesson extends AppCompatActivity {

    private TextInputEditText etTeacherName, etLessonTitle; // הוספתי שדה לכותרת
    private TextView tvRecordingTime, tvLessonSummary;
    private MaterialButton btnStart, btnStop, btnSave;
    private SwitchMaterial swPublic;

    private String eventID, outputFilePath;
    private Lecture currentLecture; // המשתנה שיחזיק את האובייקט מהמחלקה שלך

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_lesson);

        initViews();

        btnStart.setOnClickListener(v -> startRecordingProcess());
        btnStop.setOnClickListener(v -> stopRecordingProcess());
        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());
    }

    private void initViews() {
        etTeacherName = findViewById(R.id.etTeacherName);
        etLessonTitle = findViewById(R.id.etReminderName); // נניח שזה ה-ID של שדה הכותרת
        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvLessonSummary = findViewById(R.id.tvLessonSummary);
        btnStart = findViewById(R.id.btnStartRecording);
        btnStop = findViewById(R.id.btnStopRecording);
        btnSave = findViewById(R.id.btnAddSummary);
        swPublic = findViewById(R.id.swPublicAccess);
    }

    private void startRecordingProcess() {
        // 1. בדיקת הרשאות
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }

        String title = etLessonTitle.getText().toString().trim();
        String teacher = etTeacherName.getText().toString().trim();

        if (title.isEmpty()) title = "הרצאה ללא שם";

        eventID = "lecture_" + System.currentTimeMillis();
        outputFilePath = getExternalCacheDir().getAbsolutePath() + "/" + eventID + ".3gp";
        String uid = refAuth.getCurrentUser().getUid();

        currentLecture = new Lecture(eventID, title, System.currentTimeMillis(), uid, teacher, swPublic.isChecked());

        currentLecture.startRecording();

        Intent intent = new Intent(this, RecordingService.class);
        intent.putExtra("filePath", outputFilePath);
        intent.putExtra("eventID", currentLecture.getEventID());
        intent.putExtra("teacher", currentLecture.getLecturer());
        intent.putExtra("title", currentLecture.getTitle());
        intent.putExtra("isPublic", currentLecture.isPub());

        ContextCompat.startForegroundService(this, intent);

        // 5. עדכון UI
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        tvRecordingTime.setText("⏱ מקליט: " + currentLecture.getTitle());
    }

    private void stopRecordingProcess() {
        stopService(new Intent(this, RecordingService.class));

        if (currentLecture != null) {
            currentLecture.setStatus("processing");
        }

        btnStop.setEnabled(false);
        btnStart.setEnabled(true);
        btnSave.setEnabled(true);
        tvRecordingTime.setText("⏱ ההקלטה נשלחה לעיבוד");
        tvLessonSummary.setText("הסיכום עבור " + currentLecture.getLecturer() + " יופיע בקרוב...");
    }
}