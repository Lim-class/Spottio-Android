package com.example.gameplus;

public class Lega {
    private String nome;
    private String moderatoreID;
    private int creditiIniziali;
    private String password;

    // Modalità di acquisto (standard, asta, multiuso)
    private String modalitaAcquisto;

    // Variabili per gli orari
    private String giornoInizioMercato;
    private String oraInizioMercato;
    private String giornoFineMercato;
    private String oraFineMercato;
    private String giornoCalcolo;
    private String oraCalcolo;

    public Lega() {} // Necessario per Firebase

    public String getNome() { return nome; }
    public String getModeratoreID() { return moderatoreID; }
    public int getCreditiIniziali() { return creditiIniziali; }
    public String getPassword() { return password; }
    public String getModalitaAcquisto() { return modalitaAcquisto; }
    public String getGiornoInizioMercato() { return giornoInizioMercato; }
    public String getOraInizioMercato() { return oraInizioMercato; }
    public String getGiornoFineMercato() { return giornoFineMercato; }
    public String getOraFineMercato() { return oraFineMercato; }
    public String getGiornoCalcolo() { return giornoCalcolo; }
    public String getOraCalcolo() { return oraCalcolo; }

    // I setter non sono strettamente necessari se usi una Map per salvare, ma utili per coerenza
}