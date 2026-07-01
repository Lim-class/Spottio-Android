package com.example.spottio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileUIManager {
    private final Context context;

    public ProfileUIManager(Context context) {
        this.context = context;
    }

    public void configureProfileLayout(boolean isMyProfile, View view) {
        var btnLogout = view.findViewById(R.id.btnLogout);
        var btnChat = view.findViewById(R.id.btnChatProfile);
        var btnFollow = view.findViewById(R.id.btnFollow);

        // LOGICA ULTRA-COMPATTA: Imposta la visibilità in una sola passata
        if (btnLogout != null) btnLogout.setVisibility(isMyProfile ? View.VISIBLE : View.GONE);
        if (btnChat != null) btnChat.setVisibility(isMyProfile ? View.GONE : View.VISIBLE);
        if (btnFollow != null) btnFollow.setVisibility(isMyProfile ? View.GONE : View.VISIBLE);
    }

    public void updateFollowButton(Button btnFollow, boolean isFollowing) {
        btnFollow.setText(context.getString(isFollowing ? R.string.btn_following : R.string.btn_follow_action));
        btnFollow.setBackgroundColor(isFollowing ? Color.LTGRAY : Color.parseColor("#002D57"));
    }

    public void showEditBioDialog(String currentBio, OnBioEditedListener listener) {
        var input = new EditText(context);
        input.setText(currentBio);

        new AlertDialog.Builder(context)
                .setTitle("Modifica Bio")
                .setView(input)
                .setPositiveButton(context.getString(R.string.btn_save), (d, w) -> {
                    var newBio = input.getText().toString().trim();
                    listener.onBioEdited(newBio);
                    Toast.makeText(context, context.getString(R.string.toast_saving), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(context.getString(R.string.btn_cancel), null)
                .show();
    }

    public void showEditProfileImageDialog(String currentUrl, OnImageUrlEditedListener listener) {
        var input = new EditText(context);
        input.setText(currentUrl);
        input.setHint("https://...");

        new AlertDialog.Builder(context)
                .setTitle("Modifica Foto Profilo")
                .setMessage("Inserisci il nuovo URL dell'immagine:")
                .setView(input)
                .setPositiveButton(context.getString(R.string.btn_save), (d, w) -> {
                    var newUrl = input.getText().toString().trim();
                    listener.onImageUrlEdited(newUrl);
                    Toast.makeText(context, context.getString(R.string.toast_saving), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(context.getString(R.string.btn_cancel), null)
                .show();
    }

    public void performLogout(Activity activity) {
        FirebaseAuth.getInstance().signOut();

        var prefs = activity.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        var intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public void setupAdminFeatures(Boolean isAdmin, Button btnViewReports, Runnable onReportsClick) {
        if (Boolean.TRUE.equals(isAdmin)) {
            btnViewReports.setVisibility(View.VISIBLE);
            btnViewReports.setOnClickListener(v -> onReportsClick.run());
        } else {
            btnViewReports.setVisibility(View.GONE);
        }
    }

    public void setProfileImage(ImageView ivPfp, String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(context)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .into(ivPfp);
        } else {
            ivPfp.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }

    public void showEditUsernameDialog(String currentUsername, OnUsernameEditedListener listener) {
        var input = new EditText(context);
        input.setText(currentUsername);

        new AlertDialog.Builder(context)
                .setTitle("Modifica Username")
                .setView(input)
                .setPositiveButton(context.getString(R.string.btn_save), (d, w) -> {
                    var newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        listener.onUsernameEdited(newName);
                        Toast.makeText(context, context.getString(R.string.toast_saving), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "L'username non può essere vuoto", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(context.getString(R.string.btn_cancel), null)
                .show();
    }

    public interface OnBioEditedListener { void onBioEdited(String newBio); }
    public interface OnImageUrlEditedListener { void onImageUrlEdited(String newUrl); }
    public interface OnUsernameEditedListener { void onUsernameEdited(String newUsername); }
}