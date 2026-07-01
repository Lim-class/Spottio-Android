package com.example.gameplus;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class ChiVuolEssereRiccoActivity extends AppCompatActivity {

    private EditText editEtaPartenza;
    private Spinner spinnerPaese;
    private TextView txtDescrizionePaese;
    private RadioGroup groupObiettivo;
    private String paeseSelezionato = "Italia";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chi_vuol_essere_ricco);

        editEtaPartenza = findViewById(R.id.edit_eta_partenza);
        spinnerPaese = findViewById(R.id.spinner_paese);
        txtDescrizionePaese = findViewById(R.id.txt_descrizione_paese);
        groupObiettivo = findViewById(R.id.group_objective);

        setupSpinner();

        findViewById(R.id.btn_inizia_scalata).setOnClickListener(v -> avviaGioco());
    }

    private void setupSpinner() {
        String[] paesi = {"Italia", "Germania", "Delaware (USA)", "Mercato Emergente"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, paesi);
        spinnerPaese.setAdapter(adapter);

        spinnerPaese.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                paeseSelezionato = parent.getSelectedItem().toString();
                aggiornaDescrizionePaese(paeseSelezionato);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void aggiornaDescrizionePaese(String paese) {
        String desc = "";
        switch (paese) {
            case "Italia": desc = "IRPEF Progressiva. BTP tassati al 12.5%."; break;
            case "Germania": desc = "Tasse alte ma servizi top. Capital gain 25%."; break;
            case "Delaware (USA)": desc = "No tasse statali, ma costo della vita estremo."; break;
            default: desc = "Mercato selvaggio. Alto rischio, tasse casuali."; break;
        }
        txtDescrizionePaese.setText(desc);
    }

    private void avviaGioco() {
        String etaS = editEtaPartenza.getText().toString();
        if (etaS.isEmpty()) {
            Toast.makeText(this, "Inserisci la tua età!", Toast.LENGTH_SHORT).show();
            return;
        }

        long obiettivo = 0;
        int idRadio = groupObiettivo.getCheckedRadioButtonId();
        if (idRadio == R.id.radio_1mln) obiettivo = 1_000_000L;
        else if (idRadio == R.id.radio_1mld) obiettivo = 1_000_000_000L;
        else if (idRadio == R.id.radio_1bil) obiettivo = 1_000_000_000_000L;
        else {
            Toast.makeText(this, "Seleziona un obiettivo!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Creiamo l'Intent per passare alla schermata di gioco
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("ETA_PARTENZA", Integer.parseInt(etaS));
        intent.putExtra("PAESE", paeseSelezionato);
        intent.putExtra("OBIETTIVO", obiettivo);

        startActivity(intent);
        // finish(); // Decommenta se vuoi chiudere il menu così non si può tornare indietro col tasto back
    }
}