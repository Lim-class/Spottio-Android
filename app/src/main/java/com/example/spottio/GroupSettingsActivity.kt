package com.example.spottio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class GroupSettingsActivity : AppCompatActivity() {

    private var currentUser: String? = null
    private var groupId: String? = null
    private var groupAdminId: String? = null
    private var isAdmin = false

    private lateinit var ivGroupIcon: ImageView
    private lateinit var tvGroupName: TextView
    private lateinit var tvGroupDesc: TextView
    private lateinit var tvEditIconHint: TextView
    private lateinit var btnEditGroupName: ImageButton
    private lateinit var btnEditGroupDesc: ImageButton
    private lateinit var lvGroupMembers: ListView
    private lateinit var btnAddMember: Button
    private lateinit var btnLeaveGroup: Button

    private lateinit var db: FirebaseFirestore
    private lateinit var memberAdapter: GroupMemberAdapter
    private val memberIds = mutableListOf<String>()

    private val pickGroupIcon = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadGroupIcon(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_settings)

        val prefs = getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE)
        currentUser = prefs.getString("username_attivo", null)

        groupId = intent.getStringExtra("group_id")
        val passedGroupName = intent.getStringExtra("group_name")

        if (groupId == null || currentUser == null) {
            finish()
            return
        }

        db = FirebaseFirestore.getInstance()

        ivGroupIcon = findViewById(R.id.ivGroupIcon)
        tvGroupName = findViewById(R.id.tvGroupName)
        tvGroupDesc = findViewById(R.id.tvGroupDesc)
        tvEditIconHint = findViewById(R.id.tvEditIconHint)
        btnEditGroupName = findViewById(R.id.btnEditGroupName)
        btnEditGroupDesc = findViewById(R.id.btnEditGroupDesc)
        lvGroupMembers = findViewById(R.id.lvGroupMembers)
        btnAddMember = findViewById(R.id.btnAddMember)
        btnLeaveGroup = findViewById(R.id.btnLeaveGroup)

        if (!passedGroupName.isNullOrEmpty()) {
            tvGroupName.text = passedGroupName
        }

        // --- CREAZIONE DEL LISTENER PER L'ADAPTER ---
        val listener = object : GroupMemberAdapter.GroupMemberListener {
            override fun onRemoveMemberRequest(memberId: String) {
                AlertDialog.Builder(this@GroupSettingsActivity)
                    .setTitle("Rimuovi partecipante")
                    .setMessage("Vuoi rimuovere $memberId dal gruppo?")
                    .setPositiveButton("Rimuovi") { _, _ -> removeMemberFromGroup(memberId) }
                    .setNegativeButton("Annulla", null)
                    .show()
            }

            override fun onMemberClick(memberId: String) {
                // Opzionale
            }
        }

        memberAdapter = GroupMemberAdapter(this, memberIds, currentUser!!, db, groupId!!, listener)
        lvGroupMembers.adapter = memberAdapter

        loadGroupData()

        btnEditGroupName.setOnClickListener { showEditDialog("Nome", "name", tvGroupName.text.toString()) }
        btnEditGroupDesc.setOnClickListener { showEditDialog("Descrizione", "groupDescription", tvGroupDesc.text.toString()) }
        ivGroupIcon.setOnClickListener { pickGroupIcon.launch("image/*") }

        btnAddMember.setOnClickListener { showAddMemberDialog() }
        btnLeaveGroup.setOnClickListener { leaveGroup() }
    }

    private fun loadGroupData() {
        db.collection("groups").document(groupId!!).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) {
                checkAdminPermissions()
                return@addSnapshotListener
            }

            val name = snapshot.getString("name")
            val desc = snapshot.getString("groupDescription")
            val iconUri = snapshot.getString("groupIconUri")
            groupAdminId = snapshot.getString("createdBy")

            @Suppress("UNCHECKED_CAST")
            val members = snapshot.get("members") as? List<String>

            if (!name.isNullOrEmpty()) tvGroupName.text = name

            if (!desc.isNullOrEmpty()) {
                tvGroupDesc.text = desc
            } else {
                tvGroupDesc.text = "Nessuna descrizione"
            }

            if (!iconUri.isNullOrEmpty()) {
                Glide.with(this).load(iconUri).into(ivGroupIcon)
            }

            if (members != null) {
                memberIds.clear()
                memberIds.addAll(members)
                memberAdapter.setAdminId(groupAdminId)
                memberAdapter.notifyDataSetChanged()
            }

            checkAdminPermissions()
        }
    }

    private fun checkAdminPermissions() {
        isAdmin = if (groupAdminId == null) {
            true
        } else {
            currentUser == groupAdminId
        }

        btnEditGroupName.visibility = View.VISIBLE
        btnEditGroupDesc.visibility = View.VISIBLE
        tvEditIconHint.visibility = View.VISIBLE

        btnAddMember.visibility = if (isAdmin) View.VISIBLE else View.GONE
        memberAdapter.setCurrentUserIsAdmin(isAdmin)
    }

    private fun showEditDialog(titolo: String, fieldName: String, testoAttuale: String) {
        val input = EditText(this)
        input.setText(if (testoAttuale == "Nessuna descrizione") "" else testoAttuale)
        input.setSelection(input.text.length)

        AlertDialog.Builder(this)
            .setTitle("Modifica $titolo")
            .setView(input)
            .setPositiveButton("Salva") { _, _ ->
                val newText = input.text.toString().trim()

                val data = hashMapOf<String, Any>(fieldName to newText)
                db.collection("groups").document(groupId!!).set(data, SetOptions.merge())

                if (fieldName == "name") {
                    val previewData = hashMapOf<String, Any>("name" to newText)
                    db.collection("chat_previews").document(groupId!!).set(previewData, SetOptions.merge())
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun showAddMemberDialog() {
        val input = EditText(this)
        input.hint = "Es: MarioRossi"
        input.inputType = InputType.TYPE_CLASS_TEXT

        AlertDialog.Builder(this)
            .setTitle("Aggiungi Partecipante")
            .setMessage("Inserisci l'username esatto dell'utente:")
            .setView(input)
            .setPositiveButton("Aggiungi") { _, _ ->
                val username = input.text.toString().trim()
                if (username.isNotEmpty() && !memberIds.contains(username)) {
                    verificaEAggiungiUtente(username)
                } else if (memberIds.contains(username)) {
                    Toast.makeText(this, "L'utente è già nel gruppo!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun verificaEAggiungiUtente(username: String) {
        db.collection("users").document(username).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                db.collection("groups").document(groupId!!).update("members", FieldValue.arrayUnion(username))
                    .addOnSuccessListener { Toast.makeText(this, "$username aggiunto al gruppo!", Toast.LENGTH_SHORT).show() }
            } else {
                Toast.makeText(this, "Utente non trovato.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun leaveGroup() {
        AlertDialog.Builder(this)
            .setTitle("Abbandona Gruppo")
            .setMessage("Sei sicuro di voler uscire da questo gruppo?")
            .setPositiveButton("Esci") { _, _ ->
                if (isAdmin && memberIds.size > 1) {
                    val nuovoAdmin = if (memberIds[0] == currentUser) memberIds[1] else memberIds[0]

                    db.collection("groups").document(groupId!!).update(
                        "members", FieldValue.arrayRemove(currentUser),
                        "createdBy", nuovoAdmin
                    ).addOnSuccessListener { chiudiETornaAllaHome() }
                } else {
                    db.collection("groups").document(groupId!!).update("members", FieldValue.arrayRemove(currentUser))
                        .addOnSuccessListener { chiudiETornaAllaHome() }
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun chiudiETornaAllaHome() {
        Toast.makeText(this, "Hai abbandonato il gruppo", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        finish()
    }

    fun removeMemberFromGroup(memberId: String) {
        db.collection("groups").document(groupId!!)
            .update("members", FieldValue.arrayRemove(memberId))
            .addOnSuccessListener { Toast.makeText(this, "Membro rimosso", Toast.LENGTH_SHORT).show() }
    }

    private fun uploadGroupIcon(uri: Uri) {
        Toast.makeText(this, "Aggiornamento icona...", Toast.LENGTH_SHORT).show()
        val uriString = uri.toString()

        val iconData = hashMapOf<String, Any>("groupIconUri" to uriString)

        db.collection("groups").document(groupId!!).set(iconData, SetOptions.merge())
            .addOnSuccessListener {
                val previewData = hashMapOf<String, Any>("profilePicUri" to uriString)
                db.collection("chat_previews").document(groupId!!).set(previewData, SetOptions.merge())
                Toast.makeText(this, "Icona aggiornata", Toast.LENGTH_SHORT).show()
            }
    }
}