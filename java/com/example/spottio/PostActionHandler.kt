package com.example.spottio

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class PostActionHandler(
    private val context: Context,
    private val adapter: RecyclerView.Adapter<*>,
    private val currentUser: String
) {

    // --- Gestione Eliminazione ---
    fun deletePost(post: Post) {
        AlertDialog.Builder(context)
            .setTitle("Elimina Post")
            .setMessage("Eliminare definitivamente?")
            .setPositiveButton("Sì") { _, _ ->
                post.postId?.let { pId ->
                    FirebaseFirestore.getInstance().collection("posts")
                        .document(pId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Eliminato", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    // --- Gestione Like ---
    fun toggleLike(post: Post, position: Int) {
        val wasLiked = post.likes.contains(currentUser)
        post.toggleLike(currentUser)

        // Aggiorna visivamente l'elemento specifico
        adapter.notifyItemChanged(position)

        post.postId?.let { pId ->
            FirebaseFirestore.getInstance().collection("posts")
                .document(pId)
                .update("likes", post.likes)

            // Se sta aggiungendo il Like (e non togliendolo), traccia l'interesse
            if (!wasLiked) {
                post.category?.let { cat ->
                    InteractionTracker.trackInteraction(currentUser, cat)
                }
            }
        }
    }
}