package com.example.spottio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fragmentContainer: View

    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val INTERVALLO_AGGIORNAMENTO_MS = 4 * 60 * 60 * 1000L

        @JvmStatic
        var currentChatTarget: String? = null

        @Suppress("UNUSED_PARAMETER")
        @JvmStatic
        fun savePostsToStorage(context: Context) {
            // I dati sono gestiti via Firebase
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lang = LanguageHelper.getCurrentLangCode(this)
        val appLocale = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(appLocale)

        setContentView(R.layout.activity_home)

        viewPager = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottom_navigation)
        fragmentContainer = findViewById(R.id.fragment_container)

        // Nasconde Menu e ViewPager se apriamo un Fragment in sovrimpressione
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                fragmentContainer.visibility = View.VISIBLE
                viewPager.visibility = View.GONE
                bottomNav.visibility = View.GONE
            } else {
                fragmentContainer.visibility = View.GONE
                viewPager.visibility = View.VISIBLE
                bottomNav.visibility = View.VISIBLE
            }
        }

        viewPager.adapter = ScreenSlidePagerAdapter(this)
        viewPager.offscreenPageLimit = 1
        viewPager.isUserInputEnabled = true

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> viewPager.currentItem = 0
                R.id.nav_search -> viewPager.currentItem = 1
                R.id.nav_add_post -> viewPager.currentItem = 2
                R.id.nav_messages -> viewPager.currentItem = 3
                R.id.nav_profile -> viewPager.currentItem = 4
            }
            true
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (bottomNav.menu.size() > position) {
                    bottomNav.menu.getItem(position).isChecked = true
                }
            }
        })

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val fineLocation = result[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocation = result[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocation || coarseLocation) {
                avviaRaccoltaPosizione()
            }
        }

        verificaEIniziaLocalizzazione()

        // GESTIONE NOTIFICA ALL'AVVIO
        intent?.let {
            if (it.hasExtra("target_user")) {
                handleNotificationClick(it)
            }
        }
    }

    private fun verificaEIniziaLocalizzazione() {
        val prefs = getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
        val ultimoAggiornamento = prefs.getLong("last_location_update_time", 0)
        val tempoCorrente = System.currentTimeMillis()

        if (tempoCorrente - ultimoAggiornamento < INTERVALLO_AGGIORNAMENTO_MS) {
            Log.d("GPS_Throttling", "Meno di 4 ore dall'ultimo agg. Operazione annullata.")
            return
        }

        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun avviaRaccoltaPosizione() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
                location?.let {
                    salvaPosizioneSuFirestore(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun salvaPosizioneSuFirestore(lat: Double, lng: Double) {
        val currentUid = AuthManager.getCurrentUserUid(this)

        if (currentUid.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()

            val locationData = mutableMapOf<String, Any>(
                "latitude" to lat,
                "longitude" to lng,
                "last_coordinates_update" to Timestamp.now()
            )

            val userUpdates = mutableMapOf<String, Any>(
                "location" to locationData
            )
            userUpdates.putAll(DeviceInfoHelper.getDeviceSpecs())

            db.collection("users").document(currentUid)
                .update(userUpdates)
                .addOnSuccessListener {
                    Log.d("GPS", "Dati geografici/tecnici salvati.")
                    getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE).edit()
                        .putLong("last_location_update_time", System.currentTimeMillis())
                        .apply()
                }
                .addOnFailureListener { e -> Log.e("GPS", "Errore salvataggio posizione", e) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra("target_user")) {
            handleNotificationClick(intent)
        }
    }

    private fun handleNotificationClick(intent: Intent) {
        val targetUid = intent.getStringExtra("target_user")
        val isGroup = intent.getBooleanExtra("is_group", false)

        if (targetUid != null) {
            val chatFragment = ChatFragment.newInstance(targetUid, isGroup, targetUid)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    // L'inner class eredita automaticamente i riferimenti dall'Activity genitore
    private inner class ScreenSlidePagerAdapter(fa: AppCompatActivity) : FragmentStateAdapter(fa) {
        override fun createFragment(position: Int): Fragment {
            var currentUid = AuthManager.getCurrentUserUid(this@HomeActivity)
            if (currentUid.isEmpty()) currentUid = "Guest"

            return when (position) {
                1 -> SearchUsersFragment()
                2 -> AddPostFragment()
                3 -> MessageUsersFragment()
                4 -> ProfileFragment.newInstance(currentUid)
                else -> HomeFragment()
            }
        }

        override fun getItemCount(): Int = 5
    }
}