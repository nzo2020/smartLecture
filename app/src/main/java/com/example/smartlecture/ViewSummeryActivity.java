package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;
import static com.example.smartlecture.FBRef.refUsers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ViewSummeryActivity extends AppCompatActivity {

    private Spinner spSummarySelector;
    private TextView tvSummaryContent, tvLinks;
    private MaterialButton btnShare, btnBackHome;

    private List<Lecture> lectureList;
    private List<String> lectureTitles;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_summery);

        initViews();
        loadUserAndLectures();

        btnBackHome.setOnClickListener(v -> finish());

        spSummarySelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                displayLectureDetails(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnShare.setOnClickListener(v -> shareSummary());
    }

    private void initViews() {
        spSummarySelector = findViewById(R.id.spSummarySelector);
        tvSummaryContent = findViewById(R.id.tvSummaryContent);
        tvLinks = findViewById(R.id.tvLinks);
        btnShare = findViewById(R.id.btnShare);
        btnBackHome = findViewById(R.id.btnBackHome);

        lectureList = new ArrayList<>();
        lectureTitles = new ArrayList<>();

        // יצירת ה-Adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lectureTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSummarySelector.setAdapter(adapter);
    }

    private void loadUserAndLectures() {
        if (refAuth.getCurrentUser() != null) {
            String uid = refAuth.getCurrentUser().getUid();

            // הצגת הודעה "טוען..." עד שהנתונים יגיעו
            lectureTitles.clear();
            lectureTitles.add("טוען שיעורים...");
            adapter.notifyDataSetChanged();

            refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // חשוב: לוודא שמתודת fetchEvents במחלקת User מעודכנת לנתיב החדש
                        user.fetchEvents(new User.OnEventsFetchListener() {
                            @Override
                            public void onEventsFetched(List<Lecture> events) {
                                lectureList.clear();
                                lectureTitles.clear();

                                lectureTitles.add("בחר סיכום מהרשימה:");
                                lectureList.add(null);

                                if (events.isEmpty()) {
                                    lectureTitles.set(0, "לא נמצאו סיכומים");
                                } else {
                                    for (Lecture lecture : events) {
                                        lectureList.add(lecture);
                                        // שימוש בכותרת שהמשתמש הזין (Title)
                                        String title = (lecture.getTitle() != null && !lecture.getTitle().isEmpty())
                                                ? lecture.getTitle() : "שיעור ללא כותרת";
                                        String lecturer = (lecture.getLecturer() != null) ? lecture.getLecturer() : "מרצה לא ידוע";

                                        lectureTitles.add(title + " — " + lecturer);
                                    }
                                }
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(ViewSummeryActivity.this, "שגיאה בטעינה: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void displayLectureDetails(int position) {
        if (position == 0 || lectureList.get(position) == null) {
            tvSummaryContent.setText("אנא בחר סיכום כדי לצפות בפרטים");
            tvLinks.setText("");
            btnShare.setEnabled(false); // ביטול כפתור שיתוף כשאין תוכן
            return;
        }

        Lecture selected = lectureList.get(position);
        tvSummaryContent.setText(selected.getSummaryText());

        // הצגת הקישור להקלטה בצורה יפה
        if (selected.getAudioURL() != null && !selected.getAudioURL().isEmpty()) {
            tvLinks.setText("🔗 הקלטת השיעור זמינה בענן");
            tvLinks.setOnClickListener(v -> {
                // אופציונלי: פתיחת הלינק בדפדפן או בנגן
                Toast.makeText(this, "פתיחת הקלטה...", Toast.LENGTH_SHORT).show();
            });
        } else {
            tvLinks.setText("❌ אין הקלטה זמינה");
        }

        btnShare.setEnabled(true);
    }

    private void shareSummary() {
        int selectedPos = spSummarySelector.getSelectedItemPosition();
        if (selectedPos <= 0 || lectureList.get(selectedPos) == null) return;

        Lecture selected = lectureList.get(selectedPos);
        String shareBody = "סיכום שיעור: " + selected.getTitle() + "\n\n" +
                "מרצה: " + selected.getLecturer() + "\n\n" +
                "תוכן הסיכום:\n" + selected.getSummaryText() + "\n\n" +
                "להאזנה להקלטה:\n" + selected.getAudioURL();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "שתף סיכום שיעור באמצעות:");
        startActivity(shareIntent);
    }
}