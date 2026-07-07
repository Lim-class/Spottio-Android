package com.example.spottio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.spottio.UserCache.getUser
import de.hdodenhof.circleimageview.CircleImageView

class ChatPreviewAdapter(context: Context, previews: List<ChatPreview>) :
    ArrayAdapter<ChatPreview>(context, 0, previews) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // SOLUZIONE: Rinominiamo la variabile in "view" e usiamo l'operatore Elvis
        // per inflazionare il layout solo se "convertView" è null
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_chat_preview, parent, false)

        val preview = getItem(position) ?: return view

        val ivAvatar = view.findViewById<CircleImageView>(R.id.ivAvatar)
        val tvGroupBadge = view.findViewById<TextView>(R.id.tvGroupBadge)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val ivVerifiedBadge = view.findViewById<ImageView>(R.id.ivVerifiedBadge)
        val tvLastMessage = view.findViewById<TextView>(R.id.tvLastMessage)

        val lastMsg = preview.lastMessage
        tvLastMessage.text = if (!lastMsg.isNullOrEmpty()) lastMsg else "Nessun messaggio"

        if (preview.isGroup) {
            // GRUPPO: i dati sono già integrati nella preview
            tvGroupBadge.visibility = View.VISIBLE
            ivVerifiedBadge.visibility = View.GONE
            tvName.text = preview.name

            caricaAvatar(preview.profilePicUri, ivAvatar)
        } else {
            // CHAT PRIVATA: Avvia la Traduzione Dinamica UID -> Utente
            tvGroupBadge.visibility = View.GONE
            val otherUid = preview.name // Contiene l'UID passato dall'Helper

            if (otherUid != null) {
                val cached = getUser(otherUid)

                if (cached != null) {
                    // TROVATO IN CACHE LOCALE (Istantaneo)
                    tvName.text = cached.username
                    UserVerificationManager.setupVerificationBadge(ivVerifiedBadge, cached.isVerified)
                    caricaAvatar(cached.pfpUri, ivAvatar)
                } else {
                    // NON IN CACHE: Mostra solo l'interfaccia di caricamento.
                    tvName.text = "Caricamento..."
                    UserVerificationManager.setupVerificationBadge(ivVerifiedBadge, false)
                    ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            }
        }

        return view
    }

    /**
     * Metodo helper per rimuovere la duplicazione di codice di Glide
     */
    private fun caricaAvatar(url: String?, targetView: ImageView) {
        if (!url.isNullOrEmpty()) {
            Glide.with(context)
                .load(url)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .into(targetView)
        } else {
            targetView.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }
}