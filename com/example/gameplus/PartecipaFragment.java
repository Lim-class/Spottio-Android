package com.example.gameplus;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PartecipaFragment extends Fragment {

    private RecyclerView recyclerView;
    private LegaAdapter adapter;
    private List<Lega> listaLeghe;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_partecipa, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.rvLeaguesDisponibili);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        listaLeghe = new ArrayList<>();

        // Inizializziamo l'adapter con una funzione di click
        adapter = new LegaAdapter(listaLeghe, lega -> {
            // Logica quando si clicca su "ENTRA"
            controllaAccessoLega(lega);
        });

        recyclerView.setAdapter(adapter);

        caricaLegheDaFirestore();

        return view;
    }

    private void caricaLegheDaFirestore() {
        // Leggiamo tutte le leghe presenti nella collezione "leagues"
        db.collection("leagues")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Errore nel caricamento", error);
                        return;
                    }

                    listaLeghe.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Lega lega = doc.toObject(Lega.class);
                        listaLeghe.add(lega);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void controllaAccessoLega(Lega lega) {
        if (!TextUtils.isEmpty(lega.getPassword())) {
            // Mostra un semplice dialogo per inserire la password
            mostraDialogPassword(lega);
        } else {
            iscriviUtenteALega(lega.getNome());
        }
    }

    private void mostraDialogPassword(Lega lega) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Lega Privata");
        builder.setMessage("Inserisci la password per " + lega.getNome());

        final EditText input = new EditText(getContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Entra", (dialog, which) -> {
            String passInserita = input.getText().toString();
            if (passInserita.equals(lega.getPassword())) {
                iscriviUtenteALega(lega.getNome());
            } else {
                Toast.makeText(getContext(), "Password errata!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void iscriviUtenteALega(String nomeLega) {
        String mUid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 1. Aggiungi l'utente alla lista partecipanti della LEGA
        db.collection("leagues").document(nomeLega)
                .update("partecipanti", com.google.firebase.firestore.FieldValue.arrayUnion(mUid))
                .addOnSuccessListener(aVoid -> {

                    // 2. Aggiungi la lega alla lista personale dell'UTENTE (nel suo documento)
                    db.collection("users").document(mUid)
                            .update("leghe_iscritte", com.google.firebase.firestore.FieldValue.arrayUnion(nomeLega))
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(getContext(), "Ti sei iscritto a " + nomeLega, Toast.LENGTH_LONG).show();

                                // 3. Sposta l'utente sulla scheda "In Corso" o apri i dettagli
                                // Per ora ricarichiamo la home o mostriamo un messaggio
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Errore iscrizione: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}