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
import java.util.List;

public class UserAdapter extends ArrayAdapter<String> {

    public UserAdapter(Context context, List<String> users) {
        super(context, 0, users);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String uidOrUsername = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_user, parent, false);
        }

        TextView tvUserName = convertView.findViewById(R.id.tvUserName);
        ImageView ivUserImage = convertView.findViewById(R.id.ivUserImage);
        ImageView ivVerifiedBadge = convertView.findViewById(R.id.ivVerifiedBadge);

        if (uidOrUsername != null) {
            UserCache.UserData cachedUser = UserCache.getUser(uidOrUsername);

            if (cachedUser != null) {
                // TROVATO IN CACHE (Disegna subito l'interfaccia definitiva)
                UserGraphicsHelper.applicaGrafica(getContext(), tvUserName, ivUserImage, ivVerifiedBadge, cachedUser);
            } else {
                // NON TROVATO IN CACHE (Disegna il caricamento temporaneo)
                UserGraphicsHelper.applicaGrafica(getContext(), tvUserName, ivUserImage, ivVerifiedBadge, null);

                // Vai a scaricare in background usando l'Auto-Healing della Cache
                UserCache.fetchUserAsync(uidOrUsername, this::notifyDataSetChanged);
            }
        }

        return convertView;
    }
}