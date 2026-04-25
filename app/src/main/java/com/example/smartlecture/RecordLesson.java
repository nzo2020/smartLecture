package com.example.smartlecture;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import android.text.util.Linkify;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.*;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.*;

public class RecordLesson extends AppCompatActivity {

    private TextInputEditText etTeacherName, etLessonTitle;
    private TextView tvRecordingTime, tvLessonSummary, tvLocation;
    private MaterialButton btnStart, btnStop, btnAddSummary, btnBackHome;
    private SwitchMaterial swPublicAccess;

    private long startTime = 0;
    private Handler timerHandler = new Handler();
    private FusedLocationProviderClient fusedLocationClient;
    private String finalLocationName = "Unknown Location";

    private ValueEventListener sharedSummaryListener;
    private DatabaseReference currentLectureRef;
    private String lastProcessedSummary = "";
    // Google Places Autocomplete Launcher
    private final ActivityResultLauncher<Intent> autocompleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    finalLocationName = place.getAddress();
                    tvLocation.setText("📍 " + finalLocationName);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_lesson);

        // Initialize Google Places using your API Key
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.PLACES_API_KEY);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
        setupListeners();
        checkLocationPermission();

        // Register receiver for when recording and AI analysis are finished
        registerReceiver(statusReceiver, new IntentFilter("RECORDING_FINISHED"),
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Context.RECEIVER_EXPORTED : 0);
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
        // Clicking location text opens Google Places search
        tvLocation.setOnClickListener(v -> {
            List<Place.Field> fields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS);
            Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                    .setCountry("IL").build(this);
            autocompleteLauncher.launch(intent);
        });

        btnStart.setOnClickListener(v -> {
            if (checkPermissions()) startRecordingProcess();
        });

        btnStop.setOnClickListener(v -> stopRecordingProcess());
        btnBackHome.setOnClickListener(v -> finish());
        btnAddSummary.setOnClickListener(v -> resetUI());
    }

    private void startRecordingProcess() {
        String eventId = "event_" + System.currentTimeMillis();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction("START_RECORDING");
        intent.putExtra("EVENT_ID", eventId);
        intent.putExtra("USER_ID", uid);
        intent.putExtra("IS_PUBLIC", swPublicAccess.isChecked());
        intent.putExtra("TEACHER", etTeacherName.getText().toString());
        intent.putExtra("LESSON_TITLE", etLessonTitle.getText().toString());
        intent.putExtra("LOCATION", finalLocationName);
        intent.putExtra("START_TIME", System.currentTimeMillis()); // <--- הוסף את זה

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

    private final Runnable timerRunnable = new Runnable() {
        @Override public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            tvRecordingTime.setText(String.format(Locale.getDefault(), "⏱ %02d:%02d", seconds / 60, seconds % 60));
            timerHandler.postDelayed(this, 500);
        }
    };

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        finalLocationName = addresses.get(0).getAddressLine(0);
                        tvLocation.setText("📍 " + finalLocationName);
                    }
                } catch (Exception e) { finalLocationName = "Current Location"; }
            }
        });
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200);
            return false;
        }
        return true;
    }

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("RECORDING_FINISHED".equals(intent.getAction())) {
                String eventId = intent.getStringExtra("EVENT_ID");
                String summary = intent.getStringExtra("summaryText");
                String links = intent.getStringExtra("relevantLinks");

                tvLessonSummary.setText(summary + "\n\n🔗 Links:\n" + links);
                Linkify.addLinks(tvLessonSummary, Linkify.WEB_URLS);
                btnStart.setEnabled(true);

                if (swPublicAccess.isChecked() && eventId != null) {
                    startListeningForSharedUpdates(eventId);
                }
            }
        }
    };

    private void resetUI() {
        etLessonTitle.setText("");
        etTeacherName.setText("");
        tvLessonSummary.setText("Summary will appear here...");
        tvRecordingTime.setText("⏱ 00:00");
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private void startListeningForSharedUpdates(String eventId) {
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (userName == null) userName = "Student";

        // נתיב ההרצאה ב-Database
        currentLectureRef = FirebaseDatabase.getInstance().getReference()
                .child("Lectures/pub_true").child(userName).child(eventId);

        sharedSummaryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String summary = snapshot.child("summaryText").getValue(String.class);
                    String links = snapshot.child("relevantLinks").getValue(String.class);

                    // אם הגיע סיכום חדש שהוא שונה ממה שיש לנו כרגע
                    if (summary != null && !summary.equals(lastProcessedSummary)) {
                        tvLessonSummary.setText(summary + "\n\n🔗 Links:\n" + links);
                        Linkify.addLinks(tvLessonSummary, Linkify.WEB_URLS);

                        // עדכון האירועים ביומן לפי הסיכום החדש
                        Intent intent = new Intent(RecordLesson.this, RecordingService.class);
                        intent.setAction("UPDATE_EVENTS_FROM_SUMMARY");
                        intent.putExtra("NEW_SUMMARY", summary);
                        intent.putExtra("USER_ID", FirebaseAuth.getInstance().getCurrentUser().getUid());
                        startService(intent);

                        lastProcessedSummary = summary;
                        Toast.makeText(RecordLesson.this, "Better summary synced from peers!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        };
        currentLectureRef.addValueEventListener(sharedSummaryListener);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(statusReceiver); } catch (Exception e) {}

        if (currentLectureRef != null && sharedSummaryListener != null) {
            currentLectureRef.removeEventListener(sharedSummaryListener);
        }
    }
}