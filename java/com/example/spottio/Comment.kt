package com.example.spottio

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@IgnoreExtraProperties
data class Comment @JvmOverloads constructor(
    // FIX: Rimosso @JvmField. Ora Kotlin genererà il getter/setter e il warning sparirà!
    @get:PropertyName("user")
    @set:PropertyName("user")
    var author: String = "",

    @JvmField var text: String = "",
    @JvmField var timestamp: Date = Date()
) : Serializable {

    @get:Exclude
    val formattedDate: String
        get() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(timestamp)
}