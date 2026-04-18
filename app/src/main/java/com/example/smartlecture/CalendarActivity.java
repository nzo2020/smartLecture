package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private WebView webViewCalendar;
    private MaterialButton btnAddEvent, btnBackHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        initViews();
        setupWebView();

        // כפתור הוספת אירוע
        btnAddEvent.setOnClickListener(v -> showAddEventDialog());

        // כפתור חזרה
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
        // 1. ניפוח ה-XML של הדיאלוג
        View dialogView = getLayoutInflater().inflate(R.layout.activity_dialog_add_event, null);

        EditText etName = dialogView.findViewById(R.id.etEventName);
        EditText etLocation = dialogView.findViewById(R.id.etEventLocation);
        EditText etDescription = dialogView.findViewById(R.id.etEventDescription);
        EditText etDate = dialogView.findViewById(R.id.etEventDate);
        EditText etTime = dialogView.findViewById(R.id.etEventTime);

        // אובייקט לשמירת הזמן שנבחר מה-Pickers
        final Calendar selectedCalendar = Calendar.getInstance();

        // הגדרת בחירת תאריך בלחיצה על השדה
        etDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, day);
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year));
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        // הגדרת בחירת שעה בלחיצה על השדה
        etTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                selectedCalendar.set(Calendar.HOUR_OF_DAY, hour);
                selectedCalendar.set(Calendar.MINUTE, minute);
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show();
        });

        // 2. בניית הדיאלוג
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("שמור והוסף", (d, which) -> {
                    String title = etName.getText().toString().trim();
                    String location = etLocation.getText().toString().trim();
                    String desc = etDescription.getText().toString().trim();

                    if (title.isEmpty() || etDate.getText().toString().isEmpty()) {
                        Toast.makeText(this, "אנא הזן כותרת ותאריך", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long finalTime = selectedCalendar.getTimeInMillis();

                    // א. שמירה ל-Firebase (כדי שיופיע ב-ListView של התזכורות)
                    saveToFirebase(title, location, finalTime);

                    // ב. שליחה לגוגל קלנדר (פותח את האפליקציה החיצונית)
                    sendToExternalCalendar(title, location, desc, finalTime);
                })
                .setNegativeButton("ביטול", null)
                .create();

        // הפיכת רקע הדיאלוג לשקוף עבור ה-CardView המעוגל
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void saveToFirebase(String title, String location, long time) {
        if (refAuth.getCurrentUser() == null) return;

        String uid = refAuth.getCurrentUser().getUid();
        String eventID = "task_" + System.currentTimeMillis();

        // יצירת אובייקט Task ושמירתו
        Task newTask = new Task(eventID, title, time, uid, location);
        newTask.setReminder(time);
        newTask.setPriorityScore(1);
        newTask.setCompleted(false);

        FirebaseDatabase.getInstance().getReference("Reminders")
                .child(uid)
                .child(eventID)
                .setValue(newTask)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "נשמר במערכת", Toast.LENGTH_SHORT).show());
    }

    private void sendToExternalCalendar(String title, String location, String description, long startTime) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);

        intent.putExtra(CalendarContract.Events.TITLE, title);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, description);

        // העברת הזמן המדויק שנבחר ב-Picker לגוגל קלנדר
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTime + (60 * 60 * 1000)); // ברירת מחדל: שעה אחת

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "לא נמצאה אפליקציית לוח שנה", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webViewCalendar.canGoBack()) {
            webViewCalendar.goBack();
        } else {
            super.onBackPressed();
        }
    }
}