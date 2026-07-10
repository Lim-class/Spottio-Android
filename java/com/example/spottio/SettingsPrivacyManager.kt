package com.example.spottio

import android.content.Context
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SettingsPrivacyManager(
    private val mDb: FirebaseFirestore,
    private val context: Context,
    private val currentUid: String
) {

    fun setupPrivacyLogic(view: View) {
        val checkboxPrivacy = view.findViewById<CheckBox>(R.id.checkboxPrivacy)
        val textPrivacyStatus = view.findViewById<TextView>(R.id.textPrivacyStatus)

        if (currentUid == "Guest") {
            checkboxPrivacy.isEnabled = false
            textPrivacyStatus.visibility = View.VISIBLE
            textPrivacyStatus.text = "Funzionalità non disponibile per gli utenti Guest."
            return
        }

        val userDoc = mDb.collection("users").document(currentUid)

        // Recupero dello stato iniziale
        userDoc.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists() && documentSnapshot.contains("isPrivate")) {
                val isPrivate = documentSnapshot.getBoolean("isPrivate")
                checkboxPrivacy.isChecked = isPrivate == true
            }
        }

        // 1. Usiamo setOnClickListener invece di setOnCheckedChangeListener per evitare il loop infinito
        checkboxPrivacy.setOnClickListener { v ->
            val isChecked = (v as CheckBox).isChecked

            // 2. Usiamo set con SetOptions.merge() invece di update().
            // Se il documento non esiste, lo crea senza mandare in crash l'app.
            val data = mapOf("isPrivate" to isChecked)

            userDoc.set(data, SetOptions.merge())
                .addOnSuccessListener {
                    textPrivacyStatus.visibility = View.VISIBLE
                    textPrivacyStatus.text = if (isChecked) "Il tuo account è ora Privato." else "Il tuo account è ora Pubblico."
                    textPrivacyStatus.setTextColor(if (isChecked) 0xFF388E3C.toInt() else 0xFF1976D2.toInt())
                }
        }
    }
}