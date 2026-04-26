package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;
import static com.example.smartlecture.FBRef.refUsers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    // הצהרה על רכיבי ה-UI (TextViews וכפתורי Material Design)
    private TextView tvWelcome, tvStats;
    private MaterialButton btnRecordLesson, btnViewSummaries, btnLessonCalendar,
            btnSmartReminders, btnSearchLesson, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge: מאפשר לאפליקציה להשתמש בכל שטח המסך (כולל מאחורי שורת הסטטוס) למראה מודרני
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        initViews();           // קישור המשתנים לרכיבים הגרפיים ב-XML
        setupClickListeners();  // הגדרת פעולות הלחיצה על הכפתורים
        loadUserData();        // משיכת נתוני המשתמש מה-Firebase (שם וסטטיסטיקה)
    }

    private void initViews() {
        // אתחול הרכיבים באמצעות ה-ID שלהם מה-Layout
        tvWelcome = findViewById(R.id.tvWelcome);
        tvStats = findViewById(R.id.tvStats);

        btnRecordLesson = findViewById(R.id.btnRecordLesson);
        btnViewSummaries = findViewById(R.id.btnViewSummaries);
        btnLessonCalendar = findViewById(R.id.btnLessonCalendar);
        btnSmartReminders = findViewById(R.id.btnSmartReminders);
        btnSearchLesson = findViewById(R.id.btnSearchLesson);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserData() {
        // בדיקה שיש משתמש מחובר ב-Firebase Authentication
        if (refAuth.getCurrentUser() != null) {
            // שליפת ה-UID (Unique Identifier) הייחודי של המשתמש
            String uid = refAuth.getCurrentUser().getUid();

            // addValueEventListener: מאזין לשינויים ב-Database בזמן אמת.
            // אם מספר ההרצאות ישתנה (למשל אחרי הקלטה חדשה), המסך יתעדכן אוטומטית.
            refUsers.child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    // בדיקה שהנתיב של המשתמש קיים ב-Database
                    if (snapshot.exists()) {
                        // שליפת השם (String) וכמות ההרצאות (Long) מה-Snapshot
                        String name = snapshot.child("name").getValue(String.class);
                        tvWelcome.setText("Hello, " + (name != null ? name : "Student") + " 👋");

                        Long total = snapshot.child("totalLectures").getValue(Long.class);
                        if (total == null) total = 0L; // הגנה למקרה שהערך ריק

                        tvStats.setText("Stats: " + total + " Lectures Saved");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // תיעוד שגיאה במידה והגישה ל-Database נכשלה (למשל בגלל חוסר הרשאות)
                    Log.e("Dashboard", "Error loading data: " + error.getMessage());
                }
            });
        }
    }

    private void setupClickListeners() {
        // מעבר בין מסכים באמצעות Intents:
        btnRecordLesson.setOnClickListener(v -> startActivity(new Intent(this, RecordLesson.class)));
        btnViewSummaries.setOnClickListener(v -> startActivity(new Intent(this, ViewSummeryActivity.class)));
        btnLessonCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        btnSmartReminders.setOnClickListener(v -> startActivity(new Intent(this, RemindersActivity.class)));
        btnSearchLesson.setOnClickListener(v -> startActivity(new Intent(this, SearchSummariesActivity.class)));

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {
        // 1. ניתוק המשתמש מ-Firebase Authentication
        refAuth.signOut();

        // 2. עדכון SharedPreferences כדי שהאפליקציה לא תבצע חיבור אוטומטי בהפעלה הבאה
        SharedPreferences sharedPref = getSharedPreferences("USER_SETTINGS", MODE_PRIVATE);
        sharedPref.edit().putBoolean("stayConnect", false).apply();

        // 3. ניקוי המחסנית (Flags) וחזרה למסך הכניסה כדי שהמשתמש לא יוכל לחזור אחורה ל-Dashboard
        Intent intent = new Intent(DashboardActivity.this, LoginOptionsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // סגירת ה-Activity הנוכחי
    }
}