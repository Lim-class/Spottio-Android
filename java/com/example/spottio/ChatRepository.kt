package com.example.spottio

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.Date

class ChatRepository(
    context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val currentUser: String,
    private val targetUser: String,
    private val isGroup: Boolean,
    private val conversationId: String
) {
    private var snapshotListener: ListenerRegistration? = null
    private val prefs: SharedPreferences = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)

    // Recupera in tempo reale i messaggi che l'utente ha deciso di nascondere
    private val hiddenMessages: MutableSet<String>
        get() = prefs.getStringSet("hidden_messages_$currentUser", mutableSetOf()) ?: mutableSetOf()

    fun listenForMessages(onMessagesChanged: (List<ChatMessage>) -> Unit) {
        val query = if (isGroup) {
            db.collection("groups").document(targetUser).collection("chats")
        } else {
            db.collection("chats").document(conversationId).collection("messages")
        }

        snapshotListener = query.orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null || value == null) return@addSnapshotListener

                val messages = mutableListOf<ChatMessage>()
                val currentHidden = hiddenMessages

                for (doc in value) {
                    // Se il messaggio è tra quelli nascosti, lo saltiamo direttamente
                    if (currentHidden.contains(doc.id)) continue

                    val sender = doc.getString("sender") ?: "Sistema"
                    val txt = doc.getString("text") ?: ""
                    val ts = doc.getTimestamp("timestamp")?.toDate() ?: Date()
                    val isMe = sender == currentUser

                    var pfpUri = ""
                    var verified = false

                    val cachedData = UserCache.getUser(sender)
                    if (cachedData != null) {
                        // SOLUZIONE: pfpUri in UserData è già garantito essere String (non null)
                        pfpUri = cachedData.pfpUri
                        verified = cachedData.isVerified
                    } else {
                        // Se non è in cache, lo scarica in background
                        UserCache.fetchUserAsync(sender) {}
                    }

                    messages.add(
                        ChatMessage(
                            docId = doc.id, text = txt, isMe = isMe,
                            timestamp = ts, senderName = sender, isGroup = isGroup,
                            profilePicUri = pfpUri, isVerified = verified
                        )
                    )
                }
                onMessagesChanged(messages)
            }
    }

    fun sendMessage(text: String) {
        val msg = mutableMapOf<String, Any>(
            "sender" to currentUser,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp()
        )

        val previewId = if (isGroup) targetUser else conversationId

        if (isGroup) {
            db.collection("groups").document(targetUser).collection("chats").add(msg)
        } else {
            msg["receiver"] = targetUser
            msg["conversationId"] = conversationId
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

    fun editMessage(docId: String, newText: String) {
        val collectionPath = if (isGroup) "groups/$targetUser/chats" else "chats/$conversationId/messages"
        db.collection(collectionPath).document(docId).update("text", newText)
    }

    fun deleteMessage(docId: String, deleteForEveryone: Boolean, onLocalHide: () -> Unit) {
        if (deleteForEveryone) {
            val collectionPath = if (isGroup) "groups/$targetUser/chats" else "chats/$conversationId/messages"
            db.collection(collectionPath).document(docId).delete()
        } else {
            val hidden = hiddenMessages
            hidden.add(docId)
            prefs.edit().putStringSet("hidden_messages_$currentUser", hidden).apply()
            onLocalHide()
        }
    }

    fun stopListening() {
        snapshotListener?.remove()
        snapshotListener = null
    }
}