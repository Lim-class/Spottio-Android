package com.example.gameplus;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class ImpiccatoActivity extends AppCompatActivity {

    private String parolaSegreta;
    private char[] parolaVisualizzata;
    private int errori = 0;
    private final int MAX_ERRORI = 6;
    private String[] dizionario = {"ANDROID", "JAVA", "PROGRAMMAZIONE", "STUDIO", "GIOCO", "COMPUTER", "SMARTPHONE"};

    private TextView txtParola, txtErrori;
    private EditText editLettera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_impiccato);

        txtParola = findViewById(R.id.txt_parola_nascosta);
        txtErrori = findViewById(R.id.txt_errori);
        editLettera = findViewById(R.id.edit_lettera);
        Button btnTenta = findViewById(R.id.btn_tenta);

        // All'avvio chiediamo la modalità
        scegliModalita();

        btnTenta.setOnClickListener(v -> {
            String input = editLettera.getText().toString().toUpperCase().trim();
            if (!input.isEmpty()) {
                controllaLettera(input.charAt(0));
                editLettera.setText("");
            }
        });
    }

    private void scegliModalita() {
        String[] opzioni = {"1 Giocatore (PC)", "2 Giocatori (Locale)"};

        new AlertDialog.Builder(this)
                .setTitle("Seleziona Modalità")
                .setCancelable(false)
                .setItems(opzioni, (dialog, which) -> {
                    if (which == 0) {
                        nuovaPartitaSingola();
                    } else {
                        chiediParolaGiocatore();
                    }
                })
                .show();
    }

    // Modalità 1: Parola casuale dal dizionario
    private void nuovaPartitaSingola() {
        parolaSegreta = dizionario[new Random().nextInt(dizionario.length)];
        inizializzaGioco(parolaSegreta);
    }

    // Modalità 2: Input da un utente
    private void chiediParolaGiocatore() {
        EditText inputParola = new EditText(this);
        inputParola.setHint("Inserisci la parola da indovinare");
        inputParola.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); // Nasconde la parola mentre la scrivi

        new AlertDialog.Builder(this)
                .setTitle("Giocatore 1")
                .setMessage("Inserisci una parola per il tuo amico:")
                .setView(inputParola)
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    String p = inputParola.getText().toString().toUpperCase().trim();
                    if (p.length() > 0) {
                        parolaSegreta = p;
                        inizializzaGioco(parolaSegreta);
                    } else {
                        nuovaPartitaSingola(); // Fallback se vuoto
                    }
                })
                .show();
    }

    private void inizializzaGioco(String parola) {
        parolaVisualizzata = new char[parola.length()];
        for (int i = 0; i < parola.length(); i++) {
            // Se ci sono spazi nella parola (per frasi), li mostriamo già
            if (parola.charAt(i) == ' ') parolaVisualizzata[i] = ' ';
            else parolaVisualizzata[i] = '_';
        }
        errori = 0;
        aggiornaUI();
    }

    private void controllaLettera(char c) {
        boolean trovata = false;
        for (int i = 0; i < parolaSegreta.length(); i++) {
            if (parolaSegreta.charAt(i) == c) {
                parolaVisualizzata[i] = c;
                trovata = true;
            }
        }

        if (!trovata) {
            errori++;
        }

        aggiornaUI();
        verificaFineGioco();
    }

    private void aggiornaUI() {
        // Mostra la parola con gli spazi tra le lettere per leggibilità
        StringBuilder visualizza = new StringBuilder();
        for (char c : parolaVisualizzata) {
            visualizza.append(c).append(" ");
        }
        txtParola.setText(visualizza.toString().trim());

        // Vite con emoji
        StringBuilder vite = new StringBuilder("Vite: ");
        int viteRimaste = MAX_ERRORI - errori;
        for (int i = 0; i < MAX_ERRORI; i++) {
            if (i < viteRimaste) vite.append("❤️ ");
            else vite.append("🖤 ");
        }
        txtErrori.setText(vite.toString());
    }

    private void verificaFineGioco() {
        String statoAttuale = String.valueOf(parolaVisualizzata);
        if (statoAttuale.equals(parolaSegreta)) {
            mostraDialogoFinale("🎉 VITTORIA!", "Ottimo lavoro! La parola era: " + parolaSegreta);
        } else if (errori >= MAX_ERRORI) {
            mostraDialogoFinale("💀 GAME OVER", "Peccato! La parola era: " + parolaSegreta);
        }
    }

    private void mostraDialogoFinale(String titolo, String messaggio) {
        new AlertDialog.Builder(this)
                .setTitle(titolo)
                .setMessage(messaggio)
                .setCancelable(false)
                .setPositiveButton("MENU PRINCIPALE", (d, w) -> scegliModalita())
                .show();
    }
}