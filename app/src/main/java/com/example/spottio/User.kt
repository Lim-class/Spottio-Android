package com.example.spottio

// Modello Utente compatibile con Firebase Firestore.
data class User(
    @JvmField var uid: String = "",
    @JvmField var username: String = "",
    @JvmField var email: String = "",
    @JvmField var bio: String = "",
    @JvmField var following: MutableList<String> = mutableListOf(),
    @JvmField var followers: MutableList<String> = mutableListOf(),
    @JvmField var interests: MutableList<String> = mutableListOf(),

    @get:JvmName("isAdmin") // Assicura la compatibilità del nome per Firestore
    var isAdmin: Boolean = false
)