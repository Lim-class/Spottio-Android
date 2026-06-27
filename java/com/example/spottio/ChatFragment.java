package com.example.spottio;

import com.google.firebase.auth.FirebaseAuth;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatFragment extends Fragment {

    private String currentUser;
    private String targetUser;
    private boolean isGroup = false;
    private String conversationId = "";
    private String chatTitleName;

    private ListView lvMessages;
    private EditText etMessage;
    private ImageView ivChatBackground;
    private ImageButton btnSendChat;
    private TextView tvChatTitle;
    private ImageButton btnChangeBg;

    private ChatDatabaseHelper dbHelper;
    private ChatBackgroundHelper backgroundHelper;
    private ChatAdapter adapter;
    private ArrayList<ChatMessage> messageList;

    private ActivityResultLauncher<String> scegliSfondo;

    public static ChatFragment newInstance(String targetUser, boolean isGroup, String titleName) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("target_user", targetUser);
        args.putBoolean("is_group", isGroup);
        args.putString("title_name", titleName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FIX INFALLIBILE: Prendiamo l'UID in modo sicuro e diretto da Auth!
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = requireContext().getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
            currentUser = prefs.getString("uid_attivo", null);
        }

        if (getArguments() != null) {
            targetUser = getArguments().getString("target_user");
            isGroup = getArguments().getBoolean("is_group", false);
            chatTitleName = getArguments().getString("title_name");
        }

        // Calcolo sicuro del conversation ID
        if (!isGroup && currentUser != null && targetUser != null) {
            List<String> ids = new ArrayList<>();
            ids.add(currentUser);
            ids.add(targetUser);
            Collections.sort(ids);
            conversationId = ids.get(0) + "_" + ids.get(1);
        }

        scegliSfondo = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && backgroundHelper != null) {
                backgroundHelper.applicaSfondoDaUri(uri);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lvMessages = view.findViewById(R.id.lvMessages);
        etMessage = view.findViewById(R.id.etChatMessage);
        btnSendChat = view.findViewById(R.id.btnSendChat);
        ivChatBackground = view.findViewById(R.id.ivChatBackground);
        tvChatTitle = view.findViewById(R.id.tvChatTitle);
        btnChangeBg = view.findViewById(R.id.btnChangeBg);

        // TRADUZIONE DINAMICA DEL TITOLO (Risolve il problema dell'UID visibile)
        if (isGroup) {
            tvChatTitle.setText(chatTitleName != null && !chatTitleName.isEmpty() ? chatTitleName : "Gruppo");
        } else {
            if (targetUser != null) {
                UserCache.UserData cached = UserCache.getUser(targetUser);
                if (cached != null) {
                    tvChatTitle.setText(cached.username);
                } else {
                    tvChatTitle.setText("Caricamento...");
                    FirebaseFirestore.getInstance().collection("users").document(targetUser).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists() && isAdded()) {
                                    String name = doc.getString("username");
                                    tvChatTitle.setText(name != null ? name : "Utente");
                                }
                            });
                }
            } else {
                tvChatTitle.setText("Chat");
            }
        }

        tvChatTitle.setOnClickListener(v -> {
            if (isGroup) {
                Intent intent = new Intent(requireContext(), GroupSettingsActivity.class);
                intent.putExtra("group_id", targetUser);
                startActivity(intent);
            }
        });

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        messageList = new ArrayList<>();

        adapter = new ChatAdapter(requireContext(), messageList, currentUser, db, isGroup, targetUser, conversationId);
        lvMessages.setAdapter(adapter);

        dbHelper = new ChatDatabaseHelper(db, currentUser, targetUser, isGroup, conversationId, adapter, messageList, lvMessages);
        backgroundHelper = new ChatBackgroundHelper(requireContext(), db, currentUser, ivChatBackground, scegliSfondo);

        backgroundHelper.caricaSfondoDaFirestore();
        backgroundHelper.caricaSfondoSalvato();

        btnSendChat.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                dbHelper.sendMessage(text);
                etMessage.setText("");
            }
        });

        btnChangeBg.setOnClickListener(v -> backgroundHelper.mostraOpzioniSfondo());

        dbHelper.listenForMessages(this);
    }
}