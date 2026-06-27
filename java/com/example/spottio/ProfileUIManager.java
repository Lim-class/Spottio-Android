package com.example.spottio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import android.widget.ImageView;
import com.example.spottio.R;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ProfileUIManager {
    private final Context context;

    public ProfileUIManager(Context context) {
        this.context = context;
    }

    public void updateFollowButton(Button btnFollow, boolean isFollowing) {
        if (isFollowing) {
            btnFollow.setText(context.getString(R.string.btn_following));
            btnFollow.setBackgroundColor(Color.LTGRAY);
        } else {
            btnFollow.setText(context.getString(R.string.btn_follow_action));
            btnFollow.setBackgroundColor(Color.parseColor("#002D57"));
        }
    }

    public void showEditBioDialog(String currentBio, OnBioEditedListener listener) {
        final EditText input = new EditText(context);
        input.setText(currentBio);

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.dialog_edit_bio_title))
                .setView(input)
                .setPositiveButton(context.getString(R.string.btn_save), (d, w) -> {
                    listener.onBioEdited(input.getText().toString());
                    Toast.makeText(context, context.getString(R.string.toast_saving), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(context.getString(R.string.btn_cancel), null)
                .show();
    }

    public void showEditProfileImageDialog(String currentUrl, OnImageUrlEditedListener listener) {
        final EditText input = new EditText(context);
        input.setText(currentUrl);
        input.setHint("Inserisci il link (URL) dell'immagine");

        new AlertDialog.Builder(context)
                .setTitle("Modifica Foto Profilo")
                .setView(input)
                .setPositiveButton(context.getString(R.string.btn_save), (d, w) -> {
                    listener.onImageUrlEdited(input.getText().toString());
                    Toast.makeText(context, context.getString(R.string.toast_saving), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(context.getString(R.string.btn_cancel), null)
                .show();
    }

    public void performLogout(Activity activity) {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.logout_title))
                .setMessage(context.getString(R.string.logout_msg))
                .setPositiveButton(context.getString(R.string.yes), (dialog, which) -> {

                    // DELEGA TOTALE: Passiamo l'activity corrente ad AuthManager e lasciamo fare a lui
                    AuthManager authManager = new AuthManager(activity);
                    authManager.logout();

                })
                .setNegativeButton(context.getString(R.string.no), null)
                .show();
    }

    // Configurazione semplificata dell'amministratore applicata direttamente al pulsante
    public void setupAdminFeatures(boolean isAdmin, Button btnViewReports, Runnable onReportsClick) {
        if (btnViewReports != null) {
            if (isAdmin) {
                btnViewReports.setVisibility(View.VISIBLE);
                btnViewReports.setOnClickListener(v -> {
                    if (onReportsClick != null) onReportsClick.run();
                });
            } else {
                btnViewReports.setVisibility(View.GONE);
            }
        }
    }

    public void configureProfileLayout(boolean isMyProfile, View view) {
        Button btnLogout = view.findViewById(R.id.btnLogout);
        Button btnChat = view.findViewById(R.id.btnChatProfile);
        Button btnFollow = view.findViewById(R.id.btnFollow);
        View langLayout = view.findViewById(R.id.layoutLanguage);

        if (isMyProfile) {
            btnFollow.setVisibility(View.GONE);
            btnChat.setVisibility(View.GONE);
            if (langLayout != null) langLayout.setVisibility(View.VISIBLE);
            if (btnLogout != null) btnLogout.setVisibility(View.VISIBLE);
        } else {
            if (langLayout != null) langLayout.setVisibility(View.GONE);
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);
            btnFollow.setVisibility(View.VISIBLE);
            btnChat.setVisibility(View.VISIBLE);
        }
    }

    public void setProfileImage(ImageView ivPfp, String url) {
        if (ivPfp == null) return;
        if (url != null && !url.trim().isEmpty()) {
            Glide.with(ivPfp.getContext())
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .into(ivPfp);
        } else {
            ivPfp.setImageResource(android.R.drawable.sym_def_app_icon);
        }
    }

    public void showEditUsernameDialog(String currentUsername, OnUsernameEditedListener listener) {
        final EditText input = new EditText(context);
        input.setText(currentUsername);

        new AlertDialog.Builder(context)
                .setTitle("Modifica Username")
                .setView(input)
                .setPositiveButton(context.getString(R.string.btn_save), (d, w) -> {
                    String newName = input.getText().toString().trim();
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

    public interface OnBioEditedListener {
        void onBioEdited(String newBio);
    }

    public interface OnImageUrlEditedListener {
        void onImageUrlEdited(String newUrl);
    }

    public interface OnUsernameEditedListener {
        void onUsernameEdited(String newUsername);
    }
}