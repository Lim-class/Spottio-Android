package com.example.spottio;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

public class PostActionHandler {

    private final Context context;
    private final RecyclerView.Adapter<?> adapter;
    private final String currentUser;

    public PostActionHandler(Context context, RecyclerView.Adapter<?> adapter, String currentUser) {
        this.context = context;
        this.adapter = adapter;
        this.currentUser = currentUser;
    }

    // --- Gestione Eliminazione ---
    public void deletePost(Post post) {
        new AlertDialog.Builder(context)
                .setTitle("Elimina Post")
                .setMessage("Eliminare definitivamente?")
                .setPositiveButton("Sì", (d, w) -> {
                    if (post.getPostId() != null) {
                        FirebaseFirestore.getInstance().collection("posts")
                                .document(post.getPostId())
                                .delete()
                                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Eliminato", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("No", null).show();
    }

    // --- Gestione Like ---
    public void toggleLike(Post post, int position) {
        boolean wasLiked = post.getLikes().contains(currentUser);
        post.toggleLike(currentUser);

        // Aggiorna visivamente l'elemento specifico
        adapter.notifyItemChanged(position);

        if (post.getPostId() != null) {
            FirebaseFirestore.getInstance().collection("posts")
                    .document(post.getPostId())
                    .update("likes", post.getLikes());

            // Se sta aggiungendo il Like (e non togliendolo), traccia l'interesse
            if (!wasLiked) {
                InteractionTracker.trackInteraction(currentUser, post.getCategory());
            }
        }
    }
}