package com.example.spottio;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsAuthManager {

    private final FirebaseAuth mAuth;
    private final Context context;
    private final String currentUsername;

    public SettingsAuthManager(FirebaseAuth mAuth, Context context, String currentUsername) {
        this.mAuth = mAuth;
        this.context = context;
        this.currentUsername = currentUsername;
    }

    public void setupPasswordLogic(View view) {
        EditText oldPass = view.findViewById(R.id.oldPass);
        EditText newPass = view.findViewById(R.id.newPass);
        Button btnUpdatePass = view.findViewById(R.id.btnUpdatePass);

        btnUpdatePass.setOnClickListener(v -> {
            String strOld = oldPass.getText().toString().trim();
            String strNew = newPass.getText().toString().trim();

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null || user.getEmail() == null) {
                Toast.makeText(context, "Nessun utente Firebase autenticato.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (strOld.isEmpty() || strNew.isEmpty()) {
                Toast.makeText(context, "Compila tutti i campi della password.", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), strOld);
            btnUpdatePass.setEnabled(false);
            btnUpdatePass.setText("Elaborazione...");

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(strNew).addOnCompleteListener(updateTask -> {
                        btnUpdatePass.setEnabled(true);
                        btnUpdatePass.setText("Aggiorna Password");
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(context, "Password aggiornata con successo!", Toast.LENGTH_SHORT).show();
                            oldPass.setText("");
                            newPass.setText("");
                        } else {
                            Toast.makeText(context, "Errore: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    btnUpdatePass.setEnabled(true);
                    btnUpdatePass.setText("Aggiorna Password");
                    Toast.makeText(context, "Errore: Vecchia password non corretta.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    public void setupEmailLogic(View view) {
        EditText emailCurrentPass = view.findViewById(R.id.emailCurrentPass);
        EditText newEmail = view.findViewById(R.id.newEmail);
        Button btnUpdateEmail = view.findViewById(R.id.btnUpdateEmail);

        if ("Guest".equals(currentUsername)) {
            emailCurrentPass.setEnabled(false);
            newEmail.setEnabled(false);
            btnUpdateEmail.setEnabled(false);
            btnUpdateEmail.setText("Non disponibile per Guest");
            return;
        }

        btnUpdateEmail.setOnClickListener(v -> {
            String strPass = emailCurrentPass.getText().toString().trim();
            String strEmail = newEmail.getText().toString().trim();

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null || user.getEmail() == null) {
                Toast.makeText(context, "Nessun utente Firebase autenticato.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (strPass.isEmpty() || strEmail.isEmpty()) {
                Toast.makeText(context, "Compila tutti i campi.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnUpdateEmail.setEnabled(false);
            btnUpdateEmail.setText("Elaborazione...");

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), strPass);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.verifyBeforeUpdateEmail(strEmail).addOnCompleteListener(updateTask -> {
                        btnUpdateEmail.setEnabled(true);
                        btnUpdateEmail.setText("Aggiorna Email");
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(context, "Link di verifica inviato alla nuova email! Controlla la tua casella.", Toast.LENGTH_LONG).show();
                            emailCurrentPass.setText("");
                            newEmail.setText("");
                        } else {
                            Toast.makeText(context, "Errore: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    btnUpdateEmail.setEnabled(true);
                    btnUpdateEmail.setText("Aggiorna Email");
                    Toast.makeText(context, "Errore: Password attuale non corretta.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}