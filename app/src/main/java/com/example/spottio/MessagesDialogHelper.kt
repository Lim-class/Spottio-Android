package com.example.spottio

import android.content.Context
import android.graphics.Color
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class MessagesDialogHelper(
    private val context: Context,
    private val fragmentManager: FragmentManager,
    private val currentUser: String
) {
    private val db = FirebaseFirestore.getInstance()

    fun mostraDialogoNuovaChat() {
        // Recuperiamo gli utenti direttamente da Firestore
        db.collection("users").get().addOnSuccessListener { result ->
            val allUsers = mutableListOf<User>()
            for (doc in result) {
                if (doc.id != currentUser) {
                    val u = User()
                    u.uid = doc.id
                    u.username = doc.getString("username") ?: "Utente"
                    allUsers.add(u)
                }
            }

            val builder = AlertDialog.Builder(context)
            builder.setTitle("Nuova Chat")

            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
            }

            val btnCreateGroup = Button(context).apply {
                text = "👥 Crea Nuovo Gruppo"
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(Color.parseColor("#1DB954"))
            }
            layout.addView(btnCreateGroup)

            val displayNames = allUsers.map { it.username }.toTypedArray()

            val userListView = ListView(context)
            val userAdapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, displayNames)
            userListView.adapter = userAdapter
            layout.addView(userListView)

            builder.setView(layout)
            val dialog = builder.create()

            btnCreateGroup.setOnClickListener {
                dialog.dismiss()
                mostraDialogoCreazioneGruppo(allUsers)
            }

            userListView.setOnItemClickListener { _, _, position, _ ->
                dialog.dismiss()
                val selectedUid = allUsers[position].uid
                val selectedName = allUsers[position].username
                fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ChatFragment.newInstance(selectedUid, false, selectedName))
                    .addToBackStack(null)
                    .commit()
            }

            dialog.show()
        }
    }

    private fun mostraDialogoCreazioneGruppo(allUsers: List<User>) {
        val usersArray = allUsers.map { it.username }.toTypedArray()
        val checkedUsers = BooleanArray(usersArray.size)
        val selectedMemberUids = mutableListOf<String>()

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Nuovo Gruppo")

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val etGroupName = EditText(context).apply {
            hint = "Nome del gruppo..."
        }
        layout.addView(etGroupName)

        val btnSelectMembers = Button(context).apply {
            text = "Seleziona Membri (0)"
        }
        layout.addView(btnSelectMembers)

        btnSelectMembers.setOnClickListener {
            val membersBuilder = AlertDialog.Builder(context)
            membersBuilder.setTitle("Partecipanti")
            membersBuilder.setMultiChoiceItems(usersArray, checkedUsers) { _, which, isChecked ->
                checkedUsers[which] = isChecked
            }
            membersBuilder.setPositiveButton("OK") { _, _ ->
                selectedMemberUids.clear()
                for (i in checkedUsers.indices) {
                    if (checkedUsers[i]) {
                        selectedMemberUids.add(allUsers[i].uid)
                    }
                }
                btnSelectMembers.text = "Seleziona Membri (${selectedMemberUids.size})"
            }
            membersBuilder.show()
        }

        builder.setView(layout)
        builder.setPositiveButton("Crea") { _, _ ->
            val groupName = etGroupName.text.toString().trim()
            if (groupName.isEmpty()) return@setPositiveButton

            // Aggiungi anche te stesso al gruppo
            selectedMemberUids.add(currentUser)

            // Creiamo il gruppo direttamente su Firestore con un Batch
            val newGroupId = db.collection("groups").document().id
            val groupData = mapOf(
                "name" to groupName,
                "members" to selectedMemberUids,
                "createdBy" to currentUser
            )
            val previewData = mapOf(
                "isGroup" to true,
                "groupName" to groupName,
                "participants" to selectedMemberUids,
                "lastMessage" to "Gruppo creato",
                "lastUpdate" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(db.collection("groups").document(newGroupId), groupData)
            batch.set(db.collection("chat_previews").document(newGroupId), previewData)

            batch.commit().addOnSuccessListener {
                fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ChatFragment.newInstance(newGroupId, true, groupName))
                    .addToBackStack(null)
                    .commit()
            }
        }

        builder.setNegativeButton("Annulla", null)
        builder.show()
    }
}