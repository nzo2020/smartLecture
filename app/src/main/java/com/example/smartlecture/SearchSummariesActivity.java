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

    private Spinner spinnerLecturer, spinnerTitle;
    private EditText etFreeSearch; // הבונוס: שורת חיפוש חופשי
    private ListView lvSearchResults;

    private List<Lecture> allLectures;
    private List<Lecture> currentFilteredList;
    private List<String> lecturersNames, lectureTitles;

    private ArrayAdapter<String> lecturerAdapter, titleAdapter, listAdapter;
    private SearchManager searchManager; // שימוש ב-SearchManager מה-UML

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_summaries);

        initViews();
        setupUserAndLoadLectures();
        setupListeners();
    }

    private void initViews() {
        spinnerLecturer = findViewById(R.id.spinnerLecturer);
        spinnerTitle = findViewById(R.id.spinnerTitle);
        etFreeSearch = findViewById(R.id.etFreeSearch); // ודאי שיש לך ID כזה ב-XML
        lvSearchResults = findViewById(R.id.lvSearchResults);

        allLectures = new ArrayList<>();
        currentFilteredList = new ArrayList<>();

        // אתחול ספינרים
        lecturersNames = new ArrayList<>();
        lecturersNames.add("All Lecturers");
        lecturerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lecturersNames);
        spinnerLecturer.setAdapter(lecturerAdapter);

        lectureTitles = new ArrayList<>();
        lectureTitles.add("All Titles");
        titleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lectureTitles);
        spinnerTitle.setAdapter(titleAdapter);

        // אתחול רשימת תוצאות
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvSearchResults.setAdapter(listAdapter);
    }

    private void setupListeners() {
        // מאזינים לספינרים
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFiltering();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerLecturer.setOnItemSelectedListener(filterListener);
        spinnerTitle.setOnItemSelectedListener(filterListener);

        // הבונוס: מאזין להקלדה בשורת החיפוש
        etFreeSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performFiltering(); // מסנן בזמן אמת תוך כדי הקלדה
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
                        user.fetchEvents(new User.OnEventsFetchListener() {
                            @Override
                            public void onEventsFetched(List<Lecture> events) {
                                allLectures.clear();
                                allLectures.addAll(events);
                                searchManager = new SearchManager(allLectures); // אתחול ה-Manager
                                updateSpinnersData();
                                performFiltering();
                            }
                            @Override
                            public void onError(String error) {
                                Toast.makeText(SearchSummariesActivity.this, error, Toast.LENGTH_SHORT).show();
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
        lecturersNames.clear(); lecturersNames.add("All Lecturers");
        lectureTitles.clear(); lectureTitles.add("All Titles");

        for (Lecture l : allLectures) {
            if (l.getLecturer() != null && !lecturersNames.contains(l.getLecturer()))
                lecturersNames.add(l.getLecturer());
            if (l.getTitle() != null && !lectureTitles.contains(l.getTitle()))
                lectureTitles.add(l.getTitle());
        }
        lecturerAdapter.notifyDataSetChanged();
        titleAdapter.notifyDataSetChanged();
    }

    private void performFiltering() {
        if (searchManager == null) return;

        String query = etFreeSearch.getText().toString().trim();
        String selLecturer = spinnerLecturer.getSelectedItem().toString();
        String selTitle = spinnerTitle.getSelectedItem().toString();

        // 1. שלב ראשון: חיפוש חופשי בעזרת ה-SearchManager (מחפש בסיכום, בכותרת ובמרצה)
        List<ISearchable> searchResults = searchManager.search(query);

        currentFilteredList.clear();
        List<String> displayStrings = new ArrayList<>();

        // 2. שלב שני: הצלבה עם הבחירות בספינרים
        for (ISearchable item : searchResults) {
            Lecture lecture = (Lecture) item;

            boolean matchesLecturer = selLecturer.equals("All Lecturers") ||
                    selLecturer.equals(lecture.getLecturer());
            boolean matchesTitle = selTitle.equals("All Titles") ||
                    selTitle.equals(lecture.getTitle());

            if (matchesLecturer && matchesTitle) {
                currentFilteredList.add(lecture);
                displayStrings.add("📖 " + lecture.getTitle() + "\n👨‍🏫 " + lecture.getLecturer());
            }
        }

        listAdapter.clear();
        listAdapter.addAll(displayStrings);
        listAdapter.notifyDataSetChanged();
    }
}