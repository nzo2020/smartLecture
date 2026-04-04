package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String userID;
    private String email;
    private String name;
    private int totalLectures;
    private int completedTasks;

    // אתחול הרשימה כדי למנוע NullPointerException
    private List<LearningEvent> learningEvents = new ArrayList<>();

    // בנאי ריק - חובה עבור Firebase
    public User() {}

    // בנאי מלא (מקל על יצירת משתמש חדש בהרשמה)
    public User(String userID, String email, String name) {
        this.userID = userID;
        this.email = email;
        this.name = name;
        this.totalLectures = 0;
        this.completedTasks = 0;
    }

    // פעולת העזר שציינת ב-UML
    public void fetchEvents() {
        // הערה: Firebase עובד בצורה אסינכרונית.
        // הקוד כאן בד"כ יכלול מאזין (ValueEventListener)
    }

    // Getters ו-Setters חיוניים ל-Firebase
    // בלעדיהם Firebase לא ידע "להזריק" את הנתונים לשדות

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

    public List<LearningEvent> getLearningEvents() { return learningEvents; }
    public void setLearningEvents(List<LearningEvent> learningEvents) { this.learningEvents = learningEvents; }
}