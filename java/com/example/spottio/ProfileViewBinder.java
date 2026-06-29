package com.example.spottio;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class ProfileViewBinder {
    private final View view;

    public final ImageView ivPfp;
    public final TextView tvBio, tvFollowersCount, tvFollowingCount;
    public final Button btnFollow;
    public final ImageButton btnSettings;
    public final ImageButton btnEditBioPencil;
    public final RecyclerView rvMyPosts;
    public final Button btnViewReports;

    public final ImageView ivVerifiedBadge;

    private final LinearLayout layoutFollowers, layoutFollowing;
    private LanguageSelectorManager languageSelectorManager;

    public final ImageButton btnEditUsernamePencil;

    public ProfileViewBinder(View view) {
        this.view = view;

        ivPfp = view.findViewById(R.id.ivProfilePic);
        tvBio = view.findViewById(R.id.tvBio);
        tvFollowersCount = view.findViewById(R.id.tvFollowersCount);
        tvFollowingCount = view.findViewById(R.id.tvFollowingCount);
        btnFollow = view.findViewById(R.id.btnFollow);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnEditBioPencil = view.findViewById(R.id.btnEditBioPencil);

        layoutFollowers = view.findViewById(R.id.layoutFollowers);
        layoutFollowing = view.findViewById(R.id.layoutFollowing);
        rvMyPosts = view.findViewById(R.id.rvMyPosts);
        btnViewReports = view.findViewById(R.id.btnViewReports);
        ivVerifiedBadge = view.findViewById(R.id.ivVerifiedBadge);

        btnEditUsernamePencil = view.findViewById(R.id.btnEditUsernamePencil);
    }

    public void setTargetUser(String targetUser) {
        TextView tvUsername = view.findViewById(R.id.tvProfileName);
        if (tvUsername != null) {
            tvUsername.setText(targetUser);
        }
    }

    public void setupLanguageSelector(Context context, ProfileCoordinator coordinator) {
        languageSelectorManager = new LanguageSelectorManager(context, view);
        languageSelectorManager.init((langTag, flag, name) -> {
            languageSelectorManager.updateLanguageUI(flag, name);
            coordinator.changeLanguage(langTag, flag, name);
        });
    }

    public void setupButtons(ProfileUIManager uiManager, ProfileCoordinator coordinator,
                             ProfileViewModel viewModel, String currentUser, String targetUser) {

        uiManager.configureProfileLayout(coordinator.isMyProfile(), view);

        Button btnLogout = view.findViewById(R.id.btnLogout);
        Button btnChat = view.findViewById(R.id.btnChatProfile);

        if (coordinator.isMyProfile()) {
            if (btnSettings != null) {
                btnSettings.setVisibility(View.VISIBLE);
                btnSettings.setOnClickListener(v -> coordinator.navigateToSettings());
            }

            if (btnEditBioPencil != null) {
                btnEditBioPencil.setVisibility(View.VISIBLE);
                btnEditBioPencil.setOnClickListener(v -> uiManager.showEditBioDialog(
                        tvBio.getText().toString(),
                        newBio -> viewModel.updateBio(currentUser, newBio)
                ));
            }

            if (btnLogout != null) {
                btnLogout.setOnClickListener(v -> coordinator.handleLogout(uiManager));
            }

            if (btnEditUsernamePencil != null) {
                btnEditUsernamePencil.setVisibility(View.VISIBLE);
                btnEditUsernamePencil.setOnClickListener(v -> {
                    TextView tvUsername = view.findViewById(R.id.tvProfileName);
                    String currentName = tvUsername != null ? tvUsername.getText().toString() : "";
                    uiManager.showEditUsernameDialog(currentName, newUsername ->
                            viewModel.updateUsername(currentUser, newUsername)
                    );
                });
            }
        } else {
            if (btnSettings != null) btnSettings.setVisibility(View.GONE);
            if (btnEditBioPencil != null) btnEditBioPencil.setVisibility(View.GONE);
            if (btnEditUsernamePencil != null) btnEditUsernamePencil.setVisibility(View.GONE);

            // MODIFICA: Leggiamo il nome utente reale e lo passiamo al Coordinator
            btnChat.setOnClickListener(v -> {
                TextView tvUsername = view.findViewById(R.id.tvProfileName);
                String targetName = (tvUsername != null && tvUsername.getText() != null) ? tvUsername.getText().toString() : "Chat";
                coordinator.navigateToChat(targetName);
            });

            btnFollow.setOnClickListener(v -> viewModel.toggleFollow(
                    targetUser,
                    currentUser,
                    Boolean.TRUE.equals(viewModel.getIsFollowing().getValue())
            ));
        }
    }

    public void setupClickListeners(ProfileCoordinator coordinator, ProfileViewModel viewModel) {
        if (layoutFollowers != null) {
            layoutFollowers.setOnClickListener(v ->
                    coordinator.navigateToFollowers(viewModel.getFollowersList().getValue())
            );
        }

        if (layoutFollowing != null) {
            layoutFollowing.setOnClickListener(v ->
                    coordinator.navigateToFollowing(viewModel.getFollowingList().getValue())
            );
        }
    }
}