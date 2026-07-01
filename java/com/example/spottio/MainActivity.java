package com.example.spottio;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etEmail;
    private TextInputLayout layoutEmail, layoutUsername, layoutPassword;
    private Button btnLogin, btnRegister, btnStoria, btnPolicy, btnPrivacy;
    private TextView tvSwitch, tvForgotPassword;
    private CheckBox cbTerms;

    private boolean isLoginMode = true;
    private String currentLangCode = "it";

    private AuthManager authManager;
    private LoginUIHelper uiHelper;
    private LanguageSelectorManager languageSelectorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Inizializza AuthManager e controlla la sessione
        authManager = new AuthManager(this);
        authManager.checkSessionAndRedirect();

        setContentView(R.layout.activity_main);

        // 2. Inizializza l'Helper per la UI
        uiHelper = new LoginUIHelper(this);

        // 3. Inizializza Views
        initViews();

        // 4. Carica la lingua corrente dal Manager Centralizzato
        currentLangCode = LanguageHelper.getCurrentLangCode(this);

        // 5. Inizializza il Gestore del Selettore Lingua
        languageSelectorManager = new LanguageSelectorManager(this);
        languageSelectorManager.init((code, flag, name) -> {

            // Salvataggio nel cervello centralizzato
            LanguageHelper.setLanguage(this, code, flag, name);

            // Aggiornamento grafico
            languageSelectorManager.updateLanguageUI(flag, name);
            currentLangCode = code;
            aggiornaInterfacciaAlVolo(code);
        });

        // 6. Aggiorna UI ed Eventi
        aggiornaInterfacciaAlVolo(currentLangCode);
        setupListeners();

        // FIX: Sincronizza la UI costringendola a nascondere l'username all'avvio
        uiHelper.toggleLoginMode(isLoginMode, currentLangCode);
    }

    private void initViews() {
        btnStoria = findViewById(R.id.btnStoria);
        btnPolicy = findViewById(R.id.btnPolicy);
        btnPrivacy = findViewById(R.id.btnPrivacy);
        cbTerms = findViewById(R.id.cbTerms);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutUsername = findViewById(R.id.layoutUsername);
        layoutPassword = findViewById(R.id.layoutPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvSwitch = findViewById(R.id.tvSwitch);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    private void setupListeners() {
        btnStoria.setOnClickListener(v -> apriInfo("storia"));
        btnPolicy.setOnClickListener(v -> apriInfo("policy"));
        btnPrivacy.setOnClickListener(v -> apriInfo("privacy"));

        btnLogin.setOnClickListener(v -> {
            var email = etEmail.getText().toString().trim();
            var password = etPassword.getText().toString().trim();

            // Passiamo l'email ad AuthManager
            authManager.handleLogin(email, password);
        });

        btnRegister.setOnClickListener(v -> {
            if (!cbTerms.isChecked()) {
                var locCtx = LanguageHelper.getLocalizedContext(this, currentLangCode);
                Toast.makeText(this, locCtx.getString(R.string.error_terms), Toast.LENGTH_LONG).show();
                return;
            }
            authManager.handleRegister(
                    etEmail.getText().toString().trim(),
                    etPassword.getText().toString().trim(),
                    etUsername.getText().toString().trim(),
                    etUsername
            );
        });

        tvForgotPassword.setOnClickListener(v -> uiHelper.showResetPasswordDialog(currentLangCode, authManager));

        tvSwitch.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            uiHelper.toggleLoginMode(isLoginMode, currentLangCode);
        });
    }

    public void aggiornaInterfacciaAlVolo(String langCode) {
        var locCtx = LanguageHelper.getLocalizedContext(this, langCode);

        // Aggiornamento dei suggerimenti (Hint) dei campi di testo
        layoutEmail.setHint(locCtx.getString(R.string.hint_email));
        layoutUsername.setHint(locCtx.getString(R.string.hint_username));
        layoutPassword.setHint(locCtx.getString(R.string.hint_password));

        // Aggiornamento dei testi dei pulsanti e delle scritte
        tvForgotPassword.setText(locCtx.getString(R.string.forgot_password));
        btnLogin.setText(locCtx.getString(R.string.btn_login));
        btnRegister.setText(locCtx.getString(R.string.btn_register));
        btnStoria.setText(locCtx.getString(R.string.btn_storia));
        btnPolicy.setText(locCtx.getString(R.string.btn_policy));
        btnPrivacy.setText(locCtx.getString(R.string.btn_privacy));

        // Aggiornamento del testo dello switch (Login / Registrazione)
        tvSwitch.setText(isLoginMode ? locCtx.getString(R.string.switch_to_register) : locCtx.getString(R.string.switch_to_login));

        // Configurazione dei link interni alla Checkbox dei termini
        LanguageHelper.configuraCheckboxLink(cbTerms, locCtx,
                () -> apriInfo("privacy"),
                () -> apriInfo("policy"));

        // FIX: Forza la visibilità corretta dei campi
        if (uiHelper != null) {
            uiHelper.toggleLoginMode(isLoginMode, langCode);
        }
    }

    public void apriInfo(String tipo) {
        var intent = new Intent(MainActivity.this, InfoActivity.class);
        intent.putExtra("info_type", tipo);
        startActivity(intent);
    }

    // --- GETTER NECESSARI PER L'HELPER DELLA UI ---
    public TextInputLayout getLayoutEmail() { return layoutEmail; }
    public TextInputLayout getLayoutUsername() { return layoutUsername; }
    public Button getBtnLogin() { return btnLogin; }
    public Button getBtnRegister() { return btnRegister; }
    public TextView getTvForgotPassword() { return tvForgotPassword; }
    public TextView getTvSwitch() { return tvSwitch; }
    public CheckBox getCbTerms() { return cbTerms; }
}