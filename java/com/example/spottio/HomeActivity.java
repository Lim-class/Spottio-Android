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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.os.LocaleListCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

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

        var lang = LanguageHelper.getCurrentLangCode(this); // "it" di default

        // FIX: Approccio moderno (AndroidX / API 33+) per forzare/cambiare la lingua al volo
        // Sostituisce il vecchio e deprecato updateConfiguration()
        var appLocale = LocaleListCompat.forLanguageTags(lang);
        AppCompatDelegate.setApplicationLocales(appLocale);

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
                    // var dedurrà automaticamente il tipo Boolean
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

            var locationData = new HashMap<String, Object>();
            locationData.put("latitude", lat);
            locationData.put("longitude", lng);
            locationData.put("last_coordinates_update", com.google.firebase.Timestamp.now());

            // Prepariamo l'aggiornamento combinato (Posizione + Dati Tecnici)
            var userUpdates = new HashMap<String, Object>();
            userUpdates.put("location", locationData);

            // Utilizzo centralizzato dei dati tecnici del dispositivo
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

            // Switch Expression moderna con return diretto e freccia (->)
            return switch (position) {
                case 1 -> new SearchUsersFragment();
                case 2 -> new AddPostFragment();
                case 3 -> new MessageUsersFragment();
                case 4 -> ProfileFragment.newInstance(currentUid);
                default -> new HomeFragment();
            };
        }

        @Override
        public int getItemCount() { return 5; }
    }
}