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

/**
 * Represents a user in the SmartLecture system.
 * This class manages user profile data and handles the complex logic of fetching
 * and merging lecture events from both public and private Firebase database paths.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class User {
    private String userID;      // מזהה ייחודי מ-Firebase Auth
    private String email;       // כתובת המייל של המשתמש
    private String name;        // השם המלא של המשתמש
    private int totalLectures;  // מונה כמות הרצאות (אופציונלי)

    // רשימה פנימית השומרת את האירועים שנמשכו מהענן לצורך גישה מהירה
    private List<Lecture> learningEvents = new ArrayList<>();

    /**
     * Callback interface for asynchronous lecture fetching operations.
     */
    public interface OnEventsFetchListener {
        void onEventsFetched(List<Lecture> events);
        void onError(String error);
    }

    /**
     * Default constructor required for Firebase data deserialization.
     */
    // Constructor ריק נדרש עבור Firebase לצורך המרת הנתונים לאובייקט (Deserialization)
    public User() {}

    /**
     * Constructs a new User profile.
     * @param userID Unique identifier from Firebase Auth.
     * @param email User's registered email.
     * @param name User's full name.
     */
    // בנאי ליצירת משתמש חדש
    public User(String userID, String email, String name) {
        this.userID = userID;
        this.email = email;
        this.name = name;
        this.totalLectures = 0;
    }

    /**
     * Fetches all relevant lectures for the user by performing a nested query:
     * 1. Pulls global public lectures from "Lectures/pub_true".
     * 2. Pulls user-specific private lectures from "Lectures/pub_false".
     * The results are merged into a single list while preventing duplicates.
     * * @param listener The listener that will receive the merged list or error message.
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

    /**
     * Helper method to iterate through a DataSnapshot and convert JSON objects to Lecture instances.
     * Implements a check to ensure no duplicate lectures (based on ID) are added to the list.
     * * @param snapshot The Firebase data snapshot to parse.
     * @param listToFill The target list where validated lectures will be added.
     */
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