package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private WebView webViewCalendar;
    private MaterialButton btnAddEvent, btnBackHome;
    private final String CHANNEL_ID = "MyReminderChannel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        createNotificationChannel();
        requestNotificationPermission();
        initViews();
        setupWebView();

        btnAddEvent.setOnClickListener(v -> showAddEventDialog());
        btnBackHome.setOnClickListener(v -> finish());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
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

        final Calendar selectedCalendar = Calendar.getInstance();

        etDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, day);
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year));
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
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

                    if (title.isEmpty()) return;

                    long finalTime = selectedCalendar.getTimeInMillis();
                    String eventID = "task_" + System.currentTimeMillis();

                    // 1. Firebase
                    saveToFirebase(eventID, title, location, finalTime);

                    // 2. AlarmManager
                    Task t = new Task(eventID, title, finalTime, refAuth.getCurrentUser().getUid(), location);
                    t.setRemindAt(finalTime);
                    new ReminderManager(this, new ArrayList<>()).addTask(t);

                    // 3. יומן חיצוני
                    sendToExternalCalendar(title, location, desc, finalTime);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void saveToFirebase(String id, String t, String l, long time) {
        if (refAuth.getCurrentUser() == null) return;
        Task newTask = new Task(id, t, time, refAuth.getCurrentUser().getUid(), l);
        newTask.setRemindAt(time);
        FirebaseDatabase.getInstance().getReference("Reminders")
                .child(refAuth.getCurrentUser().getUid()).child(id).setValue(newTask);
    }

    private void sendToExternalCalendar(String t, String l, String d, long start) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, t)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, l)
                .putExtra(CalendarContract.Events.DESCRIPTION, d)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 3600000);
        startActivity(intent);
    }
}