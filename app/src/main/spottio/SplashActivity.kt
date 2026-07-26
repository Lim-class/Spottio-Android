package com.example.spottio

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Versione moderna per nascondere la barra di stato (Edge-to-Edge)
        // Il punto interrogativo si occupa di verificare se la barra esiste prima di nasconderla!
        supportActionBar?.hide()

        // Handler aggiornato usando Looper.getMainLooper() per evitare deprecazioni
        Handler(Looper.getMainLooper()).postDelayed({
            // Passaggio alla schermata di Login (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Chiude la Splash così non si può tornare indietro col tasto back
        }, 3000) // 3 secondi di attesa
    }
}