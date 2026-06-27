package com.example.spottio;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> cloudPostList;
    private FirebaseFirestore db;

    private String currentUid;
    private boolean isAdmin;

    // Paginazione
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private final int LIMIT = 10;

    // Preferenze utente per l'algoritmo
    private Map<String, Long> userInterests = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_feed, container, false);

        recyclerView = view.findViewById(R.id.rvHomeFeed);
        db = FirebaseFirestore.getInstance();

        // MODIFICA: Utilizzo del metodo centralizzato di AuthManager
        currentUid = AuthManager.getCurrentUserUid(requireContext());
        isAdmin = AuthManager.isCurrentUserAdmin(requireContext());

        cloudPostList = new ArrayList<>();
        adapter = new PostAdapter(cloudPostList, currentUid, isAdmin);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        setupScrollListener(layoutManager);
        loadUserInterestsAndPosts();

        return view;
    }

    private void loadUserInterestsAndPosts() {
        isLoading = true;

        if (currentUid == null || currentUid.isEmpty()) {
            loadInitialPosts();
            return;
        }

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("interests")) {
                        try {
                            userInterests = (Map<String, Long>) documentSnapshot.get("interests");
                        } catch (Exception e) {
                            Log.e("HomeFragment", "Errore cast interessi: " + e.getMessage());
                        }
                    }
                    loadInitialPosts();
                })
                .addOnFailureListener(e -> loadInitialPosts());
    }

    private void setupScrollListener(LinearLayoutManager layoutManager) {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= (totalItemCount - 2) && firstVisibleItemPosition >= 0) {
                            loadMorePosts();
                        }
                    }
                }
            }
        });
    }

    private void loadInitialPosts() {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(LIMIT)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cloudPostList.clear();
                    List<Post> pagePosts = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post p = doc.toObject(Post.class);
                        if (p != null) {
                            p.setPostId(doc.getId());
                            pagePosts.add(p);
                        }
                    }

                    sortPostsByInterest(pagePosts);
                    cloudPostList.addAll(pagePosts);
                    adapter.notifyDataSetChanged();

                    if (queryDocumentSnapshots.size() > 0) {
                        lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                    }
                    if (queryDocumentSnapshots.size() < LIMIT) {
                        isLastPage = true;
                    }
                    isLoading = false;
                })
                .addOnFailureListener(e -> isLoading = false);
    }

    private void loadMorePosts() {
        if (lastVisible == null) return;
        isLoading = true;
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(LIMIT)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> pagePosts = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post p = doc.toObject(Post.class);
                        if (p != null) {
                            p.setPostId(doc.getId());
                            pagePosts.add(p);
                        }
                    }

                    sortPostsByInterest(pagePosts);
                    int insertIndex = cloudPostList.size();
                    cloudPostList.addAll(pagePosts);

                    adapter.notifyItemRangeInserted(insertIndex, pagePosts.size());

                    if (queryDocumentSnapshots.size() > 0) {
                        lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                    }
                    if (queryDocumentSnapshots.size() < LIMIT) {
                        isLastPage = true;
                    }
                    isLoading = false;
                })
                .addOnFailureListener(e -> isLoading = false);
    }

    private void sortPostsByInterest(List<Post> postsToSort) {
        if (userInterests == null || userInterests.isEmpty()) return;

        postsToSort.sort((p1, p2) -> {
            long score1 = getCategoryScore(p1.getCategory());
            long score2 = getCategoryScore(p2.getCategory());

            if (score1 != score2) {
                return Long.compare(score2, score1);
            }

            Date t1 = p1.getTimestamp();
            Date t2 = p2.getTimestamp();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;

            return t2.compareTo(t1);
        });
    }

    private long getCategoryScore(String category) {
        if (category == null || !userInterests.containsKey(category)) return 0;
        return userInterests.get(category);
    }
}