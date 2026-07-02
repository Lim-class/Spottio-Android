package com.example.spottio

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ablanco.zoomy.Zoomy
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PostAdapter
    private var cloudPostList = mutableListOf<Post>()
    private lateinit var db: FirebaseFirestore

    private var currentUid: String = ""
    private var isAdmin: Boolean = false

    // Paginazione
    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false
    private var isLastPage = false
    private val LIMIT: Long = 10

    // Preferenze utente per l'algoritmo
    private var userInterests = mapOf<String, Long>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home_feed, container, false)

        recyclerView = view.findViewById(R.id.rvHomeFeed)
        db = FirebaseFirestore.getInstance()

        currentUid = AuthManager.getCurrentUserUid(requireContext())
        isAdmin = AuthManager.isCurrentUserAdmin(requireContext())

        cloudPostList = mutableListOf()

        val interactionListener = object : PostAdapter.PostInteractionListener {
            override fun onUserClick(userId: String) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment.newInstance(userId))
                    .addToBackStack(null)
                    .commit()
            }

            override fun onEditClick(post: Post, position: Int) {
                PostDialogHelper.showEditPostDialog(requireContext(), post, adapter, position)
            }

            override fun onCommentClick(post: Post, position: Int) {
                PostDialogHelper.showCommentsSheet(requireContext(), post, currentUid, adapter, position)
            }

            override fun onReportClick(post: Post) {
                PostDialogHelper.showReportDialog(requireContext(), post, currentUid)
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

        adapter = PostAdapter(cloudPostList, currentUid, isAdmin, interactionListener)

        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        setupScrollListener(layoutManager)
        loadUserInterestsAndPosts()

        return view
    }

    private fun loadUserInterestsAndPosts() {
        isLoading = true

        if (currentUid.isEmpty()) {
            loadInitialPosts()
            return
        }

        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists() && documentSnapshot.contains("interests")) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val interests = documentSnapshot.get("interests") as? Map<String, Long>
                        if (interests != null) {
                            userInterests = interests
                        }
                    } catch (e: Exception) {
                        Log.e("HomeFragment", "Errore cast interessi: ${e.message}")
                    }
                }
                loadInitialPosts()
            }
            .addOnFailureListener { loadInitialPosts() }
    }

    private fun setupScrollListener(layoutManager: LinearLayoutManager) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= (totalItemCount - 2) && firstVisibleItemPosition >= 0) {
                            loadMorePosts()
                        }
                    }
                }
            }
        })
    }

    private fun loadInitialPosts() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(LIMIT)
            .get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                cloudPostList.clear()

                val pagePosts = mutableListOf<Post>()

                for (doc in queryDocumentSnapshots) {
                    val p = doc.toObject(Post::class.java)
                    p.postId = doc.id
                    pagePosts.add(p)
                }

                sortPostsByInterest(pagePosts)
                cloudPostList.addAll(pagePosts)
                adapter.notifyDataSetChanged()

                if (!queryDocumentSnapshots.isEmpty) {
                    val docs = queryDocumentSnapshots.documents
                    lastVisible = docs[docs.size - 1]
                }

                if (queryDocumentSnapshots.size() < LIMIT.toInt()) {
                    isLastPage = true
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    private fun loadMorePosts() {
        val currentLastVisible = lastVisible ?: return

        isLoading = true
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(currentLastVisible)
            .limit(LIMIT)
            .get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                val pagePosts = mutableListOf<Post>()

                for (doc in queryDocumentSnapshots) {
                    val p = doc.toObject(Post::class.java)
                    p.postId = doc.id
                    pagePosts.add(p)
                }

                sortPostsByInterest(pagePosts)

                val insertIndex = cloudPostList.size
                cloudPostList.addAll(pagePosts)

                adapter.notifyItemRangeInserted(insertIndex, pagePosts.size)

                if (!queryDocumentSnapshots.isEmpty) {
                    val docs = queryDocumentSnapshots.documents
                    lastVisible = docs[docs.size - 1]
                }

                if (queryDocumentSnapshots.size() < LIMIT.toInt()) {
                    isLastPage = true
                }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    private fun sortPostsByInterest(postsToSort: MutableList<Post>) {
        if (userInterests.isEmpty()) return

        postsToSort.sortWith { p1, p2 ->
            val score1 = getCategoryScore(p1.category)
            val score2 = getCategoryScore(p2.category)

            if (score1 != score2) {
                score2.compareTo(score1)
            } else {
                // Kotlin sa che il timestamp non è nullo, quindi lo confronta direttamente!
                p2.timestamp.compareTo(p1.timestamp)
            }
        }
    }

    private fun getCategoryScore(category: String?): Long {
        if (category == null || !userInterests.containsKey(category)) return 0L
        return userInterests[category] ?: 0L
    }
}