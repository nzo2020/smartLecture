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

/**
 * Activity for viewing generated lecture summaries.
 * It provides a dropdown interface (Spinner) to select a lecture, displays the
 * summarized content, handles clickable web links via Linkify, and manages
 * Spannable styling for audio recording access.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class ViewSummeryActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש
    private Spinner spSummarySelector;
    private TextView tvSummaryContent, tvLinks;
    private MaterialButton btnShare, btnBackHome;

    // רשימות לניהול הנתונים
    /** Internal list of lecture objects retrieved from the database */
    private List<Lecture> lectureList;
    /** List of titles displayed in the selection spinner */
    private List<String> lectureTitles;
    private ArrayAdapter<String> adapter; // האדפטר שמקשר בין הרשימה ל-Spinner

    /**
     * Initializes the activity, binds views, and triggers the Firebase lecture loading process.
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_summery);

        initViews();              // אתחול רכיבי ה-UI
        loadUserAndLectures();    // טעינת רשימת ההרצאות מה-Firebase

        // כפתור חזרה למסך הבית
        btnBackHome.setOnClickListener(v -> finish());

        // מאזין לבחירת פריט ב-Spinner
        spSummarySelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // הצגת פרטי ההרצאה שנבחרה
                displayLectureDetails(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // כפתור לשיתוף הסיכום דרך אפליקציות חיצוניות
        btnShare.setOnClickListener(v -> shareSummary());
    }

    /**
     * Configures the initial UI state and initializes the ArrayAdapter for the Spinner.
     */
    private void initViews() {
        spSummarySelector = findViewById(R.id.spSummarySelector);
        tvSummaryContent = findViewById(R.id.tvSummaryContent);
        tvLinks = findViewById(R.id.tvLinks);
        btnShare = findViewById(R.id.btnShare);
        btnBackHome = findViewById(R.id.btnBackHome);

        lectureList = new ArrayList<>();
        lectureTitles = new ArrayList<>();

        // יצירת קישור בין רשימת הכותרות ל-Spinner
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lectureTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSummarySelector.setAdapter(adapter);
    }

    /**
     * Authenticates the user and fetches their lectures using the User class fetchEvents logic.
     * Updates the Spinner adapter upon successful retrieval.
     */
    private void loadUserAndLectures() {
        if (refAuth.getCurrentUser() != null) {
            String uid = refAuth.getCurrentUser().getUid();

            lectureTitles.clear();
            lectureTitles.add("Loading lessons...");
            adapter.notifyDataSetChanged();

            // פנייה ל-Firebase לשליפת נתוני המשתמש
            refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        user.setUserID(uid);

                        // שימוש במתודה fetchEvents של מחלקת User לאיסוף כל ההרצאות
                        user.fetchEvents(new User.OnEventsFetchListener() {
                            @Override
                            public void onEventsFetched(List<Lecture> events) {
                                lectureList.clear();
                                lectureTitles.clear();

                                // הוספת שורת הנחיה בראש הרשימה
                                lectureTitles.add("Select a summary from the list:");
                                lectureList.add(null);

                                if (events.isEmpty()) {
                                    lectureTitles.set(0, "No summaries found");
                                } else {
                                    // בניית רשימת הכותרות לתצוגה
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

    /**
     * Populates the screen with details of the selected lecture.
     * Cleans raw AI data, formats web links, and creates a clickable Spannable for cloud audio.
     * @param position The position of the selected item in the Spinner.
     */
    private void displayLectureDetails(int position) {
        // בדיקה אם נבחר פריט תקין (לא שורת ההנחיה)
        if (position == 0 || lectureList.get(position) == null) {
            tvSummaryContent.setText("Please select a summary to view details");
            tvLinks.setText("");
            btnShare.setEnabled(false);
            return;
        }

        Lecture selected = lectureList.get(position);
        String fullSummary = selected.getSummaryText();

        /**
         * ניקוי טקסט: ה-AI מוסיף לעיתים רשימת אירועים בפורמט SMART_EVENTS_LIST.
         * אנחנו חותכים את החלק הזה כדי שהמשתמש יראה רק את הסיכום הנקי.
         */
        if (fullSummary != null && fullSummary.contains("SMART_EVENTS_LIST:")) {
            fullSummary = fullSummary.split("SMART_EVENTS_LIST:")[0].trim();
        }

        tvSummaryContent.setText(fullSummary);

        StringBuilder sb = new StringBuilder();

        // הוספת לינקים רלוונטיים שה-AI מצא ברשת
        if (selected.getRelevantLinks() != null && !selected.getRelevantLinks().isEmpty()) {
            sb.append("🔗 Relevant Links:\n")
                    .append(selected.getRelevantLinks())
                    .append("\n\n");
        }

        // הוספת קישור להקלטה ב-Cloud Storage
        int startOfAudioText = sb.length();
        String audioText = "🎙️ Recording is available in the cloud (Click to listen)";

        if (selected.getAudioURL() != null && !selected.getAudioURL().isEmpty()) {
            sb.append(audioText);
        } else {
            sb.append("❌ No recording available");
        }

        /**
         * שימוש ב-SpannableString:
         * מאפשר לעצב חלקים מסוימים מהטקסט (צבע, קו תחתי) בתוך אותו TextView.
         */
        SpannableString spannable = new SpannableString(sb.toString());

        if (selected.getAudioURL() != null && !selected.getAudioURL().isEmpty()) {
            // עיצוב הקישור להקלטה כטקסט כחול עם קו תחתי (כמו בדפדפן)
            spannable.setSpan(new ForegroundColorSpan(Color.BLUE), startOfAudioText, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new UnderlineSpan(), startOfAudioText, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // מאזין ללחיצה על הטקסט שיפתח את הדפדפן להאזנה
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

        /**
         * Linkify: מזהה באופן אוטומטי כתובות URL בתוך הטקסט (כמו הלינקים מה-AI)
         * והופך אותן ללחיצות ללא צורך בהגדרה ידנית.
         */
        Linkify.addLinks(tvLinks, Linkify.WEB_URLS);

        btnShare.setEnabled(true);
    }

    /**
     * Compiles the summary details into a plain text message and opens
     * a system chooser to share it via external apps.
     */
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