package com.example.spottio;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

/*
Questo Helper si occupa unicamente di gestire le azioni sui messaggi:
modifica, eliminazione per tutti ed eliminazione locale (nascondi).
*/
public class ChatActionHelper {

    private final Context context;
    private final FirebaseFirestore db;
    private final String currentUser;
    private final boolean isGroup;
    private final String targetUser;
    private final String conversationId; // Aggiunto per i percorsi dinamici
    private final Set<String> hiddenMessages;
    private final SharedPreferences prefs;
    private final Runnable onLocalVisibilityChanged;

    public ChatActionHelper(Context context, FirebaseFirestore db, String currentUser, boolean isGroup, String targetUser, String conversationId, Set<String> hiddenMessages, Runnable onLocalVisibilityChanged) {
        this.context = context;
        this.db = db;
        this.currentUser = currentUser;
        this.isGroup = isGroup;
        this.targetUser = targetUser;
        this.conversationId = conversationId;
        this.hiddenMessages = hiddenMessages;
        this.prefs = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        this.onLocalVisibilityChanged = onLocalVisibilityChanged;
    }

    public void mostraDialogoOpzioni(ChatMessage msg) {
        String[] opzioni = {"Modifica", "Elimina per tutti"};
        new AlertDialog.Builder(context)
                .setTitle("Opzioni messaggio")
                .setItems(opzioni, (dialog, which) -> {
                    if (which == 0) mostraDialogoModifica(msg);
                    else mostraDialogoElimina(msg.docId, true);
                }).show();
    }

    private void mostraDialogoModifica(ChatMessage msg) {
        final EditText input = new EditText(context);
        input.setText(msg.text);
        input.setSelection(msg.text.length());

        new AlertDialog.Builder(context)
                .setTitle("Modifica messaggio")
                .setView(input)
                .setPositiveButton("Salva", (d, w) -> {
                    String nuovoTesto = input.getText().toString().trim();
                    if (!nuovoTesto.isEmpty() && !nuovoTesto.equals(msg.text)) {
                        // COSTRUISCE IL PERCORSO CORRETTO DINAMICAMENTE
                        String collectionPath = isGroup ? "groups/" + targetUser + "/chats" : "chats/" + conversationId + "/messages";

                        db.collection(collectionPath).document(msg.docId)
                                .update("text", nuovoTesto)
                                .addOnSuccessListener(a -> Toast.makeText(context, "Messaggio modificato", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    public void mostraDialogoElimina(String docId, boolean deleteForEveryone) {
        String titolo = deleteForEveryone ? "Elimina per tutti" : "Elimina per te";
        String messaggio = deleteForEveryone ? "Questo messaggio sparirà anche per l'altra persona." : "Questo messaggio non sarà più visibile sul tuo telefono.";
        new AlertDialog.Builder(context)
                .setTitle(titolo)
                .setMessage(messaggio)
                .setPositiveButton("Elimina", (dialog, which) -> {
                    if (deleteForEveryone) {
                        // COSTRUISCE IL PERCORSO CORRETTO DINAMICAMENTE
                        String collectionPath = isGroup ? "groups/" + targetUser + "/chats" : "chats/" + conversationId + "/messages";
                        db.collection(collectionPath).document(docId).delete();
                    } else {
                        nascondiMessaggioLocalmente(docId);
                    }
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void nascondiMessaggioLocalmente(String docId) {
        hiddenMessages.add(docId);
        prefs.edit().putStringSet("hidden_messages_" + currentUser, new HashSet<>(hiddenMessages)).apply();

        // Avvisa l'Adapter che un messaggio è stato nascosto così aggiorna la ListView
        if (onLocalVisibilityChanged != null) {
            onLocalVisibilityChanged.run();
        }
    }
}