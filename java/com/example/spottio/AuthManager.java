package com.example.spottio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AuthManager {

    private final Activity activity;
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;

    public AuthManager(Activity activity) {
        this.activity = activity;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public void checkSessionAndRedirect() {
        if (mAuth.getCurrentUser() != null) {
            String uidSalvato = getCurrentUserUid(activity);
            if (!uidSalvato.isEmpty()) {
                activity.startActivity(new Intent(activity, HomeActivity.class));
                activity.finish();
            }
        }
    }

    public void handleLogin(String email, String pass) {
        if (!validateInput(email, pass)) return;

        // Controlla semplicemente se c'è una @ nell'email
        if (email.contains("@")) {
            eseguiLoginFirebase(email, pass);
        } else {
            Toast.makeText(activity, "Inserisci un indirizzo email valido", Toast.LENGTH_SHORT).show();
        }
    }

    private void eseguiLoginFirebase(String email, String pass) {
        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                procediAllaHome();
            } else {
                Toast.makeText(activity, "Login fallito", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void handleRegister(String email, String pass, String username, EditText etUsername) {
        if (TextUtils.isEmpty(email) || !validateInput(username, pass)) return;

        db.collection("users").whereEqualTo("username", username).limit(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().isEmpty()) {
                    etUsername.setError("Username in uso");
                } else {
                    creaNuovoAccount(email, pass, username);
                }
            }
        });
    }

    private void creaNuovoAccount(String email, String pass, String username) {
        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = task.getResult().getUser().getUid();

                Map<String, Object> user = new HashMap<>();
                user.put("username", username);
                user.put("email", email);
                user.put("bio", "Ciao, sono nuovo su Spottio!");
                user.put("createdAt", FieldValue.serverTimestamp());
                user.put("isAdmin", false);
                user.put("isSuspended", false);
                user.put("isVerified", false);
                user.put("followers", new ArrayList<String>());
                user.put("following", new ArrayList<String>());

                // ✅ MAGIA: Aggiunge tutti i dati del dispositivo in una sola riga
                user.putAll(DeviceInfoHelper.getDeviceSpecs());

                db.collection("users").document(uid).set(user)
                        .addOnSuccessListener(aVoid -> procediAllaHome());
            } else {
                Toast.makeText(activity, "Registrazione fallita", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void gestisciRecuperoPassword(String email) {
        if (email.contains("@")) {
            inviaEmailReset(email);
        } else {
            Toast.makeText(activity, "Inserisci un indirizzo email valido per recuperare la password", Toast.LENGTH_SHORT).show();
        }
    }

    private void inviaEmailReset(String email) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(activity, "Email sent!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void procediAllaHome() {
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> updates = new HashMap<>();
        // ✅ Aggiunge i dati del dispositivo in una sola riga
        updates.putAll(DeviceInfoHelper.getDeviceSpecs());

        db.collection("users").document(uid).update(updates)
                .addOnCompleteListener(deviceTask -> {

                    db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                        SharedPreferences prefs = activity.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();

                        editor.putString("uid_attivo", uid);

                        if (documentSnapshot.exists()) {
                            String dbUsername = documentSnapshot.getString("username");
                            editor.putString("username_attivo", dbUsername != null ? dbUsername : "User");

                            Boolean isVerified = documentSnapshot.getBoolean("isVerified");
                            editor.putBoolean(uid + "_is_verified", isVerified != null && isVerified);

                            Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                            editor.putBoolean(uid + "_is_admin", isAdmin != null && isAdmin);
                        } else {
                            editor.putBoolean(uid + "_is_verified", false);
                        }

                        editor.apply();

                        activity.startActivity(new Intent(activity, HomeActivity.class));
                        activity.finish();
                    }).addOnFailureListener(e -> {
                        SharedPreferences prefs = activity.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("uid_attivo", uid)
                                .putBoolean(uid + "_is_verified", false)
                                .apply();

                        activity.startActivity(new Intent(activity, HomeActivity.class));
                        activity.finish();
                    });
                });
    }

    private boolean validateInput(String u, String p) {
        if (TextUtils.isEmpty(u) || p.length() < 6) {
            Toast.makeText(activity, "Dati non validi", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void logout() {
        SharedPreferences prefs = activity.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        // Utilizziamo il recupero dell'UID centralizzato che abbiamo ottimizzato prima
        String currentUid = getCurrentUserUid(activity);

        if (!currentUid.isEmpty()) {
            // Imposta l'utente offline su Firestore prima di chiudere la sessione
            db.collection("users").document(currentUid)
                    .update("online", false)
                    .addOnCompleteListener(task -> eseguiSignOut(prefs));
        } else {
            eseguiSignOut(prefs);
        }
    }

    private void eseguiSignOut(SharedPreferences prefs) {
        // 1. Pulisci completamente le SharedPreferences locali
        prefs.edit().clear().apply();

        // 2. Prepara l'Intent, distruggi la HomeActivity e torna alla MainActivity
        Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish(); // Chiude l'activity da cui è partito il logout

        // 3. IMPORTANTE: Esegui il signOut da Firebase come ultimissima operazione.
        // Questo evita la race-condition e i messaggi "Permission Denied" nei fragment in background.
        mAuth.signOut();
    }

    // --- METODI STATICI UTILITY (NUOVI) ---

    /**
     * Recupera l'UID dell'utente correntemente loggato dalle SharedPreferences.
     * @param context Il contesto corrente
     * @return L'UID dell'utente o una stringa vuota se non trovato
     */
    public static String getCurrentUserUid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        return prefs.getString("uid_attivo", "");
    }

    /**
     * Verifica se l'utente correntemente loggato è un amministratore.
     * @param context Il contesto corrente
     * @return true se è admin, false altrimenti
     */
    public static boolean isCurrentUserAdmin(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        String uid = prefs.getString("uid_attivo", "");
        if (uid.isEmpty()) return false;
        return prefs.getBoolean(uid + "_is_admin", false);
    }
}