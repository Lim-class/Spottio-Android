package com.example.spottio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MessageUsersFragment extends Fragment {

    private String currentUser;

    private ListView lvChats;
    private EditText etSearch;
    private ImageButton btnNewChat;

    private ChatPreviewAdapter adapter;
    private ArrayList<ChatPreview> masterList = new ArrayList<>();
    private ArrayList<ChatPreview> displayedList = new ArrayList<>();

    private MessagesDatabaseHelper dbHelper;
    private MessagesDialogHelper dialogHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = requireActivity().getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);

        // Usa l'UID per cercare le proprie chat
        currentUser = prefs.getString("uid_attivo", "");

        lvChats = view.findViewById(R.id.lvChats);
        etSearch = view.findViewById(R.id.etSearchUser);
        btnNewChat = view.findViewById(R.id.btnNewChat);

        adapter = new ChatPreviewAdapter(getContext(), displayedList);
        lvChats.setAdapter(adapter);

        dbHelper = new MessagesDatabaseHelper(db, currentUser, masterList, displayedList, adapter);
        dialogHelper = new MessagesDialogHelper(requireContext(), getParentFragmentManager(), dbHelper, currentUser);

        btnNewChat.setOnClickListener(v -> dialogHelper.mostraDialogoNuovaChat());

        lvChats.setOnItemClickListener((parent, v, position, id) -> {
            ChatPreview selected = displayedList.get(position);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ChatFragment.newInstance(selected.getTargetId(), selected.isGroup(), selected.getName()))
                    .addToBackStack(null)
                    .commit();
        });

        // UTILIZZO DELLA NUOVA CLASSE UTILITY (200ms di ritardo per non stressare l'UI)
        etSearch.addTextChangedListener(new DebouncingTextWatcher(200, query -> filterChats(query)));

        // Lettura iniziale da db
        dbHelper.loadUnifiedChats(() -> filterChats(etSearch.getText().toString()));

        return view;
    }

    private void filterChats(String query) {
        displayedList.clear();
        String lower = query.toLowerCase().trim();
        if (lower.isEmpty()) {
            displayedList.addAll(masterList);
        } else {
            for (ChatPreview preview : masterList) {
                if (preview.getName().toLowerCase().contains(lower)) {
                    displayedList.add(preview);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // KILL DEI FANTASMI: Stessa cosa qui per la lista delle anteprime!
        if (dbHelper != null) {
            // dbHelper.stopListening();
        }
    }
}