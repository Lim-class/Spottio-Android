package com.example.spottio

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class GroupMemberAdapter(
    context: Context,
    memberIds: List<String>,
    private val currentUser: String,
    private val db: FirebaseFirestore,
    private val groupId: String,
    private val listener: GroupMemberListener?
) : ArrayAdapter<String>(context, 0, memberIds) {

    // 1. DEFINIAMO L'INTERFACCIA (Il Citofono)
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

        tvName.text = memberId

        // Click standard per vedere il profilo
        view.setOnClickListener {
            listener?.onMemberClick(memberId)
        }

        if (isCurrentUserAdmin && memberId != adminId) {
            // Avvisiamo solo la schermata
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

        db.collection("users").document(memberId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val uri = doc.getString("userPfUri")
                if (!uri.isNullOrEmpty()) {
                    Glide.with(context).load(uri).into(ivAvatar)
                } else {
                    ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
                }
            }
        }

        return view
    }
}