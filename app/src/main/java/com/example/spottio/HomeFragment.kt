package com.example.spottio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ablanco.zoomy.Zoomy

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private lateinit var viewModel: HomeViewModel

    private var currentUid: String = ""
    private var isAdmin: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_feed, container, false)

        recyclerView = view.findViewById(R.id.rvHomeFeed)

        currentUid = AuthManager.getCurrentUserUid(requireContext())
        isAdmin = AuthManager.isCurrentUserAdmin(requireContext())

        // Setup ViewModel
        val repository = PostRepository()
        val factory = HomeViewModelFactory(repository, currentUid)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        val interactionListener = object : PostAdapter.PostInteractionListener {
            override fun onUserClick(userId: String) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment.newInstance(userId))
                    .addToBackStack(null)
                    .commit()
            }

            override fun onLikeClick(post: Post, position: Int) {
                viewModel.toggleLike(post)
                adapter.notifyItemChanged(position)
            }

            override fun onDeleteClick(post: Post, position: Int) {
                PostDialogHelper.showDeletePostDialog(requireContext()) {
                    viewModel.deletePost(post) {
                        Toast.makeText(context, "Post eliminato", Toast.LENGTH_SHORT).show()
                        // Il ViewModel aggiorna la lista generale osservata
                    }
                }
            }

            override fun onEditClick(post: Post, position: Int) {
                PostDialogHelper.showEditPostDialog(requireContext(), post.text) { newText ->
                    viewModel.editPost(post, newText) {
                        adapter.notifyItemChanged(position)
                        Toast.makeText(context, "Post modificato", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCommentClick(post: Post, position: Int) {
                PostDialogHelper.showCommentsSheet(requireContext(), post.comments) { commentText ->
                    viewModel.addComment(post, commentText) {
                        adapter.notifyItemChanged(position)
                    }
                }
            }

            override fun onReportClick(post: Post) {
                PostDialogHelper.showReportDialog(requireContext()) { reason, desc ->
                    viewModel.reportPost(post, reason, desc,
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

        adapter = PostAdapter(emptyList(), currentUid, isAdmin, interactionListener)

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        // Osserva i dati e la paginazione
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            adapter.setPostList(posts)
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (viewModel.isLoading.value == false && !viewModel.isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= (totalItemCount - 2) && firstVisibleItemPosition >= 0) {
                            viewModel.loadMorePosts()
                        }
                    }
                }
            }
        })

        return view
    }
}