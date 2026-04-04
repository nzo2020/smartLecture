package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

public class Lecture extends LearningEvent implements IRecordable, ISearchable {
    private String audioURL;
    private String summaryText;
    private List<String> keywords = new ArrayList<>(); // אתחול למניעת NullPointerException
    private String status; // ready / processing / draft
    private boolean pub; // האם ציבורי
    private String lecturer;

    // בנאי ריק חובה עבור Firebase
    public Lecture() {
        super();
    }

    // בנאי מלא לנוחות ביצירת אובייקט חדש
    public Lecture(String eventID, String title, long timestamp, String userID, String lecturer, boolean pub) {
        super(eventID, title, timestamp, userID);
        this.lecturer = lecturer;
        this.pub = pub;
        this.status = "draft";
    }

    // מימוש הפעולה מהמחלקה המופשטת LearningEvent
    @Override
    public long getDueDate() {
        return timestamp; // בהרצאה, מועד האירוע הוא זמן השיעור עצמו
    }

    // מימוש הממשק IRecordable
    @Override
    public void startRecording() {
        // כאן תבוא הלוגיקה של MediaRecorder
    }

    // מימוש הממשק ISearchable
    @Override
    public List<String> getSearchableFields() {
        List<String> fields = new ArrayList<>();
        fields.add(title);
        fields.add(summaryText);
        fields.add(lecturer);
        if (keywords != null) fields.addAll(keywords);
        return fields;
    }

    // פעולות נוספות מה-UML
    public String getPlaybackURL() {
        return audioURL;
    }

    public void updateSummary(String newSummary) {
        this.summaryText = newSummary;
    }

    // --- Getters & Setters חיוניים ל-Firebase ---

    public String getAudioURL() { return audioURL; }
    public void setAudioURL(String audioURL) { this.audioURL = audioURL; }

    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPub() { return pub; }
    public void setPub(boolean pub) { this.pub = pub; }

    public String getLecturer() { return lecturer; }
    public void setLecturer(String lecturer) { this.lecturer = lecturer; }
}