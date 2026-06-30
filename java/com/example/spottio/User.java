package com.example.spottio;

import java.util.ArrayList;
import java.util.List;

// Modello Utente compatibile con Firebase Firestore.
public class User {
    private String uid;
    private String username;
    private String email;
    private String bio;
    private List<String> following;
    private List<String> followers;
    private List<String> interests;
    private boolean isAdmin;

    // Costruttore vuoto necessario per Firestore
    public User() {
        this.following = new ArrayList<>();
        this.followers = new ArrayList<>();
        this.interests = new ArrayList<>();
    }

    public User(String uid, String username, String email) {
        this.uid = uid;
        this.username = username;
        this.email = email;
        this.bio = "";
        this.following = new ArrayList<>();
        this.followers = new ArrayList<>();
        this.interests = new ArrayList<>();
        this.isAdmin = false;
    }

    // Getter e Setter
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getFollowing() { return following; }
    public void setFollowing(List<String> following) { this.following = following; }

    public List<String> getFollowers() { return followers; }
    public void setFollowers(List<String> followers) { this.followers = followers; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public boolean isIsAdmin() { return isAdmin; }
    public void setIsAdmin(boolean admin) { isAdmin = admin; }
}