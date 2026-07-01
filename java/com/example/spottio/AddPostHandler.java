package com.example.spottio;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddPostHandler {

    private final FirebaseFirestore db;

    public AddPostHandler() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Interfaccia per avvisare il Fragment quando le categorie sono pronte
    public interface OnCategoriesLoadedListener {
        void onSuccess(List<String> categories);
        void onFailure(Exception e);
    }

    // Interfaccia per avvisare il Fragment dell'esito della pubblicazione
    public interface OnPostPublishedListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Scarica le categorie da Firestore.
     */
    public void loadCategories(OnCategoriesLoadedListener listener) {
        db.collection("categories")
                .orderBy("name")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> categories = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String categoryName = document.getString("name");
                            if (categoryName != null) {
                                categories.add(categoryName);
                            }
                        }
                        listener.onSuccess(categories);
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }

    /**
     * Salva il nuovo Post su Firestore.
     */
    public void publishPost(Post newPost, OnPostPublishedListener listener) {
        db.collection("posts")
                .add(newPost)
                .addOnSuccessListener(documentReference -> listener.onSuccess())
                .addOnFailureListener(listener::onFailure);
    }
}