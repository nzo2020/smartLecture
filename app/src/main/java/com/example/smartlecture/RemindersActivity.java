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
    private ReminderManager reminderManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        reminderManager = new ReminderManager(this, new ArrayList<>());
        initViews();
        loadRemindersFromFirebase();

        btnAddReminder.setOnClickListener(v -> startActivity(new Intent(this, AddReminderActivity.class)));
        btnBackHome.setOnClickListener(v -> finish());
        lvReminders.setOnItemLongClickListener((parent, view, position, id) -> {
            deleteTask(position);
            return true;
        });
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
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("reminders").child(uid);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskList.clear();
                displayList.clear();
                long now = System.currentTimeMillis();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Task task = data.getValue(Task.class);
                    if (task != null) {
                        // מחיקה אוטומטית אם הזמן עבר
                        if (task.getRemindAt() < now) {
                            data.getRef().removeValue();
                        } else {
                            taskList.add(task);
                            reminderManager.addTask(task);
                        }
                    }
                }

                // מיון לפי זמן
                Collections.sort(taskList, (t1, t2) -> Long.compare(t1.getRemindAt(), t2.getRemindAt()));

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                for (Task t : taskList) {
                    displayList.add("⏳ " + t.getTitle() + "\n📍 " + t.getLocation() + " | 📅 " + sdf.format(new Date(t.getRemindAt())));
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deleteTask(int position) {
        Task taskToDelete = taskList.get(position);
        FirebaseDatabase.getInstance().getReference("reminders").child(refAuth.getCurrentUser().getUid())
                .child(taskToDelete.getEventID()).removeValue().addOnSuccessListener(aVoid -> {
                    reminderManager.cancelReminder(taskToDelete);
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                });
    }
}