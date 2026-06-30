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

    // 1. DEFINIAMO L'INTERFACCIA
    public interface OnUserClickListener {
        void onUserClick(String uidOrUsername);
    }

    private OnUserClickListener listener;

    public UserAdapter(Context context, List<String> users) {
        super(context, 0, users);
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
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
                UserGraphicsHelper.applicaGrafica(getContext(), tvUserName, ivUserImage, ivVerifiedBadge, cachedUser);
            } else {
                UserGraphicsHelper.applicaGrafica(getContext(), tvUserName, ivUserImage, ivVerifiedBadge, null);
                UserCache.fetchUserAsync(uidOrUsername, this::notifyDataSetChanged);
            }

            // NUOVO: Gestione unificata del click
            convertView.setOnClickListener(v -> {
                if (listener != null) listener.onUserClick(uidOrUsername);
            });
        }

        return convertView;
    }
}