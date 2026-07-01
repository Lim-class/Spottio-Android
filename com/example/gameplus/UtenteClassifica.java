package com.example.gameplus;

public class UtenteClassifica {
    private String nomeUtente;
    private int punteggioTotale;

    public UtenteClassifica(String nomeUtente, int punteggioTotale) {
        this.nomeUtente = nomeUtente;
        this.punteggioTotale = punteggioTotale;
    }

    public String getNomeUtente() { return nomeUtente; }
    public int getPunteggioTotale() { return punteggioTotale; }
}