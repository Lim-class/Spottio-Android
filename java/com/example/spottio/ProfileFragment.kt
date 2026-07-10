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

            // AGGIORNATO: Ora implementa correttamente tutte le nuove regole del Listener!
            val profilePostListener = object : PostAdapter.PostInteractionListener {
                val postRepository = PostRepository() // Riutilizziamo il repository appena creato

                override fun onUserClick(userId: String) {
                    // Cliccare se stessi sul proprio profilo non fa nulla
                }

                override fun onLikeClick(post: Post, position: Int) {
                    val wasLiked = post.likes.contains(currentUser)
                    post.toggleLike(currentUser)
                    post.postId?.let {
                        postRepository.toggleLike(it, post.likes)
                        if (!wasLiked) {
                            post.category?.let { cat -> InteractionTracker.trackInteraction(currentUser, cat) }
                        }
                    }
                    viewBinder.rvMyPosts.adapter?.notifyItemChanged(position)
                }

                override fun onDeleteClick(post: Post, position: Int) {
                    PostDialogHelper.showDeletePostDialog(requireContext()) {
                        post.postId?.let {
                            postRepository.deletePost(it) {
                                Toast.makeText(context, "Post eliminato", Toast.LENGTH_SHORT).show()
                                viewModel.loadUserProfile(targetUser!!, currentUser) // Ricarica il profilo
                            }
                        }
                    }
                }

                override fun onEditClick(post: Post, position: Int) {
                    PostDialogHelper.showEditPostDialog(requireContext(), post.text) { newText ->
                        post.postId?.let {
                            postRepository.updatePostText(it, newText) {
                                post.text = newText
                                viewBinder.rvMyPosts.adapter?.notifyItemChanged(position)
                                Toast.makeText(context, "Post modificato", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onCommentClick(post: Post, position: Int) {
                    PostDialogHelper.showCommentsSheet(requireContext(), post.comments) { commentText ->
                        val newComment = Comment(currentUser, commentText)
                        post.addComment(newComment)
                        post.postId?.let {
                            postRepository.updateComments(it, post.comments) {
                                viewBinder.rvMyPosts.adapter?.notifyItemChanged(position)
                            }
                        }
                    }
                }

                override fun onReportClick(post: Post) {
                    PostDialogHelper.showReportDialog(requireContext()) { reason, desc ->
                        val report = Report(post.postId ?: "", post.user, currentUser, reason, desc, post.text ?: "")
                        postRepository.submitReport(report,
                            onSuccess = { Toast.makeText(context, "Segnalazione inviata", Toast.LENGTH_SHORT).show() },
                            onFailure = { Toast.makeText(context, "Errore invio segnalazione", Toast.LENGTH_SHORT).show() }
                        )
                    }
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