package com.example.spottio;

import android.view.View;
import android.widget.ImageView;

public class UserVerificationManager {

    /**
     * Imposta la visibilità del badge di verifica accanto al nome dell'utente.
     * Questo metodo può essere riutilizzato in Adapter, Fragment o Activity.
     *
     * @param ivVerifiedBadge L'ImageView del layout che rappresenta la spunta blu
     * @param isVerified Il valore booleano che indica se l'utente è verificato
     */
    public static void setupVerificationBadge(ImageView ivVerifiedBadge, boolean isVerified) {
        if (ivVerifiedBadge != null) {
            if (isVerified) {
                ivVerifiedBadge.setVisibility(View.VISIBLE);
            } else {
                ivVerifiedBadge.setVisibility(View.GONE);
            }
        }
    }
}

// Esempio d'uso in un qualsiasi altro ViewHolder o Fragment:
// UserVerificationManager.setupVerificationBadge(holder.ivVerifiedBadgeInChat, user.isVerified());