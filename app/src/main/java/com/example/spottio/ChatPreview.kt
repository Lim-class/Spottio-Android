package com.example.spottio

import java.util.Date

data class ChatPreview @JvmOverloads constructor(
    @JvmField var id: String? = "",
    @JvmField var name: String? = "",
    @JvmField var lastMessage: String? = "",
    @JvmField var lastUpdate: Date? = Date(),
    @JvmField var isGroup: Boolean = false,
    @JvmField var targetId: String? = "",

    // FIX: Ora accetta tranquillamente valori nulli da Firebase/Java
    @JvmField var profilePicUri: String? = "",

    @JvmField var isVerified: Boolean = false
)