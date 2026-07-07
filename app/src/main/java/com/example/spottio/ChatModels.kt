package com.example.spottio

import java.util.Date

// Il dato puro salvato su Firebase
data class ChatMessage(
    var docId: String = "",
    var text: String = "",
    var isMe: Boolean = false,
    var timestamp: Date = Date(),
    var senderName: String = "",
    var isGroup: Boolean = false,
    var profilePicUri: String = "",
    var isVerified: Boolean = false
)

// La classe che l'Adapter usa per capire se disegnare un messaggio o l'etichetta della data
sealed class ChatItem {
    data class DateHeader(val dateText: String) : ChatItem()
    data class Message(val chatMessage: ChatMessage) : ChatItem()
}