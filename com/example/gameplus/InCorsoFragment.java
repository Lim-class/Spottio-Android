package com.example.gameplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class InCorsoFragment extends Fragment {

    private RecyclerView recyclerView;
    private LegaAdapter adapter;
    private List<Lega> listaMieLeghe;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_in_corso, container, false);

        // Inizializzazione Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.rvLeaguesIscritte);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        listaMieLeghe = new ArrayList<>();

        // Usiamo lo stesso LegaAdapter creato in precedenza
        adapter = new LegaAdapter(listaMieLeghe, lega -> {
            // Qui gestiamo cosa succede quando clicchi su una tua lega
            apriDettaglioLega(lega);
        });

        recyclerView.setAdapter(adapter);

        // Caricamento dati
        caricaLeMieLeghe();

        return view;
    }

    private void caricaLeMieLeghe() {
        String uidCorrente = mAuth.getCurrentUser().getUid();

        // Query: "Mostrami tutte le leghe dove l'array 'partecipanti' contiene il mio UID"
        db.collection("leagues")
                .whereArrayContains("partecipanti", uidCorrente)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (value != null) {
                        listaMieLeghe.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Lega lega = doc.toObject(Lega.class);
                            listaMieLeghe.add(lega);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void apriDettaglioLega(Lega lega) {
        String uidCorrente = mAuth.getCurrentUser().getUid();

        if (lega.getModeratoreID().equals(uidCorrente)) {
            // Se sono il moderatore, vado a gestire
            Intent intent = new Intent(getActivity(), GestioneLegaActivity.class);
            intent.putExtra("NOME_LEGA", lega.getNome());
            startActivity(intent);
        } else {
            // Se sono un partecipante, vado a vedere il mercato
            Intent intent = new Intent(getActivity(), DettaglioLegaActivity.class);
            intent.putExtra("NOME_LEGA", lega.getNome());
            startActivity(intent);
        }
    }
}