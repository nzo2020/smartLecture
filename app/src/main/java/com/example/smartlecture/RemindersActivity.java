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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        initViews();
        loadRemindersFromFirebase();

        btnAddReminder.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddReminderActivity.class);
            startActivity(intent);
        });

        btnBackHome.setOnClickListener(v -> finish());

        // מחיקה בלחיצה ארוכה
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
                    }
                }

                // --- אלגוריתם מיון לפי תאריך (Timestamp) ---
                // ממיין מהתאריך הקרוב ביותר (הקטן ביותר) לרחוק ביותר
                Collections.sort(taskList, (t1, t2) -> Long.compare(t1.getTimestamp(), t2.getTimestamp()));

                // בניית רשימת התצוגה לאחר המיון
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
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "התזכורת נמחקה", Toast.LENGTH_SHORT).show());
    }
}