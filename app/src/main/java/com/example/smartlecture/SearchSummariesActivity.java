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
    private EditText etFreeSearch;
    private ListView lvSearchResults;

    private List<Lecture> allLectures;
    private List<Lecture> currentFilteredList;
    private List<String> lecturersNames, lectureTitles;

    private ArrayAdapter<String> lecturerAdapter, titleAdapter, listAdapter;
    private SearchManager searchManager;

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
        etFreeSearch = findViewById(R.id.etFreeSearch);
        lvSearchResults = findViewById(R.id.lvSearchResults);

        allLectures = new ArrayList<>();
        currentFilteredList = new ArrayList<>();

        // אתחול רשימות לספינרים
        lecturersNames = new ArrayList<>();
        lecturersNames.add("All Lecturers");
        lecturerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lecturersNames);
        lecturerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLecturer.setAdapter(lecturerAdapter);

        lectureTitles = new ArrayList<>();
        lectureTitles.add("All Titles");
        titleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lectureTitles);
        titleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTitle.setAdapter(titleAdapter);

        // אתחול האדפטר לרשימת התוצאות
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvSearchResults.setAdapter(listAdapter);
    }

    private void setupListeners() {
        // האזנה לשינויים בספינרים
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

        // האזנה להקלדה בשורת החיפוש
        etFreeSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performFiltering();
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

                                // אתחול מנהל החיפוש עם רשימת ההרצאות שנטענו
                                searchManager = new SearchManager(allLectures);

                                updateSpinnersData();
                                performFiltering();
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

        // סינון ראשוני באמצעות ה-SearchManager (חיפוש טקסט חופשי)
        List<ISearchable> searchResults = searchManager.search(query);

        currentFilteredList.clear();
        List<String> displayStrings = new ArrayList<>();

        // הצלבת תוצאות החיפוש עם בחירות הספינרים
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

        // עדכון הרשימה המוצגת למשתמש
        listAdapter.clear();
        listAdapter.addAll(displayStrings);
        listAdapter.notifyDataSetChanged();
    }
}