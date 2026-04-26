package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;


public class Task extends LearningEvent implements ISearchable {

    // שדות ייחודיים למשימה
    private boolean isCompleted;   // האם המשימה בוצעה
    private int priorityScore;    // דירוג עדיפות (למשל עבור ה-AI או המשתמש)
    private long remindAt;        // הזמן המדויק בו תוקפץ התזכורת (Timestamp)

    // בנאי ריק הנדרש עבור Firebase לצורך המרת הנתונים לאובייקט (Deserialization)
    public Task() {
        super();
    }

    // בנאי המאתחל משימה חדשה עם נתוני בסיס
    public Task(String eventID, String title, long timestamp, String userID, String location) {
        // קריאה לבנאי של מחלקת האם (LearningEvent)
        super(eventID, title, timestamp, userID, location);
        this.isCompleted = false; // כברירת מחדל, משימה חדשה אינה מסומנת כבוצעה
    }

    @Override
    public List<String> getSearchableFields() {
        List<String> fields = new ArrayList<>();
        fields.add(title);         // חיפוש לפי שם המשימה
        fields.add(getLocation()); // חיפוש לפי המיקום שבו נוצרה המשימה
        return fields;
    }

    @Override
    public long getDueDate() {
        return timestamp;
    }

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