package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Locale;

public class AddReminderActivity extends AppCompatActivity {

    private TextInputEditText etReminderName, etDate, etTime, etLocation, etDescription;
    private MaterialButton btnSaveReminder;
    private ImageButton btnBack;

    private Calendar selectedDateTime; // אובייקט שישמור את התאריך והשעה שנבחרו

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_reminder);

        initViews();

        selectedDateTime = Calendar.getInstance();

        // בחירת תאריך
        etDate.setOnClickListener(v -> showDatePicker());

        // בחירת שעה
        etTime.setOnClickListener(v -> showTimePicker());

        // כפתור חזרה
        btnBack.setOnClickListener(v -> finish());

        // שמירת התזכורת
        btnSaveReminder.setOnClickListener(v -> saveReminderToFirebase());
    }

    private void initViews() {
        etReminderName = findViewById(R.id.etReminderName);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        btnSaveReminder = findViewById(R.id.btnSaveReminder);
        btnBack = findViewById(R.id.btnBack);
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateTime.set(Calendar.YEAR, year);
            selectedDateTime.set(Calendar.MONTH, month);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
            etDate.setText(dateStr);
        }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedDateTime.set(Calendar.MINUTE, minute);

            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            etTime.setText(timeStr);
        }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), true);

        timePicker.show();
    }

    private void saveReminderToFirebase() {
        String title = etReminderName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty() || etDate.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill in the title and date", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = refAuth.getCurrentUser().getUid();
        String eventID = "task_" + System.currentTimeMillis();

        // שימוש במחלקה שלך: Task
        Task newTask = new Task(eventID, title, selectedDateTime.getTimeInMillis(), uid, location);
        newTask.setReminder(selectedDateTime.getTimeInMillis()); // הגדרת זמן התזכורת

        // שמירה ל-Firebase תחת הנתיב Reminders/[UID]/[eventID]
        FirebaseDatabase.getInstance().getReference("Reminders")
                .child(uid)
                .child(eventID)
                .setValue(newTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Reminder Saved!", Toast.LENGTH_SHORT).show();
                    finish(); // חזרה למסך הרשימה
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}