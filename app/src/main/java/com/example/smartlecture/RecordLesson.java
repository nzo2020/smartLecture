package com.example.smartlecture;

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

    private TextInputEditText etTeacherName;
    private TextView tvRecordingTime, tvLessonSummary;
    private MaterialButton btnStart, btnStop, btnSave;
    private SwitchMaterial swPublic;
    private String eventID, outputFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_lesson);

        etTeacherName = findViewById(R.id.etTeacherName);
        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvLessonSummary = findViewById(R.id.tvLessonSummary);
        btnStart = findViewById(R.id.btnStartRecording);
        btnStop = findViewById(R.id.btnStopRecording);
        btnSave = findViewById(R.id.btnAddSummary);
        swPublic = findViewById(R.id.swPublicAccess);

        // יצירת נתיב זמני להקלטה
        eventID = "lecture_" + System.currentTimeMillis();
        outputFilePath = getExternalCacheDir().getAbsolutePath() + "/" + eventID + ".3gp";

        btnStart.setOnClickListener(v -> startRecordingProcess());
        btnStop.setOnClickListener(v -> stopRecordingProcess());

        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());
    }

    private void startRecordingProcess() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }

        Intent intent = new Intent(this, RecordingService.class);
        intent.putExtra("filePath", outputFilePath);
        intent.putExtra("eventID", eventID);
        intent.putExtra("teacher", etTeacherName.getText().toString());
        intent.putExtra("isPublic", swPublic.isChecked());

        ContextCompat.startForegroundService(this, intent);

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        tvRecordingTime.setText("⏱ מקליט...");
    }

    private void stopRecordingProcess() {
        stopService(new Intent(this, RecordingService.class));
        btnStop.setEnabled(false);
        btnStart.setEnabled(true);
        btnSave.setEnabled(true);
        tvRecordingTime.setText("⏱ ההקלטה נשלחה לעיבוד");
        tvLessonSummary.setText("הסיכום יופיע כאן ובהתראות ברגע ש-Gemini יסיים...");
    }
}