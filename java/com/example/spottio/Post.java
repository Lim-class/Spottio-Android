package com.example.spottio;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@IgnoreExtraProperties
public class Post {
    @Exclude
    private String postId;

    private String user; // Da oggi conterrà in via esclusiva l'UID
    private String text;
    private String mediaUri;

    @PropertyName("video")
    private boolean isVideo;

    private String category;
    private Date timestamp;

    private List<Comment> comments;

    @PropertyName("Likes")
    private List<String> likes;

    public Post() {
        this.comments = new ArrayList<>();
        this.likes = new ArrayList<>();
    }

    // COSTRUTTORE PULITO: Nessuna traccia di "isVerified" o foto profilo!
    public Post(String user, String text, String mediaUri, boolean isVideo, String category) {
        this.user = user;
        this.text = text;
        this.mediaUri = mediaUri;
        this.isVideo = isVideo;
        this.category = category;
        this.timestamp = new Date();
        this.comments = new ArrayList<>();
        this.likes = new ArrayList<>();
    }

    public void addComment(Comment comment) {
        if (this.comments == null) this.comments = new ArrayList<>();
        this.comments.add(comment);
    }

    public void toggleLike(String uid) {
        if (this.likes == null) this.likes = new ArrayList<>();
        if (likes.contains(uid)) {
            likes.remove(uid);
        } else {
            likes.add(uid);
        }
    }

    @Exclude
    public String getPostId() { return postId; }
    @Exclude
    public void setPostId(String postId) { this.postId = postId; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getMediaUri() { return mediaUri; }
    public void setMediaUri(String mediaUri) { this.mediaUri = mediaUri; }

    @PropertyName("video")
    public boolean isVideo() { return isVideo; }
    @PropertyName("video")
    public void setVideo(boolean video) { this.isVideo = video; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public List<Comment> getComments() {
        if (comments == null) comments = new ArrayList<>();
        return comments;
    }
    public void setComments(List<Comment> comments) { this.comments = comments; }

    @PropertyName("Likes")
    public List<String> getLikes() {
        if (likes == null) likes = new ArrayList<>();
        return likes;
    }
    @PropertyName("Likes")
    public void setLikes(List<String> likes) { this.likes = likes; }
}

// Per inserire il verificato e riutilizzarlo
// <include layout="@layout/layout_verified_badge" />