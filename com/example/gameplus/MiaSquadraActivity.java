package com.example.gameplus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MiaSquadraActivity extends AppCompatActivity {

    private String nomeLega;
    private FirebaseFirestore db;
    private TextView tvBudget;
    private RecyclerView rv;
    private PersonaggioAdapter adapter;
    private List<Personaggio> mieiPersonaggi;
    private long budgetIniziale = 0;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mia_squadra);

        // Inizializzazione
        db = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        nomeLega = getIntent().getStringExtra("NOME_LEGA");

        tvBudget = findViewById(R.id.tvBudgetRimanente);
        rv = findViewById(R.id.rvMiaSquadra);

        rv.setLayoutManager(new LinearLayoutManager(this));
        mieiPersonaggi = new ArrayList<>();
        adapter = new PersonaggioAdapter(mieiPersonaggi);
        rv.setAdapter(adapter);

        // 1. Recuperiamo il budget iniziale della lega
        recuperaBudgetECaricaSquadra();
    }

    private void recuperaBudgetECaricaSquadra() {
        db.collection("leagues").document(nomeLega).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Long crediti = doc.getLong("creditiIniziali");
                budgetIniziale = (crediti != null) ? crediti : 0;

                // 2. Ascoltiamo i cambiamenti della squadra in tempo reale
                ascoltaCambiamentiSquadra();
            }
        });
    }

    private void ascoltaCambiamentiSquadra() {
        db.collection("leagues").document(nomeLega)
                .collection("personaggi")
                .whereEqualTo("proprietario", myUid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        mieiPersonaggi.clear();
                        long spesaTotale = 0;

                        for (QueryDocumentSnapshot doc : value) {
                            Personaggio p = doc.toObject(Personaggio.class);
                            mieiPersonaggi.add(p);
                            spesaTotale += p.getValore();
                        }

                        adapter.notifyDataSetChanged();

                        // Calcolo budget residuo
                        long rimanente = budgetIniziale - spesaTotale;
                        tvBudget.setText("Crediti residui: " + rimanente);

                        // Feedback visivo sul budget
                        if (rimanente < 20) tvBudget.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        else tvBudget.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    }
                });
    }

    private void confermaSvincolo(Personaggio p) {
        new AlertDialog.Builder(this)
                .setTitle("Svincola " + p.getNome())
                .setMessage("Sei sicuro di voler svincolare questo personaggio? Recupererai " + p.getValore() + " crediti.")
                .setPositiveButton("Sì, svincola", (dialog, which) -> eseguiSvincolo(p))
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void eseguiSvincolo(Personaggio p) {
        db.collection("leagues").document(nomeLega)
                .collection("personaggi").document(p.getNome())
                .update("proprietario", "") // Rende il personaggio libero
                .addOnSuccessListener(aVoid -> Toast.makeText(this, p.getNome() + " svincolato!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Errore durante lo svincolo", Toast.LENGTH_SHORT).show());
    }

    // --- ADAPTER ---
    private class PersonaggioAdapter extends RecyclerView.Adapter<PersonaggioAdapter.ViewHolder> {
        private List<Personaggio> mData;

        public PersonaggioAdapter(List<Personaggio> data) { this.mData = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mia_squadra, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Personaggio p = mData.get(position);
            holder.tvNome.setText(p.getNome());
            holder.tvInfo.setText("Valore: " + p.getValore() + " | Punti: " + p.getPunteggioTotale());

            holder.btnSvincola.setOnClickListener(v -> confermaSvincolo(p));
        }

        @Override
        public int getItemCount() { return mData.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNome, tvInfo;
            Button btnSvincola;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNome = itemView.findViewById(R.id.tvNomeMioP);
                tvInfo = itemView.findViewById(R.id.tvInfoMioP);
                btnSvincola = itemView.findViewById(R.id.btnSvincola);
            }
        }
    }
}