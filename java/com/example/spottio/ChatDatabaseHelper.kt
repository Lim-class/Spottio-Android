package com.example.spottio

import android.widget.ListView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatDatabaseHelper(
    private val db: FirebaseFirestore,
    private val currentUser: String?,
    private val targetUser: String?,
    private val isGroup: Boolean,
    private val conversationId: String,
    private val adapter: ChatAdapter,
    private val messageList: ArrayList<ChatMessage>,
    private val lvMessages: ListView
) {
    // Aggiunto per poter "spegnere" il listener quando usciamo dal fragment!
    private var snapshotListener: ListenerRegistration? = null

    fun listenForMessages(fragment: Fragment) {
        if (currentUser == null || targetUser == null) return

        val query = if (isGroup) {
            db.collection("groups").document(targetUser).collection("chats")
        } else {
            db.collection("chats").document(conversationId).collection("messages")
        }

        // Salviamo il listener per poterlo fermare in futuro
        snapshotListener = query.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null || value == null || !fragment.isAdded) return@addSnapshotListener

                messageList.clear()
                var ultimaDataMostrata = ""

                for (doc in value) {
                    val sender = doc.getString("sender")
                    val txt = doc.getString("text") ?: ""

                    val ts = doc.getTimestamp("timestamp")
                    val date = ts?.toDate() ?: Date()

                    val dataCorrente = formattaDataDivisore(date)
                    val mostraData = dataCorrente != ultimaDataMostrata
                    if (mostraData) {
                        ultimaDataMostrata = dataCorrente
                    }

                    val isMe = sender != null && sender == currentUser

                    var cachedUri: String? = null
                    var isVerified = false

                    if (sender != null) {
                        val cachedData = UserCache.getUser(sender)
                        if (cachedData != null) {
                            cachedUri = cachedData.pfpUri
                            isVerified = cachedData.isVerified
                        }
                    }

                    val msg = ChatMessage(
                        doc.id, txt, isMe, date, mostraData, dataCorrente, sender ?: "Sistema", isGroup, cachedUri, isVerified
                    )
                    messageList.add(msg)

                    // --- LOGICA PULITA: Usiamo il metodo asincrono ---
                    if (sender != null && UserCache.getUser(sender) == null) {
                        UserCache.fetchUserAsync(sender) {
                            if (fragment.isAdded) {
                                val data = UserCache.getUser(sender)
                                if (data != null) {
                                    // Aggiorniamo i messaggi caricati a cui mancava la foto
                                    for (m in messageList) {
                                        if (m.senderName == sender) {
                                            m.profilePicUri = data.pfpUri
                                            m.isVerified = data.isVerified
                                        }
                                    }
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged()
                if (messageList.isNotEmpty()) {
                    lvMessages.setSelection(messageList.size - 1)
                }
            }
    }

    fun sendMessage(text: String) {
        if (currentUser == null) return

        val msg = mutableMapOf<String, Any>(
            "sender" to currentUser,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        val previewId: String

        if (isGroup) {
            previewId = targetUser!!
            db.collection("groups").document(targetUser).collection("chats").add(msg)
        } else {
            msg["receiver"] = targetUser!!
            msg["conversationId"] = conversationId
            previewId = conversationId
            db.collection("chats").document(conversationId).collection("messages").add(msg)
        }

        aggiornaPreview(previewId, text)
    }

    private fun aggiornaPreview(previewId: String, text: String) {
        val previewData = mutableMapOf<String, Any>(
            "lastMessage" to text,
            "lastUpdate" to FieldValue.serverTimestamp()
        )

        if (!isGroup) {
            previewData["participants"] = listOf(currentUser, targetUser)
            previewData["isGroup"] = false
        }

        db.collection("chat_previews").document(previewId).set(previewData, SetOptions.merge())
    }

    private fun formattaDataDivisore(date: Date): String {
        val calOggi = Calendar.getInstance()
        val calIeri = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val calMsg = Calendar.getInstance().apply { time = date }

        return when {
            calMsg.get(Calendar.YEAR) == calOggi.get(Calendar.YEAR) &&
                    calMsg.get(Calendar.DAY_OF_YEAR) == calOggi.get(Calendar.DAY_OF_YEAR) -> "Oggi"

            calMsg.get(Calendar.YEAR) == calIeri.get(Calendar.YEAR) &&
                    calMsg.get(Calendar.DAY_OF_YEAR) == calIeri.get(Calendar.DAY_OF_YEAR) -> "Ieri"

            else -> SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(date)
        }
    }

    // NUOVO METODO: Ferma il flusso dati da Firebase (Evita i "Fantasmi")
    fun stopListening() {
        snapshotListener?.remove()
        snapshotListener = null
    }
}