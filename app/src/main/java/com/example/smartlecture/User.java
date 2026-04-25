package com.example.smartlecture;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userID;
    private String email;
    private String name;
    private int totalLectures;

    // רשימה פנימית השומרת את האירועים שנמשכו
    private List<Lecture> learningEvents = new ArrayList<>();

    public interface OnEventsFetchListener {
        void onEventsFetched(List<Lecture> events);
        void onError(String error);
    }

    // Constructor ריק נדרש עבור Firebase
    public User() {}

    public User(String userID, String email, String name) {
        this.userID = userID;
        this.email = email;
        this.name = name;
        this.totalLectures = 0;
    }

    /**
     * פונקציה למשיכת כל ההרצאות (ציבוריות ופרטיות)
     * מותאמת למבנה הנתונים החדש ב-Service
     */
    public void fetchEvents(final OnEventsFetchListener listener) {
        // קבלת ה-UID הנוכחי
        String currentUid = (this.userID != null && !this.userID.isEmpty()) ? this.userID :
                (FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : null);

        if (currentUid == null) {
            listener.onError("User ID is missing. Please log in again.");
            return;
        }

        // שליפת ה-DisplayName מה-Auth (זה השם שה-Service משתמש בו כתיקייה)
        String authDisplayName = "Student";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String dn = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (dn != null && !dn.isEmpty()) {
                authDisplayName = dn;
            }
        }

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        List<Lecture> myOnlyList = new ArrayList<>();

        // נתיב 1: הרצאות ציבוריות -> שם המשתמש
        DatabaseReference pubRef = db.getReference("Lectures/pub_true").child(authDisplayName);

        // נתיב 2: הרצאות פרטיות -> UID -> שם המשתמש
        DatabaseReference privRef = db.getReference("Lectures/pub_false").child(currentUid).child(authDisplayName);

        // שלב א': משיכת הרצאות ציבוריות
        pubRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot pubSnapshot) {
                addLecturesFromSnapshot(pubSnapshot, myOnlyList);

                // שלב ב': משיכת הרצאות פרטיות
                privRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot privSnapshot) {
                        addLecturesFromSnapshot(privSnapshot, myOnlyList);

                        // עדכון הרשימה המקומית והחזרת התוצאה ל-UI
                        learningEvents = myOnlyList;
                        listener.onEventsFetched(myOnlyList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onError("Private lectures fetch failed: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Public lectures fetch failed: " + error.getMessage());
            }
        });
    }

    /**
     * פונקציית עזר לעיבוד ה-Snapshot ומניעת כפילויות
     */
    private void addLecturesFromSnapshot(DataSnapshot snapshot, List<Lecture> listToFill) {
        if (!snapshot.exists()) return;

        for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
            try {
                Lecture lecture = eventSnapshot.getValue(Lecture.class);
                if (lecture != null) {
                    // הגדרת ה-ID מתוך המפתח של Firebase
                    lecture.setEventID(eventSnapshot.getKey());

                    // בדיקה אם ההרצאה כבר קיימת ברשימה (לפי ID)
                    boolean alreadyExists = false;
                    for (Lecture l : listToFill) {
                        if (l.getEventID() != null && l.getEventID().equals(lecture.getEventID())) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        listToFill.add(lecture);
                    }
                }
            } catch (Exception e) {
                Log.e("UserClass", "Error parsing lecture data", e);
            }
        }
    }

    // --- Getters & Setters ---

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTotalLectures() { return totalLectures; }
    public void setTotalLectures(int totalLectures) { this.totalLectures = totalLectures; }

    public List<Lecture> getLearningEvents() { return learningEvents; }
    public void setLearningEvents(List<Lecture> learningEvents) { this.learningEvents = learningEvents; }
}