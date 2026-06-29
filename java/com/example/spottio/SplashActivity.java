package com.example.spottio;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Versione moderna per nascondere la barra di stato (Edge-to-Edge)
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Handler aggiornato: usiamo Looper.getMainLooper() per evitare il messaggio di deprecazione
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Passaggio alla schermata di Login (MainActivity)
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Chiude la Splash così non si può tornare indietro col tasto back
        }, 3000); // 3 secondi di attesa
    }
}