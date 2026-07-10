package com.example.spottio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.TextUtils
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthManager(private val activity: Activity) {

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun checkSessionAndRedirect() {
        if (mAuth.currentUser != null) {
            val uidSalvato = getCurrentUserUid(activity)
            if (uidSalvato.isNotEmpty()) {
                activity.startActivity(Intent(activity, HomeActivity::class.java))
                activity.finish()
            }
        }
    }

    fun handleLogin(email: String, pass: String) {
        if (!validateInput(email, pass)) return

        // Controlla semplicemente se c'è una @ nell'email
        if (email.contains("@")) {
            eseguiLoginFirebase(email, pass)
        } else {
            Toast.makeText(activity, "Inserisci un indirizzo email valido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eseguiLoginFirebase(email: String, pass: String) {
        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                procediAllaHome()
            } else {
                Toast.makeText(activity, "Login fallito", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleRegister(email: String, pass: String, username: String, etUsername: EditText) {
        if (TextUtils.isEmpty(email) || !validateInput(username, pass)) return

        db.collection("users").whereEqualTo("username", username).limit(1).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (!task.result.isEmpty) {
                        etUsername.error = "Username in uso"
                    } else {
                        creaNuovoAccount(email, pass, username)
                    }
                }
            }
    }

    private fun creaNuovoAccount(email: String, pass: String, username: String) {
        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = task.result.user?.uid ?: return@addOnCompleteListener

                // Usiamo hashMapOf per creare e popolare la mappa in modo elegante
                val user = hashMapOf<String, Any>(
                    "username" to username,
                    "email" to email,
                    "bio" to "Ciao, sono nuovo su Spottio!",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "isAdmin" to false,
                    "isSuspended" to false,
                    "isVerified" to false,
                    "followers" to ArrayList<String>(),
                    "following" to ArrayList<String>()
                )

                // Aggiunge i dati del dispositivo
                user.putAll(DeviceInfoHelper.getDeviceSpecs())

                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener { procediAllaHome() }
            } else {
                Toast.makeText(activity, "Registrazione fallita", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun gestisciRecuperoPassword(email: String) {
        if (email.contains("@")) {
            inviaEmailReset(email)
        } else {
            Toast.makeText(activity, "Inserisci un indirizzo email valido per recuperare la password", Toast.LENGTH_SHORT).show()
        }
    }

    private fun inviaEmailReset(email: String) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(activity, "Email sent!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun procediAllaHome() {
        val uid = mAuth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>()

        updates.putAll(DeviceInfoHelper.getDeviceSpecs())

        db.collection("users").document(uid).update(updates).addOnCompleteListener {
            db.collection("users").document(uid).get().addOnSuccessListener { documentSnapshot ->
                val prefs = activity.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)

                // Uso di apply() per concatenare tutte le operazioni in blocco
                prefs.edit().apply {
                    putString("uid_attivo", uid)

                    if (documentSnapshot.exists()) {
                        val dbUsername = documentSnapshot.getString("username")
                        putString("username_attivo", dbUsername ?: "User")

                        val isVerified = documentSnapshot.getBoolean("isVerified")
                        putBoolean("${uid}_is_verified", isVerified == true)

                        val isAdmin = documentSnapshot.getBoolean("isAdmin")
                        putBoolean("${uid}_is_admin", isAdmin == true)
                    } else {
                        putBoolean("${uid}_is_verified", false)
                    }
                }.apply()

                activity.startActivity(Intent(activity, HomeActivity::class.java))
                activity.finish()

            }.addOnFailureListener {
                val prefs = activity.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("uid_attivo", uid)
                    putBoolean("${uid}_is_verified", false)
                }.apply()

                activity.startActivity(Intent(activity, HomeActivity::class.java))
                activity.finish()
            }
        }
    }

    private fun validateInput(u: String, p: String): Boolean {
        if (TextUtils.isEmpty(u) || p.length < 6) {
            Toast.makeText(activity, "Dati non validi", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    fun logout() {
        val prefs = activity.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
        val currentUid = getCurrentUserUid(activity)

        if (currentUid.isNotEmpty()) {
            // FIX: Aggiornamento "fire-and-forget", non blocca se manca la rete
            db.collection("users").document(currentUid).update("online", false)
        }

        // Esegue il logout locale istantaneamente
        eseguiSignOut(prefs)
    }

    private fun eseguiSignOut(prefs: SharedPreferences) {
        // 1. Pulisci completamente le SharedPreferences locali
        prefs.edit().clear().apply()

        // 2. Prepara l'Intent, distruggi la HomeActivity e torna alla MainActivity
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish() // Chiude l'activity da cui è partito il logout

        // 3. IMPORTANTE: Esegui il signOut da Firebase come ultimissima operazione.
        mAuth.signOut()
    }

    // --- METODI STATICI UTILITY ---
    companion object {
        /**
         * Recupera l'UID dell'utente correntemente loggato dalle SharedPreferences.
         * @param context Il contesto corrente
         * @return L'UID dell'utente o una stringa vuota se non trovato
         */
        // @JvmStatic dice a Java che può chiamare questo metodo proprio come se fosse "public static"
        @JvmStatic
        fun getCurrentUserUid(context: Context): String {
            val prefs = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
            return prefs.getString("uid_attivo", "") ?: ""
        }

        /**
         * Verifica se l'utente correntemente loggato è un amministratore.
         * @param context Il contesto corrente
         * @return true se è admin, false altrimenti
         */
        @JvmStatic
        fun isCurrentUserAdmin(context: Context): Boolean {
            val prefs = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
            val uid = prefs.getString("uid_attivo", "") ?: ""
            if (uid.isEmpty()) return false
            return prefs.getBoolean("${uid}_is_admin", false)
        }
    }
}