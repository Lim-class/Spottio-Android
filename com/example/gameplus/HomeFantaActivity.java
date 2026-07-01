package com.example.gameplus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeFantaActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private ImageButton btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_fanta);

        mAuth = FirebaseAuth.getInstance();
        tvWelcome = findViewById(R.id.tvWelcome);
        btnLogout = findViewById(R.id.btnLogout);

        // Recupero username per il benvenuto
        SharedPreferences prefs = getSharedPreferences("SpottioPrefs", MODE_PRIVATE);
        String username = prefs.getString("username_attivo", "Utente");
        tvWelcome.setText("Ciao, " + username);

        // Logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            prefs.edit().remove("username_attivo").apply();
            startActivity(new Intent(HomeFantaActivity.this, FantaLoginActivity.class));
            finish();
        });

        // Configurazione Menu in Basso
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_partecipa) {
                selectedFragment = new PartecipaFragment();
            } else if (itemId == R.id.nav_in_corso) {
                selectedFragment = new InCorsoFragment();
            } else if (itemId == R.id.nav_crea) {
                selectedFragment = new CreaFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Mostra il primo Fragment all'avvio dell'app (Partecipa)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PartecipaFragment())
                    .commit();
        }
    }
}