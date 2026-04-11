package com.example.smartlecture;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class RecordLesson extends AppCompatActivity {

    private TextInputEditText etTeacherName, etLessonTitle; // נוסף etLessonTitle
    private TextView tvRecordingTime, tvLessonSummary;
    private MaterialButton btnStart, btnStop, btnAddSummary, btnBackHome;
    private SwitchMaterial swPublicAccess;

    private long startTime = 0;
    private Handler timerHandler = new Handler();
    private String currentEventId;

    // Receiver שמאזין ל-Broadcast מה-Service כשהסיכום מוכן
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("RECORDING_FINISHED".equals(intent.getAction())) {
                String summary = intent.getStringExtra("summary");
                tvLessonSummary.setText(summary);
                tvRecordingTime.setText("✅ Done");
                Toast.makeText(context, "Summary generated!", Toast.LENGTH_SHORT).show();
                btnStart.setEnabled(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_lesson);

        initViews();
        setupListeners();

        // רישום ה-Receiver
        IntentFilter filter = new IntentFilter("RECORDING_FINISHED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
    }

    private void initViews() {
        etLessonTitle = findViewById(R.id.etLessonTitle); // קישור לשדה הכותרת החדש
        etTeacherName = findViewById(R.id.etTeacherName);
        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvLessonSummary = findViewById(R.id.tvLessonSummary);
        btnStart = findViewById(R.id.btnStartRecording);
        btnStop = findViewById(R.id.btnStopRecording);
        btnAddSummary = findViewById(R.id.btnAddSummary);
        btnBackHome = findViewById(R.id.btnBackHome);
        swPublicAccess = findViewById(R.id.swPublicAccess);
        btnStop.setEnabled(false);
    }

    private void setupListeners() {
        btnStart.setOnClickListener(v -> {
            if (checkPermissions()) startRecordingProcess();
        });

        btnStop.setOnClickListener(v -> stopRecordingProcess());
        btnBackHome.setOnClickListener(v -> finish());
        btnAddSummary.setOnClickListener(v -> resetScreen());
    }

    private void startRecordingProcess() {
        currentEventId = "event_" + System.currentTimeMillis();

        // שליפת הנתונים מהשדות
        String teacher = etTeacherName.getText().toString().trim();
        String lessonTitle = etLessonTitle.getText().toString().trim();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // בדיקה בסיסית לכותרת
        if (lessonTitle.isEmpty()) {
            lessonTitle = "שיעור ללא כותרת";
        }

        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction("START_RECORDING");
        intent.putExtra("EVENT_ID", currentEventId);
        intent.putExtra("USER_ID", uid);
        intent.putExtra("IS_PUBLIC", swPublicAccess.isChecked());
        intent.putExtra("TEACHER", teacher.isEmpty() ? "Unknown" : teacher);
        intent.putExtra("LESSON_TITLE", lessonTitle); // שליחת הכותרת ל-Service

        ContextCompat.startForegroundService(this, intent);

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        tvLessonSummary.setText("Recording...");
    }

    private void stopRecordingProcess() {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction("STOP_RECORDING");
        startService(intent);

        btnStop.setEnabled(false);
        timerHandler.removeCallbacks(timerRunnable);
        tvLessonSummary.setText("Analyzing with Gemini AI...");
    }

    private void resetScreen() {
        etLessonTitle.setText(""); // איפוס כותרת
        etTeacherName.setText(""); // איפוס שם מורה
        tvRecordingTime.setText("⏱ 00:00");
        tvLessonSummary.setText("Summary will appear here...");
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            tvRecordingTime.setText(String.format(Locale.getDefault(), "⏱ %02d:%02d", seconds / 60, seconds % 60));
            timerHandler.postDelayed(this, 500);
        }
    };

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(statusReceiver);
        } catch (Exception e) {
            // Receiver already unregistered
        }
    }
}