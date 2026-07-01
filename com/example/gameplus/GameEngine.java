package com.example.gameplus;

import java.util.Random;

public class GameEngine {
    public enum StatoGioco { IN_CORSO, VITTORIA, BANCAROTTA, VECCHIAIA }

    private long obiettivoFinanziario;
    private double liquidita;
    private double mesiTotaliDiVita;

    private double stipendioAnnuo = 0;
    private String titoloLavoro = "Disoccupato";
    private double tassePagateTotali = 0;
    private double tasseUltimoSemestre = 0;

    private FinanceManager financeManager;
    private MarketManager marketManager;
    private Random random = new Random();

    public GameEngine(int etaPartenza, String paeseSelezionato, long obiettivoFinanziario) {
        this.obiettivoFinanziario = obiettivoFinanziario;
        this.mesiTotaliDiVita = etaPartenza * 12;
        this.liquidita = 10000.0;

        if ("Delaware (USA)".equals(paeseSelezionato)) {
            this.liquidita -= 2000;
        }

        this.financeManager = new FinanceManager(paeseSelezionato);
        this.marketManager = new MarketManager();
    }

    public boolean compraAsset(int assetIndex, long quantita) {
        Asset a = marketManager.getAsset(assetIndex);
        double costo = quantita * a.prezzoCorrente;
        if (liquidita >= costo) {
            liquidita -= costo;
            a.registraAcquisto(quantita, a.prezzoCorrente);
            return true;
        }
        return false;
    }

    public double vendiAsset(int assetIndex, long quantita) {
        Asset a = marketManager.getAsset(assetIndex);
        if (a.quantitaPosseduta >= quantita) {
            double ricavoLordo = quantita * a.prezzoCorrente;
            double costoOriginale = quantita * a.prezzoMedioAcquisto;
            double plusvalenza = ricavoLordo - costoOriginale;

            double tasseSuVendita = financeManager.calcolaTassaCapitalGain(a, plusvalenza);

            liquidita += (ricavoLordo - tasseSuVendita);
            a.quantitaPosseduta -= quantita;
            tassePagateTotali += tasseSuVendita;

            return tasseSuVendita; // Restituisce le tasse pagate per avvisare l'utente
        }
        return -1; // Errore
    }

    public String lavoraSemestre() {
        String messaggio = "Hai lavorato per 6 mesi.";
        if (stipendioAnnuo == 0) {
            stipendioAnnuo = 18000 + random.nextInt(12000);
            titoloLavoro = "Apprendista";
            messaggio = "Hai trovato lavoro come Apprendista!";
        } else if (random.nextInt(100) < 15) {
            stipendioAnnuo += 5000;
            titoloLavoro = "Senior Manager";
            messaggio = "Promozione ottenuta!";
        }
        avanzaTempo(6);
        return messaggio;
    }

    public String tentaStartup() {
        if (liquidita < 50000) return "Servono 50.000€!";

        liquidita -= 50000;
        int roll = random.nextInt(100);
        String messaggio;

        if (roll < 10) {
            liquidita += 1000000;
            messaggio = "UNICORNO! Hai fatto il botto! +1.000.000€";
        } else if (roll < 35) {
            liquidita += 200000;
            messaggio = "Successo! Buona exit! +200.000€";
        } else {
            messaggio = "Startup Fallita.";
        }
        avanzaTempo(6);
        return messaggio;
    }

    private void avanzaTempo(int mesi) {
        mesiTotaliDiVita += mesi;

        // Interessi sul debito
        double interessiPassivi = financeManager.calcolaInteressiPassivi(liquidita);
        liquidita -= interessiPassivi;

        // Stipendio e Tasse
        double guadagnoLavoro = (stipendioAnnuo / 12.0) * mesi;
        tasseUltimoSemestre = financeManager.calcolaTasseReddito(guadagnoLavoro);
        tassePagateTotali += tasseUltimoSemestre;
        liquidita += (guadagnoLavoro - tasseUltimoSemestre);

        // Mercato e Cedole
        double cedole = marketManager.simulaVariazioniEGeneraCedole();
        liquidita += cedole;
    }

    public StatoGioco controllaStatoGioco() {
        double patrimonio = liquidita + marketManager.getValoreTotalePortafoglio();
        if (patrimonio >= obiettivoFinanziario) return StatoGioco.VITTORIA;
        if (mesiTotaliDiVita >= 100 * 12) return StatoGioco.VECCHIAIA;
        if (liquidita < -50000) return StatoGioco.BANCAROTTA;
        return StatoGioco.IN_CORSO;
    }

    // Getters
    public double getLiquidita() { return liquidita; }
    public double getMesiTotaliDiVita() { return mesiTotaliDiVita; }
    public String getTitoloLavoro() { return titoloLavoro; }
    public double getStipendioAnnuo() { return stipendioAnnuo; }
    public double getTasseUltimoSemestre() { return tasseUltimoSemestre; }
    public double getPatrimonioTotale() { return liquidita + marketManager.getValoreTotalePortafoglio(); }
    public MarketManager getMarketManager() { return marketManager; }

    // Nota: Questa classe non ha alcuna importazione di Android, quindi è testabile al 100% in modo indipendente.
}