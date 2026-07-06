package com.example.project;

public class NotificationModel {
    private String adminName;
    private String eventName;
    private String status;
    private String comment;
    private String timestamp;

    public NotificationModel(String adminName, String eventName, String status, String comment, String timestamp) {
        this.adminName = adminName;
        this.eventName = eventName;
        this.status = status;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getAdminName() { return adminName; }
    public String getEventName() { return eventName; }
    public String getStatus() { return status; }
    public String getComment() { return comment; }
    public String getTimestamp() { return timestamp; }
}