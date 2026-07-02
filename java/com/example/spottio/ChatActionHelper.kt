package com.example.spottio

import android.content.Context
import android.content.SharedPreferences
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore

class ChatActionHelper(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val currentUser: String,
    private val isGroup: Boolean,
    private val targetUser: String,
    private val conversationId: String,
    private val hiddenMessages: MutableSet<String>,
    private val onLocalVisibilityChanged: Runnable?
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)

    fun mostraDialogoOpzioni(msg: ChatMessage) {
        val opzioni = arrayOf("Modifica", "Elimina per tutti")
        AlertDialog.Builder(context)
            .setTitle("Opzioni messaggio")
            .setItems(opzioni) { _, which ->
                if (which == 0) mostraDialogoModifica(msg)
                else msg.docId?.let { mostraDialogoElimina(it, true) }
            }.show()
    }

    private fun mostraDialogoModifica(msg: ChatMessage) {
        val input = EditText(context)
        input.setText(msg.text)
        input.setSelection(msg.text?.length ?: 0)

        AlertDialog.Builder(context)
            .setTitle("Modifica messaggio")
            .setView(input)
            .setPositiveButton("Salva") { _, _ ->
                val nuovoTesto = input.text.toString().trim()
                if (nuovoTesto.isNotEmpty() && nuovoTesto != msg.text) {
                    val collectionPath = if (isGroup) "groups/$targetUser/chats" else "chats/$conversationId/messages"

                    msg.docId?.let { dId ->
                        db.collection(collectionPath).document(dId)
                            .update("text", nuovoTesto)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Messaggio modificato", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    fun mostraDialogoElimina(docId: String, deleteForEveryone: Boolean) {
        val titolo = if (deleteForEveryone) "Elimina per tutti" else "Elimina per te"
        val messaggio = if (deleteForEveryone) {
            "Questo messaggio sparirà anche per l'altra persona."
        } else {
            "Questo messaggio non sarà più visibile sul tuo telefono."
        }

        AlertDialog.Builder(context)
            .setTitle(titolo)
            .setMessage(messaggio)
            .setPositiveButton("Elimina") { _, _ ->
                if (deleteForEveryone) {
                    val collectionPath = if (isGroup) "groups/$targetUser/chats" else "chats/$conversationId/messages"
                    db.collection(collectionPath).document(docId).delete()
                } else {
                    nascondiMessaggioLocalmente(docId)
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun nascondiMessaggioLocalmente(docId: String) {
        hiddenMessages.add(docId)
        prefs.edit().putStringSet("hidden_messages_$currentUser", hiddenMessages).apply()

        onLocalVisibilityChanged?.run()
    }
}