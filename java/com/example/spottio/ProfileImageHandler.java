package com.example.spottio;

import android.widget.ImageView;

public class ProfileImageHandler {
    private ImageView ivPfp;

    public ProfileImageHandler() {
        // Non serve più il launcher della galleria
    }

    public void bindImageView(ImageView ivPfp) {
        this.ivPfp = ivPfp;
    }

    // Configura il click sull'immagine per aprire il popup del link
    public void setupProfileImageClick(ProfileUIManager uiManager, boolean isMyProfile,
                                       String currentUrl, ProfileViewModel viewModel, String currentUser) {
        if (isMyProfile && ivPfp != null) {
            ivPfp.setOnClickListener(v -> {
                uiManager.showEditProfileImageDialog(currentUrl, newUrl -> {
                    viewModel.updateProfileImageUrl(currentUser, newUrl);
                });
            });
        } else if (ivPfp != null) {
            ivPfp.setOnClickListener(null); // Chi visita il profilo non può cliccare
        }
    }
}