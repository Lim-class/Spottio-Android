package com.example.spottio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Una Factory generica che accetta un blocco lambda per istanziare qualunque ViewModel.
 * Sostituisce tutte le factory specifiche del progetto riducendo il codice boilerplate.
 */
class GenericViewModelFactory<VM : ViewModel>(
    private val creator: () -> VM
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return creator() as T
    }
}