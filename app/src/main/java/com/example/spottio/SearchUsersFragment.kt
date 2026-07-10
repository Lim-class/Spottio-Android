package com.example.spottio

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class SearchUsersFragment : BaseUserListFragment() {

    private var currentUserUid: String = "Guest"
    private lateinit var viewModel: SearchUsersViewModel

    override fun setupFragment() {
        val prefs = requireActivity().getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
        currentUserUid = prefs.getString("uid_attivo", "Guest") ?: "Guest"

        tvTitle.text = "Cerca Utenti"
        etSearch.visibility = View.VISIBLE

        // Inizializza il ViewModel usando la nostra Generic Factory!
        val factory = GenericViewModelFactory { SearchUsersViewModel(UserRepository()) }
        viewModel = ViewModelProvider(this, factory)[SearchUsersViewModel::class.java]

        // 1. Osserva i risultati e aggiorna la grafica
        viewModel.searchResults.observe(viewLifecycleOwner, Observer { uids ->
            displayedList.clear()
            displayedList.addAll(uids)
            adapter.notifyDataSetChanged()
        })

        // 2. Osserva gli eventuali errori e mostra un Toast
        viewModel.error.observe(viewLifecycleOwner, Observer { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        })

        // 3. Gestisce la digitazione passando il compito al ViewModel
        etSearch.addTextChangedListener(DebouncingTextWatcher(500) { query ->
            if (query.length < 4) {
                viewModel.clearResults()
            } else {
                viewModel.searchUsers(query, currentUserUid)
            }
        })
    }
}