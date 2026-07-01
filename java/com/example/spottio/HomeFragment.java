package com.example.spottio;

import android.content.Intent;
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

import com.ablanco.zoomy.Zoomy;
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
        // JAVA 15: Inferenza del tipo per le variabili locali
        var view = inflater.inflate(R.layout.fragment_home_feed, container, false);

        recyclerView = view.findViewById(R.id.rvHomeFeed);
        db = FirebaseFirestore.getInstance();

        currentUid = AuthManager.getCurrentUserUid(requireContext());
        isAdmin = AuthManager.isCurrentUserAdmin(requireContext());

        cloudPostList = new ArrayList<>();

        // JAVA 15: Utilizzo di 'var' per istanziare interfacce anonime complesse
        var interactionListener = new PostAdapter.PostInteractionListener() {
            @Override
            public void onUserClick(String userId) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment.newInstance(userId))
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onEditClick(Post post, int position) {
                PostDialogHelper.showEditPostDialog(requireContext(), post, adapter, position);
            }

            @Override
            public void onCommentClick(Post post, int position) {
                PostDialogHelper.showCommentsSheet(requireContext(), post, currentUid, adapter, position);
            }

            @Override
            public void onReportClick(Post post) {
                PostDialogHelper.showReportDialog(requireContext(), post, currentUid);
            }

            @Override
            public void onShareClick(Post post, String shareContent) {
                // JAVA 15: Semplificazione dell'Intent
                var sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Condividi post tramite:"));
            }

            @Override
            public void onImageZoomClick(View imageView, String mediaUri) {
                // JAVA 15: 'var' snellisce notevolmente la sintassi dei Builder concatenati
                var builder = new Zoomy.Builder(requireActivity())
                        .target(imageView)
                        .enableImmersiveMode(false)
                        .animateZooming(true);
                builder.register();
            }
        };

        adapter = new PostAdapter(cloudPostList, currentUid, isAdmin, interactionListener);

        var layoutManager = new LinearLayoutManager(getContext());
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
                            @SuppressWarnings("unchecked")
                            var interests = (Map<String, Long>) documentSnapshot.get("interests");
                            userInterests = interests;
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
                    var visibleItemCount = layoutManager.getChildCount();
                    var totalItemCount = layoutManager.getItemCount();
                    var firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

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

                    // JAVA 15: var è utilissimo per le liste tipizzate
                    var pagePosts = new ArrayList<Post>();

                    // JAVA 15: var pulisce molto la firma all'interno del foreach
                    for (var doc : queryDocumentSnapshots) {
                        var p = doc.toObject(Post.class);
                        if (p != null) {
                            p.setPostId(doc.getId());
                            pagePosts.add(p);
                        }
                    }

                    sortPostsByInterest(pagePosts);
                    cloudPostList.addAll(pagePosts);
                    adapter.notifyDataSetChanged();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        var docs = queryDocumentSnapshots.getDocuments();
                        lastVisible = docs.get(docs.size() - 1);
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
                    var pagePosts = new ArrayList<Post>();

                    for (var doc : queryDocumentSnapshots) {
                        var p = doc.toObject(Post.class);
                        if (p != null) {
                            p.setPostId(doc.getId());
                            pagePosts.add(p);
                        }
                    }

                    sortPostsByInterest(pagePosts);

                    var insertIndex = cloudPostList.size();
                    cloudPostList.addAll(pagePosts);

                    adapter.notifyItemRangeInserted(insertIndex, pagePosts.size());

                    if (!queryDocumentSnapshots.isEmpty()) {
                        var docs = queryDocumentSnapshots.getDocuments();
                        lastVisible = docs.get(docs.size() - 1);
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
            // JAVA 15: Utilizzo compatto di var per i dati primitivi calcolati
            var score1 = getCategoryScore(p1.getCategory());
            var score2 = getCategoryScore(p2.getCategory());

            if (score1 != score2) {
                return Long.compare(score2, score1);
            }

            var t1 = p1.getTimestamp();
            var t2 = p2.getTimestamp();

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