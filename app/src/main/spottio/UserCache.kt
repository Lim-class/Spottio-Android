package com.example.spottio

import android.util.LruCache
import com.google.firebase.firestore.FirebaseFirestore

object UserCache {

    data class UserData(
        @JvmField val username: String,
        @JvmField val pfpUri: String,
        @JvmField val isVerified: Boolean
    )

    private val cache = LruCache<String, UserData>(100)
    private val fetchingUids = mutableSetOf<String>()

    fun interface FetchCallback {
        fun onUserFetched()
    }

    @JvmStatic
    fun getUser(uid: String): UserData? {
        return cache.get(uid)
    }

    // RIMOSSO "private" DA QUI:
    @JvmStatic
    fun putUser(uid: String, username: String, pfpUri: String, isVerified: Boolean) {
        cache.put(uid, UserData(username, pfpUri, isVerified))
    }

    @JvmStatic
    fun fetchUserAsync(uidOrUsername: String, callback: FetchCallback?) {
        if (cache.get(uidOrUsername) != null) {
            callback?.onUserFetched()
            return
        }

        if (fetchingUids.contains(uidOrUsername)) return

        fetchingUids.add(uidOrUsername)
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uidOrUsername).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("username") ?: "Utente"
                    val pfp = doc.getString("userPfUri") ?: ""
                    val verObj = doc.getBoolean("isVerified") ?: false
                    putUser(uidOrUsername, name, pfp, verObj)

                    fetchingUids.remove(uidOrUsername)
                    callback?.onUserFetched()
                } else {
                    db.collection("users").whereEqualTo("username", uidOrUsername).limit(1).get()
                        .addOnSuccessListener { query ->
                            if (!query.isEmpty) {
                                val firstDoc = query.documents[0]
                                val name = firstDoc.getString("username") ?: "Utente"
                                val pfp = firstDoc.getString("userPfUri") ?: ""
                                val verObj = firstDoc.getBoolean("isVerified") ?: false
                                putUser(uidOrUsername, name, pfp, verObj)
                            } else {
                                putUser(uidOrUsername, "Utente Sconosciuto o Eliminato", "", false)
                            }
                            fetchingUids.remove(uidOrUsername)
                            callback?.onUserFetched()
                        }.addOnFailureListener {
                            fetchingUids.remove(uidOrUsername)
                        }
                }
            }.addOnFailureListener {
                fetchingUids.remove(uidOrUsername)
            }
    }
}