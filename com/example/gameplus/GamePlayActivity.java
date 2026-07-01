package com.example.gameplus;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GamePlayActivity extends AppCompatActivity {

    private String modalitaScelta;
    private TextView txtSfida;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_play);

        // Recupera la modalità passata dall'Intent
        modalitaScelta = getIntent().getStringExtra("MODALITA");
        txtSfida = findViewById(R.id.txt_sfida);

        Button btnVerita = findViewById(R.id.btn_verita);
        Button btnObbligo = findViewById(R.id.btn_obbligo);

        btnVerita.setOnClickListener(v -> {
            txtSfida.setText(DomandeDatabase.getCasuale("Verità", modalitaScelta));
        });

        btnObbligo.setOnClickListener(v -> {
            txtSfida.setText(DomandeDatabase.getCasuale("Obbligo", modalitaScelta));
        });
    }
}