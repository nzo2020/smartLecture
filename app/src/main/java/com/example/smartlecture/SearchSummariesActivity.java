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

    private Spinner spinnerLecturer;
    private EditText etSearchByTitle;
    private ListView lvSearchResults;

    private List<Lecture> allLectures;       // כל ההרצאות מה-DB
    private List<Lecture> filteredLectures;  // ההרצאות שמוצגות אחרי סינון
    private List<String> lecturersNames;    // שמות המרצים עבור ה-Spinner

    private ArrayAdapter<String> spinnerAdapter;
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_summaries);

        initViews();
        setupUserAndLoadLectures(); // שימוש במחלקת User לטעינה

        // מאזין לשינוי בטקסט החיפוש
        etSearchByTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performFiltering();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // מאזין לבחירת מרצה ב-Spinner
        spinnerLecturer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
        etSearchByTitle = findViewById(R.id.etSearchByTitle);
        lvSearchResults = findViewById(R.id.lvSearchResults);

        allLectures = new ArrayList<>();
        filteredLectures = new ArrayList<>();
        lecturersNames = new ArrayList<>();
        lecturersNames.add("All Lecturers");

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lecturersNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLecturer.setAdapter(spinnerAdapter);

        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lvSearchResults.setAdapter(listAdapter);
    }

    private void setupUserAndLoadLectures() {
        if (refAuth.getCurrentUser() != null) {
            String uid = refAuth.getCurrentUser().getUid();

            // טעינת אובייקט המשתמש מ-Firebase
            refUsers.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // שימוש בפעולה fetchEvents של מחלקת User
                        user.fetchEvents(new User.OnEventsFetchListener() {
                            @Override
                            public void onEventsFetched(List<Lecture> events) {
                                allLectures.clear();
                                allLectures.addAll(events);

                                // עדכון שמות המרצים ב-Spinner
                                updateLecturersSpinner();

                                // הרצת הסינון הראשוני להצגת הנתונים
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

    private void updateLecturersSpinner() {
        lecturersNames.clear();
        lecturersNames.add("All Lecturers");
        for (Lecture lecture : allLectures) {
            if (lecture.getLecturer() != null && !lecturersNames.contains(lecture.getLecturer())) {
                lecturersNames.add(lecture.getLecturer());
            }
        }
        spinnerAdapter.notifyDataSetChanged();
    }

    private void performFiltering() {
        String query = etSearchByTitle.getText().toString().toLowerCase().trim();
        String selectedLecturer = spinnerLecturer.getSelectedItem() != null ?
                spinnerLecturer.getSelectedItem().toString() : "All Lecturers";

        filteredLectures.clear();
        List<String> displayStrings = new ArrayList<>();

        for (Lecture lecture : allLectures) {
            boolean matchesLecturer = selectedLecturer.equals("All Lecturers") ||
                    (lecture.getLecturer() != null && lecture.getLecturer().equals(selectedLecturer));

            boolean matchesQuery = query.isEmpty();
            // שימוש בממשק ISearchable כפי שהגדרת
            if (!query.isEmpty() && lecture.getSearchableFields() != null) {
                for (String field : lecture.getSearchableFields()) {
                    if (field != null && field.toLowerCase().contains(query)) {
                        matchesQuery = true;
                        break;
                    }
                }
            }

            if (matchesLecturer && matchesQuery) {
                filteredLectures.add(lecture);
                displayStrings.add(lecture.getTitle() + " (" + lecture.getLecturer() + ")");
            }
        }

        listAdapter.clear();
        listAdapter.addAll(displayStrings);
        listAdapter.notifyDataSetChanged();
    }
}