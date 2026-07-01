package com.example.gameplus;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestioneLegaActivity extends AppCompatActivity {

    private String nomeLega;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private EditText etNome, etValore;
    private RecyclerView rvPersonaggi;
    private PersonaggioAdapter adapter;
    private List<Personaggio> listaPersonaggi;

    private ListenerRegistration personaggiListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestione_lega);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        nomeLega = getIntent().getStringExtra("NOME_LEGA");

        TextView tvTitolo = findViewById(R.id.tvTitoloLega);
        tvTitolo.setText("Gestione: " + nomeLega);

        etNome = findViewById(R.id.etNomePersonaggio);
        etValore = findViewById(R.id.etValorePersonaggio);

        // Setup RecyclerView
        rvPersonaggi = findViewById(R.id.rvPersonaggi);
        rvPersonaggi.setLayoutManager(new LinearLayoutManager(this));
        listaPersonaggi = new ArrayList<>();
        adapter = new PersonaggioAdapter(listaPersonaggi);
        rvPersonaggi.setAdapter(adapter);

        // Listeners dei pulsanti
        findViewById(R.id.btnAggiungiPersonaggio).setOnClickListener(v -> aggiungiPersonaggio());

        findViewById(R.id.btnVaiAlMercato).setOnClickListener(v -> {
            Intent intent = new Intent(this, DettaglioLegaActivity.class);
            intent.putExtra("NOME_LEGA", nomeLega);
            startActivity(intent);
        });

        findViewById(R.id.btnConcludiGiornata).setOnClickListener(v -> mostraDialogoConfermaGiornata());

        findViewById(R.id.btnEliminaLega).setOnClickListener(v -> confermaEliminazioneLega());

        // Carica la lista dei personaggi in tempo reale
        ascoltaPersonaggi();
    }

    private void aggiungiPersonaggio() {
        String nomeP = etNome.getText().toString().trim();
        String valoreStr = etValore.getText().toString().trim();

        if (TextUtils.isEmpty(nomeP) || TextUtils.isEmpty(valoreStr)) {
            Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show();
            return;
        }

        int valoreP = Integer.parseInt(valoreStr);

        Map<String, Object> p = new HashMap<>();
        p.put("nome", nomeP);
        p.put("valore", valoreP);
        p.put("proprietario", "");
        p.put("punteggioTotale", 0); // Nasce con 0 punti

        db.collection("leagues").document(nomeLega)
                .collection("personaggi").document(nomeP)
                .set(p)
                .addOnSuccessListener(aVoid -> {
                    etNome.setText("");
                    etValore.setText("");
                    Toast.makeText(this, "Personaggio aggiunto!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Errore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void ascoltaPersonaggi() {
        personaggiListener = db.collection("leagues").document(nomeLega)
                .collection("personaggi")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    listaPersonaggi.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        listaPersonaggi.add(doc.toObject(Personaggio.class));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // --- LOGICA ASSEGNAZIONE PUNTI ---
    private void mostraDialogoPunteggio(Personaggio p) {
        db.collection("leagues").document(nomeLega).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Map<String, Long> regolamento = (Map<String, Long>) doc.get("regolamento");
                if (regolamento == null || regolamento.isEmpty()) {
                    Toast.makeText(this, "Nessun regolamento trovato.", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> nomiRegole = new ArrayList<>(regolamento.keySet());
                String[] opzioni = nomiRegole.toArray(new String[0]);

                new AlertDialog.Builder(this)
                        .setTitle("Assegna punti a " + p.getNome())
                        .setItems(opzioni, (dialog, which) -> {
                            String regolaScelta = opzioni[which];
                            long valorePunti = regolamento.get(regolaScelta);
                            applicaPunteggio(p.getNome(), valorePunti);
                        })
                        .show();
            }
        });
    }

    private void applicaPunteggio(String nomePersonaggio, long punti) {
        db.collection("leagues").document(nomeLega)
                .collection("personaggi").document(nomePersonaggio)
                .update("punteggioTotale", com.google.firebase.firestore.FieldValue.increment(punti))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Punteggio aggiornato!", Toast.LENGTH_SHORT).show());
    }

    // --- LOGICA CONCLUDI GIORNATA E SALVA CLASSIFICA ---
    private void mostraDialogoConfermaGiornata() {
        new AlertDialog.Builder(this)
                .setTitle("Concludi Giornata")
                .setMessage("Vuoi salvare i punteggi attuali nella Classifica Generale? I punti dei personaggi verranno azzerati per la nuova giornata.")
                .setPositiveButton("Sì, Concludi", (dialog, which) -> concludiGiornata())
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void concludiGiornata() {
        db.collection("leagues").document(nomeLega).collection("personaggi")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch(); // Usiamo un batch per fare tutto in un colpo solo

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Personaggio p = doc.toObject(Personaggio.class);

                        if (p.getProprietario() != null && !p.getProprietario().isEmpty() && p.getPunteggioTotale() != 0) {
                            // Aggiunge i punti all'utente nella classifica storica
                            DocumentReference refClassifica = db.collection("leagues").document(nomeLega)
                                    .collection("classifica").document(p.getProprietario());

                            batch.set(refClassifica, new HashMap<String, Object>() {{
                                put("punteggioTotale", com.google.firebase.firestore.FieldValue.increment(p.getPunteggioTotale()));
                            }}, com.google.firebase.firestore.SetOptions.merge());
                        }

                        // Azzera i punti del personaggio per la prossima giornata
                        if (p.getPunteggioTotale() != 0) {
                            DocumentReference refPersonaggio = db.collection("leagues").document(nomeLega)
                                    .collection("personaggi").document(p.getNome());
                            batch.update(refPersonaggio, "punteggioTotale", 0);
                        }
                    }

                    // Invia le modifiche al database
                    batch.commit().addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Giornata conclusa! Classifica aggiornata.", Toast.LENGTH_LONG).show();
                    });
                });
    }

    // --- LOGICA ELIMINA LEGA ---
    private void confermaEliminazioneLega() {
        new AlertDialog.Builder(this)
                .setTitle("Elimina Lega")
                .setMessage("Sei sicuro? Questa azione cancellerà tutti i dati in modo irreversibile.")
                .setPositiveButton("SÌ, ELIMINA", (dialog, which) -> eliminaTutto())
                .setNegativeButton("Annulla", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void eliminaTutto() {
        DocumentReference legaRef = db.collection("leagues").document(nomeLega);

        legaRef.collection("personaggi").get().addOnSuccessListener(queryDocumentSnapshots -> {
            WriteBatch batch = db.batch();

            // Elimina prima i personaggi
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                batch.delete(doc.getReference());
            }

            // Poi elimina la lega
            batch.delete(legaRef);

            batch.commit().addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Lega eliminata", Toast.LENGTH_SHORT).show();

                // Torna alla home pulendo lo stack delle activity
                Intent intent = new Intent(this, HomeFantaActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    // --- ADAPTER ---
    private class PersonaggioAdapter extends RecyclerView.Adapter<PersonaggioAdapter.ViewHolder> {
        private List<Personaggio> mData;
        public PersonaggioAdapter(List<Personaggio> data) { this.mData = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Personaggio p = mData.get(position);
            holder.text1.setText(p.getNome() + " (" + p.getValore() + " crediti)");

            String stato = (p.getProprietario() == null || p.getProprietario().isEmpty()) ? "Libero" : "Assegnato";
            holder.text2.setText("Punti giornata: " + p.getPunteggioTotale() + " | Stato: " + stato);

            // Cliccando sul personaggio si apre il menu delle regole
            holder.itemView.setOnClickListener(v -> mostraDialogoPunteggio(p));
        }

        @Override
        public int getItemCount() { return mData.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (personaggiListener != null) personaggiListener.remove();
    }
}