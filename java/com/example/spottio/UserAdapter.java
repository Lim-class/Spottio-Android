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

public class UserAdapter extends ArrayAdapter<String> {

    public UserAdapter(Context context, List<String> users) {
        super(context, 0, users);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // L'elemento nella lista adesso è quasi sempre un UID
        String uidOrUsername = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
        }

        TextView tvUserName = convertView.findViewById(R.id.tvUserName);
        ImageView ivUserImage = convertView.findViewById(R.id.ivUserImage);
        ImageView ivVerifiedBadge = convertView.findViewById(R.id.ivVerifiedBadge);

        if (uidOrUsername != null) {
            // Impostiamo valori di caricamento per evitare sfarfallii o "buchi vuoti"
            tvUserName.setText("Caricamento...");
            ivUserImage.setImageResource(android.R.drawable.sym_def_app_icon);
            UserVerificationManager.setupVerificationBadge(ivVerifiedBadge, false);

            UserCache.UserData userData = UserCache.getUser(uidOrUsername);

            if (userData != null) {
                // TROVATO IN CACHE (Ricerca rapida locale)
                applicaGrafica(tvUserName, ivUserImage, ivVerifiedBadge, userData.username, userData.pfpUri, userData.isVerified);
            } else {
                // NON TROVATO IN CACHE (Es: abbiamo appena aperto Followers/Following)
                // Andiamo a pescare il documento reale su Firestore
                FirebaseFirestore.getInstance().collection("users").document(uidOrUsername).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String realUsername = doc.getString("username");
                                String pfpUri = doc.getString("userPfUri");
                                Boolean verifiedObj = doc.getBoolean("isVerified");
                                boolean isVerified = verifiedObj != null ? verifiedObj : false;

                                if (realUsername == null) realUsername = "Utente";

                                UserCache.putUser(uidOrUsername, realUsername, pfpUri, isVerified);
                                applicaGrafica(tvUserName, ivUserImage, ivVerifiedBadge, realUsername, pfpUri, isVerified);
                            } else {
                                // AUTO-HEALING: Se non lo trova come UID, era un vecchio username salvato prima della migrazione!
                                FirebaseFirestore.getInstance().collection("users").whereEqualTo("username", uidOrUsername).limit(1).get()
                                        .addOnSuccessListener(querySnapshot -> {
                                            if (!querySnapshot.isEmpty()) {
                                                String realUsername = querySnapshot.getDocuments().get(0).getString("username");
                                                String pfpUri = querySnapshot.getDocuments().get(0).getString("userPfUri");
                                                Boolean verifiedObj = querySnapshot.getDocuments().get(0).getBoolean("isVerified");
                                                boolean isVerified = verifiedObj != null ? verifiedObj : false;

                                                UserCache.putUser(uidOrUsername, realUsername, pfpUri, isVerified);
                                                applicaGrafica(tvUserName, ivUserImage, ivVerifiedBadge, realUsername, pfpUri, isVerified);
                                            } else {
                                                tvUserName.setText("Utente Sconosciuto");
                                            }
                                        });
                            }
                        });
            }
        }

        return convertView;
    }

    // Metodo di appoggio per non ripetere il codice di rendering grafico
    private void applicaGrafica(TextView tv, ImageView ivPfp, ImageView ivBadge, String username, String pfp, boolean verified) {
        tv.setText(username != null ? username : "Utente");
        UserVerificationManager.setupVerificationBadge(ivBadge, verified);
        if (pfp != null && !pfp.isEmpty()) {
            Glide.with(getContext())
                    .load(pfp)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .circleCrop()
                    .into(ivPfp);
        } else {
            ivPfp.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }
}