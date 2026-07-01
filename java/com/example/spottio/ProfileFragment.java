package com.example.spottio;

import android.content.Context;
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
        // JAVA 17: Uso di 'var' per istanziare oggetti senza ripetere il tipo
        var fragment = new ProfileFragment();
        var args = new Bundle();
        args.putString("ARG_TARGET_USER", targetUser);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) targetUser = getArguments().getString("ARG_TARGET_USER");

        currentUser = AuthManager.getCurrentUserUid(requireContext());
        if (currentUser.isEmpty()) {
            currentUser = "Guest";
        }

        // JAVA 17: 'var' rende più snella la lettura di lunghe chiamate di sistema
        var prefs = requireActivity().getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);

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
        // JAVA 17: 'var' per l'inflazione delle view
        var view = inflater.inflate(R.layout.fragment_profile, container, false);

        viewBinder = new ProfileViewBinder(view);
        viewBinder.rvMyPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        imageHandler.bindImageView(viewBinder.ivPfp);
        viewBinder.setupLanguageSelector(requireContext(), coordinator);
        viewBinder.setupButtons(uiManager, coordinator, viewModel, currentUser, targetUser);
        viewBinder.setupClickListeners(coordinator, viewModel);

        observeViewModel();

        viewModel.loadProfileData(targetUser, currentUser, coordinator.isMyProfile());
        viewModel.loadUserPosts(targetUser);

        return view;
    }

    private void observeViewModel() {
        viewModel.getUsername().observe(getViewLifecycleOwner(), name -> viewBinder.setTargetUser(name));
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

        viewModel.getIsVerified().observe(getViewLifecycleOwner(), isVerified ->
                UserVerificationManager.setupVerificationBadge(viewBinder.ivVerifiedBadge, isVerified)
        );

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getIsAdmin().observe(getViewLifecycleOwner(), isAdmin ->
                uiManager.setupAdminFeatures(isAdmin, viewBinder.btnViewReports, () -> coordinator.navigateToAdminReports())
        );

        viewModel.getUserPosts().observe(getViewLifecycleOwner(), posts -> {
            // JAVA 17: Semplifichiamo il recupero dello stato Admin
            var isAdminObj = viewModel.getIsAdmin().getValue();
            boolean userIsAdmin = isAdminObj != null ? isAdminObj : false;

            // JAVA 17: Creazione di interfacce anonime tramite var
            var profilePostListener = new PostAdapter.PostInteractionListener() {
                @Override
                public void onUserClick(String userId) {
                    if (!userId.equals(targetUser)) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, ProfileFragment.newInstance(userId))
                                .addToBackStack(null).commit();
                    }
                }

                @Override
                public void onEditClick(Post post, int position) {
                    PostDialogHelper.showEditPostDialog(requireContext(), post, (PostAdapter) viewBinder.rvMyPosts.getAdapter(), position);
                }

                @Override
                public void onCommentClick(Post post, int position) {
                    PostDialogHelper.showCommentsSheet(requireContext(), post, currentUser, (PostAdapter) viewBinder.rvMyPosts.getAdapter(), position);
                }

                @Override
                public void onReportClick(Post post) {
                    PostDialogHelper.showReportDialog(requireContext(), post, currentUser);
                }

                @Override
                public void onShareClick(Post post, String shareContent) {
                    var sendIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareContent);
                    sendIntent.setType("text/plain");
                    startActivity(android.content.Intent.createChooser(sendIntent, "Condividi post tramite:"));
                }

                @Override
                public void onImageZoomClick(View imageView, String mediaUri) {
                    var builder = new com.ablanco.zoomy.Zoomy.Builder(requireActivity())
                            .target(imageView).enableImmersiveMode(false).animateZooming(true);
                    builder.register();
                }
            };

            viewBinder.rvMyPosts.setAdapter(new PostAdapter(posts, currentUser, userIsAdmin, profilePostListener));
        });
    }
}