package com.example.smartlecture;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.text.util.Linkify;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Locale;

public class RecordLesson extends AppCompatActivity {

    private TextInputEditText etTeacherName, etLessonTitle;
    private TextView tvRecordingTime, tvLessonSummary, tvLocation;
    private MaterialButton btnStart, btnStop, btnAddSummary, btnBackHome;
    private SwitchMaterial swPublicAccess;

    private long startTime = 0;
    private Handler timerHandler = new Handler();
    private String currentEventId;

    private FusedLocationProviderClient fusedLocationClient;
    private String finalLocationName = "Location Off";

    // הרשאות מיקום
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getCurrentLocation();
                } else {
                    tvLocation.setText("📍 Permission Denied");
                }
            });

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("RECORDING_FINISHED".equals(intent.getAction())) {
                String summaryText = intent.getStringExtra("summaryText");
                String relevantLinks = intent.getStringExtra("relevantLinks");
                StringBuilder fullDisplay = new StringBuilder();
                fullDisplay.append(summaryText);
                if (relevantLinks != null && !relevantLinks.isEmpty()) {
                    fullDisplay.append("\n\n🔗 קישורים רלוונטיים:\n").append(relevantLinks);
                }
                tvLessonSummary.setText(fullDisplay.toString());
                Linkify.addLinks(tvLessonSummary, Linkify.WEB_URLS);
                tvRecordingTime.setText("✅ Done");
                btnStart.setEnabled(true);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_lesson);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
        setupListeners();
        checkLocationPermission();

        IntentFilter filter = new IntentFilter("RECORDING_FINISHED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
    }

    private void initViews() {
        etLessonTitle = findViewById(R.id.etLessonTitle);
        etTeacherName = findViewById(R.id.etTeacherName);
        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvLessonSummary = findViewById(R.id.tvLessonSummary);
        tvLocation = findViewById(R.id.tvLocation);
        btnStart = findViewById(R.id.btnStartRecording);
        btnStop = findViewById(R.id.btnStopRecording);
        btnAddSummary = findViewById(R.id.btnAddSummary);
        btnBackHome = findViewById(R.id.btnBackHome);
        swPublicAccess = findViewById(R.id.swPublicAccess);
        btnStop.setEnabled(false);
    }

    private void setupListeners() {
        // לחיצה על המיקום מאפשרת לשנות אותו ידנית בדיאלוג
        tvLocation.setOnClickListener(v -> showLocationEditDialog());

        btnStart.setOnClickListener(v -> {
            if (checkAudioPermission()) startRecordingProcess();
        });
        btnStop.setOnClickListener(v -> stopRecordingProcess());
        btnBackHome.setOnClickListener(v -> finish());
        btnAddSummary.setOnClickListener(v -> resetScreen());
    }

    // פונקציה חדשה: פתיחת דיאלוג לשינוי מיקום ידני
    private void showLocationEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Lesson Location");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(finalLocationName);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newLoc = input.getText().toString().trim();
            if (!newLoc.isEmpty()) {
                finalLocationName = newLoc;
                tvLocation.setText("📍 " + finalLocationName);
            }
        });
        builder.setNegativeButton("Auto-Detect", (dialog, which) -> getCurrentLocation());
        builder.show();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationName(location.getLatitude(), location.getLongitude());
            }
        });
    }

    private void updateLocationName(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                finalLocationName = addresses.get(0).getAddressLine(0);
                tvLocation.setText("📍 " + finalLocationName);
            }
        } catch (Exception e) {
            finalLocationName = lat + ", " + lon;
            tvLocation.setText("📍 " + finalLocationName);
        }
    }

    private void startRecordingProcess() {
        currentEventId = "event_" + System.currentTimeMillis();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction("START_RECORDING");
        intent.putExtra("EVENT_ID", currentEventId);
        intent.putExtra("USER_ID", uid);
        intent.putExtra("IS_PUBLIC", swPublicAccess.isChecked());
        intent.putExtra("TEACHER", etTeacherName.getText().toString());
        intent.putExtra("LESSON_TITLE", etLessonTitle.getText().toString());
        intent.putExtra("LOCATION", finalLocationName);

        ContextCompat.startForegroundService(this, intent);
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void stopRecordingProcess() {
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction("STOP_RECORDING");
        startService(intent);
        btnStop.setEnabled(false);
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void resetScreen() {
        etLessonTitle.setText("");
        etTeacherName.setText("");
        tvRecordingTime.setText("⏱ 00:00");
        tvLessonSummary.setText("Summary will appear here...");
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        checkLocationPermission();
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            tvRecordingTime.setText(String.format(Locale.getDefault(), "⏱ %02d:%02d", seconds / 60, seconds % 60));
            timerHandler.postDelayed(this, 500);
        }
    };

    private boolean checkAudioPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(statusReceiver); } catch (Exception e) {}
    }
}