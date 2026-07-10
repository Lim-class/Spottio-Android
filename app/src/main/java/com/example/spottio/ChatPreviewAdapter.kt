package com.example.spottio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView

class ChatPreviewAdapter(context: Context, previews: List<ChatPreview>) :
    ArrayAdapter<ChatPreview>(context, 0, previews) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
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

            // Essendo un Gruppo e non uno "User", carichiamo al volo con Glide
            if (!preview.profilePicUri.isNullOrEmpty()) {
                Glide.with(context)
                    .load(preview.profilePicUri)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .into(ivAvatar)
            } else {
                ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        } else {
            // CHAT PRIVATA: Affidiamo l'intero layout all'Helper Grafico
            tvGroupBadge.visibility = View.GONE
            val otherUid = preview.name // L'Helper ci passa l'UID al posto del nome

            if (otherUid != null) {
                val cachedUser = UserCache.getUser(otherUid)

                if (cachedUser != null) {
                    // TROVATO IN CACHE (Istantaneo)
                    UserGraphicsHelper.applicaGrafica(context, tvName, ivAvatar, ivVerifiedBadge, cachedUser)
                } else {
                    // NON IN CACHE: Helper imposta "Caricamento...", e noi andiamo a recuperare l'utente!
                    UserGraphicsHelper.applicaGrafica(context, tvName, ivAvatar, ivVerifiedBadge, null)
                    UserCache.fetchUserAsync(otherUid) { notifyDataSetChanged() }
                }
            }
        }

        return view
    }
}