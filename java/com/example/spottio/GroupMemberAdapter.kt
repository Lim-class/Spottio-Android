package com.example.spottio

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView

class GroupMemberAdapter(
    context: Context,
    memberIds: List<String>,
    private val currentUser: String,
    // Rimosso "private val db: FirebaseFirestore" dal costruttore!
    private val groupId: String,
    private val listener: GroupMemberListener?
) : ArrayAdapter<String>(context, 0, memberIds) {

    interface GroupMemberListener {
        fun onRemoveMemberRequest(memberId: String)
        fun onMemberClick(memberId: String)
    }

    private var adminId: String? = null
    private var isCurrentUserAdmin = false

    fun setAdminId(adminId: String?) {
        this.adminId = adminId
    }

    fun setCurrentUserIsAdmin(isAdmin: Boolean) {
        this.isCurrentUserAdmin = isAdmin
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_group_member, parent, false)

        val memberId = getItem(position) ?: return view

        val ivAvatar = view.findViewById<CircleImageView>(R.id.ivMemberAvatar)
        val tvName = view.findViewById<TextView>(R.id.tvMemberName)
        val tvRole = view.findViewById<TextView>(R.id.tvMemberRole)

        // Click standard per vedere il profilo
        view.setOnClickListener {
            listener?.onMemberClick(memberId)
        }

        if (isCurrentUserAdmin && memberId != adminId) {
            view.setOnLongClickListener {
                listener?.onRemoveMemberRequest(memberId)
                true
            }
            tvRole.text = "Tieni premuto per rimuovere"
            tvRole.setTextColor(Color.parseColor("#FF0000"))
        } else if (memberId == adminId) {
            tvRole.text = "Amministratore"
            tvRole.setTextColor(Color.parseColor("#1DB954"))
            view.setOnLongClickListener(null)
        } else {
            tvRole.text = "Partecipante"
            tvRole.setTextColor(Color.parseColor("#888888"))
            view.setOnLongClickListener(null)
        }

        // REFACTORING: Lettura intelligente da Cache
        val cachedUser = UserCache.getUser(memberId)

        if (cachedUser != null) {
            // Applica Grafica automaticamente. (Passiamo null per il badge visto che non c'è)
            UserGraphicsHelper.applicaGrafica(context, tvName, ivAvatar, null, cachedUser)
        } else {
            // Mostra interfaccia di caricamento e recupera l'utente da Firebase in background
            UserGraphicsHelper.applicaGrafica(context, tvName, ivAvatar, null, null)
            UserCache.fetchUserAsync(memberId) { notifyDataSetChanged() }
        }

        return view
    }
}