package com.example.gameplus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MarketManager {
    private List<Asset> mercatoAssets;
    private Random random;

    public MarketManager() {
        mercatoAssets = new ArrayList<>();
        random = new Random();
        inizializzaDatiMercato();
    }

    private void inizializzaDatiMercato() {
        mercatoAssets.add(new Asset("BTP Italia 10Y", "BOND", 100.0, 0.01, 0.045));
        mercatoAssets.add(new Asset("US Treasury", "BOND", 100.0, 0.005, 0.035));
        mercatoAssets.add(new Asset("Azioni Tech", "AZIONI", 150.0, 0.20, 0.0));
        mercatoAssets.add(new Asset("ETF S&P 500", "AZIONI", 400.0, 0.08, 0.0));
        mercatoAssets.add(new Asset("Iniziale", "AZIONI", 0.0, 100.0, 0.0));
        mercatoAssets.add(new Asset("Oro Fisico", "COMMODITY", 1800.0, 0.03, 0.0));
        mercatoAssets.add(new Asset("Bitcoin", "CRYPTO", 45000.0, 0.30, 0.0));
    }

    public List<Asset> getAssets() {
        return mercatoAssets;
    }

    public Asset getAsset(int index) {
        return mercatoAssets.get(index);
    }

    public double simulaVariazioniEGeneraCedole() {
        double cedoleIncassate = 0;

        for (Asset a : mercatoAssets) {
            // Calcolo Cedole
            if (a.tipo.equals("BOND") && a.quantitaPosseduta > 0) {
                cedoleIncassate += (a.quantitaPosseduta * 100.0) * (a.cedolaAnnuale / 2.0);
            }

            // Variazione Prezzi
            double variazione;
            if (a.tipo.equals("BOND")) {
                variazione = (random.nextDouble() * 0.04) - 0.02; // +/- 2%
            } else if (a.tipo.equals("COMMODITY")) {
                variazione = (random.nextDouble() * 0.05) - 0.01; // Tra -1% e +4%
            } else {
                variazione = (random.nextDouble() * a.volatilita * 2) - a.volatilita;
                if (random.nextInt(100) < 5) variazione = -0.30; // Shock (crollo)
            }

            a.prezzoCorrente *= (1 + variazione);
            if (a.prezzoCorrente < 0.01) a.prezzoCorrente = 0.01;
        }

        return cedoleIncassate;
    }

    public double getValoreTotalePortafoglio() {
        double tot = 0;
        for (Asset a : mercatoAssets) tot += a.getValoreTotale();
        return tot;
    }
}