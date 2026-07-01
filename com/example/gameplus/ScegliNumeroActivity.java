package com.example.gameplus;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class ScegliNumeroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scegli_numero);

        EditText editMin = findViewById(R.id.edit_min);
        EditText editMax = findViewById(R.id.edit_max);
        Button btnGenera = findViewById(R.id.btn_genera);
        TextView txtRisultato = findViewById(R.id.txt_risultato);

        btnGenera.setOnClickListener(v -> {
            String sMin = editMin.getText().toString();
            String sMax = editMax.getText().toString();

            if (!sMin.isEmpty() && !sMax.isEmpty()) {
                try {
                    // Usiamo Long per leggere il numero e verificare se supera il limite Integer
                    long minLong = Long.parseLong(sMin);
                    long maxLong = Long.parseLong(sMax);

                    // Controllo superamento limite Intero (2.147.483.647)
                    if (minLong > Integer.MAX_VALUE || maxLong > Integer.MAX_VALUE) {
                        Toast.makeText(this, "Errore: superato il valore massimo intero (2.147.483.647)", Toast.LENGTH_LONG).show();
                        return; // Interrompe l'esecuzione
                    }

                    int min = (int) minLong;
                    int max = (int) maxLong;

                    if (max > min) {
                        Random random = new Random();
                        int estratto = random.nextInt((max - min) + 1) + min;
                        txtRisultato.setText(String.valueOf(estratto));
                    } else {
                        Toast.makeText(this, "Il massimo deve essere maggiore del minimo", Toast.LENGTH_SHORT).show();
                    }

                } catch (NumberFormatException e) {
                    // Questo cattura numeri talmente grandi da superare anche il tipo Long
                    Toast.makeText(this, "Il numero inserito è troppo grande o non valido", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Inserisci entrambi i numeri", Toast.LENGTH_SHORT).show();
            }
        });
    }
}