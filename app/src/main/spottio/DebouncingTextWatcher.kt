package com.example.spottio

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher

class DebouncingTextWatcher(
    private val delayMillis: Long,
    private val callback: SearchCallback
) : TextWatcher {

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    // Interfaccia per restituire il testo pronto dopo il ritardo
    fun interface SearchCallback {
        fun onSearch(query: String)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // Nessuna azione richiesta
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // Rimuove la chiamata precedente se l'utente sta ancora digitando
        runnable?.let { handler.removeCallbacks(it) }

        val query = s?.toString()?.trim() ?: ""

        // Prepara la nuova esecuzione
        runnable = Runnable { callback.onSearch(query) }

        // La esegue solo dopo il ritardo specificato
        handler.postDelayed(runnable!!, delayMillis)
    }

    override fun afterTextChanged(s: Editable?) {
        // Nessuna azione richiesta
    }
}