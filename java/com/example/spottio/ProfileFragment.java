package com.example.spottio;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

public class ProfileFragment extends Fragment {

    private String targetUser;
    private String currentUser;

    private ProfileViewModel viewModel;
    private ProfileUIManager uiManager;
    private ProfileCoordinator coordinator;

    private ProfileViewBinder viewBinder;
    private ProfileImageHandler imageHandler;

    public ProfileFragment() {}

    public static ProfileFragment newInstance(String targetUser) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("ARG_TARGET_USER", targetUser);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) targetUser = getArguments().getString("ARG_TARGET_USER");

        // Recupero centralizzato tramite AuthManager
        currentUser = AuthManager.getCurrentUserUid(requireContext());
        if (currentUser.isEmpty()) {
            currentUser = "Guest";
        }
        SharedPreferences prefs = requireActivity().getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE); // Ti serve ancora sotto per viewModel.checkAdminStatus(currentUser, prefs);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        uiManager = new ProfileUIManager(requireContext());
        coordinator = new ProfileCoordinator(this, currentUser, targetUser);

        if (coordinator.isMyProfile()) {
            targetUser = currentUser;
        }

        imageHandler = new ProfileImageHandler();
        viewModel.checkAdminStatus(currentUser, prefs);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 1. Inizializza il Binder per la UI
        viewBinder = new ProfileViewBinder(view);
        viewBinder.rvMyPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        // ATTENZIONE: Abbiamo rimosso viewBinder.setTargetUser(targetUser) da qui,
        // perché lo assegniamo in modo dinamico leggendolo dal database (vedi sotto).

        // 2. Configura le immagini
        imageHandler.bindImageView(viewBinder.ivPfp);

        // 3. Configura componenti e listener
        viewBinder.setupLanguageSelector(requireContext(), coordinator);
        viewBinder.setupButtons(uiManager, coordinator, viewModel, currentUser, targetUser);
        viewBinder.setupClickListeners(coordinator, viewModel);

        // 4. Collega Dati e View
        observeViewModel();

        // 5. Richiedi dati a Firebase e scarica i post
        viewModel.loadProfileData(targetUser, currentUser, coordinator.isMyProfile());
        viewModel.loadUserPosts(targetUser);

        return view;
    }

    private void observeViewModel() {
        // NUOVO: Osserva l'username dal database e lo stampa correttamente a schermo
        viewModel.getUsername().observe(getViewLifecycleOwner(), name -> {
            viewBinder.setTargetUser(name);
        });

        viewModel.getBio().observe(getViewLifecycleOwner(), bio -> viewBinder.tvBio.setText(bio));
        viewModel.getFollowersCount().observe(getViewLifecycleOwner(), count -> viewBinder.tvFollowersCount.setText(String.valueOf(count)));
        viewModel.getFollowingCount().observe(getViewLifecycleOwner(), count -> viewBinder.tvFollowingCount.setText(String.valueOf(count)));

        viewModel.getIsFollowing().observe(getViewLifecycleOwner(), isFollowing ->
                uiManager.updateFollowButton(viewBinder.btnFollow, isFollowing)
        );

        viewModel.getProfileImageUrl().observe(getViewLifecycleOwner(), url -> {
            uiManager.setProfileImage(viewBinder.ivPfp, url);
            imageHandler.setupProfileImageClick(uiManager, coordinator.isMyProfile(), url, viewModel, currentUser);
        });

        viewModel.getIsVerified().observe(getViewLifecycleOwner(), isVerified -> {
            UserVerificationManager.setupVerificationBadge(viewBinder.ivVerifiedBadge, isVerified);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Configurazione dello stato Admin
        viewModel.getIsAdmin().observe(getViewLifecycleOwner(), isAdmin -> {
            uiManager.setupAdminFeatures(isAdmin, viewBinder.btnViewReports,
                    () -> coordinator.navigateToAdminReports());

            if (viewModel.getUserPosts().getValue() != null && !viewModel.getUserPosts().getValue().isEmpty()) {
                viewBinder.rvMyPosts.setAdapter(new PostAdapter(viewModel.getUserPosts().getValue(), currentUser, isAdmin));
            }
        });

        viewModel.getUserPosts().observe(getViewLifecycleOwner(), posts -> {
            Boolean isAdmin = viewModel.getIsAdmin().getValue();
            viewBinder.rvMyPosts.setAdapter(new PostAdapter(posts, currentUser, isAdmin != null ? isAdmin : false));
        });
    }
}