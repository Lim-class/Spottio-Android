package com.example.spottio;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;

public class DebouncingTextWatcher implements TextWatcher {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnable;
    private final long delayMillis;
    private final SearchCallback callback;

    // Interfaccia per restituire il testo pronto dopo il ritardo
    public interface SearchCallback {
        void onSearch(String query);
    }

    public DebouncingTextWatcher(long delayMillis, SearchCallback callback) {
        this.delayMillis = delayMillis;
        this.callback = callback;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nessuna azione richiesta
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Rimuove la chiamata precedente se l'utente sta ancora digitando
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }

        String query = s.toString().trim();

        // Prepara la nuova esecuzione
        runnable = () -> callback.onSearch(query);

        // La esegue solo dopo il ritardo specificato
        handler.postDelayed(runnable, delayMillis);
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Nessuna azione richiesta
    }
}