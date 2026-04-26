package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;
import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddReminderActivity extends AppCompatActivity {

    private TextInputEditText etReminderName, etDate, etTime, etLocation, etDescription;
    private TextInputLayout tilLocation;
    private MaterialButton btnSaveReminder;
    private ImageButton btnBack;
    private Calendar selectedDateTime; // שימוש ב-Calendar מאפשר ניהול תאריך ושעה באובייקט אחד והוצאת זמן במילישניות (Long).
    private FusedLocationProviderClient fusedLocationClient; // רכיב של Google Play Services לשליפת מיקום מדויק במינימום צריכת סוללה.

    // Activity Result API: הדרך המודרנית להפעלת מסך חיצוני וקבלת נתונים (מחליפה את onActivityResult הישן).
    private final ActivityResultLauncher<Intent> autocompleteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    etLocation.setText(place.getAddress());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        // אתחול ספריית המקומות של גוגל באמצעות ה-API Key שהוגדר ב-BuildConfig.
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.PLACES_API_KEY);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initViews();
        selectedDateTime = Calendar.getInstance();
        setupListeners();
        checkLocationPermission(); // בקשת הרשאת מיקום בזמן ריצה (Runtime Permission) בהתאם למדיניות האבטחה של אנדרואיד.
    }

    private void initViews() {
        etReminderName = findViewById(R.id.etReminderName);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLocation = findViewById(R.id.etLocation);
        tilLocation = findViewById(R.id.tilLocation);
        etDescription = findViewById(R.id.etDescription);
        btnSaveReminder = findViewById(R.id.btnSaveReminder);
        btnBack = findViewById(R.id.btnBack);

        // ביטול מקלדת בשדה המיקום כדי לאלץ בחירה מתוך רשימת הכתובות הרשמית של גוגל.
        etLocation.setFocusable(false);
        etLocation.setClickable(true);
    }

    private void setupListeners() {
        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());
        btnBack.setOnClickListener(v -> finish());
        btnSaveReminder.setOnClickListener(v -> saveReminderWithCheck());
        etLocation.setOnClickListener(v -> openPlacesSearch());
        tilLocation.setEndIconOnClickListener(v -> openPlacesSearch());
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateTime.set(Calendar.YEAR, year);
            selectedDateTime.set(Calendar.MONTH, month);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            // %02d מבטיח תצוגה תקנית של שתי ספרות (למשל 09 במקום 9) עבור ימים וחודשים.
            etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year));
        }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH));

        datePicker.getDatePicker().setMinDate(System.currentTimeMillis()); // חסימת היכולת לבחור תאריך שכבר עבר.
        datePicker.show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedDateTime.set(Calendar.MINUTE, minute);
            etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
        }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true).show();
    }

    private void saveReminderWithCheck() {
        String title = etReminderName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        long selectedMillis = selectedDateTime.getTimeInMillis();

        if (title.isEmpty() || etDate.getText().toString().isEmpty() || etTime.getText().toString().isEmpty()) {
            Toast.makeText(this, "Title, Date and Time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "Cannot set a reminder for a past time", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = refAuth.getCurrentUser().getUid();

        // addListenerForSingleValueEvent: קריאה חד-פעמית של הנתונים מהענן לצורך בדיקת לוגיקה מקומית.
        FirebaseDatabase.getInstance().getReference("reminders").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // לולאה הסורקת את המשימות הקיימות למניעת כפילות של שם וזמן בבסיס הנתונים.
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Task t = ds.getValue(Task.class);
                            if (t != null && t.getTitle().equalsIgnoreCase(title) && t.getRemindAt() == selectedMillis) {
                                Toast.makeText(AddReminderActivity.this, "This reminder already exists!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        // יצירת מפתח ייחודי המבוסס על זמן המערכת כדי למנוע דריסת נתונים ב-Firebase.
                        String eventID = "task_" + System.currentTimeMillis();
                        Task newTask = new Task(eventID, title, selectedMillis, uid, location);
                        newTask.setRemindAt(selectedMillis);

                        // setValue(newTask): הפיכת אובייקט ה-Java למבנה JSON ושמירתו בענן.
                        FirebaseDatabase.getInstance().getReference("reminders").child(uid).child(eventID).setValue(newTask)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AddReminderActivity.this, "Reminder Saved!", Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void openPlacesSearch() {
        // הגדרת השדות המבוקשים מה-API של גוגל (חוסך במשאבים ועלויות על ידי בקשת מידע רלוונטי בלבד).
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("IL").setTypeFilter(TypeFilter.ADDRESS).build(this);
        autocompleteLauncher.launch(intent);
    }

    private void checkLocationPermission() {
        // בדיקה האם המשתמש כבר אישר הרשאת מיקום בעבר.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        // שליפת המיקום האחרון הידוע של המכשיר דרך שירותי גוגל.
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // Geocoder: רכיב המבצע Reverse Geocoding - הפיכת קואורדינטות גיאוגרפיות לכתובת טקסטואלית.
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) etLocation.setText(addresses.get(0).getAddressLine(0));
                } catch (Exception e) { e.printStackTrace(); }
                //הקוד הזה לוקח את המיקום הגולמי שהתקבל מהחיישן, מתרגם אותו לכתובת אנושית בעזרת ה-Geocoder, ומציג אותה אוטומטית למשתמש בתוך ה-EditText. השתמשתי בבדיקות null וב-try-catch כדי להבטיח יציבות גם אם אין חיבור לרשת באותו רגע
            }
        });
    }
}