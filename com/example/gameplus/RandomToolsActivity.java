package com.example.gameplus;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class RandomToolsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random_tools);

        Button btnMoneta = findViewById(R.id.btn_lancia_moneta);
        TextView txtMoneta = findViewById(R.id.txt_risultato_moneta);

        Random random = new Random();

        // Logica Moneta
        btnMoneta.setOnClickListener(v -> {
            boolean testa = random.nextBoolean();
            txtMoneta.setText(testa ? "TESTA" : "CROCE");
        });
    }
}