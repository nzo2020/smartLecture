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

    // הגדרת רכיבי ממשק המשתמש (UI) ומשתני מחלקה
    private WebView webViewCalendar;
    private MaterialButton btnAddEvent, btnBackHome;
    private final String CHANNEL_ID = "MyReminderChannel"; // מזהה ייחודי עבור ערוץ ההתראות (נדרש מ-Android 8.0+)

    private FusedLocationProviderClient fusedLocationClient; // רכיב לשליפת מיקום גיאוגרפי
    private EditText etCurrentDialogLocation;

    // הגדרת משגר (Launcher) לקבלת תוצאה מחיפוש מקומות של גוגל (Autocomplete)
    private final ActivityResultLauncher<Intent> autocompleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // שליפת אובייקט המקום (Place) מתוך ה-Intent שחזר
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

        // אתחול ספריית Google Places עבור חיפוש כתובות חכם
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.PLACES_API_KEY);
        }

        // אתחול רכיב המיקום
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // הפעלת הגדרות ראשוניות: ערוץ התראות, הרשאות, אתחול תצוגה ו-WebView
        createNotificationChannel();
        requestNotificationPermission();
        initViews();
        setupWebView();

        // הגדרת מאזינים לכפתורים
        btnAddEvent.setOnClickListener(v -> showAddEventDialog());
        btnBackHome.setOnClickListener(v -> finish());
    }

    private void initViews() {
        webViewCalendar = findViewById(R.id.webViewCalendar);
        btnAddEvent = findViewById(R.id.btnAddEvent);
        btnBackHome = findViewById(R.id.btnBackHome);
    }

    private void setupWebView() {
        // הגדרת ה-WebView להצגת יומן גוגל בתוך האפליקציה
        WebSettings webSettings = webViewCalendar.getSettings();
        webSettings.setJavaScriptEnabled(true); // הפעלת JS (נדרש עבור אתר היומן)
        webSettings.setDomStorageEnabled(true); // הפעלת אחסון מקומי (נדרש עבור תהליך התחברות)
        webViewCalendar.setWebViewClient(new WebViewClient()); // גרימת לינקים להיפתח בתוך ה-WebView ולא בדפדפן החיצוני
        webViewCalendar.loadUrl("https://calendar.google.com/calendar/u/0/r");
    }

    private void showAddEventDialog() {
        // ניפוח (Inflate) של עיצוב הדיאלוג מתוך קובץ XML
        View dialogView = getLayoutInflater().inflate(R.layout.activity_dialog_add_event, null);
        EditText etName = dialogView.findViewById(R.id.etEventName);
        EditText etLocation = dialogView.findViewById(R.id.etEventLocation);
        EditText etDescription = dialogView.findViewById(R.id.etEventDescription);
        EditText etDate = dialogView.findViewById(R.id.etEventDate);
        EditText etTime = dialogView.findViewById(R.id.etEventTime);

        // הגדרת שדה המיקום: ביטול מקלדת והפעלת חיפוש מקומות בלחיצה
        etCurrentDialogLocation = etLocation;
        etLocation.setFocusable(false);
        etLocation.setClickable(true);
        etLocation.setOnClickListener(v -> openPlacesSearch());

        // ניסיון למלא את המיקום הנוכחי באופן אוטומטי בעת פתיחת הדיאלוג
        setCurrentLocationInDialog(etLocation);

        final Calendar selectedCalendar = Calendar.getInstance();

        // הגדרת בחירת תאריך (DatePicker)
        etDate.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, day) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, day);
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year));
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH));

            // חסימת בחירת תאריך שכבר עבר
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
            datePicker.show();
        });

        // הגדרת בחירת שעה (TimePicker)
        etTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hour);
                selectedCalendar.set(Calendar.MINUTE, minute);
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show();
        });

        // בנייה והצגה של הדיאלוג
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("שמור והוסף", (d, which) -> {
                    String title = etName.getText().toString().trim();
                    String location = etLocation.getText().toString().trim();
                    String desc = etDescription.getText().toString().trim();

                    // בדיקת תקינות: האם שדות החובה מולאו
                    if (title.isEmpty() || etDate.getText().toString().isEmpty() || etTime.getText().toString().isEmpty()) {
                        Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long finalTime = selectedCalendar.getTimeInMillis();

                    // הגנה מפני קביעת אירוע בזמן שכבר חלף
                    if (finalTime <= System.currentTimeMillis()) {
                        Toast.makeText(this, "Cannot set an event for a past time!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // מעבר לשלב בדיקת כפילויות ושמירה
                    checkDuplicateAndSave(title, location, desc, finalTime);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void checkDuplicateAndSave(String title, String location, String desc, long time) {
        if (refAuth.getCurrentUser() == null) return;
        String uid = refAuth.getCurrentUser().getUid();

        // גישה למסד הנתונים לבדיקה האם קיימת תזכורת זהה (שם וזמן)
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
                                return; // עצירת השמירה אם נמצאה כפילות
                            }
                        }

                        // יצירת מזהה ייחודי (ID) ושמירה ל-Firebase
                        String eventID = "task_" + System.currentTimeMillis();
                        saveToFirebase(eventID, title, location, time);

                        // יצירת אובייקט משימה ורישום ההתראה במנהל ההתראות המקומי
                        Task t = new Task(eventID, title, time, uid, location);
                        t.setRemindAt(time);
                        new ReminderManager(CalendarActivity.this, new ArrayList<>()).addTask(t);

                        // שליחת האירוע ליומן המובנה של אנדרואיד (סנכרון חיצוני)
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

        // יצירת אובייקט ושמירה תחת הנתיב של המשתמש המחובר
        Task newTask = new Task(id, title, time, uid, location);
        newTask.setRemindAt(time);

        FirebaseDatabase.getInstance().getReference("reminders")
                .child(uid)
                .child(id)
                .setValue(newTask);
    }

    private void setCurrentLocationInDialog(EditText locationField) {
        // בדיקת הרשאת מיקום לפני שליפה
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        // שליפת המיקום האחרון הידוע של המכשיר
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // שימוש ב-Geocoder לתרגום קואורדינטות (Lat/Long) לכתובת טקסטואלית
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
        // הפעלת ממשק ה-Autocomplete של גוגל לחיפוש כתובת
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("IL") // הגבלה לישראל
                .setTypeFilter(TypeFilter.ADDRESS)
                .build(this);
        autocompleteLauncher.launch(intent);
    }

    private void sendToExternalCalendar(String t, String l, String d, long start) {
        // יצירת Intent המפעיל את אפליקציית היומן החיצונית (כמו Google Calendar) להוספת אירוע
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, t)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, l)
                .putExtra(CalendarContract.Events.DESCRIPTION, d)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 3600000); // ברירת מחדל: משך של שעה
        startActivity(intent);
    }

    private void requestNotificationPermission() {
        // באנדרואיד 13 ומעלה חובה לבקש הרשאת שליחת התראות באופן מפורש
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void createNotificationChannel() {
        // יצירת ערוץ התראות כחלק מדרישות ה-API המודרני של אנדרואיד
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}