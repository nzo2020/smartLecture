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
    private String userID;      // מזהה ייחודי מ-Firebase Auth
    private String email;       // כתובת המייל של המשתמש
    private String name;        // השם המלא של המשתמש
    private int totalLectures;  // מונה כמות הרצאות (אופציונלי)

    // רשימה פנימית השומרת את האירועים שנמשכו מהענן לצורך גישה מהירה
    private List<Lecture> learningEvents = new ArrayList<>();

    public interface OnEventsFetchListener {
        void onEventsFetched(List<Lecture> events);
        void onError(String error);
    }

    // Constructor ריק נדרש עבור Firebase לצורך המרת הנתונים לאובייקט (Deserialization)
    public User() {}

    // בנאי ליצירת משתמש חדש
    public User(String userID, String email, String name) {
        this.userID = userID;
        this.email = email;
        this.name = name;
        this.totalLectures = 0;
    }

    /**
     * פונקציה מורכבת למשיכת כל ההרצאות.
     * הלוגיקה כאן מחברת בין שני מקומות שונים ב-Database:
     * 1. Lectures/pub_true (הרצאות שכולם יכולים לראות)
     * 2. Lectures/pub_false (הרצאות אישיות של המשתמש)
     */
    public void fetchEvents(final OnEventsFetchListener listener) {
        // וידוא שיש לנו UID עבודה - או מהאובייקט או ישירות מה-Auth
        String currentUid = (this.userID != null && !this.userID.isEmpty()) ? this.userID :
                (FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : null);

        if (currentUid == null) {
            listener.onError("User ID is missing. Please log in again.");
            return;
        }

        // שליפת ה-DisplayName. השירות (Service) שומר הרצאות תחת שם המשתמש בתיקייה הסופית.
        String authDisplayName = "Student";
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String dn = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            if (dn != null && !dn.isEmpty()) {
                authDisplayName = dn;
            }
        }

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        List<Lecture> myOnlyList = new ArrayList<>();

        // הגדרת הנתיבים ב-Database לפי המבנה שקבעת ב-Service
        // נתיב 1: ציבורי -> תחת שם המשתמש
        DatabaseReference pubRef = db.getReference("Lectures/pub_true").child(authDisplayName);

        DatabaseReference privRef = db.getReference("Lectures/pub_false").child(currentUid).child(authDisplayName);


        pubRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot pubSnapshot) {
                addLecturesFromSnapshot(pubSnapshot, myOnlyList);


                privRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot privSnapshot) {
                        addLecturesFromSnapshot(privSnapshot, myOnlyList);

                        // סיום התהליך: עדכון הרשימה המקומית וקריאה ל-Listener לעדכון המסך
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


    private void addLecturesFromSnapshot(DataSnapshot snapshot, List<Lecture> listToFill) {
        if (!snapshot.exists()) return;

        for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
            try {
                // המרת ה-JSON מהענן לאובייקט Java מסוג Lecture
                Lecture lecture = eventSnapshot.getValue(Lecture.class);
                if (lecture != null) {
                    // שמירת המפתח (Key) של Firebase בתור ה-ID של ההרצאה
                    lecture.setEventID(eventSnapshot.getKey());

                    // מנגנון מניעת כפילויות: בודק אם הרצאה עם אותו ID כבר קיימת ברשימה
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
    // מאפשרים גישה מבוקרת למשתני המחלקה (Encapsulation)

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