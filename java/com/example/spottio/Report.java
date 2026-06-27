package com.example.spottio;

import com.google.firebase.firestore.Exclude;

public class Report {
    private String postId;
    private String postAuthor;
    private String reporterUser;
    private String reason;
    private String description; // Nuovo campo
    private String postText;    // Nuovo campo
    private long timestamp;

    public Report() {} // Richiesto per Firebase

    public Report(String postId, String postAuthor, String reporterUser, String reason, String description, String postText) {
        this.postId = postId;
        this.postAuthor = postAuthor;
        this.reporterUser = reporterUser;
        this.reason = reason;
        this.description = description;
        this.postText = postText;
        this.timestamp = System.currentTimeMillis();
    }

    @Exclude
    private String reportId;

    @Exclude
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    // Getter e Setter
    public String getPostId() { return postId; }
    public String getPostAuthor() { return postAuthor; }
    public String getReporterUser() { return reporterUser; }
    public String getReason() { return reason; }
    public String getDescription() { return description; } // Getter nuovo
    public String getPostText() { return postText; }       // Getter nuovo
    public long getTimestamp() { return timestamp; }
}