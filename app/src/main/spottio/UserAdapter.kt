package com.example.spottio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

class UserAdapter(context: Context, users: List<String>) : ArrayAdapter<String>(context, 0, users) {

    interface OnUserClickListener {
        fun onUserClick(uidOrUsername: String)
    }

    private var listener: OnUserClickListener? = null

    fun setOnUserClickListener(listener: OnUserClickListener) {
        this.listener = listener
    }

    // Definiamo il ViewHolder aggiornato per la riga utente
    private class ViewHolder(view: View) {
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val ivUserImage: ImageView = view.findViewById(R.id.ivUserAvatar) // ID aggiornato!
        val ivVerifiedBadge: ImageView = view.findViewById(R.id.ivVerifiedBadge)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        // Pattern di riciclo
        if (convertView == null) {
            // Adesso gonfiamo il nuovo layout unificato
            view = LayoutInflater.from(context).inflate(R.layout.item_user_row, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val uidOrUsername = getItem(position) ?: return view

        // Usiamo UserCache ed evochiamo l'helper passando le viste dal ViewHolder
        val cachedUser = UserCache.getUser(uidOrUsername)

        if (cachedUser != null) {
            UserGraphicsHelper.applicaGrafica(context, viewHolder.tvUserName, viewHolder.ivUserImage, viewHolder.ivVerifiedBadge, cachedUser)
        } else {
            UserGraphicsHelper.applicaGrafica(context, viewHolder.tvUserName, viewHolder.ivUserImage, viewHolder.ivVerifiedBadge, null)
            UserCache.fetchUserAsync(uidOrUsername) { notifyDataSetChanged() }
        }

        view.setOnClickListener {
            listener?.onUserClick(uidOrUsername)
        }

        return view
    }
}