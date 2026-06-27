package com.example.spottio;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // NUOVO: Aggiunto per recuperare l'username dal database
    private final MutableLiveData<String> username = new MutableLiveData<>("");

    private final MutableLiveData<String> bio = new MutableLiveData<>();
    private final MutableLiveData<Integer> followersCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> followingCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isFollowing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<String>> followersList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> followingList = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<List<Post>> userPosts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isAdmin = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isVerified = new MutableLiveData<>(false);

    private final MutableLiveData<String> profileImageUrl = new MutableLiveData<>("");

    public LiveData<String> getUsername() { return username; } // Getter per l'username
    public LiveData<List<String>> getFollowersList() { return followersList; }
    public LiveData<List<String>> getFollowingList() { return followingList; }

    public LiveData<String> getBio() { return bio; }
    public LiveData<Integer> getFollowersCount() { return followersCount; }
    public LiveData<Integer> getFollowingCount() { return followingCount; }
    public LiveData<Boolean> getIsFollowing() { return isFollowing; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<List<Post>> getUserPosts() { return userPosts; }
    public LiveData<Boolean> getIsAdmin() { return isAdmin; }
    public LiveData<Boolean> getIsVerified() { return isVerified; }

    public LiveData<String> getProfileImageUrl() { return profileImageUrl; }

    public void checkAdminStatus(String currentUserUid, SharedPreferences prefs) {
        // 1. Controllo provvisorio sulla memoria locale
        boolean localAdmin = prefs.getBoolean(currentUserUid + "_is_admin", false);
        isAdmin.setValue(localAdmin);

        // 2. Controllo REALE sul server (Risolve il problema del mancato riconoscimento)
        if (currentUserUid != null && !currentUserUid.isEmpty()) {
            db.collection("users").document(currentUserUid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Boolean serverAdmin = doc.getBoolean("isAdmin");
                    boolean actualAdmin = serverAdmin != null && serverAdmin;
                    isAdmin.postValue(actualAdmin);
                    // Aggiorna la memoria per la prossima volta
                    prefs.edit().putBoolean(currentUserUid + "_is_admin", actualAdmin).apply();
                }
            });
        }
    }

    public void loadUserPosts(String targetUser) {
        db.collection("posts")
                .whereEqualTo("user", targetUser) // Filtra direttamente lato server
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Post> posts = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post p = doc.toObject(Post.class);
                        if (p != null) {
                            p.setPostId(doc.getId());
                            posts.add(p);
                        }
                    }

                    // Ordiniamo la lista localmente dal post più recente al più vecchio
                    posts.sort((p1, p2) -> {
                        if (p1.getTimestamp() == null || p2.getTimestamp() == null) return 0;
                        return p2.getTimestamp().compareTo(p1.getTimestamp());
                    });

                    userPosts.postValue(posts);
                })
                .addOnFailureListener(e -> {
                    errorMessage.postValue("Errore caricamento post: " + e.getMessage());
                });
    }

    public void loadProfileData(String targetUser, String currentUser, boolean isMyProfile) {
        db.collection("users").document(targetUser)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        // RISOLUZIONE: Se l'utente si sta disconnettendo (o non è loggato),
                        // ignoriamo l'errore "Permission Denied" causato dal logout.
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                            errorMessage.postValue("Errore di caricamento: " + error.getMessage());
                        }
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // ... tutto il resto del tuo codice rimane invariato ...
                        // NUOVO: Estrai l'username e passalo alla grafica!
                        String dbUsername = documentSnapshot.getString("username");
                        username.postValue((dbUsername != null && !dbUsername.isEmpty()) ? dbUsername : "Utente");

                        String bioText = documentSnapshot.getString("bio");
                        bio.postValue((bioText != null && !bioText.isEmpty()) ? bioText : "Nessuna biografia.");

                        List<String> followers = (List<String>) documentSnapshot.get("followers");
                        List<String> following = (List<String>) documentSnapshot.get("following");

                        Boolean verified = documentSnapshot.getBoolean("isVerified");
                        isVerified.postValue(verified != null ? verified : false);

                        String pfpUri = documentSnapshot.getString("userPfUri");
                        profileImageUrl.postValue(pfpUri != null ? pfpUri : "");

                        followersList.postValue(followers != null ? followers : new ArrayList<>());
                        followingList.postValue(following != null ? following : new ArrayList<>());

                        followersCount.postValue(followers != null ? followers.size() : 0);
                        followingCount.postValue(following != null ? following.size() : 0);

                        if (!isMyProfile && followers != null) {
                            isFollowing.postValue(followers.contains(currentUser));
                        }
                    }
                });
    }

    public void updateBio(String currentUser, String newBio) {
        db.collection("users").document(currentUser)
                .update("bio", newBio)
                .addOnFailureListener(e -> errorMessage.postValue("Errore aggiornamento bio: " + e.getMessage()));
    }

    public void updateProfileImageUrl(String currentUser, String newUrl) {
        db.collection("users").document(currentUser)
                .update("userPfUri", newUrl)
                .addOnFailureListener(e -> errorMessage.postValue("Errore aggiornamento foto: " + e.getMessage()));
    }

    public void toggleFollow(String targetUser, String currentUser, boolean currentlyFollowing) {
        if (currentlyFollowing) {
            db.collection("users").document(targetUser).update("followers", FieldValue.arrayRemove(currentUser));
            db.collection("users").document(currentUser).update("following", FieldValue.arrayRemove(targetUser));
        } else {
            db.collection("users").document(targetUser).update("followers", FieldValue.arrayUnion(currentUser));
            db.collection("users").document(currentUser).update("following", FieldValue.arrayUnion(targetUser));
        }
    }

    public void updateUsername(String currentUser, String newUsername) {
        db.collection("users").document(currentUser)
                .update("username", newUsername)
                .addOnFailureListener(e -> errorMessage.postValue("Errore aggiornamento username: " + e.getMessage()));
    }
}