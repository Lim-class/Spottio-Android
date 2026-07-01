package com.example.gameplus;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class ImpostoreActivity extends AppCompatActivity {

    // Database delle parole
    private String[] listaParole = {
            // Cibo e Bevande
            "Hamburger", "Gelato", "Cioccolato", "Lasagna", "Caffè", "Panettone", "Kebab", "Mortadella", "Nutella", "Popcorn",
            "Spaghetti", "Biscotto", "Formaggio", "Torta", "Arancia", "Banana", "Vino", "Birra", "Miele", "Patatine",

            // Luoghi e Città
            "Roma", "Milano", "Londra", "New York", "Giappone", "Egitto", "Spiaggia", "Montagna", "Ospedale", "Aeroporto",
            "Stazione", "Biblioteca", "Colosseo", "Deserto", "Giungla", "Piscina", "Bosco", "Castello", "Museo", "Stadio",

            // Animali
            "Cane", "Leone", "Elefante", "Giraffa", "Squalo", "Delfino", "Pinguino", "Tigre", "Scimmia", "Farfalla",
            "Pappagallo", "Zanzara", "Serpente", "Cavallo", "Pecora", "Gallina", "Aquila", "Balena", "Criceto", "Lupo",

            // Oggetti Comuni
            "Zaino", "Martello", "Ombrello", "Chitarra", "Specchio", "Occhiali", "Orologio", "Portafoglio", "Lampada", "Cuscino",
            "Forbici", "Divano", "Quadro", "Computer", "Frigorifero", "Bicicletta", "Candela", "Palla", "Pennello", "Radio",

            // Professioni e Hobby
            "Dottore", "Poliziotto", "Cuoco", "Pompiere", "Calciatore", "Pittore", "Meccanico", "Pilota", "Insegnante", "Avvocato",
            "Cantante", "Fotografo", "Giardiniere", "Attore", "Scrittore", "Ballerino", "Sarto", "Mago", "Marinaio", "Fidanzato",

            // Varie / Astratte
            "Amore", "Sogno", "Vittoria", "Natale", "Musica", "Soldi", "Guerra", "Silenzio", "Fuoco", "Pioggia"
    };

    private String parolaScelta;
    private String nomeImpostore;
    private ArrayList<String> nomiInGioco = new ArrayList<>(); // Giocatori ancora vivi nel round corrente
    private static Map<String, Integer> punteggiGlobali = new HashMap<>(); // Punteggi permanenti tra i round

    private int indexCorrente = 0;
    private boolean parolaRivelata = false;
    private boolean primoGiroRound = true; // Per mostrare la parola solo la prima volta

    private TextView txtGiocatore, txtParola, txtClassifica;
    private EditText editNomi;
    private LinearLayout layoutSetup, layoutGioco;
    private Button btnProssimo, btnConferma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_impostore);

        // Inizializzazione UI
        txtGiocatore = findViewById(R.id.txt_giocatore_num);
        txtParola = findViewById(R.id.txt_parola_segreta);
        txtClassifica = findViewById(R.id.txt_classifica);
        editNomi = findViewById(R.id.edit_nomi_giocatori);
        layoutSetup = findViewById(R.id.layout_setup_nomi);
        layoutGioco = findViewById(R.id.layout_gioco);
        btnProssimo = findViewById(R.id.btn_prossimo);
        btnConferma = findViewById(R.id.btn_conferma_nomi);

        // Eventi
        btnConferma.setOnClickListener(v -> setupPartita());
        btnProssimo.setOnClickListener(v -> gestisciTurno());
        txtParola.setOnClickListener(v -> rivelaParola());
    }

    private void setupPartita() {
        String input = editNomi.getText().toString();
        if (input.trim().isEmpty()) {
            Toast.makeText(this, "Inserisci almeno 3 nomi!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] split = input.split(",");
        punteggiGlobali.clear();
        for (String s : split) {
            String nome = s.trim();
            if (!nome.isEmpty()) punteggiGlobali.put(nome, 0);
        }

        if (punteggiGlobali.size() < 3) {
            Toast.makeText(this, "Servono almeno 3 giocatori!", Toast.LENGTH_SHORT).show();
            return;
        }

        mostraRegoleEInizia();
    }

    private void mostraRegoleEInizia() {
        new AlertDialog.Builder(this)
                .setTitle("Regole dell'Impostore")
                .setMessage("OBIETTIVO:\nI Civili devono trovare l'Impostore. L'Impostore deve sopravvivere.\n\nPUNTEGGI:\n" +
                        "✅ +1 punto ai Civili superstiti se vincono.\n" +
                        "❌ -1 punto a chi viene eliminato per errore.\n" +
                        "🏆 +3 punti all'Impostore se vince (rimangono in 2).\n\n" +
                        "ATTENZIONE: Vedrete la parola solo al primo turno!")
                .setPositiveButton("INIZIA", (d, w) -> nuovoRound())
                .setCancelable(false)
                .show();
    }

    private void nuovoRound() {
        // Ripristiniamo tutti i partecipanti per il nuovo round
        nomiInGioco = new ArrayList<>(punteggiGlobali.keySet());

        // Scegliamo parola e impostore casualmente
        parolaScelta = listaParole[new Random().nextInt(listaParole.length)];
        nomeImpostore = nomiInGioco.get(new Random().nextInt(nomiInGioco.size()));

        primoGiroRound = true; // Nel primo giro mostriamo la parola
        indexCorrente = 0;
        parolaRivelata = false;

        layoutSetup.setVisibility(View.GONE);
        layoutGioco.setVisibility(View.VISIBLE);

        aggiornaUI();
        aggiornaClassificaUI();
    }

    private void rivelaParola() {
        if (parolaRivelata || !primoGiroRound) return;

        String giocatoreAttuale = nomiInGioco.get(indexCorrente);
        if (giocatoreAttuale.equals(nomeImpostore)) {
            txtParola.setText("SEI L'IMPOSTORE!");
            txtParola.setTextColor(android.graphics.Color.RED);
        } else {
            txtParola.setText(parolaScelta);
            txtParola.setTextColor(android.graphics.Color.GREEN);
        }
        parolaRivelata = true;
    }

    private void gestisciTurno() {
        if (!parolaRivelata && primoGiroRound) {
            Toast.makeText(this, "Tocca il riquadro per vedere la parola!", Toast.LENGTH_SHORT).show();
            return;
        }

        indexCorrente++;

        if (indexCorrente < nomiInGioco.size()) {
            // C'è un altro giocatore che deve vedere la parola
            parolaRivelata = false;
            aggiornaUI();
        } else {
            // Tutti hanno visto la parola, passiamo alla votazione
            primoGiroRound = false;
            avviaVotazione();
        }
    }

    private void avviaVotazione() {
        txtParola.setText("DISCUTETE E VOTATE!");
        txtParola.setTextColor(android.graphics.Color.YELLOW);
        txtGiocatore.setText("Fase di Votazione");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chi volete eliminare?");

        String[] vivi = nomiInGioco.toArray(new String[0]);
        builder.setItems(vivi, (dialog, which) -> processaVoto(vivi[which]));
        builder.setCancelable(false);
        builder.show();
    }

    private void processaVoto(String votato) {
        if (votato.equals(nomeImpostore)) {
            // Vittoria Civili
            for (String n : nomiInGioco) {
                if (!n.equals(nomeImpostore)) aggiungiPunto(n, 1);
            }
            fineRoundDialog("CIVILI VINCITORI! L'impostore era " + nomeImpostore);
        } else {
            // Errore
            aggiungiPunto(votato, -1);
            nomiInGioco.remove(votato);

            if (nomiInGioco.size() <= 2) {
                // Vittoria Impostore
                aggiungiPunto(nomeImpostore, 3);
                fineRoundDialog("L'IMPOSTORE HA VINTO! " + nomeImpostore + " vi ha ingannati.");
            } else {
                // Continua il round senza far rivedere la parola
                Toast.makeText(this, votato + " era innocente ed è fuori. Continuate a discutere!", Toast.LENGTH_LONG).show();
                aggiornaClassificaUI();
                avviaVotazione();
            }
        }
    }

    private void aggiungiPunto(String nome, int punti) {
        if (punteggiGlobali.containsKey(nome)) {
            punteggiGlobali.put(nome, punteggiGlobali.get(nome) + punti);
        }
    }

    private void fineRoundDialog(String messaggio) {
        aggiornaClassificaUI();
        new AlertDialog.Builder(this)
                .setTitle("Round Concluso")
                .setMessage(messaggio + "\n\nCosa volete fare?")
                .setPositiveButton("Prossimo Round", (d, w) -> nuovoRound())
                .setNegativeButton("Reset Partita", (d, w) -> {
                    layoutGioco.setVisibility(View.GONE);
                    layoutSetup.setVisibility(View.VISIBLE);
                })
                .setCancelable(false)
                .show();
    }

    private void aggiornaUI() {
        txtGiocatore.setText("Giocatore: " + nomiInGioco.get(indexCorrente));
        txtParola.setText("TOCCA PER VEDERE");
        txtParola.setTextColor(android.graphics.Color.WHITE);
    }

    private void aggiornaClassificaUI() {
        StringBuilder sb = new StringBuilder("CLASSIFICA PUNTI:\n");
        // Ordiniamo per far vedere i punteggi (opzionale)
        for (Map.Entry<String, Integer> entry : punteggiGlobali.entrySet()) {
            sb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        txtClassifica.setText(sb.toString());
    }
}