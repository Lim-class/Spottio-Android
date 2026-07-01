package com.example.gameplus;

public class Asset {
    public String nome;
    public String tipo;
    public double prezzoCorrente;
    public double volatilita;
    public double cedolaAnnuale;

    public long quantitaPosseduta = 0;
    public double prezzoMedioAcquisto = 0;

    public Asset(String nome, String tipo, double prezzoBase, double volatilita, double cedolaAnnuale) {
        this.nome = nome;
        this.tipo = tipo;
        this.prezzoCorrente = prezzoBase;
        this.volatilita = volatilita;
        this.cedolaAnnuale = cedolaAnnuale;
    }

    public void registraAcquisto(long quantita, double prezzo) {
        double costoTotaleVecchio = quantitaPosseduta * prezzoMedioAcquisto;
        double costoNuovo = quantita * prezzo;
        quantitaPosseduta += quantita;
        prezzoMedioAcquisto = (costoTotaleVecchio + costoNuovo) / quantitaPosseduta;
    }

    public double getValoreTotale() {
        return quantitaPosseduta * prezzoCorrente;
    }
}