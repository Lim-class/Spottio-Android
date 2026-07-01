package com.example.spottio;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class InteractionTracker {

    /**
     * Metodo di base per retrocompatibilità.
     * Incrementa il punteggio della categoria di 1.
     */
    public static void trackInteraction(String userId, String category) {
        trackInteraction(userId, category, 1);
    }

    /**
     * Nuovo metodo che accetta incrementi o decrementi (es. -1 per rimozione like)
     * e aggiorna il timestamp per l'algoritmo di decadimento (Time Decay).
     */
    public static void trackInteraction(String userId, String category, long value) {
        if (userId == null || category == null || category.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Prepariamo l'aggiornamento nidificato per il nodo "preferences"
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("preferences." + category + ".score", FieldValue.increment(value));
        updateData.put("preferences." + category + ".lastUpdate", FieldValue.serverTimestamp());

        // Prova ad aggiornare la mappa nidificata
        db.collection("users").document(userId)
                .update(updateData)
                .addOnFailureListener(e -> {
                    // Se fallisce (perché l'utente o il nodo preferences non esistono ancora), creiamo la struttura
                    Map<String, Object> data = new HashMap<>();
                    Map<String, Object> catData = new HashMap<>();
                    catData.put("score", value);
                    catData.put("lastUpdate", FieldValue.serverTimestamp());

                    Map<String, Object> preferences = new HashMap<>();
                    preferences.put(category, catData);
                    data.put("preferences", preferences);

                    db.collection("users").document(userId)
                            .set(data, SetOptions.merge());
                });
    }
}