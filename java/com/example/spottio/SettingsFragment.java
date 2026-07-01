package com.example.spottio;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment implements SettingsAdapter.OnBindLayoutListener {

    private SettingsAdapter adapter;
    private String currentUsername;

    private SettingsAuthManager authManager;
    private SettingsPrivacyManager privacyManager;
    private SettingsExportManager exportManager;

    public SettingsFragment() {
        // Costruttore pubblico obbligatorio
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        var view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Recupero sessione utente locale
        var context = getContext();
        if (context != null) {
            currentUsername = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
                    .getString("username_attivo", "Guest");
        } else {
            currentUsername = "Guest";
        }

        // Inizializzazione Manager
        authManager = new SettingsAuthManager(FirebaseAuth.getInstance(), context, currentUsername);
        privacyManager = new SettingsPrivacyManager(FirebaseFirestore.getInstance(), context, currentUsername);
        exportManager = new SettingsExportManager(FirebaseFirestore.getInstance(), context, currentUsername);

        var searchSettings = (EditText) view.findViewById(R.id.searchSettings);
        var recyclerView = (RecyclerView) view.findViewById(R.id.recyclerViewSettings);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SettingsAdapter(SettingsMenuProvider.getMenuItems(), this);
        recyclerView.setAdapter(adapter);

        searchSettings.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    @Override
    public void onBindInnerLayout(SettingsAdapter.SettingType type, View innerView, SettingsAdapter.SettingItem item) {
        // MAGIA JAVA 14+: Switch potenziato senza break!
        switch (type) {
            case PASSWORD -> authManager.setupPasswordLogic(innerView);
            case CAMBIO_EMAIL -> authManager.setupEmailLogic(innerView);
            case PRIVACY_PROFILO -> privacyManager.setupPrivacyLogic(innerView);
            case ESPORTA_DATI -> exportManager.setupExportLogic(innerView);
            case COLORE_SFONDO -> setStaticInfo(innerView, "La modifica del colore di sfondo è disponibile esclusivamente nella versione web dell'applicazione.");
            case ELIMINA_ACCOUNT -> setStaticInfo(innerView, "admin".equalsIgnoreCase(currentUsername) ?
                    "L'account amministratore (admin) non può essere rimosso dal sistema di storage locale o remoto." :
                    "Per eliminare definitivamente il tuo account, invia una richiesta formale al supporto tecnico tramite i canali ufficiali.");
            case SCARICA_APK -> setStaticInfo(innerView, "Stai già utilizzando l'applicazione Android nativa. Gli aggiornamenti futuri verranno notificati in questa sezione.");
            case STORIA -> setStaticInfo(innerView, "Cronologia Account: Nessuna attività recente registrata nelle ultime 48 ore.");
            case INFORMATIVA_PRIVACY -> setStaticInfo(innerView, "Spottio tutela i tuoi dati personali ai sensi del GDPR. I dati di profilazione dell'algoritmo vengono cancellati automaticamente dopo 30 giorni di totale inattività.");
            case POLICY_COMMUNITY -> setStaticInfo(innerView, "È severamente vietato pubblicare contenuti offensivi, spam o multimediali protetti da copyright. Le violazioni ripetute comportano la sospensione permanente.");
        }
    }

    private void setStaticInfo(View innerView, String text) {
        var txtInfoText = (TextView) innerView.findViewById(R.id.txtInfoText);
        if (txtInfoText != null) {
            txtInfoText.setText(text);
        }
    }
}