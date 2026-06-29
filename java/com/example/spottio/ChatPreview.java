package com.example.spottio;

import java.util.Date;

public class ChatPreview {
    private String id;
    private String name;
    private String lastMessage;
    private Date lastUpdate;
    private boolean isGroup;
    private String targetId;
    private String profilePicUri;
    private boolean isVerified;

    public ChatPreview(String id, String name, String lastMessage, Date lastUpdate, boolean isGroup, String targetId, String profilePicUri, boolean isVerified) {
        this.id = id;
        this.name = name;
        this.lastMessage = lastMessage;
        this.lastUpdate = lastUpdate;
        this.isGroup = isGroup;
        this.targetId = targetId;
        this.profilePicUri = profilePicUri;
        this.isVerified = isVerified;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLastMessage() { return lastMessage; }
    public Date getLastUpdate() { return lastUpdate; }
    public boolean isGroup() { return isGroup; }
    public String getTargetId() { return targetId; }

    public String getProfilePicUri() { return profilePicUri; }
    public void setProfilePicUri(String profilePicUri) { this.profilePicUri = profilePicUri; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }
}