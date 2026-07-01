package com.example.gameplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ObbligoVeritaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obbligo_verita);

        Button btnClassic = findViewById(R.id.btn_mode_classic);
        Button btnParty = findViewById(R.id.btn_mode_party);
        Button btnHard = findViewById(R.id.btn_mode_hard);

        // Esempio di gestione dei clic per le diverse modalità
        btnClassic.setOnClickListener(v -> avviaGioco("Classico"));
        btnParty.setOnClickListener(v -> avviaGioco("Party"));
        btnHard.setOnClickListener(v -> { Intent intent = new Intent(this, LoginEstremoActivity.class); startActivity(intent); });
    }

    private void avviaGioco(String modalita) {
        Intent intent = new Intent(this, GamePlayActivity.class);
        intent.putExtra("MODALITA", modalita); // Passa la modalità scelta
        startActivity(intent);
    }
}