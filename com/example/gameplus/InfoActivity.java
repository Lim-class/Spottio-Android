package com.example.gameplus;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        TextView tvTitle = findViewById(R.id.tvInfoTitle);
        TextView tvContent = findViewById(R.id.tvInfoContent);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Recupera il tipo di informazione da mostrare
        String type = getIntent().getStringExtra("info_type");

        if (type != null) {
            switch (type) {
                case "storia":
                    tvTitle.setText("L'Universo GamePlus");
                    tvContent.setText("GamePlus nasce dalla passione di un singolo sviluppatore indipendente per il mondo del gioco e della socialità. Il viaggio è iniziato nel 2023 con Spottio, un'app dedicata alla condivisione di momenti reali.\n\n" +
                            "Nel 2024, il progetto si è evoluto diventando un vero e proprio ecosistema competitivo. GamePlus non è limitato a un singolo torneo, ma è una piattaforma aperta dove puoi creare e gestire diverse leghe per vari tipi di giochi. Supportare questo progetto significa scegliere un software trasparente, in costante crescita e costruito attorno ai desideri della propria community.");
                    break;
                case "policy":
                    tvTitle.setText("Linee Guida della Community");
                    tvContent.setText("La convivenza rispettosa è alla base di ogni sfida su GamePlus. Gli utenti si impegnano a seguire queste regole:\n\n" +
                            "Rispetto Reciproco: Non è tollerata alcuna forma di insulto o discriminazione, sia nelle sezioni social di Spottio che nelle chat delle leghe.\n\n" +
                            "Fair Play e Integrità: In ogni lega creata su GamePlus, il gioco deve essere pulito. Ogni tentativo di sfruttare bug o manipolare i risultati rovina l'esperienza di tutti.\n\n" +
                            "Segnalazioni: La community è parte attiva della moderazione. Se riscontri comportamenti scorretti in una lega o contenuti inappropriati, usa gli strumenti di segnalazione integrati.");
                    break;
                case "privacy":
                    tvTitle.setText("Privacy & Account Unico");
                    tvContent.setText("In GamePlus, la tua esperienza è fluida e protetta. Ecco come gestiamo i tuoi dati tra le diverse sezioni:\n\n" +
                            "Un Solo Account per Tutto: Proprio come accade con i servizi Google, non hai bisogno di registrazioni multiple. Le credenziali che usi per Spottio sono le stesse per accedere al mondo delle leghe e del Fanta. Un'unica identità sicura per gestire tutta la tua attività nell'ecosistema GamePlus.\n\n" +
                            "Gestione delle Leghe: Le informazioni relative alle leghe che crei o a cui partecipi sono collegate al tuo profilo ma restano distinte dalla tua attività social. Raccogliamo solo i dati essenziali (username ed email) per garantirti un accesso rapido e sicuro tramite l'infrastruttura Firebase.\n\n" +
                            "Sicurezza Garantita: Non vendiamo i tuoi dati a terzi. Tutto è archiviato sui server Google Cloud per assicurare protezione e privacy, permettendoti di concentrarti solo sulla competizione e sul divertimento.");
                    break;
            }
        }
    }
}