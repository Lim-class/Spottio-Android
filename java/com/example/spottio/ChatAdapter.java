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
    private final FirebaseFirestore db;

    public ChatAdapter(Context context, List<ChatMessage> messages, String currentUser, FirebaseFirestore db, boolean isGroup, String targetUser, String conversationId) {
        super(context, 0, messages);
        this.db = db;
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

        // Nascondi messaggi eliminati localmente
        if (hiddenMessages.contains(msg.docId)) {
            if (containerSent != null) containerSent.setVisibility(View.GONE);
            if (containerReceived != null) containerReceived.setVisibility(View.GONE);
            if (tvDateDivider != null) tvDateDivider.setVisibility(View.GONE);
            return convertView;
        }

        // Data divisore
        if (tvDateDivider != null) {
            if (msg.mostraData) {
                tvDateDivider.setVisibility(View.VISIBLE);
                tvDateDivider.setText(msg.testoData);
            } else {
                tvDateDivider.setVisibility(View.GONE);
            }
        }

        String timeText = timeFormat.format(msg.timestamp);

        // TRADUZIONE DINAMICA: UID -> Dati Grafici Reali
        // Ricorda: Il ChatDatabaseHelper ci sta passando l'UID dentro al campo msg.senderName
        String senderUid = msg.senderName;
        String realUsername = "Utente";
        String realPfpUrl = "";
        boolean realVerified = false;

        if (senderUid != null && !senderUid.equals("Sistema")) {
            UserCache.UserData cached = UserCache.getUser(senderUid);

            if (cached != null) {
                realUsername = cached.username;
                realPfpUrl = cached.pfpUri;
                realVerified = cached.isVerified;
            } else {
                // Se non c'è in cache, scarica al volo e poi ricarica la lista!
                UserCache.putUser(senderUid, "Caricamento...", "", false); // Previene chiamate multiple infinite

                db.collection("users").document(senderUid).get().addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String name = userDoc.getString("username");
                        String pfp = userDoc.getString("userPfUri");
                        Boolean verObj = userDoc.getBoolean("isVerified");
                        boolean ver = verObj != null && verObj;

                        UserCache.putUser(senderUid, name != null ? name : "Sconosciuto", pfp, ver);
                        notifyDataSetChanged(); // Fai riaggiornare la grafica al volo
                    }
                });
            }
        }

        // --- GESTIONE GRAFICA: MESSAGGIO INVIATO (MIO) ---
        if (msg.isMe) {
            if (containerSent != null) containerSent.setVisibility(View.VISIBLE);
            if (containerReceived != null) containerReceived.setVisibility(View.GONE);

            if (tvMsgOut != null) tvMsgOut.setText(msg.text);
            if (tvTimeOut != null) tvTimeOut.setText(timeText);

            if (ivMessageAvatarSent != null) {
                if (realPfpUrl != null && !realPfpUrl.isEmpty()) {
                    Glide.with(getContext()).load(realPfpUrl)
                            .placeholder(android.R.drawable.sym_def_app_icon).circleCrop().into(ivMessageAvatarSent);
                } else {
                    ivMessageAvatarSent.setImageResource(android.R.drawable.sym_def_app_icon);
                }
            }

            if (layoutSent != null) {
                layoutSent.setOnLongClickListener(v -> {
                    actionHelper.mostraDialogoOpzioni(msg);
                    return true;
                });
            }

        }
        // --- GESTIONE GRAFICA: MESSAGGIO RICEVUTO (ALTRI) ---
        else {
            if (containerReceived != null) containerReceived.setVisibility(View.VISIBLE);
            if (containerSent != null) containerSent.setVisibility(View.GONE);

            if (tvMsgIn != null) tvMsgIn.setText(msg.text);
            if (tvTimeIn != null) tvTimeIn.setText(timeText);

            if (ivMessageAvatar != null) {
                if (realPfpUrl != null && !realPfpUrl.isEmpty()) {
                    Glide.with(getContext()).load(realPfpUrl)
                            .placeholder(android.R.drawable.sym_def_app_icon).circleCrop().into(ivMessageAvatar);
                } else {
                    ivMessageAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                }
            }

            if (msg.isGroup) {
                if (tvSenderName != null) {
                    tvSenderName.setVisibility(View.VISIBLE);
                    tvSenderName.setText(realUsername); // Usa il nome scaricato dalla cache
                }
                if (ivVerifiedBadgeSender != null) {
                    UserVerificationManager.setupVerificationBadge(ivVerifiedBadgeSender, realVerified); // Usa il badge della cache
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
}