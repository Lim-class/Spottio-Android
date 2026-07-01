package com.example.gameplus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Nasconde la barra di stato per il pieno schermo
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash);

        // Nasconde l'ActionBar se presente
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageView logo = findViewById(R.id.img_logo);

        // Animazione di entrata
        logo.setAlpha(0f);
        logo.animate().alpha(1f).setDuration(1500).start();

        // Passaggio alla MainActivity dopo 3 secondi
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 3000);
    }
}