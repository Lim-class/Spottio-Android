package com.example.spottio;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsPrivacyManager {

    private final FirebaseFirestore mDb;
    private final Context context;
    private final String currentUsername;

    public SettingsPrivacyManager(FirebaseFirestore mDb, Context context, String currentUsername) {
        this.mDb = mDb;
        this.context = context;
        this.currentUsername = currentUsername;
    }

    public void setupPrivacyLogic(View view) {
        CheckBox checkboxPrivacy = view.findViewById(R.id.checkboxPrivacy);
        TextView textPrivacyStatus = view.findViewById(R.id.textPrivacyStatus);

        if ("Guest".equals(currentUsername)) {
            checkboxPrivacy.setEnabled(false);
            textPrivacyStatus.setVisibility(View.VISIBLE);
            textPrivacyStatus.setText("Funzionalità non disponibile per gli utenti Guest.");
            return;
        }

        DocumentReference userDoc = mDb.collection("users").document(currentUsername);

        userDoc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("isPrivate")) {
                Boolean isPrivate = documentSnapshot.getBoolean("isPrivate");
                checkboxPrivacy.setChecked(isPrivate != null && isPrivate);
            }
        });

        checkboxPrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userDoc.update("isPrivate", isChecked)
                    .addOnSuccessListener(aVoid -> {
                        textPrivacyStatus.setVisibility(View.VISIBLE);
                        if (isChecked) {
                            textPrivacyStatus.setText("Il tuo account è ora Privato.");
                            textPrivacyStatus.setTextColor(0xFF388E3C);
                        } else {
                            textPrivacyStatus.setText("Il tuo account è ora Pubblico.");
                            textPrivacyStatus.setTextColor(0xFF1976D2);
                        }
                    })
                    .addOnFailureListener(e -> {
                        checkboxPrivacy.setChecked(!isChecked);
                        Toast.makeText(context, "Errore di rete durante l'aggiornamento.", Toast.LENGTH_SHORT).show();
                    });
        });
    }
}