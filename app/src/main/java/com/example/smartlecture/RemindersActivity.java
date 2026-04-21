package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersActivity extends AppCompatActivity {

    private ListView lvReminders;
    private MaterialButton btnAddReminder, btnBackHome;

    private List<Task> taskList;
    private List<String> displayList;
    private ArrayAdapter<String> adapter;

    // הגדרת ערוץ ההתראות
    private final String CHANNEL_ID = "MyReminderChannel";
    private ReminderManager reminderManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        // --- הוספת ניהול התראות ---
        createNotificationChannel();
        requestNotificationPermission();
        reminderManager = new ReminderManager(this, new ArrayList<>());

        initViews();
        loadRemindersFromFirebase();

        btnAddReminder.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddReminderActivity.class);
            startActivity(intent);
        });

        btnBackHome.setOnClickListener(v -> finish());

        lvReminders.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteTask(position);
            return true;
        });
    }

    // בקשת הרשאה להתראות (לאנדרואיד 13 ומעלה)
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    // יצירת ערוץ ההתראות (חובה לאנדרואיד 8 ומעלה)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void initViews() {
        lvReminders = findViewById(R.id.lvReminders);
        btnAddReminder = findViewById(R.id.btnAddReminder);
        btnBackHome = findViewById(R.id.btnBackHome);

        taskList = new ArrayList<>();
        displayList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        lvReminders.setAdapter(adapter);
    }

    private void loadRemindersFromFirebase() {
        if (refAuth.getCurrentUser() == null) return;

        String uid = refAuth.getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Reminders").child(uid);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskList.clear();
                displayList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Task task = data.getValue(Task.class);
                    if (task != null) {
                        taskList.add(task);

                        // --- תזמון ההתראה במכשיר ---
                        // אם הזמן של המשימה טרם עבר, נתזמן אותה מחדש ב-AlarmManager
                        if (task.getTimestamp() > System.currentTimeMillis() && !task.isCompleted()) {
                            reminderManager.addTask(task);
                        }
                    }
                }

                // מיון
                Collections.sort(taskList, (t1, t2) -> Long.compare(t1.getTimestamp(), t2.getTimestamp()));

                // תצוגה
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                for (Task t : taskList) {
                    String dateStr = sdf.format(new Date(t.getTimestamp()));
                    String status = t.isCompleted() ? "✅" : "⏳";
                    displayList.add(status + " " + t.getTitle() + "\n📍 " + t.getLocation() + " | 📅 " + dateStr);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RemindersActivity.this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteTask(int position) {
        Task taskToDelete = taskList.get(position);
        String uid = refAuth.getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference("Reminders")
                .child(uid)
                .child(taskToDelete.getEventID())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    // ביטול ההתראה גם מה-AlarmManager במידה ונמחקה
                    reminderManager.cancelReminder(taskToDelete);
                    Toast.makeText(this, "התזכורת נמחקה", Toast.LENGTH_SHORT).show();
                });
    }
}