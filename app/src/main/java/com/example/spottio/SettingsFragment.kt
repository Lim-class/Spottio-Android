package com.example.spottio

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.spottio.SettingsAdapter.OnBindLayoutListener
import com.example.spottio.SettingsAdapter.SettingItem
import com.example.spottio.SettingsAdapter.SettingType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsFragment : Fragment(), OnBindLayoutListener {

    private var adapter: SettingsAdapter? = null
    private var currentUsername: String = "Guest"

    // Manager per le varie sezioni
    private var authManager: SettingsAuthManager? = null
    private var privacyManager: SettingsPrivacyManager? = null
    private var exportManager: SettingsExportManager? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Inizializzazione sicura: eseguiamo questo blocco solo se il context esiste
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)

            // RECUPERIAMO ENTRAMBI: Username e UID
            currentUsername = prefs.getString("username_attivo", "Guest") ?: "Guest"
            val currentUid = prefs.getString("uid_attivo", "Guest") ?: "Guest"

            // In alternativa, puoi prendere l'UID direttamente da Firebase Auth:
            // val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "Guest"

            val mDb = FirebaseFirestore.getInstance()
            val mAuth = FirebaseAuth.getInstance()

            authManager = SettingsAuthManager(mAuth, ctx, currentUsername)

            // ATTENZIONE QUI: Passiamo currentUid al PrivacyManager perché il documento
            // su Firestore nella collection "users" è nominato con l'UID!
            privacyManager = SettingsPrivacyManager(mDb, ctx, currentUid)

            // L'export manager continua a usare currentUsername se deve cercare i post
            exportManager = SettingsExportManager(mDb, ctx, currentUsername)
        }

        // Recupero UI components senza vecchi cast manuali
        val searchSettings = view.findViewById<EditText>(R.id.searchSettings)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewSettings)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = SettingsAdapter(SettingsMenuProvider.getMenuItems(), this)
        recyclerView.adapter = adapter

        // Setup della barra di ricerca con TextWatcher ottimizzato
        searchSettings?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter?.filter(s?.toString() ?: "")
            }
        })

        return view
    }

    override fun onBindInnerLayout(type: SettingType, innerView: View, item: SettingItem) {
        // Uso del costrutto 'when' e safe call (?.) per evitare qualsiasi NullPointerException
        when (type) {
            SettingType.PASSWORD -> authManager?.setupPasswordLogic(innerView)
            SettingType.CAMBIO_EMAIL -> authManager?.setupEmailLogic(innerView)
            SettingType.PRIVACY_PROFILO -> privacyManager?.setupPrivacyLogic(innerView)
            SettingType.ESPORTA_DATI -> exportManager?.setupExportLogic(innerView)

            SettingType.COLORE_SFONDO -> setStaticInfo(
                innerView,
                "La modifica del colore di sfondo è disponibile esclusivamente nella versione web dell'applicazione."
            )

            SettingType.ELIMINA_ACCOUNT -> setStaticInfo(
                innerView,
                if (currentUsername.equals("admin", ignoreCase = true)) {
                    "L'account amministratore (admin) non può essere rimosso dal sistema di storage locale o remoto."
                } else {
                    "Per eliminare definitivamente il tuo account, invia una richiesta formale al supporto tecnico tramite i canali ufficiali."
                }
            )

            SettingType.SCARICA_APK -> setStaticInfo(
                innerView,
                "Stai già utilizzando l'applicazione Android nativa. Gli aggiornamenti futuri verranno notificati in questa sezione."
            )

            SettingType.STORIA -> setStaticInfo(
                innerView,
                "Cronologia Account: Nessuna attività recente registrata nelle ultime 48 ore."
            )

            SettingType.INFORMATIVA_PRIVACY -> setStaticInfo(
                innerView,
                "Spottio tutela i tuoi dati personali ai sensi del GDPR. I dati di profilazione dell'algoritmo vengono cancellati automaticamente dopo 30 giorni di totale inattività."
            )

            SettingType.POLICY_COMMUNITY -> setStaticInfo(
                innerView,
                "È severamente vietato pubblicare contenuti offensivi, spam o multimediali protetti da copyright. Le violazioni ripetute comportano la sospensione permanente."
            )
        }
    }

    private fun setStaticInfo(innerView: View, text: String) {
        // Cerca la textView e imposta il testo in modo sicuro su una sola riga
        innerView.findViewById<TextView>(R.id.txtInfoText)?.text = text
    }
}