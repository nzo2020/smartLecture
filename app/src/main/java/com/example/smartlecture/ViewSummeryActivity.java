package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;
import static com.example.smartlecture.FBRef.refUsers;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
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

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lectureTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSummarySelector.setAdapter(adapter);
    }

    private void loadUserAndLectures() {
        if (refAuth.getCurrentUser() != null) {
            String uid = refAuth.getCurrentUser().getUid();

            lectureTitles.clear();
            lectureTitles.add("טוען שיעורים...");
            adapter.notifyDataSetChanged();

            refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
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
            btnShare.setEnabled(false);
            return;
        }

        Lecture selected = lectureList.get(position);
        tvSummaryContent.setText(selected.getSummaryText());

        // 1. בניית הטקסט הגולמי
        StringBuilder sb = new StringBuilder();

        // הוספת קישורי ה-AI
        if (selected.getRelevantLinks() != null && !selected.getRelevantLinks().isEmpty()) {
            sb.append("🔗 קישורים רלוונטיים:\n")
                    .append(selected.getRelevantLinks())
                    .append("\n\n");
        }

        // חישוב המיקום לצביעה בכחול
        int startOfAudioText = sb.length();
        String audioText = "🎙️ הקלטת השיעור זמינה בענן (לחץ כאן להאזנה)";

        if (selected.getAudioURL() != null && !selected.getAudioURL().isEmpty()) {
            sb.append(audioText);
        } else {
            sb.append("❌ אין הקלטה זמינה");
        }

        // 2. יצירת Spannable לעיצוב
        SpannableString spannable = new SpannableString(sb.toString());

        if (selected.getAudioURL() != null && !selected.getAudioURL().isEmpty()) {
            // צביעה בכחול
            spannable.setSpan(
                    new ForegroundColorSpan(Color.BLUE),
                    startOfAudioText,
                    sb.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // הוספת קו תחתון
            spannable.setSpan(
                    new UnderlineSpan(),
                    startOfAudioText,
                    sb.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            // הגדרת לחיצה על ה-TextView
            tvLinks.setOnClickListener(v -> {
                try {
                    Uri uri = Uri.parse(selected.getAudioURL());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "שגיאה בפתיחת ההקלטה", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            tvLinks.setOnClickListener(null);
        }

        tvLinks.setText(spannable);

        // 3. הפעלת Linkify לזיהוי כתובות אינטרנט של Gemini
        Linkify.addLinks(tvLinks, Linkify.WEB_URLS);

        btnShare.setEnabled(true);
    }

    private void shareSummary() {
        int selectedPos = spSummarySelector.getSelectedItemPosition();
        if (selectedPos <= 0 || lectureList.get(selectedPos) == null) return;

        Lecture selected = lectureList.get(selectedPos);
        String shareBody = "📖 סיכום שיעור: " + selected.getTitle() + "\n" +
                "👨‍🏫 מרצה: " + selected.getLecturer() + "\n\n" +
                "📝 סיכום:\n" + selected.getSummaryText() + "\n\n" +
                "🔗 קישורים נוספים:\n" + selected.getRelevantLinks() + "\n\n" +
                "🎙️ הקלטה:\n" + selected.getAudioURL();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        sendIntent.setType("text/plain");

        startActivity(Intent.createChooser(sendIntent, "שתף סיכום שיעור"));
    }
}