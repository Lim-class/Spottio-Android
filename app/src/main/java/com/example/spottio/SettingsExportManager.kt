package com.example.spottio

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SettingsExportManager(
    private val mDb: FirebaseFirestore,
    private val context: Context,
    private val currentUsername: String
) {

    fun setupExportLogic(view: View) {
        val btnExportJson = view.findViewById<Button>(R.id.btnExportJson)
        val btnExportHtml = view.findViewById<Button>(R.id.btnExportHtml)

        btnExportJson.setOnClickListener { eseguiEstrazioneDati(formatoJson = true, btn = btnExportJson) }
        btnExportHtml.setOnClickListener { eseguiEstrazioneDati(formatoJson = false, btn = btnExportHtml) }
    }

    private fun eseguiEstrazioneDati(formatoJson: Boolean, btn: Button) {
        val stringaOriginale = btn.text.toString()
        btn.isEnabled = false
        btn.text = "Estrazione in corso..."

        // Logica Firestore per l'estrazione e formattazione dei dati
        // Nota: Assicurati di inserire qui le tue query reali sui post
        mDb.collection("posts").whereEqualTo("username", currentUsername).get()
            .addOnCompleteListener { task ->
                try {
                    val exportRoot = JSONObject()
                    val postArray = JSONArray()

                    // Popolamento ipotetico basato sui dati estratti dal task
                    if (task.isSuccessful && task.result != null) {
                        for (doc in task.result) {
                            val postObj = JSONObject(doc.data)
                            postArray.put(postObj)
                        }
                    }

                    exportRoot.put("post_pubblicati", postArray)

                    // Salvataggio tramite FileExportHelper
                    var nomeFile = "Spottio_Dati_$currentUsername"
                    val mimeType: String
                    val contenutoFile: String

                    if (formatoJson) {
                        nomeFile += ".json"
                        mimeType = "application/json"
                        contenutoFile = exportRoot.toString(2)
                    } else {
                        nomeFile += ".html"
                        mimeType = "text/html"
                        contenutoFile = HtmlExportGenerator.generaPaginaHtmlArchivio(exportRoot)
                    }

                    FileExportHelper.salvaInCartellaDownloadPubblica(context, nomeFile, mimeType, contenutoFile)
                    Toast.makeText(context, "Dati esportati con successo!", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Errore formattazione dati.", Toast.LENGTH_SHORT).show()
                } finally {
                    ripristinaBottone(btn, stringaOriginale)
                }
            }
    }

    private fun ripristinaBottone(btn: Button, stringaOriginale: String) {
        btn.isEnabled = true
        btn.text = stringaOriginale
    }

    private fun ottieniDataIso(data: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ITALIAN)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(data)
    }
}