package com.example.spottio

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/*
Questo Helper si occupa della gestione completa dello sfondo in Kotlin:
caricamento, salvataggio (Firestore e SharedPreferences) e visualizzazione dei menu di scelta.
*/
class ChatBackgroundHelper(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val currentUser: String?,
    private val ivChatBackground: ImageView,
    private val scegliSfondoLauncher: ActivityResultLauncher<String>
) {

    private val prefs: SharedPreferences = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)

    fun caricaSfondoDaFirestore() {
        if (currentUser == null) return

        db.collection("users").document(currentUser).get().addOnSuccessListener { doc ->
            if (doc.exists() && doc.contains("chatBackgroundColor")) {
                val hexColor = doc.getString("chatBackgroundColor")
                if (!hexColor.isNullOrEmpty()) {
                    ivChatBackground.setImageDrawable(null)
                    try {
                        ivChatBackground.setBackgroundColor(Color.parseColor(hexColor))
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun caricaSfondoSalvato() {
        val configString = prefs.getString("chat_bg_$currentUser", null) ?: return

        try {
            when {
                configString.startsWith("color:") -> {
                    ivChatBackground.setImageDrawable(null)
                    ivChatBackground.setBackgroundColor(Color.parseColor(configString.substring(6)))
                }
                configString.startsWith("drawable:") -> {
                    val drawableName = configString.substring(9)
                    val resID = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
                    if (resID != 0) {
                        ivChatBackground.setBackgroundColor(Color.TRANSPARENT)
                        ivChatBackground.setImageResource(resID)
                    }
                }
                configString.startsWith("uri:") -> {
                    ivChatBackground.setBackgroundColor(Color.TRANSPARENT)
                    val uri = Uri.parse(configString.substring(4))
                    ivChatBackground.setImageURI(uri)
                }
                else -> {
                    val uri = Uri.parse(configString)
                    ivChatBackground.setImageURI(uri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun mostraOpzioniSfondo() {
        val opzioni = arrayOf("Scegli immagine dalla galleria", "Scegli un colore a tinta unita")
        AlertDialog.Builder(context)
            .setTitle("Personalizza Sfondo")
            .setItems(opzioni) { _, which ->
                if (which == 0) {
                    scegliSfondoLauncher.launch("image/*")
                } else {
                    scegliSfondoColoreDaDb()
                }
            }.show()
    }

    private fun scegliSfondoColoreDaDb() {
        db.collection("ColoriSfondo").get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null && !task.result!!.isEmpty) {
                val colorNames = mutableListOf<String>()
                val colorHexes = mutableListOf<String>()

                for (doc in task.result!!) {
                    val name = doc.getString("nome")
                    val hex = doc.getString("hex")
                    if (name != null && hex != null) {
                        colorNames.add(name)
                        colorHexes.add(hex)
                    }
                }

                if (colorNames.isEmpty()) {
                    Toast.makeText(context, "Nessun colore trovato", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                val options = colorNames.toTypedArray()
                AlertDialog.Builder(context)
                    .setTitle("Seleziona un colore")
                    .setItems(options) { _, which ->
                        val selectedHex = colorHexes[which]
                        salvaColoreSfondo(selectedHex)
                    }
                    .show()
            } else {
                Toast.makeText(context, "Errore nel caricamento dei colori", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun applicaSfondoDaUri(uri: Uri) {
        ivChatBackground.setBackgroundColor(Color.TRANSPARENT)
        ivChatBackground.setImageURI(uri)
        prefs.edit().putString("chat_bg_$currentUser", "uri:$uri").apply()

        if (currentUser != null) {
            db.collection("users").document(currentUser)
                .update("chatBackgroundColor", FieldValue.delete())
        }
    }

    // Adesso questo metodo può essere privato, dato che viene usato solo internamente alla classe
    private fun salvaColoreSfondo(hexCode: String) {
        if (currentUser == null) return

        ivChatBackground.setImageDrawable(null)
        try {
            ivChatBackground.setBackgroundColor(Color.parseColor(hexCode))
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Toast.makeText(context, "Codice colore non valido", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit().remove("chat_bg_$currentUser").apply()

        db.collection("users").document(currentUser)
            .update("chatBackgroundColor", hexCode)
            .addOnSuccessListener {
                Toast.makeText(context, "Sfondo salvato!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Errore durante il salvataggio remoto", Toast.LENGTH_SHORT).show()
            }
    }
}