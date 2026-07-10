package com.example.spottio

import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    fun searchUsers(
        query: String,
        currentUserUid: String,
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("users")
            .orderBy("username")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val foundUids = mutableListOf<String>()

                for (doc in result) {
                    val uid = doc.id
                    val username = doc.getString("username")
                    val pfpUri = doc.getString("userPfUri") ?: ""
                    val isVerified = doc.getBoolean("isVerified") ?: false

                    if (username != null && uid != currentUserUid) {
                        // Inseriamo in Cache direttamente qui, così l'UI non ci deve pensare!
                        UserCache.putUser(uid, username, pfpUri, isVerified)
                        foundUids.add(uid)
                    }
                }
                onSuccess(foundUids)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}