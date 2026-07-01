package com.example.gameplus;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CharadesActivity extends AppCompatActivity {

    private TextView tvAzione;
    private List<String> mazzoMimi;
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charades);

        tvAzione = findViewById(R.id.tvAzioneDaMimare);
        Button btnSuccessivo = findViewById(R.id.btnSuccessivoMimo);

        // Lista di azioni tipiche per Charades/Mimo
        String[] azioni = {
                "CUCINARE UNA PIZZA", "CAMBIARE UNA LAMPADINA", "SCIARE",
                "LAVARSI I DENTI", "GIOCARE A TENNIS", "GUIDARE UN BUS",
                "PESCARE", "DIPINGERE UN QUADRO", "FARE IL CAFFÈ",
                "SUONARE IL VIOLINO", "SCATTARE UN SELFIE"
        };

        mazzoMimi = new ArrayList<>(Arrays.asList(azioni));
        Collections.shuffle(mazzoMimi);

        aggiornaMimo();

        btnSuccessivo.setOnClickListener(v -> {
            index++;
            if (index >= mazzoMimi.size()) {
                index = 0;
                Collections.shuffle(mazzoMimi);
            }
            aggiornaMimo();
        });
    }

    private void aggiornaMimo() {
        tvAzione.setText(mazzoMimi.get(index));
    }
}