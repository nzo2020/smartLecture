package com.example.smartlecture;

public abstract class LearningEvent {

    protected String eventID;
    protected String title;
    protected long timestamp;
    protected String userID;
    protected String location;

    // בנאי ריק חובה עבור Firebase
    public LearningEvent() {}

    public LearningEvent(String eventID, String title, long timestamp, String userID, String location) {
        this.eventID = eventID;
        this.title = title;
        this.timestamp = timestamp;
        this.userID = userID;
        this.location = location;
    }
    //זה אומר שהמחלקה הנוכחית רק מבטיחה שכל "משימה" או "אירוע" (או מה שהמחלקה שלך מייצגת) חייב שיהיה לו תאריך יעד, אבל כל סוג של משימה יחשב את התאריך הזה בצורה שונה.
    public abstract long getDueDate();

    // Getters & Setters
    public String getEventID() { return eventID; }
    public void setEventID(String eventID) { this.eventID = eventID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // תיקון ה-Getter: עכשיו מחזיר את המיקום ולא את ה-ID
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }
}