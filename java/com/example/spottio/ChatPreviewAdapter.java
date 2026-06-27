package com.example.spottio;

import android.content.Context;
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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatPreviewAdapter extends ArrayAdapter<ChatPreview> {

    public ChatPreviewAdapter(Context context, List<ChatPreview> previews) {
        super(context, 0, previews);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_chat_preview, parent, false);
        }

        ChatPreview preview = getItem(position);
        if (preview == null) return convertView;

        CircleImageView ivAvatar = convertView.findViewById(R.id.ivAvatar);
        TextView tvGroupBadge = convertView.findViewById(R.id.tvGroupBadge);
        TextView tvName = convertView.findViewById(R.id.tvName);
        ImageView ivVerifiedBadge = convertView.findViewById(R.id.ivVerifiedBadge);
        TextView tvLastMessage = convertView.findViewById(R.id.tvLastMessage);

        String lastMsg = preview.getLastMessage();
        tvLastMessage.setText(lastMsg != null && !lastMsg.isEmpty() ? lastMsg : "Nessun messaggio");

        if (preview.isGroup()) {
            // GRUPPO: i dati sono già integrati nella preview
            tvGroupBadge.setVisibility(View.VISIBLE);
            ivVerifiedBadge.setVisibility(View.GONE);
            tvName.setText(preview.getName());

            if (preview.getProfilePicUri() != null && !preview.getProfilePicUri().isEmpty()) {
                Glide.with(getContext())
                        .load(preview.getProfilePicUri())
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .error(android.R.drawable.sym_def_app_icon)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
            }
        } else {
            // CHAT PRIVATA: Avvia la Traduzione Dinamica UID -> Utente
            tvGroupBadge.setVisibility(View.GONE);
            String otherUid = preview.getName(); // Contiene l'UID passato dall'Helper

            UserCache.UserData cached = UserCache.getUser(otherUid);

            if (cached != null) {
                // TROVATO IN CACHE LOCALE (Istantaneo)
                tvName.setText(cached.username);
                UserVerificationManager.setupVerificationBadge(ivVerifiedBadge, cached.isVerified);

                if (cached.pfpUri != null && !cached.pfpUri.isEmpty()) {
                    Glide.with(getContext())
                            .load(cached.pfpUri)
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                }
            } else {
                // NON IN CACHE: Mostra il caricamento e vai su Internet a scaricare i dati dell'altro utente
                tvName.setText("Caricamento...");
                UserVerificationManager.setupVerificationBadge(ivVerifiedBadge, false);
                ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                UserCache.putUser(otherUid, "Caricamento...", "", false);

                FirebaseFirestore.getInstance().collection("users").document(otherUid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("username");
                                String pfp = doc.getString("userPfUri");
                                Boolean verObj = doc.getBoolean("isVerified");
                                boolean ver = verObj != null && verObj;

                                // Salva in cache i dati corretti e fai ricaricare la grafica!
                                UserCache.putUser(otherUid, name != null ? name : "Utente", pfp, ver);
                                notifyDataSetChanged();
                            }
                        });
            }
        }

        return convertView;
    }
}