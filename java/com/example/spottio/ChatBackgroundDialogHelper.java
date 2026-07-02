package com.example.spottio;

import android.content.Context;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/*
Questo Helper si occupa esclusivamente di mostrare i menu di scelta all'utente (i Dialog)
e di scaricare la lista dei colori disponibili da Firestore.
*/
public class ChatBackgroundDialogHelper {

    private final Context context;
    private final FirebaseFirestore db;
    private final ActivityResultLauncher<String> scegliSfondoLauncher;
    private final ChatBackgroundHelper backgroundHelper;

    public ChatBackgroundDialogHelper(Context context, FirebaseFirestore db,
                                      ActivityResultLauncher<String> scegliSfondoLauncher,
                                      ChatBackgroundHelper backgroundHelper) {
        this.context = context;
        this.db = db;
        this.scegliSfondoLauncher = scegliSfondoLauncher;
        this.backgroundHelper = backgroundHelper;
    }

    public void mostraOpzioniSfondo() {
        String[] opzioni = {"Scegli immagine dalla galleria", "Scegli un colore a tinta unita"};
        new AlertDialog.Builder(context)
                .setTitle("Personalizza Sfondo")
                .setItems(opzioni, (dialog, which) -> {
                    if (which == 0) {
                        scegliSfondoLauncher.launch("image/*");
                    } else {
                        scegliSfondoColoreDaDb();
                    }
                }).show();
    }

    private void scegliSfondoColoreDaDb() {
        db.collection("ColoriSfondo").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                List<String> colorNames = new ArrayList<>();
                List<String> colorHexes = new ArrayList<>();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String name = doc.getString("nome");
                    String hex = doc.getString("hex");
                    if (name != null && hex != null) {
                        colorNames.add(name);
                        colorHexes.add(hex);
                    }
                }

                if (colorNames.isEmpty()) {
                    Toast.makeText(context, "Nessun colore trovato", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] options = colorNames.toArray(new String[0]);
                new AlertDialog.Builder(context)
                        .setTitle("Seleziona un colore")
                        .setItems(options, (dialog, which) -> {
                            String selectedHex = colorHexes.get(which);
                            // Passa il colore selezionato all'Helper principale per salvarlo e applicarlo
                            backgroundHelper.salvaColoreSfondo(selectedHex);
                        })
                        .show();
            } else {
                Toast.makeText(context, "Errore nel caricamento dei colori", Toast.LENGTH_SHORT).show();
            }
        });
    }
}