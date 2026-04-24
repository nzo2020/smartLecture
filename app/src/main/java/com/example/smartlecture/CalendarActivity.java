package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private WebView webViewCalendar;
    private MaterialButton btnAddEvent, btnBackHome;
    private final String CHANNEL_ID = "MyReminderChannel";

    private FusedLocationProviderClient fusedLocationClient;
    private EditText etCurrentDialogLocation;

    private final ActivityResultLauncher<Intent> autocompleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (etCurrentDialogLocation != null) {
                        etCurrentDialogLocation.setText(place.getAddress());
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        // אתחול Google Places
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.PLACES_API_KEY);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();
        requestNotificationPermission();
        initViews();
        setupWebView();

        btnAddEvent.setOnClickListener(v -> showAddEventDialog());
        btnBackHome.setOnClickListener(v -> finish());
    }

    private void initViews() {
        webViewCalendar = findViewById(R.id.webViewCalendar);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnBackHome = findViewById(R.id.btnBackHome);
    }

    private void setupWebView() {
        WebSettings webSettings = webViewCalendar.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webViewCalendar.setWebViewClient(new WebViewClient());
        webViewCalendar.loadUrl("https://calendar.google.com/calendar/u/0/r");
    }

    private void showAddEventDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.activity_dialog_add_event, null);
        EditText etName = dialogView.findViewById(R.id.etEventName);
        EditText etLocation = dialogView.findViewById(R.id.etEventLocation);
        EditText etDescription = dialogView.findViewById(R.id.etEventDescription);
        EditText etDate = dialogView.findViewById(R.id.etEventDate);
        EditText etTime = dialogView.findViewById(R.id.etEventTime);

        etCurrentDialogLocation = etLocation;
        etLocation.setFocusable(false);
        etLocation.setClickable(true);
        etLocation.setOnClickListener(v -> openPlacesSearch());

        // מילוי מיקום נוכחי אוטומטי
        setCurrentLocationInDialog(etLocation);

        final Calendar selectedCalendar = Calendar.getInstance();

        etDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, day) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, day);
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year));
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH));

            // חסימת בחירת תאריך עבר בלוח השנה
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
            datePicker.show();
        });

        etTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hour);
                selectedCalendar.set(Calendar.MINUTE, minute);
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show();
        });

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("שמור והוסף", (d, which) -> {
                    String title = etName.getText().toString().trim();
                    String location = etLocation.getText().toString().trim();
                    String desc = etDescription.getText().toString().trim();

                    if (title.isEmpty() || etDate.getText().toString().isEmpty() || etTime.getText().toString().isEmpty()) {
                        Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long finalTime = selectedCalendar.getTimeInMillis();

                    // בדיקה שהזמן לא עבר (למקרה שנבחר היום אבל שעה מוקדמת)
                    if (finalTime <= System.currentTimeMillis()) {
                        Toast.makeText(this, "Cannot set an event for a past time!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    checkDuplicateAndSave(title, location, desc, finalTime);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void checkDuplicateAndSave(String title, String location, String desc, long time) {
        if (refAuth.getCurrentUser() == null) return;
        String uid = refAuth.getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("reminders").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Task existingTask = ds.getValue(Task.class);
                            if (existingTask != null &&
                                    existingTask.getTitle().equalsIgnoreCase(title) &&
                                    existingTask.getRemindAt() == time) {
                                Toast.makeText(CalendarActivity.this, "This reminder already exists!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        // אם לא נמצאה כפילות - שומרים
                        String eventID = "task_" + System.currentTimeMillis();
                        saveToFirebase(eventID, title, location, time);

                        Task t = new Task(eventID, title, time, uid, location);
                        t.setRemindAt(time);
                        new ReminderManager(CalendarActivity.this, new ArrayList<>()).addTask(t);

                        sendToExternalCalendar(title, location, desc, time);
                        Toast.makeText(CalendarActivity.this, "Reminder Saved & Synced!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void saveToFirebase(String id, String title, String location, long time) {
        if (refAuth.getCurrentUser() == null) return;
        String uid = refAuth.getCurrentUser().getUid();

        Task newTask = new Task(id, title, time, uid, location);
        newTask.setRemindAt(time);

        FirebaseDatabase.getInstance().getReference("reminders")
                .child(uid)
                .child(id)
                .setValue(newTask);
    }

    private void setCurrentLocationInDialog(EditText locationField) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        locationField.setText(addresses.get(0).getAddressLine(0));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void openPlacesSearch() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("IL")
                .setTypeFilter(TypeFilter.ADDRESS)
                .build(this);
        autocompleteLauncher.launch(intent);
    }

    private void sendToExternalCalendar(String t, String l, String d, long start) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, t)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, l)
                .putExtra(CalendarContract.Events.DESCRIPTION, d)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 3600000); // ברירת מחדל שעה אחת
        startActivity(intent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}