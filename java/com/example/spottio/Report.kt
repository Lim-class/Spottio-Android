package com.example.spottio

import com.google.firebase.firestore.Exclude

data class Report @JvmOverloads constructor(
    @JvmField var postId: String = "",
    @JvmField var postAuthor: String = "",
    @JvmField var reporterUser: String = "",
    @JvmField var reason: String = "",
    @JvmField var description: String = "",
    @JvmField var postText: String = "",
    @JvmField var timestamp: Long = System.currentTimeMillis()
) {
    @JvmField
    @Exclude
    var reportId: String? = null
}