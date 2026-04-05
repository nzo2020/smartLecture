package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;
import static com.example.smartlecture.FBRef.refUsers;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

    private TextView tvWelcome, tvStats;
    private MaterialButton btnRecordLesson, btnViewSummaries, btnLessonCalendar,
            btnSmartReminders, btnSearchLesson, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        initViews();
        setupClickListeners();
        loadUserData();
    }

    private void initViews() {
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
        if (refAuth.getCurrentUser() != null) {
            String uid = refAuth.getCurrentUser().getUid();

            // שלב 1: משיכת אובייקט המשתמש הבסיסי
            refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            tvWelcome.setText("Hello, " + user.getName() + " 👋");

                            // שלב 2: שימוש בפעולה החדשה בתוך מחלקת User למשיכת ההרצאות
                            user.fetchEvents(new User.OnEventsFetchListener() {
                                @Override
                                public void onEventsFetched(List<Lecture> events) {
                                    // עדכון ה-UI עם כמות ההרצאות האמיתית שנמשכה מהענן
                                    tvStats.setText("Stats: " + events.size() + " Lectures Saved");
                                }

                                @Override
                                public void onError(String error) {
                                    tvStats.setText("Stats: Error loading count");
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(DashboardActivity.this, "Error loading user profile", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupClickListeners() {
        btnRecordLesson.setOnClickListener(v -> startActivity(new Intent(this, RecordLesson.class)));
        btnViewSummaries.setOnClickListener(v -> startActivity(new Intent(this, ViewSummeryActivity.class)));
        btnLessonCalendar.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        btnSmartReminders.setOnClickListener(v -> startActivity(new Intent(this, RemindersActivity.class)));
        btnSearchLesson.setOnClickListener(v -> startActivity(new Intent(this, SearchSummariesActivity.class)));

        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser() {
        refAuth.signOut();
        SharedPreferences sharedPref = getSharedPreferences("USER_SETTINGS", MODE_PRIVATE);
        sharedPref.edit().putBoolean("stayConnect", false).apply();

        Intent intent = new Intent(DashboardActivity.this, LoginOptionsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}