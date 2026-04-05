package com.example.smartlecture;

// שים לב: הורדנו את java.util.Calendar כי החישוב יעבור למחלקות היורשות
public abstract class LearningEvent {

    // שימוש ב-protected מאפשר ל-Lecture ו-Task לגשת ישירות לשדות
    protected String eventID;
    protected String title;
    protected long timestamp; // שינוי ל-long עבור תאימות מלאה ל-Firebase
    protected String userID;

    // בנאי ריק חובה עבור Firebase
    public LearningEvent() {}

    public LearningEvent(String eventID, String title, long timestamp, String userID) {
        this.eventID = eventID;
        this.title = title;
        this.timestamp = timestamp;
        this.userID = userID;
    }

    public abstract long getDueDate();

    // Getters & Setters
    public String getEventID() { return eventID; }
    public void setEventID(String eventID) { this.eventID = eventID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }
}