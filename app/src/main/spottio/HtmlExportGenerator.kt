package com.example.spottio

import org.json.JSONArray
import org.json.JSONObject

object HtmlExportGenerator {

    @Throws(Exception::class)
    fun generaPaginaHtmlArchivio(root: JSONObject): String {
        val metadata = root.getJSONObject("metadata")
        val profilo = root.getJSONObject("profilo")
        val preferenzeAlgoritmo = profilo.getJSONObject("preferenze_algoritmo")
        val statistiche = root.getJSONObject("statistiche_post")
        val posts = root.getJSONArray("post_pubblicati")

        return buildString {
            // Stringhe multiline: addio infiniti .append()
            append("""
                <!DOCTYPE html>
                <html lang="it">
                <head>
                  <meta charset="UTF-8">
                  <title>Archivio Dati - ${metadata.getString("utente")}</title>
                  <style>
                    body { font-family: 'Segoe UI', sans-serif; background-color: #f3f4f6; color: #1f2937; padding: 20px; max-width: 800px; margin: auto; }
                    h1, h2 { border-bottom: 2px solid #e5e7eb; padding-bottom: 10px; }
                    .card { background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 5px rgba(0,0,0,0.05); margin-bottom: 20px; }
                    .post { border-left: 4px solid #3b82f6; background: white; padding: 15px 20px; margin-bottom: 15px; border-radius: 0 8px 8px 0; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
                    .badge { display: inline-block; background-color: #dbeafe; color: #1e40af; font-size: 12px; padding: 4px 8px; border-radius: 999px; font-weight: bold; margin-top: 10px; }
                    .date { font-size: 12px; color: #6b7280; float: right; margin-top: 12px; }
                    .json-block { background: #f9fafb; border: 1px solid #e5e7eb; padding: 10px; border-radius: 6px; font-family: monospace; font-size: 13px; white-space: pre-wrap; word-wrap: break-word; }
                  </style>
                </head>
                <body>
                  <h1>📝 Archivio Dati Spottio</h1>
                  <div class="card">
                    <h2>Informazioni Generali</h2>
                    <p><strong>Utente:</strong> ${metadata.getString("utente")}</p>
                    <p><strong>Data di richiesta:</strong> ${metadata.getString("dataRichiesta")}</p>
                  </div>
                  <div class="card">
                    <h2>👤 Dettagli Profilo Completi</h2>
                    <ul style="line-height: 1.8;">
            """.trimIndent() + "\n")

            // Ciclo per i dettagli del profilo
            for (key in profilo.keys()) {
                if (key == "preferenze_algoritmo") continue

                val valObj = profilo.get(key)

                // Il 'when' con i tipi sostituisce gli if-else con instanceof
                val displayVal = when (valObj) {
                    is JSONArray -> {
                        // Creazione di una lista al volo a partire da un JSONArray
                        val listItems = List(valObj.length()) { i -> valObj.getString(i) }
                        if (listItems.isEmpty()) "Nessuno" else listItems.joinToString(", ")
                    }
                    is JSONObject -> "<div class=\"json-block\">${valObj.toString(2)}</div>"
                    else -> valObj.toString()
                }

                append("      <li><strong>$key</strong>: $displayVal</li>\n")
            }

            append("""
                    </ul>
                  </div>
                  <div class="card">
                    <h2>🤖 Profilo Algoritmo (Interessi)</h2>
                    <p style="font-size: 14px; color: #6b7280; margin-bottom: 15px;">Dati utilizzati dall'algoritmo di Spottio per personalizzare il tuo feed.</p>
            """.trimIndent() + "\n")

            if (preferenzeAlgoritmo.length() > 0) {
                append("    <ul style=\"line-height: 1.8;\">\n")
                for (catName in preferenzeAlgoritmo.keys()) {
                    val details = preferenzeAlgoritmo.getJSONObject(catName)
                    append("      <li><strong>$catName</strong>: Punteggio <span class=\"badge\">" +
                            "${details.getInt("punteggio")}</span> <span style=\"color:#6b7280;\">(Ultima interazione: " +
                            "${details.getString("ultimo_aggiornamento")})</span></li>\n")
                }
                append("    </ul>\n")
            } else {
                append("    <p>L'algoritmo non ha ancora registrato preferenze sufficienti.</p>\n")
            }

            append("""
                  </div>
                  <div class="card">
                    <h2>📊 Statistiche Post</h2>
                    <p><strong>Post Pubblicati totali:</strong> ${statistiche.getInt("numero_post_pubblicati")}</p>
                  </div>
                  <h2>I Tuoi Post</h2>
            """.trimIndent() + "\n")

            if (posts.length() == 0) {
                append("  <p>Non hai ancora pubblicato nessun post.</p>\n")
            } else {
                // Iterazione pulita usando i range di Kotlin
                for (i in 0 until posts.length()) {
                    val p = posts.getJSONObject(i)
                    val safeText = p.getString("text").replace("<", "&lt;").replace(">", "&gt;")
                    append("""
                      <div class="post">
                        <p>$safeText</p>
                        <span class="badge">${p.getString("category")}</span>
                        <span class="date">${p.getString("timestamp")}</span>
                        <div style="clear: both;"></div>
                      </div>
                    """.trimIndent() + "\n")
                }
            }

            append("""
                </body>
                </html>
            """.trimIndent())
        }
    }
}