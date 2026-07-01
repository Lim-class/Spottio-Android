package com.example.gameplus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassificaActivity extends AppCompatActivity {

    private String nomeLega;
    private FirebaseFirestore db;
    private RecyclerView rv;
    private ClassificaAdapter adapter;
    private List<UtenteClassifica> listaClassifica;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_classifica);

        nomeLega = getIntent().getStringExtra("NOME_LEGA");
        db = FirebaseFirestore.getInstance();
        rv = findViewById(R.id.rvClassifica);
        rv.setLayoutManager(new LinearLayoutManager(this));

        listaClassifica = new ArrayList<>();
        adapter = new ClassificaAdapter(listaClassifica);
        rv.setAdapter(adapter);

        // Ora legge i dati storici salvati a fine giornata
        calcolaClassificaGenerale();
    }

    private void calcolaClassificaGenerale() {
        // Leggiamo i punteggi definitivi dalla nuova sottocollezione
        db.collection("leagues").document(nomeLega)
                .collection("classifica")
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    listaClassifica.clear();

                    for (QueryDocumentSnapshot doc : value) {
                        String uidUtente = doc.getId();
                        Long puntiLong = doc.getLong("punteggioTotale");
                        int puntiTotali = (puntiLong != null) ? puntiLong.intValue() : 0;

                        // Cerchiamo il nome dell'utente
                        db.collection("users").document(uidUtente).get()
                                .addOnSuccessListener(userDoc -> {
                                    // FIX UTENTE ANONIMO: Se non ha salvato il profilo, mostriamo almeno parte del suo ID
                                    String nick = "Manager " + uidUtente.substring(0, 4).toUpperCase();

                                    if (userDoc.exists() && userDoc.contains("nickname")) {
                                        nick = userDoc.getString("nickname");
                                    }

                                    // Evitiamo duplicati grafici a causa del caricamento asincrono
                                    String finalNick = nick;
                                    listaClassifica.removeIf(u -> u.getNomeUtente().equals(finalNick));

                                    // Aggiungiamo alla classifica
                                    listaClassifica.add(new UtenteClassifica(finalNick, puntiTotali));

                                    // Ordiniamo e aggiorniamo l'interfaccia
                                    Collections.sort(listaClassifica, (a, b) -> b.getPunteggioTotale() - a.getPunteggioTotale());
                                    adapter.notifyDataSetChanged();
                                });
                    }
                });
    }

    // --- ADAPTER PER LA CLASSIFICA ---
    private class ClassificaAdapter extends RecyclerView.Adapter<ClassificaAdapter.ViewHolder> {
        private List<UtenteClassifica> mData;
        public ClassificaAdapter(List<UtenteClassifica> data) { this.mData = data; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UtenteClassifica u = mData.get(position);
            holder.t1.setText((position + 1) + "° Posto");
            holder.t2.setText(u.getNomeUtente() + " - Punti: " + u.getPunteggioTotale());
        }

        @Override
        public int getItemCount() { return mData.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView t1, t2;
            public ViewHolder(View itemView) {
                super(itemView);
                t1 = itemView.findViewById(android.R.id.text1);
                t2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}