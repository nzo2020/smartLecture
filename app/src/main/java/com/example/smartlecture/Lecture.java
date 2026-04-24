package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

public class Lecture extends LearningEvent implements IRecordable, ISearchable {
    private String audioURL;
    private String summaryText;
    private List<String> keywords = new ArrayList<>();
    private String status;
    private boolean pub;
    private String lecturer;
    private String relevantLinks;

    public Lecture() {
        super();
    }

    public Lecture(String eventID, String title, long timestamp, String userID, String location, String lecturer, boolean pub) {
        super(eventID, title, timestamp, userID, location);
        this.lecturer = lecturer;
        this.pub = pub;
        this.status = "draft";
    }

    @Override
    public long getDueDate() {
        return timestamp;
    }

    @Override
    public void startRecording() {
        this.status = "processing";
    }

    @Override
    public List<String> getSearchableFields() {
        List<String> fields = new ArrayList<>();
        fields.add(title);
        fields.add(summaryText);
        fields.add(lecturer);
        fields.add(getLocation()); // הוספנו את המיקום לחיפוש
        if (keywords != null) fields.addAll(keywords);
        return fields;
    }

    public String getPlaybackURL() { return audioURL; }

    public void updateSummary(String newSummary) { this.summaryText = newSummary; }

    // Getters & Setters
    public String getAudioURL() { return audioURL; }
    public void setAudioURL(String audioURL) { this.audioURL = audioURL; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public String getRelevantLinks() { return relevantLinks; }
    public void setRelevantLinks(String relevantLinks) { this.relevantLinks = relevantLinks; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isPub() { return pub; }
    public void setPub(boolean pub) { this.pub = pub; }
    public String getLecturer() { return lecturer; }
    public void setLecturer(String lecturer) { this.lecturer = lecturer; }
}