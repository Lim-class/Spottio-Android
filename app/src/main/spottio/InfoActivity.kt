package com.example.spottio

import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val tvTitle = findViewById<TextView>(R.id.tvInfoTitle)
        val tvContent = findViewById<TextView>(R.id.tvInfoContent)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        // Recupera la lingua salvata dalle SharedPreferences
        val prefs = getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
        val currentLangCode = prefs.getString("app_lang_code", "it") ?: "it"

        // Crea il contesto tradotto localizzato
        val locCtx = LanguageHelper.getLocalizedContext(this, currentLangCode)

        // Imposta i testi in base al tipo richiesto senza duplicare la logica nel Fragment
        when (intent.getStringExtra("info_type")) {
            "storia" -> {
                tvTitle.text = locCtx.getString(R.string.storia_titolo)
                tvContent.text = locCtx.getString(R.string.storia_testo)
            }
            "policy" -> {
                tvTitle.text = locCtx.getString(R.string.policy_titolo)
                tvContent.text = locCtx.getString(R.string.policy_testo)
            }
            "privacy" -> {
                tvTitle.text = locCtx.getString(R.string.privacy_titolo)
                tvContent.text = locCtx.getString(R.string.privacy_testo)
            }
        }
    }
}