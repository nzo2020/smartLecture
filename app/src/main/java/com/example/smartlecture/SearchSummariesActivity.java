package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;
import static com.example.smartlecture.FBRef.refUsers;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchSummariesActivity extends AppCompatActivity {

    private Spinner spinnerLecturer, spinnerTitle;
    private ListView lvSearchResults;

    private List<Lecture> allLectures;       // כל ההרצאות שהגיעו מה-DB
    private List<Lecture> filteredLectures;  // רשימת האובייקטים המסוננת
    private List<String> lecturersNames;     // רשימה לספינר מרצים
    private List<String> lectureTitles;      // רשימה לספינר כותרות

    private ArrayAdapter<String> lecturerAdapter;
    private ArrayAdapter<String> titleAdapter;
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_summaries);

        initViews();
        setupUserAndLoadLectures();

        // מאזין לבחירת מרצה
        spinnerLecturer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFiltering();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // מאזין לבחירת כותרת
        spinnerTitle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFiltering();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());
    }

    private void initViews() {
        spinnerLecturer = findViewById(R.id.spinnerLecturer);
        spinnerTitle = findViewById(R.id.spinnerTitle);
        lvSearchResults = findViewById(R.id.lvSearchResults);

        allLectures = new ArrayList<>();
        filteredLectures = new ArrayList<>();

        // אתחול ספינר מרצים
        lecturersNames = new ArrayList<>();
        lecturersNames.add("All Lecturers");
        lecturerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lecturersNames);
        lecturerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLecturer.setAdapter(lecturerAdapter);

        // אתחול ספינר כותרות
        lectureTitles = new ArrayList<>();
        lectureTitles.add("All Titles");
        titleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lectureTitles);
        titleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTitle.setAdapter(titleAdapter);

        // אתחול רשימת התוצאות
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvSearchResults.setAdapter(listAdapter);
    }

    private void setupUserAndLoadLectures() {
        if (refAuth.getCurrentUser() != null) {
            String uid = refAuth.getCurrentUser().getUid();

            // שלב 1: טעינת המשתמש
            refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // שלב 2: שליפת כל ההרצאות (משתמש ב-fetchEvents שתיקנו ב-User.java)
                        user.fetchEvents(new User.OnEventsFetchListener() {
                            @Override
                            public void onEventsFetched(List<Lecture> events) {
                                allLectures.clear();
                                allLectures.addAll(events);

                                // שלב 3: עדכון הרשימות בספינרים לפי הנתונים שהגיעו
                                updateSpinnersData();

                                // הצגה ראשונית
                                performFiltering();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(SearchSummariesActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void updateSpinnersData() {
        lecturersNames.clear();
        lecturersNames.add("All Lecturers");

        lectureTitles.clear();
        lectureTitles.add("All Titles");

        for (Lecture lecture : allLectures) {
            // הוספת שם מרצה (בדיקה שלא כפול)
            String lName = lecture.getLecturer();
            if (lName != null && !lName.isEmpty() && !lecturersNames.contains(lName)) {
                lecturersNames.add(lName);
            }

            // הוספת כותרת שיעור (בדיקה שלא כפול)
            String lTitle = lecture.getTitle();
            if (lTitle != null && !lTitle.isEmpty() && !lectureTitles.contains(lTitle)) {
                lectureTitles.add(lTitle);
            }
        }

        // עדכון האדפטרים שהנתונים השתנו
        lecturerAdapter.notifyDataSetChanged();
        titleAdapter.notifyDataSetChanged();
    }

    private void performFiltering() {
        String selectedLecturer = spinnerLecturer.getSelectedItem() != null ?
                spinnerLecturer.getSelectedItem().toString() : "All Lecturers";

        String selectedTitle = spinnerTitle.getSelectedItem() != null ?
                spinnerTitle.getSelectedItem().toString() : "All Titles";

        filteredLectures.clear();
        List<String> displayStrings = new ArrayList<>();

        for (Lecture lecture : allLectures) {
            // בדיקת התאמה למרצה
            boolean matchesLecturer = selectedLecturer.equals("All Lecturers") ||
                    (lecture.getLecturer() != null && lecture.getLecturer().equals(selectedLecturer));

            // בדיקת התאמה לכותרת
            boolean matchesTitle = selectedTitle.equals("All Titles") ||
                    (lecture.getTitle() != null && lecture.getTitle().equals(selectedTitle));

            // אם שניהם מתאימים - הוסף לרשימה התצוגה
            if (matchesLecturer && matchesTitle) {
                filteredLectures.add(lecture);
                displayStrings.add(lecture.getTitle() + " — " + lecture.getLecturer());
            }
        }

        // עדכון ה-ListView
        listAdapter.clear();
        listAdapter.addAll(displayStrings);
        listAdapter.notifyDataSetChanged();

        if (displayStrings.isEmpty() && !allLectures.isEmpty()) {
            Toast.makeText(this, "No summaries match these criteria", Toast.LENGTH_SHORT).show();
        }
    }
}