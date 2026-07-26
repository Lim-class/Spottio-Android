package com.example.spottio

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

object UserGraphicsHelper {

    /**
     * Applica Nome, Foto Profilo e Spunta Verificata agli elementi della UI in un colpo solo.
     * Se userData è null, imposta lo stato di "Caricamento...".
     */
    fun applicaGrafica(
        context: Context,
        tvName: TextView?,
        ivPfp: ImageView?,
        ivBadge: ImageView?,
        userData: UserCache.UserData?
    ) {
        if (userData == null) {
            // Dati non ancora in cache: Mostra placeholder
            tvName?.text = "Caricamento..."
            ivPfp?.setImageResource(android.R.drawable.sym_def_app_icon)
            UserVerificationManager.setupVerificationBadge(ivBadge, false)
            return
        }

        // SOLUZIONE: userData.username è già non-nullabile, rimosso "?: 'Utente'"
        tvName?.text = userData.username
        UserVerificationManager.setupVerificationBadge(ivBadge, userData.isVerified)

        ivPfp?.let {
            if (userData.pfpUri.isNotEmpty()) {
                Glide.with(context)
                    .load(userData.pfpUri)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .circleCrop()
                    .into(it)
            } else {
                it.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }
    }
}