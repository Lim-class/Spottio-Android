package com.example.spottio;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatAdapter extends ArrayAdapter<ChatMessage> {
    private final Set<String> hiddenMessages;
    private final SimpleDateFormat timeFormat;
    private final ChatActionHelper actionHelper;

    public ChatAdapter(Context context, List<ChatMessage> messages, String currentUser, FirebaseFirestore db, boolean isGroup, String targetUser, String conversationId) {
        super(context, 0, messages);
        // NOTA: il parametro 'db' qui serve ormai solo per passarlo a ChatActionHelper
        SharedPreferences prefs = context.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        this.hiddenMessages = prefs.getStringSet("hidden_messages_" + currentUser, new HashSet<>());
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        this.actionHelper = new ChatActionHelper(
                context, db, currentUser, isGroup, targetUser, conversationId, hiddenMessages, this::notifyDataSetChanged
        );
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_message, parent, false);
        }

        ChatMessage msg = getItem(position);
        if (msg == null) return convertView;

        View containerSent = convertView.findViewById(R.id.containerSent);
        View layoutSent = convertView.findViewById(R.id.layoutSent);
        CircleImageView ivMessageAvatarSent = convertView.findViewById(R.id.ivMessageAvatarSent);
        TextView tvMsgOut = convertView.findViewById(R.id.tvMessageOut);
        TextView tvTimeOut = convertView.findViewById(R.id.tvTimeOut);

        View containerReceived = convertView.findViewById(R.id.containerReceived);
        View layoutReceived = convertView.findViewById(R.id.layoutReceived);
        CircleImageView ivMessageAvatar = convertView.findViewById(R.id.ivMessageAvatar);
        TextView tvMsgIn = convertView.findViewById(R.id.tvMessageIn);
        TextView tvTimeIn = convertView.findViewById(R.id.tvTimeIn);
        TextView tvSenderName = convertView.findViewById(R.id.tvSenderName);
        ImageView ivVerifiedBadgeSender = convertView.findViewById(R.id.ivVerifiedBadgeSender);
        TextView tvDateDivider = convertView.findViewById(R.id.tvDateDivider);

        // 1. GESTIONE MESSAGGI NASCOSTI
        if (hiddenMessages.contains(msg.docId)) {
            if (containerSent != null) containerSent.setVisibility(View.GONE);
            if (containerReceived != null) containerReceived.setVisibility(View.GONE);
            if (tvDateDivider != null) tvDateDivider.setVisibility(View.GONE);
            return convertView;
        }

        // 2. GESTIONE DATA DIVISORE
        if (tvDateDivider != null) {
            if (msg.mostraData) {
                tvDateDivider.setVisibility(View.VISIBLE);
                tvDateDivider.setText(msg.testoData);
            } else {
                tvDateDivider.setVisibility(View.GONE);
            }
        }

        String timeText = timeFormat.format(msg.timestamp);

        // 3. RECUPERO DATI UTENTE DALLA CACHE (SENZA CHIAMATE DI RETE!)
        String senderUid = msg.senderName;
        String realUsername = "Caricamento..."; // Testo di default finché il DatabaseHelper non lo scarica
        String realPfpUrl = "";
        boolean realVerified = false;

        if (senderUid != null && !senderUid.equals("Sistema")) {
            UserCache.UserData cached = UserCache.getUser(senderUid);

            if (cached != null) {
                realUsername = cached.username;
                realPfpUrl = cached.pfpUri;
                realVerified = cached.isVerified;
            }
            // FINE. Non c'è più il blocco 'else' con la chiamata a Firestore!
            // Ci pensa già il ChatDatabaseHelper in background a fare fetch e aggiornare l'adapter.
        }

        // 4. GESTIONE GRAFICA: MESSAGGIO INVIATO (MIO)
        if (msg.isMe) {
            if (containerSent != null) containerSent.setVisibility(View.VISIBLE);
            if (containerReceived != null) containerReceived.setVisibility(View.GONE);

            if (tvMsgOut != null) tvMsgOut.setText(msg.text);
            if (tvTimeOut != null) tvTimeOut.setText(timeText);

            if (ivMessageAvatarSent != null) {
                caricaAvatar(realPfpUrl, ivMessageAvatarSent);
            }

            if (layoutSent != null) {
                layoutSent.setOnLongClickListener(v -> {
                    actionHelper.mostraDialogoOpzioni(msg);
                    return true;
                });
            }

        }
        // 5. GESTIONE GRAFICA: MESSAGGIO RICEVUTO (ALTRI)
        else {
            if (containerReceived != null) containerReceived.setVisibility(View.VISIBLE);
            if (containerSent != null) containerSent.setVisibility(View.GONE);

            if (tvMsgIn != null) tvMsgIn.setText(msg.text);
            if (tvTimeIn != null) tvTimeIn.setText(timeText);

            if (ivMessageAvatar != null) {
                caricaAvatar(realPfpUrl, ivMessageAvatar);
            }

            if (msg.isGroup) {
                if (tvSenderName != null) {
                    tvSenderName.setVisibility(View.VISIBLE);
                    tvSenderName.setText(realUsername);
                }
                if (ivVerifiedBadgeSender != null) {
                    UserVerificationManager.setupVerificationBadge(ivVerifiedBadgeSender, realVerified);
                }
            } else {
                if (tvSenderName != null) tvSenderName.setVisibility(View.GONE);
                if (ivVerifiedBadgeSender != null) ivVerifiedBadgeSender.setVisibility(View.GONE);
            }

            if (layoutReceived != null) {
                layoutReceived.setOnLongClickListener(v -> {
                    actionHelper.mostraDialogoElimina(msg.docId, false);
                    return true;
                });
            }
        }

        return convertView;
    }

    /**
     * Metodo helper per rimuovere la duplicazione di codice di Glide
     */
    private void caricaAvatar(String url, ImageView targetView) {
        if (url != null && !url.isEmpty()) {
            Glide.with(getContext()).load(url)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .circleCrop()
                    .into(targetView);
        } else {
            targetView.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }
}