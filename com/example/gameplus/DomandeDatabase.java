package com.example.gameplus;

import java.util.Random;

public class DomandeDatabase {

    private static final String[][] VERITA = {
            // {Classico, Party, Estremo}
            {"Qual è la tua paura più grande?", "Chi è la persona più fastidiosa in questa stanza?", "Qual è la cosa più illegale che hai fatto?"},
            {"Qual è il tuo sogno nel cassetto?", "Hai mai fatto finta di stare male per saltare un impegno?", "Qual è il tuo feticcio più strano?"},
            {"Qual è stato il tuo momento più imbarazzante a scuola?", "Qual è l'ultima bugia che hai detto?", "Hai mai curiosato nel telefono di qualcun altro?"},
            {"Cosa cambieresti del tuo aspetto fisico?", "Chi è la tua cotta segreta tra i presenti?", "Descrivi la tua peggiore esperienza amorosa."},
            {"Qual è il regalo più brutto che hai ricevuto?", "Hai mai stalkerato un tuo ex sui social?", "Qual è il posto più strano dove l'hai fatto?"},
            {"Se potessi essere invisibile per un giorno, cosa faresti?", "Qual è il segreto che non hai mai detto ai tuoi genitori?", "Ti sei mai pentito di essere andato a letto con qualcuno?"},
            {"Qual è la tua abitudine più disgustosa?", "Hai mai rubato qualcosa, anche di piccolo?", "Chi è la persona più sexy in questa stanza?"},
            {"Qual è il tuo più grande rimpianto?", "Chi sceglieresti di baciare tra i presenti?", "Hai mai avuto un'avventura di una notte?"},
            {"Hai mai pianto per un film? Quale?", "Qual è la cosa più imbarazzante che i tuoi genitori ti hanno visto fare?", "Hai mai inviato un messaggio piccante alla persona sbagliata?"},
            {"Qual è il tuo pregio migliore?", "Hai mai mentito per far colpo su qualcuno?", "Qual è la tua fantasia più nascosta?"},
            {"Qual è la cosa più infantile che fai ancora?", "Qual è il voto più basso che hai preso a scuola?", "Cosa cerchi segretamente su internet?"}
    };

    private static final String[][] OBBLIGHI = {
            // {Classico, Party, Estremo}
            {"Fai 10 flessioni.", "Canta una canzone a squarciagola.", "Manda un messaggio piccante al terzo contatto della tua rubrica."},
            {"Imita un animale per un minuto.", "Balla senza musica per 30 secondi.", "Togliti un indumento a scelta degli altri giocatori."},
            {"Bevi un bicchiere d'acqua tutto d'un fiato.", "Fai finta di essere un cameriere e prendi le ordinazioni di tutti.", "Fai un massaggio ai piedi alla persona alla tua destra."},
            {"Recita l'alfabeto al contrario.", "Prova a toccarti il naso con la lingua.", "Manda un selfie imbarazzante nelle tue storie social."},
            {"Chiama un tuo amico e digli che ti sposi domani.", "Fatti truccare (male) dagli altri giocatori.", "Siediti sulle ginocchia di un giocatore per i prossimi due turni."},
            {"Prova a fare una verticale contro il muro.", "Urla 'Voglio andare a vivere in Antartide!' dalla finestra.", "Lascia che qualcuno legga i tuoi ultimi 5 messaggi WhatsApp."},
            {"Mangia un cucchiaio di ketchup o senape.", "Fai una dichiarazione d'amore a un oggetto nella stanza.", "Bacia il collo della persona alla tua sinistra."},
            {"Fai 20 addominali.", "Scambia i vestiti (solo maglietta/camicia) con la persona a sinistra.", "Fatti bendare e indovina chi ti sta toccando la mano."},
            {"Tieni un cubetto di ghiaccio in mano finché non si scioglie.", "Posta una foto buffa e lasciala per 10 minuti.", "Manda un vocale al tuo ex dicendo 'Mi manchi' (poi puoi cancellarlo)."},
            {"Fai la sfilata di moda per la stanza.", "Parla con accento straniero fino al tuo prossimo turno.", "Lascia che un giocatore ti faccia un succhiotto o un segno sul braccio."},
            {"Cerca di non ridere mentre gli altri ti fanno il solletico.", "Mangia qualcosa senza usare le mani.", "Sussurra qualcosa di provocante all'orecchio della persona di fronte."}
    };

    public static String getCasuale(String tipo, String modalita) {
        int modIndex;
        switch (modalita) {
            case "Classico":
                modIndex = 0;
                break;
            case "Party":
                modIndex = 1;
                break;
            case "Estremo":
            default:
                modIndex = 2;
                break;
        }

        Random r = new Random();
        if (tipo.equals("Verità")) {
            return VERITA[r.nextInt(VERITA.length)][modIndex];
        } else {
            return OBBLIGHI[r.nextInt(OBBLIGHI.length)][modIndex];
        }
    }
}