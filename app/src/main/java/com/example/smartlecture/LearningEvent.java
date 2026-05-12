package com.example.smartlecture;

/**
 * Abstract base class representing a generic learning event within the system.
 * This class serves as a blueprint for specific event types (like Lectures or Tasks),
 * centralizing common properties such as ID, title, and location.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public abstract class LearningEvent {

    /** Unique identifier for the event */
    protected String eventID;
    /** The title or name of the event */
    protected String title;
    /** The exact time the event occurred or was created (in milliseconds) */
    protected long timestamp;
    /** The ID of the user associated with this event */
    protected String userID;
    /** The physical address or place where the event is associated */
    protected String location;

    /**
     * Default constructor required for Firebase data mapping (deserialization).
     */
    // בנאי ריק חובה עבור Firebase
    public LearningEvent() {}

    /**
     * Constructs a new LearningEvent with specified details.
     * @param eventID Unique ID of the event.
     * @param title Title of the event.
     * @param timestamp Time of the event.
     * @param userID Associated User ID.
     * @param location Event location.
     */
    public LearningEvent(String eventID, String title, long timestamp, String userID, String location) {
        this.eventID = eventID;
        this.title = title;
        this.timestamp = timestamp;
        this.userID = userID;
        this.location = location;
    }

    /**
     * Abstract method to be implemented by subclasses to define the specific due date logic.
     * @return The due date of the specific event type as a long timestamp.
     */
    //זה אומר שהמחלקה הנוכחית רק מבטיחה שכל "משימה" או "אירוע" (או מה שהמחלקה שלך מייצגת) חייב שיהיה לו תאריך יעד, אבל כל סוג של משימה יחשב את התאריך הזה בצורה שונה.
    public abstract long getDueDate();

    // Getters & Setters
    public String getEventID() { return eventID; }
    public void setEventID(String eventID) { this.eventID = eventID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    /**
     * Gets the location of the event.
     * @return The location string.
     */
    // תיקון ה-Getter: עכשיו מחזיר את המיקום ולא את ה-ID
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getUserID() { return userID; }
    public void setUserID(String userID) { this.userID = userID; }
}