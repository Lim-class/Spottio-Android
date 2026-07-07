package com.example.spottio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(
    context: Context,
    private val items: List<ChatItem>,
    private val viewModel: ChatViewModel // Passiamo il ViewModel per le azioni
) : ArrayAdapter<ChatItem>(context, 0, items) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // L'Adapter gestisce due layout diversi nello stesso file XML
    override fun getViewTypeCount(): Int = 2
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ChatItem.DateHeader -> 0
            is ChatItem.Message -> 1
            else -> -1
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)

        // 1. GESTIONE ETICHETTA DATA
        if (item is ChatItem.DateHeader) {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)

            // Nascondiamo i contenitori dei messaggi
            view.findViewById<View>(R.id.containerSent)?.visibility = View.GONE
            view.findViewById<View>(R.id.containerReceived)?.visibility = View.GONE

            val tvDateDivider = view.findViewById<TextView>(R.id.tvDateDivider)
            tvDateDivider?.visibility = View.VISIBLE
            tvDateDivider?.text = item.dateText
            return view
        }

        // 2. GESTIONE BUBBLE MESSAGGI
        if (item is ChatItem.Message) {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)
            val msg = item.chatMessage

            val tvDateDivider = view.findViewById<TextView>(R.id.tvDateDivider)
            tvDateDivider?.visibility = View.GONE // Gestito dal DateHeader!

            val containerSent = view.findViewById<View>(R.id.containerSent)
            val layoutSent = view.findViewById<View>(R.id.layoutSent)
            val ivMessageAvatarSent = view.findViewById<CircleImageView>(R.id.ivMessageAvatarSent)
            val tvMsgOut = view.findViewById<TextView>(R.id.tvMessageOut)
            val tvTimeOut = view.findViewById<TextView>(R.id.tvTimeOut)

            val containerReceived = view.findViewById<View>(R.id.containerReceived)
            val layoutReceived = view.findViewById<View>(R.id.layoutReceived)
            val ivMessageAvatar = view.findViewById<CircleImageView>(R.id.ivMessageAvatar)
            val tvMsgIn = view.findViewById<TextView>(R.id.tvMessageIn)
            val tvTimeIn = view.findViewById<TextView>(R.id.tvTimeIn)
            val tvSenderName = view.findViewById<TextView>(R.id.tvSenderName)
            val ivVerifiedBadgeSender = view.findViewById<ImageView>(R.id.ivVerifiedBadgeSender)

            val timeText = timeFormat.format(msg.timestamp)

            val cachedUser = if (msg.senderName.isNotEmpty() && msg.senderName != "Sistema") {
                UserCache.getUser(msg.senderName)
            } else null

            val realUsername = cachedUser?.username ?: "Caricamento..."
            val realPfpUrl = cachedUser?.pfpUri ?: msg.profilePicUri
            val realVerified = cachedUser?.isVerified ?: msg.isVerified

            if (msg.isMe) {
                containerSent?.visibility = View.VISIBLE
                containerReceived?.visibility = View.GONE

                tvMsgOut?.text = msg.text
                tvTimeOut?.text = timeText
                ivMessageAvatarSent?.let { caricaAvatar(realPfpUrl, it) }

                layoutSent?.setOnLongClickListener {
                    mostraDialogoOpzioni(msg)
                    true
                }
            } else {
                containerReceived?.visibility = View.VISIBLE
                containerSent?.visibility = View.GONE

                tvMsgIn?.text = msg.text
                tvTimeIn?.text = timeText
                ivMessageAvatar?.let { caricaAvatar(realPfpUrl, it) }

                if (msg.isGroup) {
                    tvSenderName?.apply {
                        visibility = View.VISIBLE
                        text = realUsername
                    }
                    ivVerifiedBadgeSender?.let {
                        UserVerificationManager.setupVerificationBadge(it, realVerified)
                    }
                } else {
                    tvSenderName?.visibility = View.GONE
                    ivVerifiedBadgeSender?.visibility = View.GONE
                }

                layoutReceived?.setOnLongClickListener {
                    mostraDialogoElimina(msg.docId, false)
                    true
                }
            }
            return view
        }
        return super.getView(position, convertView, parent)
    }

    private fun caricaAvatar(url: String?, targetView: ImageView) {
        if (!url.isNullOrEmpty()) {
            Glide.with(context).load(url)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .circleCrop()
                .into(targetView)
        } else {
            targetView.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    private fun mostraDialogoOpzioni(msg: ChatMessage) {
        val opzioni = arrayOf("Modifica", "Elimina per tutti")
        AlertDialog.Builder(context)
            .setTitle("Opzioni messaggio")
            .setItems(opzioni) { _, which ->
                if (which == 0) mostraDialogoModifica(msg)
                else mostraDialogoElimina(msg.docId, true)
            }.show()
    }

    private fun mostraDialogoModifica(msg: ChatMessage) {
        val input = EditText(context)
        input.setText(msg.text)
        input.setSelection(msg.text.length)

        AlertDialog.Builder(context)
            .setTitle("Modifica messaggio")
            .setView(input)
            .setPositiveButton("Salva") { _, _ ->
                val nuovoTesto = input.text.toString().trim()
                if (nuovoTesto.isNotEmpty() && nuovoTesto != msg.text) {
                    viewModel.editMessage(msg.docId, nuovoTesto)
                    Toast.makeText(context, "Messaggio modificato", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun mostraDialogoElimina(docId: String, deleteForEveryone: Boolean) {
        val titolo = if (deleteForEveryone) "Elimina per tutti" else "Elimina per te"
        val messaggio = if (deleteForEveryone) {
            "Questo messaggio sparirà anche per l'altra persona."
        } else {
            "Questo messaggio non sarà più visibile sul tuo telefono."
        }

        AlertDialog.Builder(context)
            .setTitle(titolo)
            .setMessage(messaggio)
            .setPositiveButton("Elimina") { _, _ ->
                viewModel.deleteMessage(docId, deleteForEveryone)
            }
            .setNegativeButton("Annulla", null)
            .show()
    }
}