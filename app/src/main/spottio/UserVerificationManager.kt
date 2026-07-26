package com.example.spottio

import android.view.View
import android.widget.ImageView

object UserVerificationManager {

    /**
     * Imposta la visibilità del badge di verifica accanto al nome dell'utente.
     * Questo metodo può essere riutilizzato in Adapter, Fragment o Activity.
     *
     * @param ivVerifiedBadge L'ImageView del layout che rappresenta la spunta blu
     * @param isVerified Il valore booleano che indica se l'utente è verificato
     */
    fun setupVerificationBadge(ivVerifiedBadge: ImageView?, isVerified: Boolean) {
        ivVerifiedBadge?.visibility = if (isVerified) View.VISIBLE else View.GONE
    }
}