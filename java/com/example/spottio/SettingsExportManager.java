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
        var btnExportJson = (Button) view.findViewById(R.id.btnExportJson);
        var btnExportHtml = (Button) view.findViewById(R.id.btnExportHtml);

        btnExportJson.setOnClickListener(v -> eseguiEstrazioneDati(true, btnExportJson));
        btnExportHtml.setOnClickListener(v -> eseguiEstrazioneDati(false, btnExportHtml));
    }

    private void eseguiEstrazioneDati(boolean formatoJson, Button bottoneSorgente) {
        if ("Guest".equals(currentUsername)) {
            Toast.makeText(context, "Devi effettuare il login per esportare i dati.", Toast.LENGTH_SHORT).show();
            return;
        }

        var testoOriginale = bottoneSorgente.getText().toString();
        bottoneSorgente.setText("Elaborazione...");
        bottoneSorgente.setEnabled(false);

        mDb.collection("users").document(currentUsername).get().addOnCompleteListener(taskUtente -> {
            if (!taskUtente.isSuccessful() || taskUtente.getResult() == null) {
                ripristinaBottone(bottoneSorgente, testoOriginale);
                Toast.makeText(context, "Errore lettura profilo utente.", Toast.LENGTH_SHORT).show();
                return;
            }

            var userDoc = taskUtente.getResult();

            mDb.collection("posts").whereEqualTo("user", currentUsername).get().addOnCompleteListener(taskPost -> {
                ripristinaBottone(bottoneSorgente, testoOriginale);

                if (!taskPost.isSuccessful() || taskPost.getResult() == null) {
                    Toast.makeText(context, "Errore recupero post dell'utente.", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    var exportRoot = new JSONObject();

                    // Metadata
                    var metadata = new JSONObject();
                    metadata.put("utente", currentUsername);
                    metadata.put("dataRichiesta", ottieniDataIso(new Date()));
                    metadata.put("app", "Spottio");
                    exportRoot.put("metadata", metadata);

                    // Profilo
                    var profiloJson = new JSONObject();
                    var preferenzeAlgoritmo = new JSONObject();

                    if (userDoc.exists()) {
                        var dataMap = userDoc.getData();
                        if (dataMap != null) {
                            for (var entry : dataMap.entrySet()) {
                                var key = entry.getKey();
                                var value = entry.getValue();

                                if (!"preferences".equals(key)) {
                                    if (value instanceof com.google.firebase.Timestamp ts) {
                                        profiloJson.put(key, ottieniDataIso(ts.toDate()));
                                    } else if (value instanceof List) {
                                        profiloJson.put(key, new JSONArray((List<?>) value));
                                    } else if (value instanceof Map<?, ?> mapVal) {
                                        var mapObj = new JSONObject();
                                        for (var e : mapVal.entrySet()) {
                                            if (e.getValue() instanceof com.google.firebase.Timestamp innerTs) {
                                                mapObj.put(String.valueOf(e.getKey()), ottieniDataIso(innerTs.toDate()));
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

                            var prefsRaw = dataMap.get("preferences");
                            if (prefsRaw instanceof Map<?, ?> prefsMap) {
                                for (var prefEntry : prefsMap.entrySet()) {
                                    var categoria = String.valueOf(prefEntry.getKey());
                                    if (prefEntry.getValue() instanceof Map<?, ?> dettagli) {
                                        var catObj = new JSONObject();

                                        var score = dettagli.get("score");
                                        catObj.put("punteggio", score != null ? score : 0);

                                        var lastUpdate = dettagli.get("lastUpdate");
                                        if (lastUpdate instanceof com.google.firebase.Timestamp ts) {
                                            catObj.put("ultimo_aggiornamento", ottieniDataIso(ts.toDate()));
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
                    var postArray = new JSONArray();
                    var categorieSet = new HashSet<String>();

                    for (var doc : taskPost.getResult()) {
                        var postObj = new JSONObject();
                        postObj.put("id", doc.getId());

                        var pData = doc.getData();
                        var testoPost = pData.containsKey("text") ? String.valueOf(pData.get("text")) : "Contenuto multimediale";
                        var categoria = pData.containsKey("category") ? String.valueOf(pData.get("category")) : "Generale";

                        categorieSet.add(categoria);
                        postObj.put("text", testoPost);
                        postObj.put("category", categoria);

                        var ts = pData.get("timestamp");
                        if (ts instanceof com.google.firebase.Timestamp fTs) {
                            postObj.put("timestamp", ottieniDataIso(fTs.toDate()));
                        } else {
                            postObj.put("timestamp", "Data sconosciuta");
                        }
                        postArray.put(postObj);
                    }

                    var statistiche = new JSONObject();
                    statistiche.put("numero_post_pubblicati", postArray.length());
                    statistiche.put("categorie_utilizzate", new JSONArray(categorieSet));
                    exportRoot.put("statistiche_post", statistiche);
                    exportRoot.put("post_pubblicati", postArray);

                    // Salvataggio tramite FileExportHelper
                    var nomeFile = "Spottio_Dati_" + currentUsername;
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
        var sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ITALIAN);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(data);
    }
}