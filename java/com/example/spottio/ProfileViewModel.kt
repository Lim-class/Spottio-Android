package com.example.spottio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()

    private val _username = MutableLiveData("")
    val username: LiveData<String> get() = _username

    private val _bio = MutableLiveData<String>()
    val bio: LiveData<String> get() = _bio

    private val _followersCount = MutableLiveData(0)
    val followersCount: LiveData<Int> get() = _followersCount

    private val _followingCount = MutableLiveData(0)
    val followingCount: LiveData<Int> get() = _followingCount

    private val _isFollowing = MutableLiveData(false)
    val isFollowing: LiveData<Boolean> get() = _isFollowing

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _followersList = MutableLiveData<List<String>>(emptyList())
    val followersList: LiveData<List<String>> get() = _followersList

    private val _followingList = MutableLiveData<List<String>>(emptyList())
    val followingList: LiveData<List<String>> get() = _followingList

    private val _profileImageUrl = MutableLiveData<String>()
    val profileImageUrl: LiveData<String> get() = _profileImageUrl

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> get() = _userPosts

    private val _isVerified = MutableLiveData(false)
    val isVerified: LiveData<Boolean> get() = _isVerified

    fun loadUserProfile(targetUser: String, currentUser: String) {
        db.collection("users").document(targetUser)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null || documentSnapshot == null || !documentSnapshot.exists()) {
                    _errorMessage.postValue("Utente non trovato")
                    return@addSnapshotListener
                }

                _username.postValue(documentSnapshot.getString("username") ?: "")
                _bio.postValue(documentSnapshot.getString("bio") ?: "")
                _profileImageUrl.postValue(documentSnapshot.getString("userPfUri") ?: "")

                @Suppress("UNCHECKED_CAST")
                val followers = documentSnapshot.get("followers") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val following = documentSnapshot.get("following") as? List<String> ?: emptyList()

                _followersList.postValue(followers)
                _followingList.postValue(following)

                _followersCount.postValue(followers.size)
                _followingCount.postValue(following.size)

                _isFollowing.postValue(followers.contains(currentUser))
                _isVerified.postValue(documentSnapshot.getBoolean("isVerified") ?: false)
            }

        loadUserPosts(targetUser)
    }

    fun loadUserPosts(targetUser: String) {
        db.collection("posts")
            .whereEqualTo("user", targetUser)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { queryDocumentSnapshots, e ->
                if (e != null || queryDocumentSnapshots == null) return@addSnapshotListener

                val posts = queryDocumentSnapshots.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.apply {
                        postId = doc.id
                    }
                }
                _userPosts.postValue(posts)
            }
    }

    fun updateBio(currentUser: String, newBio: String) {
        db.collection("users").document(currentUser)
            .update("bio", newBio)
            .addOnFailureListener { e -> _errorMessage.postValue("Errore aggiornamento bio: ${e.message}") }
    }

    fun updateProfileImageUrl(currentUser: String, newUrl: String) {
        db.collection("users").document(currentUser)
            .update("userPfUri", newUrl)
            .addOnFailureListener { e -> _errorMessage.postValue("Errore aggiornamento foto: ${e.message}") }
    }

    fun toggleFollow(targetUser: String, currentUser: String, currentlyFollowing: Boolean) {
        if (currentlyFollowing) {
            db.collection("users").document(targetUser).update("followers", FieldValue.arrayRemove(currentUser))
            db.collection("users").document(currentUser).update("following", FieldValue.arrayRemove(targetUser))
        } else {
            db.collection("users").document(targetUser).update("followers", FieldValue.arrayUnion(currentUser))
            db.collection("users").document(currentUser).update("following", FieldValue.arrayUnion(targetUser))
        }
    }

    fun updateUsername(currentUser: String, newUsername: String) {
        db.collection("users").document(currentUser)
            .update("username", newUsername)
            .addOnFailureListener { e -> _errorMessage.postValue("Errore aggiornamento username: ${e.message}") }
    }
}