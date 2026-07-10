package com.example.spottio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: PostRepository,
    private val currentUid: String
) : ViewModel() {

    private val _posts = MutableLiveData<MutableList<Post>>(mutableListOf())
    val posts: LiveData<MutableList<Post>> get() = _posts

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private var userInterests = mapOf<String, Long>()
    private var lastVisible: DocumentSnapshot? = null
    var isLastPage = false
    private val LIMIT: Long = 10

    init {
        loadInterestsAndInitialPosts()
    }

    private fun loadInterestsAndInitialPosts() {
        _isLoading.value = true
        viewModelScope.launch {
            userInterests = repository.getUserInterests(currentUid)
            fetchPosts(isInitialLoad = true)
        }
    }

    fun loadMorePosts() {
        if (_isLoading.value == true || isLastPage || lastVisible == null) return
        _isLoading.value = true
        viewModelScope.launch {
            fetchPosts(isInitialLoad = false)
        }
    }

    private suspend fun fetchPosts(isInitialLoad: Boolean) {
        try {
            val result = repository.getPosts(LIMIT, if (isInitialLoad) null else lastVisible)
            val newPosts = result.posts.toMutableList()

            sortPostsByInterest(newPosts)

            val currentList = if (isInitialLoad) mutableListOf() else _posts.value ?: mutableListOf()
            currentList.addAll(newPosts)

            _posts.value = currentList
            lastVisible = result.lastVisible
            isLastPage = newPosts.size < LIMIT
        } catch (e: Exception) {
            // Gestione errore
        } finally {
            _isLoading.value = false
        }
    }

    private fun sortPostsByInterest(postsToSort: MutableList<Post>) {
        if (userInterests.isEmpty()) return
        postsToSort.sortWith { p1, p2 ->
            val score1 = userInterests[p1.category] ?: 0L
            val score2 = userInterests[p2.category] ?: 0L
            if (score1 != score2) score2.compareTo(score1)
            else p2.timestamp.compareTo(p1.timestamp)
        }
    }

    // --- Azioni Utente ---
    fun toggleLike(post: Post) {
        val wasLiked = post.likes.contains(currentUid)
        post.toggleLike(currentUid)

        post.postId?.let {
            repository.toggleLike(it, post.likes)
            if (!wasLiked) {
                post.category?.let { cat -> InteractionTracker.trackInteraction(currentUid, cat) }
            }
        }
    }

    fun deletePost(post: Post, onSuccess: () -> Unit) {
        post.postId?.let {
            repository.deletePost(it) {
                val currentList = _posts.value ?: mutableListOf()
                currentList.remove(post)
                _posts.value = currentList
                onSuccess()
            }
        }
    }

    fun editPost(post: Post, newText: String, onSuccess: () -> Unit) {
        post.postId?.let {
            repository.updatePostText(it, newText) {
                post.text = newText
                onSuccess()
            }
        }
    }

    fun addComment(post: Post, commentText: String, onSuccess: () -> Unit) {
        val newComment = Comment(currentUid, commentText)
        post.addComment(newComment)
        post.postId?.let {
            repository.updateComments(it, post.comments) { onSuccess() }
        }
    }

    fun reportPost(post: Post, reason: String, description: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val report = Report(post.postId ?: "", post.user, currentUid, reason, description, post.text ?: "")
        repository.submitReport(report, onSuccess, onFailure)
    }
}