package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

import android.content.Intent;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersActivity extends AppCompatActivity {

    private ListView lvReminders;
    private MaterialButton btnAddReminder, btnBackHome;

    private List<Task> taskList;
    private List<String> displayList; // הרשימה שתופיע ב-ListView
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        initViews();
        loadRemindersFromFirebase();

        // כפתור הוספת תזכורת חדשה
        btnAddReminder.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddReminderActivity.class);
            startActivity(intent);
        });

        // חזרה לבית
        btnBackHome.setOnClickListener(v -> finish());

        // בונוס: לחיצה ארוכה למחיקה
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

                        // עיצוב הטקסט שיוצג ברשימה
                        String date = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                                .format(new Date(task.getTimestamp()));
                        displayList.add("📌 " + task.getTitle() + "\n📍 " + task.getLocation() + " | ⏰ " + date);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RemindersActivity.this, "Failed to load reminders", Toast.LENGTH_SHORT).show();
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
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Reminder deleted", Toast.LENGTH_SHORT).show());
    }
}