package com.example.gameplus;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity {

    private GameEngine engine;

    // UI Elements
    private TextView txtLiquidita, txtValorePortafoglio, txtTassePagate, txtEta, txtStipendio;
    private TextView txtAssetPrezzo, txtAssetPosseduti, txtMaxAcquistabili, lblLiquidita;
    private EditText editQuantitaAsset;
    private Spinner spinnerStrumenti;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        inizializzaEngine();
        collegaUI();
        setupListeners();
        aggiornaDashboard();
    }

    private void inizializzaEngine() {
        Intent i = getIntent();
        int etaPartenza = i.getIntExtra("ETA_PARTENZA", 18);
        String paeseSelezionato = i.getStringExtra("PAESE");
        long obiettivoFinanziario = i.getLongExtra("OBIETTIVO", 1000000);

        engine = new GameEngine(etaPartenza, paeseSelezionato, obiettivoFinanziario);
    }

    private void collegaUI() {
        lblLiquidita = findViewById(R.id.lbl_liquidita);
        txtLiquidita = findViewById(R.id.txt_liquidita);
        txtValorePortafoglio = findViewById(R.id.txt_valore_portafoglio);
        txtTassePagate = findViewById(R.id.txt_tasse_pagate);
        txtEta = findViewById(R.id.txt_eta);
        txtStipendio = findViewById(R.id.txt_stipendio);

        txtAssetPrezzo = findViewById(R.id.txt_asset_prezzo);
        txtAssetPosseduti = findViewById(R.id.txt_asset_posseduti);
        txtMaxAcquistabili = findViewById(R.id.txt_max_acquistabili);
        editQuantitaAsset = findViewById(R.id.edit_quantita_asset);
        spinnerStrumenti = findViewById(R.id.spinner_strumenti_finanziari);

        List<String> nomiAssets = new ArrayList<>();
        for (Asset a : engine.getMarketManager().getAssets()) nomiAssets.add(a.nome);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, nomiAssets);
        spinnerStrumenti.setAdapter(adapter);
    }

    private void setupListeners() {
        spinnerStrumenti.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { aggiornaDettaglioAsset(); }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });

        findViewById(R.id.btn_cerca_lavoro).setOnClickListener(v -> gestisciTurno(engine.lavoraSemestre()));
        findViewById(R.id.btn_startup).setOnClickListener(v -> gestisciTurno(engine.tentaStartup()));
        findViewById(R.id.btn_dettaglio_portafoglio).setOnClickListener(v -> mostraPortafoglio());
        findViewById(R.id.btn_investi_asset).setOnClickListener(v -> eseguiAcquisto());
        findViewById(R.id.btn_vendi_asset).setOnClickListener(v -> eseguiVendita());
    }

    private void eseguiAcquisto() {
        String qS = editQuantitaAsset.getText().toString();
        if (qS.isEmpty()) return;

        try {
            long q = Long.parseLong(qS);
            int index = spinnerStrumenti.getSelectedItemPosition();
            if (engine.compraAsset(index, q)) {
                editQuantitaAsset.setText("");
                Toast.makeText(this, "Acquisto effettuato!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Liquidità insufficiente!", Toast.LENGTH_SHORT).show();
            }
            aggiornaDashboard();
            aggiornaDettaglioAsset();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valore non valido", Toast.LENGTH_SHORT).show();
        }
    }

    private void eseguiVendita() {
        String qS = editQuantitaAsset.getText().toString();
        if (qS.isEmpty()) return;

        try {
            long q = Long.parseLong(qS);
            int index = spinnerStrumenti.getSelectedItemPosition();
            double tasse = engine.vendiAsset(index, q);

            if (tasse >= 0) {
                editQuantitaAsset.setText("");
                Toast.makeText(this, "Vendita eseguita. Tasse trattenute: " + formatCurrency(tasse), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Non ne possiedi abbastanza!", Toast.LENGTH_SHORT).show();
            }
            aggiornaDashboard();
            aggiornaDettaglioAsset();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Valore non valido", Toast.LENGTH_SHORT).show();
        }
    }

    private void gestisciTurno(String messaggioEsito) {
        Toast.makeText(this, messaggioEsito, Toast.LENGTH_SHORT).show();
        aggiornaDashboard();
        aggiornaDettaglioAsset();
        controllaFineGioco();
    }

    private void aggiornaDettaglioAsset() {
        int pos = spinnerStrumenti.getSelectedItemPosition();
        if (pos < 0) return;

        Asset a = engine.getMarketManager().getAsset(pos);
        txtAssetPrezzo.setText("Prezzo: " + formatCurrency(a.prezzoCorrente));
        txtAssetPosseduti.setText("Tuoi: " + a.quantitaPosseduta);

        long maxBuy = engine.getLiquidita() > 0 ? (long) (engine.getLiquidita() / a.prezzoCorrente) : 0;
        txtMaxAcquistabili.setText("Max: " + maxBuy);
    }

    private void aggiornaDashboard() {
        double liquidita = engine.getLiquidita();
        if (liquidita >= 0) {
            lblLiquidita.setText("DISPONIBILITÀ");
            txtLiquidita.setTextColor(Color.parseColor("#52BE80")); // VERDE
        } else {
            lblLiquidita.setText("DEBITO BANCA");
            txtLiquidita.setTextColor(Color.parseColor("#C0392B")); // ROSSO
        }

        txtLiquidita.setText(formatCurrency(liquidita));
        txtValorePortafoglio.setText(formatCurrency(engine.getMarketManager().getValoreTotalePortafoglio()));
        txtTassePagate.setText(formatCurrency(engine.getTasseUltimoSemestre()));
        txtEta.setText("Età: " + (int)(engine.getMesiTotaliDiVita() / 12));
        txtStipendio.setText(engine.getTitoloLavoro() + ": " + formatCurrency(engine.getStipendioAnnuo()));
    }

    private void controllaFineGioco() {
        GameEngine.StatoGioco stato = engine.controllaStatoGioco();
        if (stato == GameEngine.StatoGioco.VITTORIA) {
            mostraFine("VITTORIA!", "Obiettivo raggiunto! Patrimonio: " + formatCurrency(engine.getPatrimonioTotale()));
        } else if (stato == GameEngine.StatoGioco.VECCHIAIA) {
            mostraFine("FINE GIOCO", "Sei arrivato a 100 anni.");
        } else if (stato == GameEngine.StatoGioco.BANCAROTTA) {
            mostraFine("BANCAROTTA", "Sei fallito.");
        }
    }

    private void mostraPortafoglio() {
        StringBuilder sb = new StringBuilder();
        for (Asset a : engine.getMarketManager().getAssets()) {
            if (a.quantitaPosseduta > 0) {
                sb.append(a.nome).append(" (").append(a.tipo).append(")\n")
                        .append("Qtà: ").append(a.quantitaPosseduta).append("\n")
                        .append("Valore: ").append(formatCurrency(a.getValoreTotale())).append("\n\n");
            }
        }
        if (sb.length() == 0) sb.append("Portafoglio vuoto.");
        new AlertDialog.Builder(this).setTitle("I tuoi Asset").setMessage(sb.toString()).setPositiveButton("OK", null).show();
    }

    private void mostraFine(String t, String m) {
        new AlertDialog.Builder(this).setTitle(t).setMessage(m).setCancelable(false)
                .setPositiveButton("Menu", (d, w) -> {
                    startActivity(new Intent(this, ChiVuolEssereRiccoActivity.class));
                    finish();
                }).show();
    }

    private String formatCurrency(double val) {
        return String.format("%,.0f €", val);
    }
}