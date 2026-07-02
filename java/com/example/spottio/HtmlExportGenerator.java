package com.example.spottio;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HtmlExportGenerator {

    public static String generaPaginaHtmlArchivio(JSONObject root) throws Exception {
        JSONObject metadata = root.getJSONObject("metadata");
        JSONObject profilo = root.getJSONObject("profilo");
        JSONObject preferenzeAlgoritmo = profilo.getJSONObject("preferenze_algoritmo");
        JSONObject statistiche = root.getJSONObject("statistiche_post");
        JSONArray posts = root.getJSONArray("post_pubblicati");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"it\">\n<head>\n")
                .append("  <meta charset=\"UTF-8\">\n")
                .append("  <title>Archivio Dati - ").append(metadata.getString("utente")).append("</title>\n")
                .append("  <style>\n")
                .append("    body { font-family: 'Segoe UI', sans-serif; background-color: #f3f4f6; color: #1f2937; padding: 20px; max-width: 800px; margin: auto; }\n")
                .append("    h1, h2 { border-bottom: 2px solid #e5e7eb; padding-bottom: 10px; }\n")
                .append("    .card { background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 5px rgba(0,0,0,0.05); margin-bottom: 20px; }\n")
                .append("    .post { border-left: 4px solid #3b82f6; background: white; padding: 15px 20px; margin-bottom: 15px; border-radius: 0 8px 8px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n")
                .append("    .badge { display: inline-block; background-color: #dbeafe; color: #1e40af; font-size: 12px; padding: 4px 8px; border-radius: 999px; font-weight: bold; margin-top: 10px; }\n")
                .append("    .date { font-size: 12px; color: #6b7280; float: right; margin-top: 12px; }\n")
                .append("    .json-block { background: #f9fafb; border: 1px solid #e5e7eb; padding: 10px; border-radius: 6px; font-family: monospace; font-size: 13px; white-space: pre-wrap; word-wrap: break-word; }\n")
                .append("  </style>\n</head>\n<body>\n")
                .append("  <h1>📦 Archivio Dati Spottio</h1>\n")
                .append("  <div class=\"card\">\n")
                .append("    <h2>Informazioni Generali</h2>\n")
                .append("    <p><strong>Utente:</strong> ").append(metadata.getString("utente")).append("</p>\n")
                .append("    <p><strong>Data di richiesta:</strong> ").append(metadata.getString("dataRichiesta")).append("</p>\n")
                .append("  </div>\n")
                .append("  <div class=\"card\">\n")
                .append("    <h2>👤 Dettagli Profilo Completi</h2>\n")
                .append("    <ul style=\"line-height: 1.8;\">\n");

        java.util.Iterator<String> keysProfilo = profilo.keys();
        while(keysProfilo.hasNext()) {
            String key = keysProfilo.next();
            if (key.equals("preferenze_algoritmo")) continue;

            Object val = profilo.get(key);
            String displayVal;

            if (val instanceof JSONArray) {
                JSONArray arr = (JSONArray) val;
                List<String> listItems = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    listItems.add(arr.getString(i));
                }
                displayVal = listItems.isEmpty() ? "Nessuno" : String.join(", ", listItems);
            } else if (val instanceof JSONObject) {
                displayVal = "<div class=\"json-block\">" + ((JSONObject)val).toString(2) + "</div>";
            } else {
                displayVal = val.toString();
            }

            html.append("      <li><strong>").append(key).append("</strong>: ").append(displayVal).append("</li>\n");
        }
        html.append("    </ul>\n")
                .append("  </div>\n")
                .append("  <div class=\"card\">\n")
                .append("    <h2>🧠 Profilo Algoritmo (Interessi)</h2>\n")
                .append("    <p style=\"font-size: 14px; color: #6b7280; margin-bottom: 15px;\">Dati utilizzati dall'algoritmo di Spottio per personalizzare il tuo feed.</p>\n");

        if (preferenzeAlgoritmo.length() > 0) {
            html.append("    <ul style=\"line-height: 1.8;\">\n");
            java.util.Iterator<String> keys = preferenzeAlgoritmo.keys();
            while(keys.hasNext()) {
                String catName = keys.next();
                JSONObject details = preferenzeAlgoritmo.getJSONObject(catName);
                html.append("      <li><strong>").append(catName).append("</strong>: Punteggio <span class=\"badge\">")
                        .append(details.getInt("punteggio")).append("</span> <span style=\"color:#6b7280;\">(Ultima interazione: ")
                        .append(details.getString("ultimo_aggiornamento")).append(")</span></li>\n");
            }
            html.append("    </ul>\n");
        } else {
            html.append("    <p>L'algoritmo non ha ancora registrato preferenze sufficienti.</p>\n");
        }

        html.append("  </div>\n")
                .append("  <div class=\"card\">\n")
                .append("    <h2>📊 Statistiche Post</h2>\n")
                .append("    <p><strong>Post Pubblicati totali:</strong> ").append(statistiche.getInt("numero_post_pubblicati")).append("</p>\n")
                .append("  </div>\n")
                .append("  <h2>I Tuoi Post</h2>\n");

        if (posts.length() == 0) {
            html.append("  <p>Non hai ancora pubblicato nessun post.</p>\n");
        } else {
            for (int i = 0; i < posts.length(); i++) {
                JSONObject p = posts.getJSONObject(i);
                html.append("  <div class=\"post\">\n")
                        .append("    <p>").append(p.getString("text").replace("<", "&lt;").replace(">", "&gt;")).append("</p>\n")
                        .append("    <span class=\"badge\">").append(p.getString("category")).append("</span>\n")
                        .append("    <span class=\"date\">").append(p.getString("timestamp")).append("</span>\n")
                        .append("    <div style=\"clear: both;\"></div>\n")
                        .append("  </div>\n");
            }
        }

        html.append("</body>\n</html>");
        return html.toString();
    }
}