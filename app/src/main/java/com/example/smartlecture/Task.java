package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

public class Task extends LearningEvent implements ISearchable {
    private boolean isCompleted;
    private int priorityScore;
    private long remindAt;

    public Task() {
        super();
    }

    public Task(String eventID, String title, long timestamp, String userID, String location) {
        super(eventID, title, timestamp, userID, location);
        this.isCompleted = false;
    }

    @Override
    public List<String> getSearchableFields() {
        List<String> fields = new ArrayList<>();
        fields.add(title);
        fields.add(getLocation()); // הוספנו את המיקום לחיפוש
        return fields;
    }

    @Override
    public long getDueDate() {
        return timestamp;
    }

    public void setReminder(long time) {
        this.remindAt = time;
    }

    // Getters & Setters
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }
    public long getRemindAt() { return remindAt; }
    public void setRemindAt(long remindAt) { this.remindAt = remindAt; }
    public long getReminder() { return remindAt; }
}