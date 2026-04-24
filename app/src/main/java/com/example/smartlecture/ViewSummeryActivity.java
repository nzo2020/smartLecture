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
            lectureTitles.add("Loading lessons...");
            adapter.notifyDataSetChanged();

            // Fetching user object to get the 'name' field for the database path
            refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // Ensure the User object has the UID correctly
                        user.setUserID(uid);

                        user.fetchEvents(new User.OnEventsFetchListener() {
                            @Override
                            public void onEventsFetched(List<Lecture> events) {
                                lectureList.clear();
                                lectureTitles.clear();

                                lectureTitles.add("Select a summary from the list:");
                                lectureList.add(null);

                                if (events.isEmpty()) {
                                    lectureTitles.set(0, "No summaries found");
                                } else {
                                    for (Lecture lecture : events) {
                                        lectureList.add(lecture);
                                        String title = (lecture.getTitle() != null && !lecture.getTitle().isEmpty())
                                                ? lecture.getTitle() : "Untitled Lesson";
                                        String lecturer = (lecture.getLecturer() != null) ? lecture.getLecturer() : "Unknown Lecturer";

                                        lectureTitles.add(title + " — " + lecturer);
                                    }
                                }
                                adapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(ViewSummeryActivity.this, "Load Error: " + error, Toast.LENGTH_SHORT).show();
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
            tvSummaryContent.setText("Please select a summary to view details");
            tvLinks.setText("");
            btnShare.setEnabled(false);
            return;
        }

        Lecture selected = lectureList.get(position);
        String fullSummary = selected.getSummaryText();

        // ניקוי טקסט האירועים מהסיכום כדי שלא יופיע בתיבת הטקסט
        if (fullSummary != null && fullSummary.contains("SMART_EVENTS_LIST:")) {
            fullSummary = fullSummary.split("SMART_EVENTS_LIST:")[0].trim();
        }

        tvSummaryContent.setText(fullSummary);

        StringBuilder sb = new StringBuilder();

        // הצגת קישורים רלוונטיים מה-AI
        if (selected.getRelevantLinks() != null && !selected.getRelevantLinks().isEmpty()) {
            sb.append("🔗 Relevant Links:\n")
                    .append(selected.getRelevantLinks())
                    .append("\n\n");
        }

        // הוספת קישור להקלטה
        int startOfAudioText = sb.length();
        String audioText = "🎙️ Recording is available in the cloud (Click to listen)";

        if (selected.getAudioURL() != null && !selected.getAudioURL().isEmpty()) {
            sb.append(audioText);
        } else {
            sb.append("❌ No recording available");
        }

        SpannableString spannable = new SpannableString(sb.toString());

        // הגדרת הקישור להקלטה כקישור לחיץ כחול עם קו תחתי
        if (selected.getAudioURL() != null && !selected.getAudioURL().isEmpty()) {
            spannable.setSpan(new ForegroundColorSpan(Color.BLUE), startOfAudioText, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new UnderlineSpan(), startOfAudioText, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvLinks.setOnClickListener(v -> {
                try {
                    Uri uri = Uri.parse(selected.getAudioURL());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Error opening audio", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            tvLinks.setOnClickListener(null);
        }

        tvLinks.setText(spannable);

        // הפיכת הלינקים הלימודיים בתוך ה-Relevant Links ללחיצים אוטומטית
        Linkify.addLinks(tvLinks, Linkify.WEB_URLS);

        btnShare.setEnabled(true);
    }

    private void shareSummary() {
        int selectedPos = spSummarySelector.getSelectedItemPosition();
        if (selectedPos <= 0 || lectureList.get(selectedPos) == null) return;

        Lecture selected = lectureList.get(selectedPos);
        String shareBody = "📖 Lesson Summary: " + selected.getTitle() + "\n" +
                "👨‍🏫 Lecturer: " + selected.getLecturer() + "\n\n" +
                "📝 Summary:\n" + selected.getSummaryText() + "\n\n" +
                "🔗 Extra Links:\n" + selected.getRelevantLinks() + "\n\n" +
                "🎙️ Audio:\n" + selected.getAudioURL();

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        sendIntent.setType("text/plain");

        startActivity(Intent.createChooser(sendIntent, "Share Summary"));
    }
}