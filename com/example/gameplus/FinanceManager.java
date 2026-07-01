package com.example.gameplus;

public class FinanceManager {
    private String paeseSelezionato;

    public FinanceManager(String paeseSelezionato) {
        this.paeseSelezionato = paeseSelezionato;
    }

    public double calcolaTasseReddito(double redditoLordoSemestre) {
        double aliquota = paeseSelezionato.equals("Italia") ? 0.43 : 0.23;
        return redditoLordoSemestre * aliquota;
    }

    public double calcolaTassaCapitalGain(Asset a, double plusvalenza) {
        if (plusvalenza <= 0) return 0;
        double aliquota = 0.26;
        if (a.tipo.equals("BOND") && paeseSelezionato.equals("Italia")) {
            aliquota = 0.125; // Tassa agevolata BTP in Italia
        }
        return plusvalenza * aliquota;
    }

    public double calcolaInteressiPassivi(double liquidita) {
        if (liquidita < 0) {
            // Tasso interesse passivo: 5% ogni semestre
            return Math.abs(liquidita) * 0.05;
        }
        return 0;
    }
}