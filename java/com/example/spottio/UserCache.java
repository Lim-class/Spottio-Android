package com.example.spottio;

import java.util.HashMap;
import java.util.Map;

public class UserCache {

    // Contenitore per i dati dinamici dell'utente
    public static class UserData {
        public String username; // NUOVO CAMPO
        public String pfpUri;
        public boolean isVerified;

        public UserData(String username, String pfpUri, boolean isVerified) {
            this.username = username;
            this.pfpUri = pfpUri;
            this.isVerified = isVerified;
        }
    }

    // Mappa locale che associa l'UID ai suoi dati aggiornati
    private static final Map<String, UserData> cache = new HashMap<>();

    public static UserData getUser(String uid) {
        return cache.get(uid);
    }

    public static void putUser(String uid, String username, String pfpUri, boolean isVerified) {
        cache.put(uid, new UserData(username, pfpUri, isVerified));
    }
}