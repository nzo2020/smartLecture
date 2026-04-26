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

    private ListView lvReminders;               // רכיב להצגת רשימה נגללת
    private MaterialButton btnAddReminder, btnBackHome;
    private List<Task> taskList;               // רשימת אובייקטים מסוג Task (הנתונים הגולמיים)
    private List<String> displayList;          // רשימת מחרוזות מעוצבות להצגה ב-ListView
    private ArrayAdapter<String> adapter;      // המקשר בין רשימת הנתונים לרכיב ה-ListView
    private ReminderManager reminderManager;   // מנהל ההתראות במערכת (AlarmManager)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        // אתחול מנהל התזכורות
        reminderManager = new ReminderManager(this, new ArrayList<>());

        initViews();
        loadRemindersFromFirebase(); // טעינת הנתונים מהענן

        // כפתור למעבר למסך הוספת תזכורת ידנית
        btnAddReminder.setOnClickListener(v -> startActivity(new Intent(this, AddReminderActivity.class)));

        // כפתור חזרה למסך הבית
        btnBackHome.setOnClickListener(v -> finish());

        // הגדרת מאזין ללחיצה ארוכה על פריט ברשימה לצורך מחיקה
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

        // יצירת אדפטר פשוט המשתמש בעיצוב ברירת מחדל של אנדרואיד (simple_list_item_1)
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        lvReminders.setAdapter(adapter);
    }

    private void loadRemindersFromFirebase() {
        if (refAuth.getCurrentUser() == null) return;
        String uid = refAuth.getCurrentUser().getUid();

        // נתיב ב-Database: reminders/UID_USER
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
                        //מנגנון ניקוי אוטומטי:אם זמן התזכורת כבר עבר (קטן מ-now), המערכת מוחקת אותה מה-Database.

                        if (task.getRemindAt() < now) {
                            data.getRef().removeValue();
                        } else {
                            taskList.add(task);
                            // רישום מחדש של ההתראה במערכת (AlarmManager) כדי להבטיח שהיא תפעל
                            reminderManager.addTask(task);
                        }
                    }
                }

                //מיון הרשימה: מסדר את התזכורות לפי זמן (מהקרובה ביותר לרחוקה ביותר).

                Collections.sort(taskList, (t1, t2) -> Long.compare(t1.getRemindAt(), t2.getRemindAt()));

                // עיצוב התצוגה של כל שורה ברשימה
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                for (Task t : taskList) {
                    displayList.add("⏳ " + t.getTitle() + "\n📍 " + t.getLocation() + " | 📅 " + sdf.format(new Date(t.getRemindAt())));
                }

                // עדכון האדפטר שהנתונים השתנו וצריך לרענן את המסך
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void deleteTask(int position) {
        Task taskToDelete = taskList.get(position);
        FirebaseDatabase.getInstance().getReference("reminders").child(refAuth.getCurrentUser().getUid())
                .child(taskToDelete.getEventID()).removeValue().addOnSuccessListener(aVoid -> {
                    // ביטול ההתראה המתוזמנת בטלפון
                    reminderManager.cancelReminder(taskToDelete);
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                });
    }
}