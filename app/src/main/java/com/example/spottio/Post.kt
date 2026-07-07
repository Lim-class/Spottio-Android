package com.example.spottio

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.Date

@IgnoreExtraProperties
// @JvmOverloads genera automaticamente per Java il costruttore da 5 argomenti che ti serve!
data class Post @JvmOverloads constructor(
    @JvmField var user: String = "",
    @JvmField var text: String? = null,
    @JvmField var mediaUri: String? = null,

    @JvmField
    @PropertyName("video")
    var isVideo: Boolean = false,

    @JvmField var category: String? = null,
    @JvmField var timestamp: Date = Date(),

    var comments: MutableList<Comment> = mutableListOf(),

    @get:PropertyName("Likes")
    @set:PropertyName("Likes")
    var likes: MutableList<String> = mutableListOf()
) {
    @JvmField
    @Exclude
    var postId: String? = null

    fun addComment(comment: Comment) {
        comments.add(comment)
    }

    fun toggleLike(uid: String) {
        if (likes.contains(uid)) {
            likes.remove(uid)
        } else {
            likes.add(uid)
        }
    }
}

// Per inserire il verificato e riutilizzarlo
// <include layout="@layout/layout_verified_badge" />

