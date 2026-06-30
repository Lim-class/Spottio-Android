package com.example.spottio;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class ProfileCoordinator {
    private final Fragment fragment;
    private final String currentUser;
    private final String targetUser;

    public ProfileCoordinator(Fragment fragment, String currentUser, String targetUser) {
        this.fragment = fragment;
        this.currentUser = currentUser;
        this.targetUser = targetUser;
    }

    public boolean isMyProfile() {
        return targetUser != null && targetUser.equalsIgnoreCase(currentUser);
    }

    public void navigateToFollowers(List<String> followers) {
        if (followers != null && !followers.isEmpty()) {
            Context ctx = fragment.getContext();
            String titolo = ctx != null ? ctx.getString(R.string.label_followers) : "Followers";

            replaceFragment(UserConnectionsFragment.newInstance(titolo, new ArrayList<>(followers)));
        } else {
            Toast.makeText(fragment.getContext(), "Nessun follower", Toast.LENGTH_SHORT).show();
        }
    }

    public void navigateToFollowing(List<String> following) {
        if (following != null && !following.isEmpty()) {
            Context ctx = fragment.getContext();
            String titolo = ctx != null ? ctx.getString(R.string.label_following) : "Seguiti";

            replaceFragment(UserConnectionsFragment.newInstance(titolo, new ArrayList<>(following)));
        } else {
            Toast.makeText(fragment.getContext(), "Nessun utente seguito", Toast.LENGTH_SHORT).show();
        }
    }

    public void navigateToChat(String titleName) {
        ChatFragment chatFrag = ChatFragment.newInstance(targetUser, false, titleName);
        replaceFragment(chatFrag);
    }

    public void navigateToSettings() {
        replaceFragment(new SettingsFragment());
    }

    public void changeLanguage(String langTag, String flag, String name) {
        // CHIAMATA AL CERVELLO UNICO PER IL CAMBIO LINGUA
        LanguageHelper.setLanguage(fragment.requireContext(), langTag, flag, name);

        if (fragment.getActivity() != null) {
            fragment.getActivity().recreate();
        }
    }

    public void handleLogout(ProfileUIManager uiManager) {
        if (fragment.getActivity() != null) {
            uiManager.performLogout(fragment.getActivity());
        }
    }

    public void pickProfileImage(ActivityResultLauncher<String> launcher) {
        launcher.launch("image/*");
    }

    private void replaceFragment(Fragment newFragment) {
        if (fragment.getParentFragmentManager() != null) {
            fragment.getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, newFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void navigateToAdminReports() {
        if (fragment.getActivity() != null) {
            android.content.Intent intent = new android.content.Intent(fragment.requireContext(), AdminReportsActivity.class);
            fragment.startActivity(intent);
        }
    }
}