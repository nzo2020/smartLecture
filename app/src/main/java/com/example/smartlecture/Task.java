package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a specific task or reminder derived from a learning session.
 * This class extends {@link LearningEvent} and implements {@link ISearchable},
 * adding task-specific features such as completion status, priority scoring,
 * and scheduled reminder timestamps.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class Task extends LearningEvent implements ISearchable {

    // שדות ייחודיים למשימה
    private boolean isCompleted;   // האם המשימה בוצעה
    private int priorityScore;    // דירוג עדיפות (למשל עבור ה-AI או המשתמש)
    private long remindAt;        // הזמן המדויק בו תוקפץ התזכורת (Timestamp)

    /**
     * Default constructor required for Firebase data deserialization.
     */
    // בנאי ריק הנדרש עבור Firebase לצורך המרת הנתונים לאובייקט (Deserialization)
    public Task() {
        super();
    }

    /**
     * Constructs a new Task with basic event details.
     * @param eventID Unique identifier for the event.
     * @param title The name of the task.
     * @param timestamp The time the task was created or is due.
     * @param userID The owner of the task.
     * @param location Associated physical location.
     */
    // בנאי המאתחל משימה חדשה עם נתוני בסיס
    public Task(String eventID, String title, long timestamp, String userID, String location) {
        // קריאה לבנאי של מחלקת האם (LearningEvent)
        super(eventID, title, timestamp, userID, location);
        this.isCompleted = false; // כברירת מחדל, משימה חדשה אינה מסומנת כבוצעה
    }

    /**
     * Returns the fields available for the search engine.
     * @return A list containing the title and location of the task.
     */
    @Override
    public List<String> getSearchableFields() {
        List<String> fields = new ArrayList<>();
        fields.add(title);         // חיפוש לפי שם המשימה
        fields.add(getLocation()); // חיפוש לפי המיקום שבו נוצרה המשימה
        return fields;
    }

    /**
     * Implementation of the abstract method from LearningEvent.
     * @return The timestamp representing the task's due date.
     */
    @Override
    public long getDueDate() {
        return timestamp;
    }

    /**
     * Sets the exact time the system alarm should trigger for this task.
     * @param time Timestamp in milliseconds.
     */
    // הגדרת זמן התזכורת
    public void setReminder(long time) {
        this.remindAt = time;
    }

    // --- Getters & Setters ---
    // פונקציות אלו מאפשרות גישה ועדכון של השדות הפרטיים (Encapsulation)

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public int getPriorityScore() { return priorityScore; }
    public void setPriorityScore(int priorityScore) { this.priorityScore = priorityScore; }

    public long getRemindAt() { return remindAt; }
    public void setRemindAt(long remindAt) { this.remindAt = remindAt; }

    public long getReminder() { return remindAt; }
}