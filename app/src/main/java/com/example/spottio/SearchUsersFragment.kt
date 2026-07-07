package com.example.spottio

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

class SearchUsersFragment : BaseUserListFragment() {

    private var currentUserUid: String = "Guest"

    override fun setupFragment() {
        val prefs = requireActivity().getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
        currentUserUid = prefs.getString("uid_attivo", "Guest") ?: "Guest"

        tvTitle.text = "Cerca Utenti"
        etSearch.visibility = View.VISIBLE

        // UTILIZZO DELLA CLASSE UTILITY (500ms di ritardo) con sintassi Kotlin
        etSearch.addTextChangedListener(DebouncingTextWatcher(500) { query ->
            if (query.length < 4) {
                displayedList.clear()
                adapter.notifyDataSetChanged()
            } else {
                searchUsersInFirestore(query)
            }
        })
    }

    private fun searchUsersInFirestore(query: String) {
        FirebaseFirestore.getInstance().collection("users")
            .orderBy("username")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(10)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    displayedList.clear()

                    for (doc in task.result!!) {
                        val uid = doc.id // Estraggo l'UID dal database
                        val username = doc.getString("username")
                        val pfpUri: String = doc.getString("userPfUri") ?: "default_uri"

                        // Operatore Elvis: se isVerified è null, diventa automaticamente false
                        val isVerified = doc.getBoolean("isVerified") ?: false

                        if (username != null && uid != currentUserUid) {
                            UserCache.putUser(uid, username, pfpUri, isVerified)
                            displayedList.add(uid) // Aggiungo l'UID alla lista grafica
                        }
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(context, "Errore ricerca utenti", Toast.LENGTH_SHORT).show()
                }
            }
    }
}