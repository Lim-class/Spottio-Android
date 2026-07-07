package com.example.spottio

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class SettingsAuthManager(
    private val mAuth: FirebaseAuth,
    private val context: Context,
    private val currentUsername: String
) {

    fun setupPasswordLogic(view: View) {
        val oldPass = view.findViewById<EditText>(R.id.oldPass)
        val newPass = view.findViewById<EditText>(R.id.newPass)
        val btnUpdatePass = view.findViewById<Button>(R.id.btnUpdatePass)

        btnUpdatePass.setOnClickListener {
            val strOld = oldPass.text.toString().trim()
            val strNew = newPass.text.toString().trim()
            val user = mAuth.currentUser

            if (user?.email == null) {
                Toast.makeText(context, "Nessun utente Firebase autenticato.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (strOld.isEmpty() || strNew.isEmpty()) {
                Toast.makeText(context, "Compila tutti i campi della password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = EmailAuthProvider.getCredential(user.email!!, strOld)
            btnUpdatePass.isEnabled = false
            btnUpdatePass.text = "Elaborazione..."

            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.updatePassword(strNew).addOnCompleteListener { updateTask ->
                        btnUpdatePass.isEnabled = true
                        btnUpdatePass.text = "Aggiorna Password"

                        if (updateTask.isSuccessful) {
                            Toast.makeText(context, "Password aggiornata con successo!", Toast.LENGTH_SHORT).show()
                            oldPass.setText("")
                            newPass.setText("")
                        } else {
                            Toast.makeText(context, "Errore: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    btnUpdatePass.isEnabled = true
                    btnUpdatePass.text = "Aggiorna Password"
                    Toast.makeText(context, "Errore: Vecchia password non corretta.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun setupEmailLogic(view: View) {
        val emailCurrentPass = view.findViewById<EditText>(R.id.emailCurrentPass)
        val newEmail = view.findViewById<EditText>(R.id.newEmail)
        val btnUpdateEmail = view.findViewById<Button>(R.id.btnUpdateEmail)

        if (currentUsername == "Guest") {
            emailCurrentPass.isEnabled = false
            newEmail.isEnabled = false
            btnUpdateEmail.isEnabled = false
            btnUpdateEmail.text = "Non disponibile per Guest"
            return
        }

        btnUpdateEmail.setOnClickListener {
            val strPass = emailCurrentPass.text.toString().trim()
            val strEmail = newEmail.text.toString().trim()
            val user = mAuth.currentUser

            if (user?.email == null) {
                Toast.makeText(context, "Nessun utente Firebase autenticato.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (strPass.isEmpty() || strEmail.isEmpty()) {
                Toast.makeText(context, "Compila tutti i campi.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnUpdateEmail.isEnabled = false
            btnUpdateEmail.text = "Elaborazione..."

            val credential = EmailAuthProvider.getCredential(user.email!!, strPass)

            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.verifyBeforeUpdateEmail(strEmail).addOnCompleteListener { updateTask ->
                        btnUpdateEmail.isEnabled = true
                        btnUpdateEmail.text = "Aggiorna Email"

                        if (updateTask.isSuccessful) {
                            Toast.makeText(context, "Link di verifica inviato alla nuova email! Controlla la tua casella.", Toast.LENGTH_LONG).show()
                            emailCurrentPass.setText("")
                            newEmail.setText("")
                        } else {
                            Toast.makeText(context, "Errore: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    btnUpdateEmail.isEnabled = true
                    btnUpdateEmail.text = "Aggiorna Email"
                    Toast.makeText(context, "Errore: Password attuale non corretta.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}