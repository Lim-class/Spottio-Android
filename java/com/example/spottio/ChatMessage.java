package com.example.spottio;

import java.util.Date;

public class ChatMessage {
    public String docId;
    public String text;
    public boolean isMe;
    public Date timestamp;
    public boolean mostraData;
    public String testoData;
    public String senderName;
    public boolean isGroup;
    public String profilePicUri;
    public boolean isVerified;

    public ChatMessage(String docId, String text, boolean isMe, Date timestamp, boolean mostraData, String testoData, String senderName, boolean isGroup, String profilePicUri, boolean isVerified) {
        this.docId = docId;
        this.text = text;
        this.isMe = isMe;
        this.timestamp = timestamp;
        this.mostraData = mostraData;
        this.testoData = testoData;
        this.senderName = senderName;
        this.isGroup = isGroup;
        this.profilePicUri = profilePicUri;
        this.isVerified = isVerified;
    }
}