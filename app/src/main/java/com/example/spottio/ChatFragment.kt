package com.example.spottio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatFragment : Fragment() {

    private var currentUser: String? = null
    private var targetUser: String? = null
    private var isGroup = false
    private var conversationId = ""
    private var chatTitleName: String? = null

    private lateinit var lvMessages: ListView
    private lateinit var etMessage: EditText
    private lateinit var ivChatBackground: ImageView
    private lateinit var btnSendChat: ImageButton
    private lateinit var tvChatTitle: TextView
    private lateinit var btnChangeBg: ImageButton

    private lateinit var viewModel: ChatViewModel
    private var backgroundHelper: ChatBackgroundHelper? = null
    private lateinit var scegliSfondo: ActivityResultLauncher<String>

    companion object {
        @JvmStatic
        fun newInstance(targetUser: String, isGroup: Boolean, titleName: String?): ChatFragment {
            return ChatFragment().apply {
                arguments = Bundle().apply {
                    putString("target_user", targetUser)
                    putBoolean("is_group", isGroup)
                    putString("title_name", titleName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            currentUser = authUser.uid
        } else {
            val prefs = requireContext().getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
            currentUser = prefs.getString("uid_attivo", null)
        }

        arguments?.let {
            targetUser = it.getString("target_user")
            isGroup = it.getBoolean("is_group", false)
            chatTitleName = it.getString("title_name")
        }

        if (!isGroup && currentUser != null && targetUser != null) {
            val ids = listOf(currentUser!!, targetUser!!).sorted()
            conversationId = "${ids[0]}_${ids[1]}"
        }

        scegliSfondo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                backgroundHelper?.applicaSfondoDaUri(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lvMessages = view.findViewById(R.id.lvMessages)
        etMessage = view.findViewById(R.id.etChatMessage)
        btnSendChat = view.findViewById(R.id.btnSendChat)
        ivChatBackground = view.findViewById(R.id.ivChatBackground)
        tvChatTitle = view.findViewById(R.id.tvChatTitle)
        btnChangeBg = view.findViewById(R.id.btnChangeBg)

        setupTitle()

        // 1. Gestione Sfondo (manteniamo intatto il tuo Helper UI originario)
        backgroundHelper = ChatBackgroundHelper(requireContext(), FirebaseFirestore.getInstance(), currentUser, ivChatBackground, scegliSfondo)
        backgroundHelper?.caricaSfondoDaFirestore()
        backgroundHelper?.caricaSfondoSalvato()

        btnChangeBg.setOnClickListener {
            backgroundHelper?.mostraOpzioniSfondo()
        }

        // 2. Inizializzazione pulita dell'architettura Dati
        val repository = ChatRepository(
            context = requireContext(),
            currentUser = currentUser ?: "",
            targetUser = targetUser ?: "",
            isGroup = isGroup,
            conversationId = conversationId
        )
        val factory = GenericViewModelFactory { ChatViewModel(repository) }
        viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        // 3. Osserviamo la reattività del ViewModel! (Zero lag, zero calcoli pesanti nella View)
        viewModel.chatItems.observe(viewLifecycleOwner) { items ->
            val adapter = ChatAdapter(requireContext(), items, viewModel)
            lvMessages.adapter = adapter

            if (items.isNotEmpty()) {
                lvMessages.setSelection(items.size - 1)
            }
        }

        // 4. Invio messaggi
        btnSendChat.setOnClickListener {
            val text = etMessage.text.toString()
            viewModel.sendMessage(text)
            etMessage.setText("")
        }
    }

    private fun setupTitle() {
        if (isGroup) {
            tvChatTitle.text = if (!chatTitleName.isNullOrEmpty()) chatTitleName else "Gruppo"
        } else {
            val tUser = targetUser
            if (tUser != null) {
                val cached = UserCache.getUser(tUser)
                if (cached != null) {
                    tvChatTitle.text = cached.username
                } else {
                    tvChatTitle.text = "Caricamento..."
                    FirebaseFirestore.getInstance().collection("users").document(tUser).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists() && isAdded) {
                                tvChatTitle.text = doc.getString("username") ?: "Utente"
                            }
                        }
                }
            } else {
                tvChatTitle.text = "Chat"
            }
        }

        tvChatTitle.setOnClickListener {
            if (isGroup) {
                val intent = Intent(requireContext(), GroupSettingsActivity::class.java).apply {
                    putExtra("group_id", targetUser)
                }
                startActivity(intent)
            }
        }
    }
}