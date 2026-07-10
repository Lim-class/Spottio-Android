package com.example.spottio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchUsersViewModel(private val repository: UserRepository) : ViewModel() {

    // Lista degli UID trovati
    private val _searchResults = MutableLiveData<List<String>>()
    val searchResults: LiveData<List<String>> get() = _searchResults

    // Eventuali messaggi di errore
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun searchUsers(query: String, currentUserUid: String) {
        if (query.length < 4) {
            _searchResults.value = emptyList()
            return
        }

        repository.searchUsers(
            query = query,
            currentUserUid = currentUserUid,
            onSuccess = { uids ->
                _searchResults.value = uids
            },
            onError = { e ->
                _error.value = "Errore ricerca utenti: ${e.message}"
            }
        )
    }

    fun clearResults() {
        _searchResults.value = emptyList()
    }
}