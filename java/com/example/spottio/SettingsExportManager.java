package com.example.spottio;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class SettingsExportManager {

    private final FirebaseFirestore mDb;
    private final Context context;
    private final String currentUsername;

    public SettingsExportManager(FirebaseFirestore mDb, Context context, String currentUsername) {
        this.mDb = mDb;
        this.context = context;
        this.currentUsername = currentUsername;
    }

    public void setupExportLogic(View view) {
        Button btnExportJson = view.findViewById(R.id.btnExportJson);
        Button btnExportHtml = view.findViewById(R.id.btnExportHtml);

        btnExportJson.setOnClickListener(v -> eseguiEstrazioneDati(true, btnExportJson));
        btnExportHtml.setOnClickListener(v -> eseguiEstrazioneDati(false, btnExportHtml));
    }

    private void eseguiEstrazioneDati(boolean formatoJson, Button bottoneSorgente) {
        if ("Guest".equals(currentUsername)) {
            Toast.makeText(context, "Devi effettuare il login per esportare i dati.", Toast.LENGTH_SHORT).show();
            return;
        }

        String testoOriginale = bottoneSorgente.getText().toString();
        bottoneSorgente.setText("Elaborazione...");
        bottoneSorgente.setEnabled(false);

        mDb.collection("users").document(currentUsername).get().addOnCompleteListener(taskUtente -> {
            if (!taskUtente.isSuccessful() || taskUtente.getResult() == null) {
                ripristinaBottone(bottoneSorgente, testoOriginale);
                Toast.makeText(context, "Errore lettura profilo utente.", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentSnapshot userDoc = taskUtente.getResult();

            mDb.collection("posts").whereEqualTo("user", currentUsername).get().addOnCompleteListener(taskPost -> {
                ripristinaBottone(bottoneSorgente, testoOriginale);

                if (!taskPost.isSuccessful() || taskPost.getResult() == null) {
                    Toast.makeText(context, "Errore recupero post dell'utente.", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject exportRoot = new JSONObject();

                    // Metadata
                    JSONObject metadata = new JSONObject();
                    metadata.put("utente", currentUsername);
                    metadata.put("dataRichiesta", ottieniDataIso(new Date()));
                    metadata.put("app", "Spottio");
                    exportRoot.put("metadata", metadata);

                    // Profilo
                    JSONObject profiloJson = new JSONObject();
                    JSONObject preferenzeAlgoritmo = new JSONObject();

                    if (userDoc.exists()) {
                        Map<String, Object> dataMap = userDoc.getData();
                        if (dataMap != null) {
                            for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                                String key = entry.getKey();
                                Object value = entry.getValue();

                                if (!"preferences".equals(key)) {
                                    if (value instanceof com.google.firebase.Timestamp) {
                                        profiloJson.put(key, ottieniDataIso(((com.google.firebase.Timestamp) value).toDate()));
                                    } else if (value instanceof List) {
                                        profiloJson.put(key, new JSONArray((List<?>) value));
                                    } else if (value instanceof Map) {
                                        JSONObject mapObj = new JSONObject();
                                        Map<?, ?> mapVal = (Map<?, ?>) value;
                                        for (Map.Entry<?, ?> e : mapVal.entrySet()) {
                                            if (e.getValue() instanceof com.google.firebase.Timestamp) {
                                                mapObj.put(String.valueOf(e.getKey()), ottieniDataIso(((com.google.firebase.Timestamp) e.getValue()).toDate()));
                                            } else {
                                                mapObj.put(String.valueOf(e.getKey()), e.getValue());
                                            }
                                        }
                                        profiloJson.put(key, mapObj);
                                    } else {
                                        profiloJson.put(key, value);
                                    }
                                }
                            }

                            Object prefsRaw = dataMap.get("preferences");
                            if (prefsRaw instanceof Map) {
                                Map<?, ?> prefsMap = (Map<?, ?>) prefsRaw;
                                for (Map.Entry<?, ?> prefEntry : prefsMap.entrySet()) {
                                    String categoria = String.valueOf(prefEntry.getKey());
                                    Object datiCategoria = prefEntry.getValue();

                                    if (datiCategoria instanceof Map) {
                                        Map<?, ?> dettagli = (Map<?, ?>) datiCategoria;
                                        JSONObject catObj = new JSONObject();

                                        Object score = dettagli.get("score");
                                        catObj.put("punteggio", score != null ? score : 0);

                                        Object lastUpdate = dettagli.get("lastUpdate");
                                        if (lastUpdate instanceof com.google.firebase.Timestamp) {
                                            catObj.put("ultimo_aggiornamento", ottieniDataIso(((com.google.firebase.Timestamp) lastUpdate).toDate()));
                                        } else if (lastUpdate != null) {
                                            catObj.put("ultimo_aggiornamento", lastUpdate.toString());
                                        } else {
                                            catObj.put("ultimo_aggiornamento", "Sconosciuto");
                                        }
                                        preferenzeAlgoritmo.put(categoria, catObj);
                                    }
                                }
                            }
                        }
                    }
                    profiloJson.put("preferenze_algoritmo", preferenzeAlgoritmo);
                    exportRoot.put("profilo", profiloJson);

                    // Post
                    JSONArray postArray = new JSONArray();
                    HashSet<String> categorieSet = new HashSet<>();

                    for (QueryDocumentSnapshot doc : taskPost.getResult()) {
                        JSONObject postObj = new JSONObject();
                        postObj.put("id", doc.getId());

                        Map<String, Object> pData = doc.getData();
                        String testoPost = pData.containsKey("text") ? String.valueOf(pData.get("text")) : "Contenuto multimediale";
                        String categoria = pData.containsKey("category") ? String.valueOf(pData.get("category")) : "Generale";

                        categorieSet.add(categoria);
                        postObj.put("text", testoPost);
                        postObj.put("category", categoria);

                        Object ts = pData.get("timestamp");
                        if (ts instanceof com.google.firebase.Timestamp) {
                            postObj.put("timestamp", ottieniDataIso(((com.google.firebase.Timestamp) ts).toDate()));
                        } else {
                            postObj.put("timestamp", "Data sconosciuta");
                        }
                        postArray.put(postObj);
                    }

                    JSONObject statistiche = new JSONObject();
                    statistiche.put("numero_post_pubblicati", postArray.length());
                    statistiche.put("categorie_utilizzate", new JSONArray(categorieSet));
                    exportRoot.put("statistiche_post", statistiche);
                    exportRoot.put("post_pubblicati", postArray);

                    // Salvataggio tramite FileExportHelper
                    String nomeFile = "Spottio_Dati_" + currentUsername;
                    String mimeType;
                    String contenutoFile;

                    if (formatoJson) {
                        nomeFile += ".json";
                        mimeType = "application/json";
                        contenutoFile = exportRoot.toString(2);
                    } else {
                        nomeFile += ".html";
                        mimeType = "text/html";
                        contenutoFile = HtmlExportGenerator.generaPaginaHtmlArchivio(exportRoot);
                    }

                    FileExportHelper.salvaInCartellaDownloadPubblica(context, nomeFile, mimeType, contenutoFile);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Errore formattazione dati.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void ripristinaBottone(Button btn, String stringaOriginale) {
        btn.setEnabled(true);
        btn.setText(stringaOriginale);
    }

    private String ottieniDataIso(Date data) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ITALIAN);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(data);
    }
}
