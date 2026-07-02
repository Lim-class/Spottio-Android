package com.example.spottio;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;

public class UserGraphicsHelper {

    /**
     * Applica Nome, Foto Profilo e Spunta Verificata agli elementi della UI in un colpo solo.
     * Se userData è null, imposta lo stato di "Caricamento...".
     */
    public static void applicaGrafica(Context context, TextView tvName, ImageView ivPfp, ImageView ivBadge, UserCache.UserData userData) {
        if (userData == null) {
            // Dati non ancora in cache: Mostra placeholder
            if (tvName != null) tvName.setText("Caricamento...");
            if (ivPfp != null) ivPfp.setImageResource(android.R.drawable.sym_def_app_icon);
            UserVerificationManager.setupVerificationBadge(ivBadge, false);
            return;
        }

        // Dati presenti: Applica nome, badge e scarica foto profilo
        if (tvName != null) tvName.setText(userData.username != null ? userData.username : "Utente");
        UserVerificationManager.setupVerificationBadge(ivBadge, userData.isVerified);

        if (ivPfp != null) {
            if (userData.pfpUri != null && !userData.pfpUri.isEmpty()) {
                Glide.with(context)
                        .load(userData.pfpUri)
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .error(android.R.drawable.sym_def_app_icon)
                        .circleCrop()
                        .into(ivPfp);
            } else {
                ivPfp.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        }
    }
}