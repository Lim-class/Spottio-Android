package com.example.spottio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ablanco.zoomy.Zoomy

class ProfileFragment : Fragment() {

    private var targetUser: String? = null
    private lateinit var currentUser: String

    private lateinit var viewModel: ProfileViewModel
    private lateinit var uiManager: ProfileUIManager
    private lateinit var coordinator: ProfileCoordinator
    private lateinit var viewBinder: ProfileViewBinder
    private lateinit var imageHandler: ProfileImageHandler

    companion object {
        @JvmStatic
        fun newInstance(targetUser: String): ProfileFragment {
            return ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString("ARG_TARGET_USER", targetUser)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetUser = arguments?.getString("ARG_TARGET_USER")
        currentUser = AuthManager.getCurrentUserUid(requireContext())

        if (targetUser.isNullOrEmpty()) {
            targetUser = currentUser
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]
        uiManager = ProfileUIManager(requireContext())
        coordinator = ProfileCoordinator(this, currentUser, targetUser)
        viewBinder = ProfileViewBinder(view)
        imageHandler = ProfileImageHandler()

        imageHandler.bindImageView(viewBinder.ivPfp)
        viewBinder.rvMyPosts.layoutManager = LinearLayoutManager(requireContext())

        val isMyProfile = coordinator.isMyProfile()
        uiManager.configureProfileLayout(isMyProfile, view)

        viewBinder.configureForUser(
            isMyProfile,
            coordinator,
            viewModel,
            currentUser,
            targetUser!!,
            uiManager
        )

        viewBinder.setupClickListeners(coordinator, viewModel)

        setupLanguageSelector(view)
        setupAdminReportsButton()

        observeViewModel()

        viewModel.loadUserProfile(targetUser!!, currentUser)

        return view
    }

    private fun setupLanguageSelector(rootView: View) {
        val languageManager = LanguageSelectorManager(requireContext(), rootView)
        languageManager.init(object : LanguageSelectorManager.LanguageSelectionListener {
            override fun onLanguageSelected(code: String, flag: String, name: String) {
                coordinator.changeLanguage(code, flag, name)
            }
        })
    }

    private fun setupAdminReportsButton() {
        if (AuthManager.isCurrentUserAdmin(requireContext()) && coordinator.isMyProfile()) {
            viewBinder.btnViewReports?.visibility = View.VISIBLE
            viewBinder.btnViewReports?.setOnClickListener {
                coordinator.navigateToAdminReports()
            }
        } else {
            viewBinder.btnViewReports?.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        viewModel.username.observe(viewLifecycleOwner) { name ->
            view?.findViewById<TextView>(R.id.tvProfileName)?.text = name
        }

        viewModel.bio.observe(viewLifecycleOwner) { bioText ->
            viewBinder.tvBio.text = bioText
        }

        viewModel.followersCount.observe(viewLifecycleOwner) { count ->
            viewBinder.tvFollowersCount.text = count.toString()
        }

        viewModel.followingCount.observe(viewLifecycleOwner) { count ->
            viewBinder.tvFollowingCount.text = count.toString()
        }

        viewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            uiManager.updateFollowButton(isFollowing, viewBinder.btnFollow)
        }

        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            uiManager.loadProfileImage(url, viewBinder.ivPfp)
            imageHandler.setupProfileImageClick(
                uiManager,
                coordinator.isMyProfile(),
                url,
                viewModel,
                currentUser
            )
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isVerified.observe(viewLifecycleOwner) { isVerified ->
            viewBinder.ivVerifiedBadge?.let {
                UserVerificationManager.setupVerificationBadge(it, isVerified)
            }
        }

        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            val userIsAdmin = AuthManager.isCurrentUserAdmin(requireContext())

            val profilePostListener = object : PostAdapter.PostInteractionListener {
                override fun onUserClick(userId: String) {
                    // Cliccare se stessi sul proprio profilo non fa nulla
                }

                override fun onEditClick(post: Post, position: Int) {
                    viewBinder.rvMyPosts.adapter?.let { adapter ->
                        PostDialogHelper.showEditPostDialog(requireContext(), post, adapter, position)
                    }
                }

                override fun onCommentClick(post: Post, position: Int) {
                    viewBinder.rvMyPosts.adapter?.let { adapter ->
                        PostDialogHelper.showCommentsSheet(requireContext(), post, currentUser, adapter, position)
                    }
                }

                override fun onReportClick(post: Post) {
                    PostDialogHelper.showReportDialog(requireContext(), post, currentUser)
                }

                override fun onShareClick(post: Post, shareContent: String) {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, shareContent)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(sendIntent, "Condividi post tramite:"))
                }

                override fun onImageZoomClick(imageView: View, mediaUri: String) {
                    Zoomy.Builder(requireActivity())
                        .target(imageView)
                        .enableImmersiveMode(false)
                        .animateZooming(true)
                        .register()
                }
            }

            viewBinder.rvMyPosts.adapter = PostAdapter(posts, currentUser, userIsAdmin, profilePostListener)
        }
    }
}