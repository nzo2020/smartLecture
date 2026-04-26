package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;
import static com.example.smartlecture.FBRef.refUsers;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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

    // רכיבי ממשק המשתמש
    private Spinner spinnerLecturer, spinnerTitle;
    private EditText etFreeSearch;
    private ListView lvSearchResults;

    // מבני נתונים לניהול הרשימות
    private List<Lecture> allLectures;           // כל ההרצאות שנטענו מה-Firebase
    private List<Lecture> currentFilteredList;   // ההרצאות שמוצגות כרגע לאחר סינון
    private List<String> lecturersNames, lectureTitles; // נתונים עבור התיבות הנפתחות (Spinners)

    // אדפטרים לקישור הנתונים לרכיבי התצוגה
    private ArrayAdapter<String> lecturerAdapter, titleAdapter, listAdapter;
    private SearchManager searchManager; // המערכת המרכזית לביצוע חיפוש פולימורפי

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_summaries);

        initViews();                  // אתחול רכיבי ה-UI
        setupUserAndLoadLectures();   // טעינת הנתונים מהענן
        setupListeners();             // הגדרת מאזינים לשינויים בחיפוש
    }

    private void initViews() {
        spinnerLecturer = findViewById(R.id.spinnerLecturer);
        spinnerTitle = findViewById(R.id.spinnerTitle);
        etFreeSearch = findViewById(R.id.etFreeSearch);
        lvSearchResults = findViewById(R.id.lvSearchResults);

        allLectures = new ArrayList<>();
        currentFilteredList = new ArrayList<>();

        // הגדרת ה-Spinner למרצים עם ערך ברירת מחדל "כל המרצים"
        lecturersNames = new ArrayList<>();
        lecturersNames.add("All Lecturers");
        lecturerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lecturersNames);
        lecturerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLecturer.setAdapter(lecturerAdapter);

        // הגדרת ה-Spinner לכותרות עם ערך ברירת מחדל "כל הכותרות"
        lectureTitles = new ArrayList<>();
        lectureTitles.add("All Titles");
        titleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lectureTitles);
        titleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTitle.setAdapter(titleAdapter);

        // אתחול ה-ListView שבו יוצגו תוצאות החיפוש
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvSearchResults.setAdapter(listAdapter);
    }


    private void setupListeners() {
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFiltering(); // הפעלת סינון בעת בחירה ב-Spinner
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerLecturer.setOnItemSelectedListener(filterListener);
        spinnerTitle.setOnItemSelectedListener(filterListener);

        // מאזין לשינויים בזמן אמת בתיבת החיפוש החופשי
        etFreeSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performFiltering(); // סינון מחדש בכל הקלדה של תו
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());
    }

    private void setupUserAndLoadLectures() {
        if (refAuth.getCurrentUser() != null) {
            String uid = refAuth.getCurrentUser().getUid();
            refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        user.setUserID(uid); // הגדרת מזהה המשתמש לצורך השאילתה

                        // הפעלת פונקציית הטעינה במחלקת User
                        user.fetchEvents(new User.OnEventsFetchListener() {
                            @Override
                            public void onEventsFetched(List<Lecture> events) {
                                allLectures.clear();
                                allLectures.addAll(events);

                                // פולימורפיזם: המרת רשימת ה-Lecture לרשימת ISearchable עבור ה-SearchManager
                                List<ISearchable> searchables = new ArrayList<>(allLectures);
                                searchManager = new SearchManager(searchables);

                                updateSpinnersData(); // עדכון רשימת המרצים והכותרות בתיבות הבחירה
                                performFiltering();   // ביצוע סינון ראשוני להצגת הכל
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(SearchSummariesActivity.this, "Error loading: " + error, Toast.LENGTH_SHORT).show();
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

        for (Lecture l : allLectures) {
            if (l.getLecturer() != null && !lecturersNames.contains(l.getLecturer())) {
                lecturersNames.add(l.getLecturer());
            }
            if (l.getTitle() != null && !lectureTitles.contains(l.getTitle())) {
                lectureTitles.add(l.getTitle());
            }
        }
        lecturerAdapter.notifyDataSetChanged();
        titleAdapter.notifyDataSetChanged();
    }

    private void performFiltering() {
        if (searchManager == null) return;

        String query = etFreeSearch.getText().toString().trim();
        String selLecturer = spinnerLecturer.getSelectedItem().toString();
        String selTitle = spinnerTitle.getSelectedItem().toString();

        // שלב 1: סינון לפי מחרוזת החיפוש (חיפוש בכל השדות של ההרצאה)
        List<ISearchable> searchResults = searchManager.search(query);

        currentFilteredList.clear();
        List<String> displayStrings = new ArrayList<>();

        // שלב 2: מעבר על תוצאות החיפוש ובדיקה אם הן עומדות בקריטריונים של ה-Spinners
        for (ISearchable item : searchResults) {
            Lecture lecture = (Lecture) item; // Casting חזרה לסוג Lecture

            // בדיקה אם המרצה והכותרת תואמים לבחירה (או שביקשו "הכל")
            boolean matchesLecturer = selLecturer.equals("All Lecturers") ||
                    selLecturer.equals(lecture.getLecturer());
            boolean matchesTitle = selTitle.equals("All Titles") ||
                    selTitle.equals(lecture.getTitle());

            if (matchesLecturer && matchesTitle) {
                currentFilteredList.add(lecture);
                // יצירת מחרוזת מעוצבת לתצוגה ברשימה
                displayStrings.add("📖 Title: " + lecture.getTitle() + "\n👨‍🏫 Lecturer: " + lecture.getLecturer());
            }
        }

        // עדכון האדפטר והצגת התוצאות הסופיות למשתמש
        listAdapter.clear();
        listAdapter.addAll(displayStrings);
        listAdapter.notifyDataSetChanged();
    }
}