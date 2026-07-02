package com.example.spottio

import java.util.Date

data class ChatMessage @JvmOverloads constructor(
    @JvmField var docId: String? = "",
    @JvmField var text: String? = "",
    @JvmField var isMe: Boolean = false,
    @JvmField var timestamp: Date? = Date(),
    @JvmField var mostraData: Boolean = false,
    @JvmField var testoData: String? = "",
    @JvmField var senderName: String? = "",
    @JvmField var isGroup: Boolean = false,

    // FIX: A prova di crash
    @JvmField var profilePicUri: String? = "",

    @JvmField var isVerified: Boolean = false
)