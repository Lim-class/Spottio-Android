package com.example.gameplus;

public class Personaggio {
    private String nome;
    private int valore;
    private String proprietario;

    public Personaggio() {} // Obbligatorio

    public Personaggio(String nome, int valore, String proprietario) {
        this.nome = nome;
        this.valore = valore;
        this.proprietario = proprietario;
    }
    private int punteggioTotale; // Aggiungi questo campo


    // Aggiorna il costruttore vuoto e aggiungi Getter e Setter
    public int getPunteggioTotale() { return punteggioTotale; }
    public void setPunteggioTotale(int punteggioTotale) { this.punteggioTotale = punteggioTotale; }

    public String getNome() { return nome; }
    public int getValore() { return valore; }
    public String getProprietario() { return proprietario; }
}