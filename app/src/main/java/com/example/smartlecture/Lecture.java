package com.example.smartlecture;

import java.util.ArrayList;
import java.util.List;

/**
 * represents a specific lecture event in the system.
 * This class extends {@link LearningEvent} and implements recording and searching capabilities.
 * It stores lecture-specific data such as audio URLs, generated summaries, and keywords.
 * @author Noa Zohar(nz2020@bs.amalnet.k12.il)
 * @version 1.0
 * @since 22.1.2026
 */
public class Lecture extends LearningEvent implements IRecordable, ISearchable {
    /** URL to the stored audio file in Firebase Storage */
    private String audioURL;
    /** The AI-generated summary text of the lecture */
    private String summaryText;
    /** A list of key terms extracted from the lecture content */
    private List<String> keywords = new ArrayList<>();
    /** Current state of the lecture (e.g., "draft", "processing", "completed") */
    private String status;
    /** Privacy flag: true if the lecture is public, false if private */
    private boolean pub;
    /** The name of the person delivering the lecture */
    private String lecturer;
    /** Optional external links related to the lecture topic */
    private String relevantLinks;

    /**
     * Default constructor required for Firebase data mapping.
     */
    public Lecture() {
        super();
    }

    /**
     * Constructs a new Lecture with essential details.
     * @param eventID Unique identifier.
     * @param title Lecture title.
     * @param timestamp Occurrence time.
     * @param userID Owner ID.
     * @param location Where the lecture took place.
     * @param lecturer The name of the speaker.
     * @param pub Publication status.
     */
    public Lecture(String eventID, String title, long timestamp, String userID, String location, String lecturer, boolean pub) {
        super(eventID, title, timestamp, userID, location);
        this.lecturer = lecturer;
        this.pub = pub;
        this.status = "draft";
    }

    /**
     * Implementation of getDueDate from LearningEvent.
     * @return The lecture timestamp as the due date.
     */
    @Override
    public long getDueDate() {
        return timestamp;
    }

    /**
     * Implementation of IRecordable. Updates the status to indicate recording is in progress.
     */
    @Override
    public void startRecording() {
        this.status = "processing";
    }

    /**
     * Implementation of ISearchable. Gathers all relevant text fields for the search engine.
     * @return A list of strings containing title, summary, lecturer, location, and keywords.
     */
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

    /**
     * Returns the URL for audio playback.
     * @return The audio URL string.
     */
    public String getPlaybackURL() { return audioURL; }

    /**
     * Updates the existing summary with new content.
     * @param newSummary The updated summary text.
     */
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