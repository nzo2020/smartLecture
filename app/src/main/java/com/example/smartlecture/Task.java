package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

public class Task extends LearningEvent implements ISearchable {
    private boolean isCompleted;
    private int priorityScore;
    private String location;
    private long remindAt;

    private SearchManager searchManagerRef;



    // בנאי ריק חובה עבור Firebase
    public Task() {
        super();
    }

    // בנאי מלא לנוחות
    public Task(String eventID, String title, long timestamp, String userID, String location) {
        super(eventID, title, timestamp, userID);
        this.location = location;
        this.isCompleted = false;
    }

    @Override
    public List<String> getSearchableFields() {
        List<String> fields = new ArrayList<>();
        fields.add(title);
        fields.add(location);
        return fields;
    }

    @Override
    public long getDueDate() {
        return timestamp; // עבור משימה, ה-timestamp הוא דד-ליין
    }

    public void setReminder(long time) {
        this.remindAt = time;
    }

    // --- Getters & Setters ---

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public long getRemindAt() { return remindAt; }
    public void setRemindAt(long remindAt) { this.remindAt = remindAt; }
}