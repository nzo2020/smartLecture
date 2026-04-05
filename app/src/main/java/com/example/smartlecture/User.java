package com.example.smartlecture;

import androidx.annotation.NonNull;
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
    private int completedTasks;

    private List<Lecture> learningEvents = new ArrayList<>();

    public interface OnEventsFetchListener {
        void onEventsFetched(List<Lecture> events);
        void onError(String error);
    }

    public User() {}

    public User(String userID, String email, String name) {
        this.userID = userID;
        this.email = email;
        this.name = name;
        this.totalLectures = 0;
        this.completedTasks = 0;
    }

    public void fetchEvents(OnEventsFetchListener listener) {
        if (userID == null || userID.isEmpty()) {
            if (listener != null) listener.onError("User ID is missing");
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Lectures/pub_false/" + userID);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                learningEvents.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Lecture lecture = data.getValue(Lecture.class);
                    if (lecture != null) {
                        learningEvents.add(lecture);
                    }
                }
                totalLectures = learningEvents.size();

                if (listener != null) {
                    listener.onEventsFetched(learningEvents);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onError(error.getMessage());
                }
            }
        });
    }


    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTotalLectures() { return totalLectures; }
    public void setTotalLectures(int totalLectures) { this.totalLectures = totalLectures; }

    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }

    public List<Lecture> getLearningEvents() { return learningEvents; }
    public void setLearningEvents(List<Lecture> learningEvents) { this.learningEvents = learningEvents; }
}