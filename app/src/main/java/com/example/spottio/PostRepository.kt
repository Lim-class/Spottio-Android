package com.example.spottio

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

// Risultato della query di paginazione
data class PostFetchResult(val posts: List<Post>, val lastVisible: DocumentSnapshot?)

class PostRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    suspend fun getUserInterests(uid: String): Map<String, Long> {
        if (uid.isEmpty()) return emptyMap()
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists() && doc.contains("interests")) {
                @Suppress("UNCHECKED_CAST")
                doc.get("interests") as? Map<String, Long> ?: emptyMap()
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getPosts(limit: Long, startAfter: DocumentSnapshot? = null): PostFetchResult {
        var query = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)

        if (startAfter != null) {
            query = query.startAfter(startAfter)
        }

        val snapshot = query.get().await()
        val posts = mutableListOf<Post>()
        for (doc in snapshot) {
            val p = doc.toObject(Post::class.java)
            p.postId = doc.id
            posts.add(p)
        }

        val lastVisible = if (!snapshot.isEmpty) snapshot.documents[snapshot.size() - 1] else null
        return PostFetchResult(posts, lastVisible)
    }

    fun toggleLike(postId: String, likesList: List<String>) {
        db.collection("posts").document(postId).update("likes", likesList)
    }

    fun deletePost(postId: String, onSuccess: () -> Unit) {
        db.collection("posts").document(postId).delete().addOnSuccessListener { onSuccess() }
    }

    fun updatePostText(postId: String, newText: String, onSuccess: () -> Unit) {
        db.collection("posts").document(postId).update("text", newText).addOnSuccessListener { onSuccess() }
    }

    fun updateComments(postId: String, comments: List<Comment>, onSuccess: () -> Unit) {
        db.collection("posts").document(postId).update("comments", comments).addOnSuccessListener { onSuccess() }
    }

    fun submitReport(report: Report, onSuccess: () -> Unit, onFailure: () -> Unit) {
        db.collection("reports").add(report)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }
}