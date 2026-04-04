package com.example.smartlecture;

import static com.example.smartlecture.FBRef.refAuth;

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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ViewSummeryActivity extends AppCompatActivity {

    private Spinner spSummarySelector;
    private TextView tvSummaryContent, tvLinks;
    private MaterialButton btnShare, btnBackHome;

    private List<Lecture> lectureList; // רשימה של אובייקטי ההרצאה
    private List<String> lectureTitles; // רשימת הכותרות להצגה ב-Spinner
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_summery); // וודאי שזה שם ה-XML שלך

        initViews();
        loadLecturesFromFirebase();

        btnBackHome.setOnClickListener(v -> finish());

        // מאזין לבחירה ב-Spinner
        spSummarySelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                displayLectureDetails(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // כפתור שיתוף
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

        // הגדרת האדפטר ל-Spinner
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, lectureTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSummarySelector.setAdapter(adapter);
    }

    private void loadLecturesFromFirebase() {
        String uid = refAuth.getCurrentUser().getUid();
        // נתיב להרצאות הפרטיות של המשתמש
        DatabaseReference refPrivate = FirebaseDatabase.getInstance().getReference("Lectures/pub_false/" + uid);

        refPrivate.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lectureList.clear();
                lectureTitles.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Lecture lecture = data.getValue(Lecture.class);
                    if (lecture != null) {
                        lectureList.add(lecture);
                        // מציג ב-Spinner את כותרת ההרצאה או שם המרצה
                        lectureTitles.add(lecture.getTitle() != null ? lecture.getTitle() : lecture.getLecturer());
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewSummeryActivity.this, "Error loading data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayLectureDetails(int position) {
        if (lectureList.isEmpty()) return;

        Lecture selected = lectureList.get(position);
        tvSummaryContent.setText(selected.getSummaryText());

        // הצגת לינק להקלטה אם קיים
        if (selected.getAudioURL() != null) {
            tvLinks.setText("Audio Record: " + selected.getAudioURL());
        } else {
            tvLinks.setText("No recording available");
        }
    }

    private void shareSummary() {
        String content = tvSummaryContent.getText().toString();
        if (content.isEmpty() || content.startsWith("Summary content")) {
            Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this lecture summary: \n\n" + content);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }
}