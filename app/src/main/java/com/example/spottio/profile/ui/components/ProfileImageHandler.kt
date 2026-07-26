package com.example.spottio.profile.ui.components

import android.widget.ImageView
import com.example.spottio.profile.presentation.ProfileViewModel

class ProfileImageHandler {
    private var ivPfp: ImageView? = null

    fun bindImageView(ivPfp: ImageView) {
        this.ivPfp = ivPfp
    }

    // Configura il click sull'immagine per aprire il popup del link
    fun setupProfileImageClick(
        uiManager: ProfileUIManager,
        isMyProfile: Boolean,
        currentUrl: String?,
        viewModel: ProfileViewModel,
        currentUser: String
    ) {
        if (isMyProfile && ivPfp != null) {
            ivPfp?.setOnClickListener {
                uiManager.showEditProfileImageDialog(currentUrl) { newUrl ->
                    viewModel.updateProfileImageUrl(currentUser, newUrl)
                }
            }
        } else {
            ivPfp?.setOnClickListener(null) // Chi visita il profilo non può cliccare
        }
    }
}