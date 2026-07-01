package com.example.spottio;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

/*
Questo Helper si occupa del caricamento e dell'applicazione visiva dello sfondo,
oltre a salvare le preferenze in Firestore e nelle SharedPreferences.
Delega i popup a ChatBackgroundDialogHelper.
*/
public class ChatBackgroundHelper {

    private final Context context;
    private final FirebaseFirestore db;
    private final String currentUser;
    private final ImageView ivChatBackground;
    private final SharedPreferences prefs;
    private final ChatBackgroundDialogHelper dialogHelper;

    public ChatBackgroundHelper(Context context, FirebaseFirestore db, String currentUser,
                                ImageView ivChatBackground, ActivityResultLauncher<String> scegliSfondoLauncher) {
        this.context = context;
        this.db = db;
        this.currentUser = currentUser;
        this.ivChatBackground = ivChatBackground;
        this.prefs = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);

        // Inizializza il gestore dei Dialog, passandogli "this" (se stesso)
        this.dialogHelper = new ChatBackgroundDialogHelper(context, db, scegliSfondoLauncher, this);
    }

    public void caricaSfondoDaFirestore() {
        if (currentUser == null) return;

        db.collection("users").document(currentUser).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("chatBackgroundColor")) {
                String hexColor = doc.getString("chatBackgroundColor");
                if (hexColor != null && !hexColor.isEmpty()) {
                    ivChatBackground.setImageDrawable(null);
                    try {
                        ivChatBackground.setBackgroundColor(Color.parseColor(hexColor));
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void caricaSfondoSalvato() {
        String configString = prefs.getString("chat_bg_" + currentUser, null);

        if (configString != null) {
            try {
                if (configString.startsWith("color:")) {
                    ivChatBackground.setImageDrawable(null);
                    ivChatBackground.setBackgroundColor(Color.parseColor(configString.substring(6)));
                } else if (configString.startsWith("drawable:")) {
                    String drawableName = configString.substring(9);
                    int resID = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
                    if (resID != 0) {
                        ivChatBackground.setBackgroundColor(Color.TRANSPARENT);
                        ivChatBackground.setImageResource(resID);
                    }
                } else if (configString.startsWith("uri:")) {
                    ivChatBackground.setBackgroundColor(Color.TRANSPARENT);
                    Uri uri = Uri.parse(configString.substring(4));
                    ivChatBackground.setImageURI(uri);
                } else {
                    Uri uri = Uri.parse(configString);
                    ivChatBackground.setImageURI(uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Viene chiamato dal Fragment, ma noi lo deleghiamo al DialogHelper
    public void mostraOpzioniSfondo() {
        dialogHelper.mostraOpzioniSfondo();
    }

    public void applicaSfondoDaUri(Uri uri) {
        ivChatBackground.setBackgroundColor(Color.TRANSPARENT);
        ivChatBackground.setImageURI(uri);
        prefs.edit().putString("chat_bg_" + currentUser, "uri:" + uri.toString()).apply();

        if (currentUser != null) {
            db.collection("users").document(currentUser)
                    .update("chatBackgroundColor", FieldValue.delete());
        }
    }

    // Ora è "public" così il DialogHelper può richiamarlo dopo la scelta
    public void salvaColoreSfondo(String hexCode) {
        if (currentUser == null) return;

        ivChatBackground.setImageDrawable(null);
        try {
            ivChatBackground.setBackgroundColor(Color.parseColor(hexCode));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Toast.makeText(context, "Codice colore non valido", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit().remove("chat_bg_" + currentUser).apply();

        db.collection("users").document(currentUser)
                .update("chatBackgroundColor", hexCode)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Sfondo salvato!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Errore durante il salvataggio remoto", Toast.LENGTH_SHORT).show());
    }
}