package com.example.spottio.utils

import android.R
import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.spottio.users.UserCache

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
            ivPfp?.setImageResource(R.drawable.sym_def_app_icon)
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
                    .placeholder(R.drawable.sym_def_app_icon)
                    .error(R.drawable.sym_def_app_icon)
                    .circleCrop()
                    .into(it)
            } else {
                it.setImageResource(R.drawable.sym_def_app_icon)
            }
        }
    }
}