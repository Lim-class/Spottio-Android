package com.example.spottio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        TextView tvTitle = findViewById(R.id.tvInfoTitle);
        TextView tvContent = findViewById(R.id.tvInfoContent);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Recupera la lingua salvata dalla MainActivity
        SharedPreferences prefs = getSharedPreferences("SpottioPrefs", MODE_PRIVATE);
        String currentLangCode = prefs.getString("app_lang_code", "it");

        // Crea il contesto tradotto
        Context locCtx = LanguageHelper.getLocalizedContext(this, currentLangCode);

        String type = getIntent().getStringExtra("info_type");

        if (type != null) {
            if (type.equals("storia")) {
                tvTitle.setText(locCtx.getString(R.string.storia_titolo));
                tvContent.setText(locCtx.getString(R.string.storia_testo));
            } else if (type.equals("policy")) {
                tvTitle.setText(locCtx.getString(R.string.policy_titolo));
                tvContent.setText(locCtx.getString(R.string.policy_testo));
            } else if (type.equals("privacy")) {
                tvTitle.setText(locCtx.getString(R.string.privacy_titolo));
                tvContent.setText(locCtx.getString(R.string.privacy_testo));
            }
        }
    }
}