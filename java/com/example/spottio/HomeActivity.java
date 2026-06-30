package com.example.spottio;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private View fragmentContainer;

    // Gestione della geolocalizzazione
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;

    // Costante per l'intervallo di tempo (4 ore in millisecondi)
    private static final long INTERVALLO_AGGIORNAMENTO_MS = 4 * 60 * 60 * 1000;

    public static String currentChatTarget = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // JAVA 15: Utilizzo di 'var' per snellire le dichiarazioni locali
        var prefs = getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        var lang = prefs.getString("selected_lang", "it"); // "it" di default
        var locale = new java.util.Locale(lang);
        java.util.Locale.setDefault(locale);

        var config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        setContentView(R.layout.activity_home);

        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottom_navigation);
        fragmentContainer = findViewById(R.id.fragment_container);

        // Listener per nascondere ViewPager e BottomNav quando un Fragment (come Chat) è aperto
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                fragmentContainer.setVisibility(View.VISIBLE);
                viewPager.setVisibility(View.GONE);
                bottomNav.setVisibility(View.GONE);
            } else {
                fragmentContainer.setVisibility(View.GONE);
                viewPager.setVisibility(View.VISIBLE);
                bottomNav.setVisibility(View.VISIBLE);
            }
        });

        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        viewPager.setOffscreenPageLimit(1);
        viewPager.setUserInputEnabled(true);

        // NOTA: Manteniamo gli if-else per l'id del menu perché i Resource ID (R.id.*)
        // nelle app Android recenti non sono più costanti 'final', e lo switch standard darebbe errore.
        bottomNav.setOnItemSelectedListener(item -> {
            var id = item.getItemId();
            if (id == R.id.nav_home) viewPager.setCurrentItem(0);
            else if (id == R.id.nav_search) viewPager.setCurrentItem(1);
            else if (id == R.id.nav_add_post) viewPager.setCurrentItem(2);
            else if (id == R.id.nav_messages) viewPager.setCurrentItem(3);
            else if (id == R.id.nav_profile) viewPager.setCurrentItem(4);
            return true;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (bottomNav != null && bottomNav.getMenu().size() > position) {
                    bottomNav.getMenu().getItem(position).setChecked(true);
                }
            }
        });

        // Inizializza il launcher per la richiesta dei permessi di geolocalizzazione
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // JAVA 15: var dedurrà automaticamente il tipo Boolean
                    var fineLocation = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    var coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocation || coarseLocation) {
                        avviaRaccoltaPosizione();
                    }
                }
        );

        // Controlla il tempo ed eventualmente richiedi i permessi/posizione all'avvio della Home
        verificaEIniziaLocalizzazione();

        // --- GESTIONE NOTIFICA ALL'AVVIO ---
        if (getIntent() != null && getIntent().hasExtra("target_user")) {
            handleNotificationClick(getIntent());
        }
    }

    private void verificaEIniziaLocalizzazione() {
        var prefs = getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        var ultimoAggiornamento = prefs.getLong("last_location_update_time", 0);
        var tempoCorrente = System.currentTimeMillis();

        // Se il tempo trascorso è inferiore a 4 ore, non facciamo nulla (0 letture, 0 scritture)
        if (tempoCorrente - ultimoAggiornamento < INTERVALLO_AGGIORNAMENTO_MS) {
            Log.d("GPS_Throttling", "Meno di 4 ore dall'ultimo aggiornamento. Operazione annullata per risparmiare quote database.");
            return;
        }

        // Se sono passate più di 4 ore, avviamo il flusso dei permessi e della posizione
        richiediPermessiPosizione();
    }

    private void richiediPermessiPosizione() {
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void avviaRaccoltaPosizione() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    salvaPosizioneSuFirestore(location.getLatitude(), location.getLongitude());
                }
            });
        }
    }

    private void salvaPosizioneSuFirestore(double lat, double lng) {
        // Recupero UID centralizzato
        var currentUid = AuthManager.getCurrentUserUid(this);

        if (!currentUid.isEmpty()) {
            var db = FirebaseFirestore.getInstance();

            // JAVA 15: Istanziazione semplificata della HashMap tramite 'var'
            var locationData = new HashMap<String, Object>();
            locationData.put("latitude", lat);
            locationData.put("longitude", lng);
            locationData.put("last_coordinates_update", com.google.firebase.Timestamp.now());

            // Prepariamo l'aggiornamento combinato (Posizione + Dati Tecnici)
            var userUpdates = new HashMap<String, Object>();
            userUpdates.put("location", locationData);

            // ✅ Utilizzo centralizzato dei dati tecnici del dispositivo
            userUpdates.putAll(DeviceInfoHelper.getDeviceSpecs());

            db.collection("users").document(currentUid)
                    .update(userUpdates)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("GPS", "Posizione, Dispositivo e Info tecniche salvate con successo!");
                        var prefs = getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
                        prefs.edit().putLong("last_location_update_time", System.currentTimeMillis()).apply();
                    })
                    .addOnFailureListener(e -> Log.e("GPS", "Errore nel salvataggio della posizione/dispositivo", e));
        }
    }

    // Gestisce il clic se l'attività è già in esecuzione
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra("target_user")) {
            handleNotificationClick(intent);
        }
    }

    private void handleNotificationClick(Intent intent) {
        var targetUid = intent.getStringExtra("target_user");
        var isGroup = intent.getBooleanExtra("is_group", false);

        if (targetUid != null) {
            var chatFragment = ChatFragment.newInstance(targetUid, isGroup, targetUid);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, chatFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public static void savePostsToStorage(Context context) {
        // I dati sono gestiti via Firebase
    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(AppCompatActivity fa) { super(fa); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            var currentUid = AuthManager.getCurrentUserUid(HomeActivity.this);
            if (currentUid.isEmpty()) {
                currentUid = "Guest";
            }

            // JAVA 15: Switch Expression moderna con return diretto e freccia (->)
            // Sostituisce il vecchio e verboso costrutto a case separati.
            return switch (position) {
                case 1 -> new SearchUsersFragment();
                case 2 -> new AddPostFragment();
                case 3 -> new MessageUsersFragment();
                case 4 -> ProfileFragment.newInstance(currentUid);
                default -> new HomeFragment(); // Sostituisce logicamente il 'case 0'
            };
        }

        @Override
        public int getItemCount() { return 5; }
    }
}