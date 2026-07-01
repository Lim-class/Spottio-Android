package com.example.gameplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnTris, btnSudoku, btnImpostore, btnDama, btnObbligoVerita,
            btnImpiccato, btnTapChallenge, btnFlappyBird,
            btnIndovinaParola, btnRandomTools, btnScegliNumero, btnCharades, btnRicco, btnFanta;

    private Button btnUser; // Il nuovo pulsante per il profilo utente
    private EditText searchBar;
    private GridLayout mainGrid;

    private ImageButton[] tuttiIBottoni;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inizializzazione componenti UI
        searchBar = findViewById(R.id.search_bar);
        mainGrid = findViewById(R.id.main_grid);
        btnUser = findViewById(R.id.btn_user); // Inizializzazione del tasto profilo

        btnTris = findViewById(R.id.btn_tris);
        btnSudoku = findViewById(R.id.btn_sudoku);
        btnImpostore = findViewById(R.id.btn_impostore);
        btnDama = findViewById(R.id.btn_dama);
        btnObbligoVerita = findViewById(R.id.btn_obbligo_verita);
        btnImpiccato = findViewById(R.id.btn_impiccato);
        btnTapChallenge = findViewById(R.id.btn_tap_challenge);
        btnFlappyBird = findViewById(R.id.btn_flappy_bird);
        btnIndovinaParola = findViewById(R.id.btn_indovina_parola);
        btnRandomTools = findViewById(R.id.btn_random_tools);
        btnScegliNumero = findViewById(R.id.btn_scegli_numero);
        btnCharades = findViewById(R.id.btn_charades);
        btnRicco = findViewById(R.id.btn_ricco);
        btnFanta = findViewById(R.id.btn_fanta);

        // Riempimento array per la ricerca
        tuttiIBottoni = new ImageButton[]{
                btnTris, btnSudoku, btnImpostore, btnDama,
                btnObbligoVerita, btnImpiccato, btnTapChallenge, btnFlappyBird,
                btnIndovinaParola, btnRandomTools, btnScegliNumero, btnCharades, btnRicco, btnFanta
        };

        // Adatta la griglia in base alla larghezza schermo
        int screenWidthDp = getResources().getConfiguration().screenWidthDp;
        if (screenWidthDp >= 600) {
            mainGrid.setColumnCount(4);
        } else {
            mainGrid.setColumnCount(2);
        }

        // --- Logica di Navigazione Giochi ---
        btnTris.setOnClickListener(v -> startActivity(new Intent(this, TrisActivity.class)));
        btnSudoku.setOnClickListener(v -> startActivity(new Intent(this, SudokuActivity.class)));
        btnImpostore.setOnClickListener(v -> startActivity(new Intent(this, ImpostoreActivity.class)));
        btnDama.setOnClickListener(v -> startActivity(new Intent(this, DamaActivity.class)));
        btnObbligoVerita.setOnClickListener(v -> startActivity(new Intent(this, ObbligoVeritaActivity.class)));
        btnImpiccato.setOnClickListener(v -> startActivity(new Intent(this, ImpiccatoActivity.class)));
        btnTapChallenge.setOnClickListener(v -> startActivity(new Intent(this, TapChallengeActivity.class)));
        btnFlappyBird.setOnClickListener(v -> startActivity(new Intent(this, FlappyBirdActivity.class)));
        btnIndovinaParola.setOnClickListener(v -> startActivity(new Intent(this, IndovinaParolaActivity.class)));
        btnRandomTools.setOnClickListener(v -> startActivity(new Intent(this, RandomToolsActivity.class)));
        btnScegliNumero.setOnClickListener(v -> startActivity(new Intent(this, ScegliNumeroActivity.class)));
        btnCharades.setOnClickListener(v -> startActivity(new Intent(this, CharadesActivity.class)));
        btnRicco.setOnClickListener(v -> startActivity(new Intent(this, ChiVuolEssereRiccoActivity.class)));
        btnFanta.setOnClickListener(v -> {
            // Recuperiamo lo stato del login
            SharedPreferences prefs = getSharedPreferences("SpottioPrefs", MODE_PRIVATE);
            String usernameSalvato = prefs.getString("username_attivo", "Guest");

            if (usernameSalvato.equals("Guest")) {
                // Se l'utente non è loggato, mostriamo un avviso e lo mandiamo alla login
                Toast.makeText(this, "Devi effettuare il login per accedere a FantaPlus!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, FantaLoginActivity.class));
            } else {
                // Se è loggato, può entrare
                startActivity(new Intent(this, HomeFantaActivity.class));
            }
        });

        // --- Logica Tasto Utente ---
        btnUser.setOnClickListener(v -> {
            // Indirizza alla FantaLoginActivity
            startActivity(new Intent(MainActivity.this, FantaLoginActivity.class));
        });

        // --- Logica della Barra di Ricerca ---
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                if (tuttiIBottoni == null) return;

                for (ImageButton btn : tuttiIBottoni) {
                    if (btn == null) continue;
                    String contentDesc = btn.getContentDescription() != null ? btn.getContentDescription().toString().toLowerCase() : "";
                    if (contentDesc.contains(query)) {
                        btn.setVisibility(View.VISIBLE);
                    } else {
                        btn.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recupero il nome dell'utente loggato dalle SharedPreferences di Spottio
        SharedPreferences prefs = getSharedPreferences("SpottioPrefs", MODE_PRIVATE);
        String usernameSalvato = prefs.getString("username_attivo", "Guest");

        // Imposta il nome sul pulsante (appare "Guest" se non loggato)
        if (btnUser != null) {
            btnUser.setText(usernameSalvato);
        }
    }
}