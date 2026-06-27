package com.example.spottio;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UserCache {

    public static class UserData {
        public String username;
        public String pfpUri;
        public boolean isVerified;

        public UserData(String username, String pfpUri, boolean isVerified) {
            this.username = username;
            this.pfpUri = pfpUri;
            this.isVerified = isVerified;
        }
    }

    private static final Map<String, UserData> cache = new HashMap<>();
    // Tiene traccia dei download in corso per evitare chiamate duplicate
    private static final Set<String> fetchingUids = new HashSet<>();

    public interface FetchCallback {
        void onUserFetched();
    }

    public static UserData getUser(String uid) {
        return cache.get(uid);
    }

    public static void putUser(String uid, String username, String pfpUri, boolean isVerified) {
        cache.put(uid, new UserData(username, pfpUri, isVerified));
    }

    /**
     * Esegue il recupero intelligente dell'utente da Firestore (incluso l'Auto-Healing)
     */
    public static void fetchUserAsync(String uidOrUsername, FetchCallback callback) {
        // Se c'è già in cache, restituisci subito
        if (cache.containsKey(uidOrUsername)) {
            if (callback != null) callback.onUserFetched();
            return;
        }

        // Se lo stiamo già scaricando in background, non inviare un'altra richiesta a Firebase!
        if (fetchingUids.contains(uidOrUsername)) return;

        fetchingUids.add(uidOrUsername);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uidOrUsername).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("username");
                        String pfp = doc.getString("userPfUri");
                        Boolean verObj = doc.getBoolean("isVerified");
                        putUser(uidOrUsername, name != null ? name : "Utente", pfp != null ? pfp : "", verObj != null && verObj);
                        fetchingUids.remove(uidOrUsername);
                        if (callback != null) callback.onUserFetched();
                    } else {
                        // AUTO-HEALING: Se non lo trova per UID, cercalo per Username
                        db.collection("users").whereEqualTo("username", uidOrUsername).limit(1).get()
                                .addOnSuccessListener(query -> {
                                    if (!query.isEmpty()) {
                                        String name = query.getDocuments().get(0).getString("username");
                                        String pfp = query.getDocuments().get(0).getString("userPfUri");
                                        Boolean verObj = query.getDocuments().get(0).getBoolean("isVerified");
                                        putUser(uidOrUsername, name != null ? name : "Utente", pfp != null ? pfp : "", verObj != null && verObj);
                                    } else {
                                        putUser(uidOrUsername, "Utente Sconosciuto o Eliminato", "", false);
                                    }
                                    fetchingUids.remove(uidOrUsername);
                                    if (callback != null) callback.onUserFetched();
                                }).addOnFailureListener(e -> fetchingUids.remove(uidOrUsername));
                    }
                }).addOnFailureListener(e -> fetchingUids.remove(uidOrUsername));
    }
}