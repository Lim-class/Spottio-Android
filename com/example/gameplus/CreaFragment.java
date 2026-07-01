package com.example.gameplus;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CreaFragment extends Fragment {

    private EditText etNomeLega, etPassword, etCrediti, etNomeRegola, etPuntiRegola;
    private Spinner spinGiornoInizio, spinGiornoFine, spinGiornoCalcolo;
    private EditText etOraInizio, etOraFine, etOraCalcolo;
    private RadioGroup rgModalita;
    private TextView tvAnteprimaRegole;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Map<String, Integer> mappaRegolamento = new HashMap<>();
    private final String[] GIORNI = {"Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crea, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etNomeLega = view.findViewById(R.id.etNomeLegaCrea);
        etPassword = view.findViewById(R.id.etPasswordLegaCrea);
        etCrediti = view.findViewById(R.id.etCreditiLegaCrea);
        etNomeRegola = view.findViewById(R.id.etNomeRegola);
        etPuntiRegola = view.findViewById(R.id.etPuntiRegola);
        tvAnteprimaRegole = view.findViewById(R.id.tvAnteprimaRegole);
        rgModalita = view.findViewById(R.id.rgModalita);

        // Calendari
        spinGiornoInizio = view.findViewById(R.id.spinGiornoInizio);
        spinGiornoFine = view.findViewById(R.id.spinGiornoFine);
        spinGiornoCalcolo = view.findViewById(R.id.spinGiornoCalcolo);
        etOraInizio = view.findViewById(R.id.etOraInizio);
        etOraFine = view.findViewById(R.id.etOraFine);
        etOraCalcolo = view.findViewById(R.id.etOraCalcolo);

        ArrayAdapter<String> adapterGiorni = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, GIORNI);
        adapterGiorni.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinGiornoInizio.setAdapter(adapterGiorni);
        spinGiornoFine.setAdapter(adapterGiorni);
        spinGiornoCalcolo.setAdapter(adapterGiorni);

        view.findViewById(R.id.btnAddRegola).setOnClickListener(v -> aggiungiRegola());
        view.findViewById(R.id.btnConfermaCreaLega).setOnClickListener(v -> salvaLega());

        return view;
    }

    private void aggiungiRegola() {
        String nome = etNomeRegola.getText().toString().trim();
        String puntiStr = etPuntiRegola.getText().toString().trim();

        if (nome.isEmpty() || puntiStr.isEmpty()) return;
        mappaRegolamento.put(nome, Integer.parseInt(puntiStr));

        StringBuilder sb = new StringBuilder("Regole salvate:\n");
        for (String key : mappaRegolamento.keySet()) {
            sb.append("- ").append(key).append(": ").append(mappaRegolamento.get(key)).append("\n");
        }
        tvAnteprimaRegole.setText(sb.toString());
        etNomeRegola.setText("");
        etPuntiRegola.setText("");
    }

    private void salvaLega() {
        String nome = etNomeLega.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String creditiStr = etCrediti.getText().toString().trim();
        String oInizio = etOraInizio.getText().toString().trim();
        String oFine = etOraFine.getText().toString().trim();
        String oCalcolo = etOraCalcolo.getText().toString().trim();

        if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(creditiStr)) {
            Toast.makeText(getContext(), "Nome e crediti obbligatori", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(oInizio) || TextUtils.isEmpty(oFine) || TextUtils.isEmpty(oCalcolo)) {
            Toast.makeText(getContext(), "Inserisci gli orari (es. 15:00)", Toast.LENGTH_SHORT).show();
            return;
        }

        String modalita = "standard";
        int selectedId = rgModalita.getCheckedRadioButtonId();
        if (selectedId == R.id.rbAsta) modalita = "asta";
        else if (selectedId == R.id.rbMultiuso) modalita = "multiuso";

        int crediti = Integer.parseInt(creditiStr);
        String modId = mAuth.getCurrentUser().getUid();

        Map<String, Object> nuovaLega = new HashMap<>();
        nuovaLega.put("nome", nome);
        nuovaLega.put("password", pass);
        nuovaLega.put("creditiIniziali", crediti);
        nuovaLega.put("moderatoreID", modId);
        nuovaLega.put("modalitaAcquisto", modalita);
        nuovaLega.put("partecipanti", new ArrayList<String>());
        nuovaLega.put("regolamento", mappaRegolamento);

        nuovaLega.put("giornoInizioMercato", spinGiornoInizio.getSelectedItem().toString());
        nuovaLega.put("oraInizioMercato", oInizio);
        nuovaLega.put("giornoFineMercato", spinGiornoFine.getSelectedItem().toString());
        nuovaLega.put("oraFineMercato", oFine);
        nuovaLega.put("giornoCalcolo", spinGiornoCalcolo.getSelectedItem().toString());
        nuovaLega.put("oraCalcolo", oCalcolo);

        db.collection("leagues").document(nome)
                .set(nuovaLega)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Lega creata con successo!", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(getActivity(), GestioneLegaActivity.class);
                    i.putExtra("NOME_LEGA", nome);
                    startActivity(i);
                });
    }
}