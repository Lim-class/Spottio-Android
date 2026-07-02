package com.example.spottio;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMemberAdapter extends ArrayAdapter<String> {

    // 1. DEFINIAMO L'INTERFACCIA (Il Citofono)
    public interface GroupMemberListener {
        void onRemoveMemberRequest(String memberId);
        void onMemberClick(String memberId);
    }

    private final String currentUser;
    private final FirebaseFirestore db;
    private final String groupId;
    private final GroupMemberListener listener;

    private String adminId;
    private boolean isCurrentUserAdmin = false;

    // NUOVO: Ora accetta Context generico e il Listener, non l'Activity fissa!
    public GroupMemberAdapter(Context context, List<String> memberIds, String currentUser, FirebaseFirestore db, String groupId, GroupMemberListener listener) {
        super(context, 0, memberIds);
        this.currentUser = currentUser;
        this.db = db;
        this.groupId = groupId;
        this.listener = listener;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public void setCurrentUserIsAdmin(boolean isAdmin) {
        this.isCurrentUserAdmin = isAdmin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_group_member, parent, false);
        }

        String memberId = getItem(position);
        if (memberId == null) return convertView;

        CircleImageView ivAvatar = convertView.findViewById(R.id.ivMemberAvatar);
        TextView tvName = convertView.findViewById(R.id.tvMemberName);
        TextView tvRole = convertView.findViewById(R.id.tvMemberRole);

        tvName.setText(memberId);

        // Click standard per vedere il profilo
        convertView.setOnClickListener(v -> {
            if (listener != null) listener.onMemberClick(memberId);
        });

        if (isCurrentUserAdmin && !memberId.equals(adminId)) {
            // NUOVO: Niente più AlertDialog qui. Avvisiamo solo la schermata!
            convertView.setOnLongClickListener(v -> {
                if (listener != null) listener.onRemoveMemberRequest(memberId);
                return true;
            });
            tvRole.setText("Tieni premuto per rimuovere");
            tvRole.setTextColor(Color.parseColor("#FF0000"));
        } else if (memberId.equals(adminId)) {
            tvRole.setText("Amministratore");
            tvRole.setTextColor(Color.parseColor("#1DB954"));
            convertView.setOnLongClickListener(null);
        } else {
            tvRole.setText("Partecipante");
            tvRole.setTextColor(Color.parseColor("#888888"));
            convertView.setOnLongClickListener(null);
        }

        db.collection("users").document(memberId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String uri = doc.getString("userPfUri");
                if (uri != null && !uri.isEmpty()) {
                    Glide.with(getContext()).load(uri).into(ivAvatar);
                } else {
                    ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                }
            }
        });

        return convertView;
    }
}