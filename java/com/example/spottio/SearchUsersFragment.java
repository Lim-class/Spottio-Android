package com.example.spottio;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class SearchUsersFragment extends BaseUserListFragment {

    private String currentUserUid;

    @Override
    protected void setupFragment() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        currentUserUid = prefs.getString("uid_attivo", "Guest");

        if (tvTitle != null) tvTitle.setText("Cerca Utenti");
        etSearch.setVisibility(View.VISIBLE);

        // UTILIZZO DELLA NUOVA CLASSE UTILITY (500ms di ritardo)
        etSearch.addTextChangedListener(new DebouncingTextWatcher(500, query -> {
            if (query.length() < 4) {
                displayedList.clear();
                adapter.notifyDataSetChanged();
            } else {
                searchUsersInFirestore(query);
            }
        }));
    }

    private void searchUsersInFirestore(String query) {
        FirebaseFirestore.getInstance().collection("users")
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        displayedList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String uid = doc.getId(); // Estraggo l'UID dal database
                            String username = doc.getString("username");
                            String pfpUri = doc.getString("userPfUri");
                            Boolean isVerified = doc.getBoolean("isVerified");
                            if (isVerified == null) isVerified = false;

                            if (username != null && !uid.equals(currentUserUid)) {
                                UserCache.putUser(uid, username, pfpUri, isVerified);
                                displayedList.add(uid); // Aggiungo l'UID alla lista grafica
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Errore ricerca utenti", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}