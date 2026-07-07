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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)

        val uidOrUsername = getItem(position) ?: return view

        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val ivUserImage: ImageView = view.findViewById(R.id.ivUserImage)
        val ivVerifiedBadge: ImageView = view.findViewById(R.id.ivVerifiedBadge)

        val cachedUser = UserCache.getUser(uidOrUsername)

        if (cachedUser != null) {
            UserGraphicsHelper.applicaGrafica(context, tvUserName, ivUserImage, ivVerifiedBadge, cachedUser)
        } else {
            UserGraphicsHelper.applicaGrafica(context, tvUserName, ivUserImage, ivVerifiedBadge, null)
            UserCache.fetchUserAsync(uidOrUsername) { notifyDataSetChanged() }
        }

        // Il click è gestito in modo sicuro con "listener?"
        view.setOnClickListener {
            listener?.onUserClick(uidOrUsername)
        }

        return view
    }
}