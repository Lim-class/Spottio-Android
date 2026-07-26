package com.example.spottio

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object InteractionTracker {

    /**
     * Metodo di base per retrocompatibilità.
     * Incrementa il punteggio della categoria di 1.
     */
    fun trackInteraction(userId: String?, category: String?) {
        trackInteraction(userId, category, 1)
    }

    /**
     * Nuovo metodo che accetta incrementi o decrementi (es. -1 per rimozione like)
     * e aggiorna il timestamp per l'algoritmo di decadimento (Time Decay).
     */
    fun trackInteraction(userId: String?, category: String?, value: Long) {
        if (userId.isNullOrEmpty() || category.isNullOrEmpty()) return

        val db = FirebaseFirestore.getInstance()

        // Prepariamo l'aggiornamento nidificato per il nodo "preferences"
        val updateData = hashMapOf<String, Any>(
            "preferences.$category.score" to FieldValue.increment(value),
            "preferences.$category.lastUpdate" to FieldValue.serverTimestamp()
        )

        // Prova ad aggiornare la mappa nidificata
        db.collection("users").document(userId)
            .update(updateData)
            .addOnFailureListener {
                // Se fallisce (perché l'utente o il nodo preferences non esistono ancora), creiamo la struttura
                val catData = hashMapOf<String, Any>(
                    "score" to value,
                    "lastUpdate" to FieldValue.serverTimestamp()
                )

                val preferences = hashMapOf<String, Any>(category to catData)
                val data = hashMapOf<String, Any>("preferences" to preferences)

                db.collection("users").document(userId)
                    .set(data, SetOptions.merge())
            }
    }
}