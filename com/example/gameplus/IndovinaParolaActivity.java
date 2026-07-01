package com.example.gameplus;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IndovinaParolaActivity extends AppCompatActivity {

    private TextView tvParola;
    private List<String> mazzoParole;
    private int indiceCorrente = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indovina_parola);

        tvParola = findViewById(R.id.tvParolaDaIndovinare);
        Button btnProssima = findViewById(R.id.btnProssima);

        // Prepariamo un mazzo di parole (puoi aggiungerne quante ne vuoi)
        String[] parole = {
                "PIZZA", "CALCIO", "ELEFANTE", "CHITARRA", "CINEMA",
                "TELEFONO", "SPAGHETTI", "BATTERIA", "DENTISTA",
                "SCUOLA", "GIUNGLA", "AEREO", "BICICLETTA", "GATTO"
        };

        mazzoParole = new ArrayList<>(Arrays.asList(parole));

        // Mischia il mazzo all'inizio
        Collections.shuffle(mazzoParole);

        // Mostra la prima parola
        aggiornaParola();

        btnProssima.setOnClickListener(v -> {
            indiceCorrente++;
            if (indiceCorrente < mazzoParole.size()) {
                aggiornaParola();
            } else {
                // Se le parole finiscono, ricomincia da capo e rimischia
                indiceCorrente = 0;
                Collections.shuffle(mazzoParole);
                aggiornaParola();
            }
        });
    }

    private void aggiornaParola() {
        tvParola.setText(mazzoParole.get(indiceCorrente));
    }
}

//Heads Up!