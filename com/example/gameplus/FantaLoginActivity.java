package com.example.gameplus;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FantaLoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etEmail;
    private TextInputLayout layoutEmail;
    private Button btnLogin, btnRegister;
    private TextView tvSwitch;
    private TextView tvForgotPassword;
    private boolean isLoginMode = true;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Inizializziamo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // --- INIZIO BLOCCO LOGIN AUTOMATICO ---
        if (mAuth.getCurrentUser() != null) {
            SharedPreferences prefs = getSharedPreferences("SpottioPrefs", MODE_PRIVATE);
            String usernameSalvato = prefs.getString("username_attivo", null);

            if (usernameSalvato != null && !usernameSalvato.equals("Guest")) {
                // Reindirizzamento alla nuova HomeFantaActivity
                Intent intent = new Intent(FantaLoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }
        // --- FINE BLOCCO LOGIN AUTOMATICO ---

        // IMPORTANTE: Assicurati che il nome del file XML sia activity_fanta_login.xml
        setContentView(R.layout.activity_fanta_login);

        db = FirebaseFirestore.getInstance();

        // Inizializzazione Viste (Bottoni in basso)
        Button btnStoria = findViewById(R.id.btnStoria);
        Button btnPolicy = findViewById(R.id.btnPolicy);
        Button btnPrivacy = findViewById(R.id.btnPrivacy);

        // Controllo di sicurezza per i bottoni (evita crash se mancano nell'XML)
        if (btnStoria != null) btnStoria.setOnClickListener(v -> apriInfo("storia"));
        if (btnPolicy != null) btnPolicy.setOnClickListener(v -> apriInfo("policy"));
        if (btnPrivacy != null) btnPrivacy.setOnClickListener(v -> apriInfo("privacy"));

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        layoutEmail = findViewById(R.id.layoutEmail);

        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvSwitch = findViewById(R.id.tvSwitch);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Logica per cambiare tra Login e Registrazione
        tvSwitch.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            if (isLoginMode) {
                layoutEmail.setVisibility(View.GONE);
                btnLogin.setVisibility(View.VISIBLE);
                btnRegister.setVisibility(View.GONE);
                tvForgotPassword.setVisibility(View.VISIBLE);
                tvSwitch.setText("Non hai un account? Registrati");
            } else {
                layoutEmail.setVisibility(View.VISIBLE);
                btnLogin.setVisibility(View.GONE);
                btnRegister.setVisibility(View.VISIBLE);
                tvForgotPassword.setVisibility(View.GONE);
                tvSwitch.setText("Hai già un account? Accedi");
            }
        });

        btnLogin.setOnClickListener(v -> handleLogin());
        btnRegister.setOnClickListener(v -> handleRegister());
        tvForgotPassword.setOnClickListener(v -> showResetPasswordDialog());
    }

    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recupera Password");
        builder.setMessage("Inserisci il tuo Username o la tua Email per ricevere il link di reset.");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Invia", (dialog, which) -> {
            String text = input.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(FantaLoginActivity.this, "Inserisci un valore valido", Toast.LENGTH_SHORT).show();
                return;
            }
            gestisciRecuperoPassword(text);
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void gestisciRecuperoPassword(String input) {
        if (input.contains("@")) {
            inviaEmailReset(input);
        } else {
            db.collection("users").document(input).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String emailSalvata = documentSnapshot.getString("email");
                            if (emailSalvata != null && !emailSalvata.isEmpty()) {
                                inviaEmailReset(emailSalvata);
                            } else {
                                Toast.makeText(FantaLoginActivity.this, "Email non trovata per questo utente.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(FantaLoginActivity.this, "Username non esistente.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(FantaLoginActivity.this, "Errore connessione.", Toast.LENGTH_SHORT).show());
        }
    }

    private void inviaEmailReset(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(FantaLoginActivity.this, "Link di reset inviato a: " + email, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(FantaLoginActivity.this, "Errore invio mail: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleRegister() {
        final String username = etUsername.getText().toString().trim();
        final String pass = etPassword.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Inserisci la tua email");
            return;
        }
        if (!validateInput(username, pass)) return;

        // CONTROLLO UNICITÀ USERNAME
        db.collection("users").document(username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult().exists()) {
                    etUsername.setError("Questo username è già stato scelto");
                    Toast.makeText(FantaLoginActivity.this, "Username non disponibile", Toast.LENGTH_SHORT).show();
                } else {
                    creaNuovoAccount(email, pass, username);
                }
            } else {
                Toast.makeText(FantaLoginActivity.this, "Errore di connessione", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void creaNuovoAccount(String email, String pass, String username) {
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("username", username);
                        user.put("email", email);
                        user.put("bio", "Ciao, sono nuovo su Spottio!");
                        user.put("createdAt", FieldValue.serverTimestamp());
                        user.put("isAdmin", false);
                        user.put("isSuspended", false);
                        user.put("followers", new ArrayList<String>());
                        user.put("following", new ArrayList<String>());

                        db.collection("users").document(username)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(FantaLoginActivity.this, "Registrazione completata!", Toast.LENGTH_SHORT).show();
                                    procediAllaHome(username);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(FantaLoginActivity.this, "Errore DB: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });

                    } else {
                        Toast.makeText(FantaLoginActivity.this, "Errore reg: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        if (!validateInput(username, pass)) return;

        db.collection("users").document(username).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String email = doc.getString("email");
                if (email != null) {
                    eseguiLoginFirebase(email, pass, username);
                } else {
                    eseguiLoginFirebase(username + "@spottio.it", pass, username); // Fallback
                }
            } else {
                Toast.makeText(FantaLoginActivity.this, "Username non trovato", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void eseguiLoginFirebase(String email, String pass, String username) {
        mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                procediAllaHome(username);
            } else {
                Toast.makeText(FantaLoginActivity.this, "Password errata", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- METODO REINDIRIZZAMENTO ---
    private void procediAllaHome(String username) {
        SharedPreferences prefs = getSharedPreferences("SpottioPrefs", MODE_PRIVATE);
        prefs.edit().putString("username_attivo", username).apply();

        // Reindirizza a HomeFantaActivity
        Intent intent = new Intent(FantaLoginActivity.this, HomeFantaActivity.class);
        startActivity(intent);
        finish(); // Impedisce di tornare al login col tasto "Indietro"
    }

    private boolean validateInput(String u, String p) {
        if (TextUtils.isEmpty(u)) { etUsername.setError("Username richiesto"); return false; }
        if (p.length() < 6) { etPassword.setError("Min 6 caratteri"); return false; }
        return true;
    }

    private void apriInfo(String tipo) {
        Intent intent = new Intent(FantaLoginActivity.this, InfoActivity.class);
        intent.putExtra("info_type", tipo);
        startActivity(intent);
    }
}