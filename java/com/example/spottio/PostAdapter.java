package com.example.spottio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ablanco.zoomy.Zoomy;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostViewHolder> {

    private List<Post> postList;
    private final String currentUser;
    private final boolean isAdmin;
    private PostActionHandler actionHandler;

    public PostAdapter(List<Post> postList, String currentUser, boolean isAdmin) {
        this.postList = postList;
        this.currentUser = currentUser;
        this.isAdmin = isAdmin;
    }

    public void setPostList(List<Post> newPosts) {
        this.postList = newPosts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        Context context = holder.itemView.getContext();

        if (actionHandler == null) {
            actionHandler = new PostActionHandler(context, this, currentUser);
        }

        // --- 1. Data di pubblicazione ---
        Date timestamp = post.getTimestamp();
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(timestamp));
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        // --- 2. Dati Dinamici (Username, Foto Profilo & Spunta Verificato in tempo reale) ---
        String postUidOrUsername = post.getUser(); // Può essere il nuovo UID o un vecchio Username

        // Valori di caricamento provvisori
        holder.tvUser.setText("Caricamento...");
        holder.ivUserPfp.setImageResource(android.R.drawable.sym_def_app_icon);
        UserVerificationManager.setupVerificationBadge(holder.ivVerifiedBadge, false);

        if (postUidOrUsername != null && !postUidOrUsername.isEmpty()) {
            UserCache.UserData cachedUser = UserCache.getUser(postUidOrUsername);

            if (cachedUser != null) {
                // TROVATO IN CACHE: Applica subito tutto senza usare internet
                holder.tvUser.setText(cachedUser.username);
                UserVerificationManager.setupVerificationBadge(holder.ivVerifiedBadge, cachedUser.isVerified);

                if (cachedUser.pfpUri != null && !cachedUser.pfpUri.isEmpty()) {
                    Glide.with(context)
                            .load(cachedUser.pfpUri)
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .error(android.R.drawable.sym_def_app_icon)
                            .circleCrop()
                            .into(holder.ivUserPfp);
                }
            } else {
                // NON TROVATO IN CACHE: Cerchiamo su Firestore il documento con questo UID
                FirebaseFirestore.getInstance().collection("users").document(postUidOrUsername).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                applicaDatiUtente(context, holder, postUidOrUsername, documentSnapshot);
                            } else {
                                // AUTO-HEALING: Se non lo trova come UID, forse è un vecchio post pre-migrazione!
                                // Lo cerchiamo tramite il campo "username"
                                FirebaseFirestore.getInstance().collection("users")
                                        .whereEqualTo("username", postUidOrUsername).limit(1).get()
                                        .addOnSuccessListener(querySnapshot -> {
                                            if (!querySnapshot.isEmpty()) {
                                                applicaDatiUtente(context, holder, postUidOrUsername, querySnapshot.getDocuments().get(0));
                                            } else {
                                                // L'utente si è cancellato dall'app
                                                UserCache.putUser(postUidOrUsername, "Utente Eliminato", "", false);
                                                holder.tvUser.setText("Utente Eliminato");
                                            }
                                        });
                            }
                        });
            }
        }

        // --- 3. Contenuto Post (Testo e Media) ---
        holder.tvText.setText(post.getText());
        holder.tvText.setVisibility(post.getText() != null && !post.getText().isEmpty() ? View.VISIBLE : View.GONE);

        if (post.getMediaUri() != null && !post.getMediaUri().isEmpty()) {
            try {
                Uri mediaUri = Uri.parse(post.getMediaUri());
                if (post.isVideo()) {
                    holder.videoView.setVisibility(View.VISIBLE);
                    holder.imageView.setVisibility(View.GONE);
                    holder.videoView.setVideoURI(mediaUri);
                    holder.videoView.setOnClickListener(v -> {
                        if (holder.videoView.isPlaying()) holder.videoView.pause();
                        else holder.videoView.start();
                    });
                } else {
                    holder.imageView.setVisibility(View.VISIBLE);
                    holder.videoView.setVisibility(View.GONE);

                    Glide.with(context).load(post.getMediaUri()).into(holder.imageView);

                    Activity activity = getActivitySafely(context);
                    if (activity != null) {
                        Zoomy.Builder builder = new Zoomy.Builder(activity)
                                .target(holder.imageView)
                                .enableImmersiveMode(false)
                                .animateZooming(true);
                        builder.register();
                    }
                }
            } catch (Exception e) {
                holder.imageView.setVisibility(View.GONE);
                holder.videoView.setVisibility(View.GONE);
            }
        } else {
            holder.imageView.setVisibility(View.GONE);
            holder.videoView.setVisibility(View.GONE);
        }

        // --- 4. Eliminazione Post ---
        boolean isMyPost = post.getUser() != null && post.getUser().equals(currentUser);
        if (isAdmin || isMyPost) {
            holder.btnDeletePost.setVisibility(View.VISIBLE);
            holder.btnDeletePost.setOnClickListener(v -> actionHandler.deletePost(post));
        } else {
            holder.btnDeletePost.setVisibility(View.GONE);
        }

        // --- 5. Modifica Post ---
        if (isMyPost) {
            holder.btnEditPost.setVisibility(View.VISIBLE);
            holder.btnEditPost.setOnClickListener(v ->
                    PostDialogHelper.showEditPostDialog(context, post, this, position)
            );
        } else {
            holder.btnEditPost.setVisibility(View.GONE);
        }

        // --- 6. Gestione Like ---
        boolean isLiked = post.getLikes().contains(currentUser);
        holder.ivLikeIcon.setColorFilter(isLiked ? Color.RED : Color.GRAY);
        holder.tvLikeCount.setText(String.valueOf(post.getLikes().size()));
        holder.tvLikeCount.setTextColor(isLiked ? Color.RED : Color.GRAY);
        holder.btnLikeArea.setOnClickListener(v -> actionHandler.toggleLike(post, position));

        // --- 7. Commenti ---
        int commentCount = post.getComments().size();
        holder.tvCommentCount.setText(String.valueOf(commentCount));
        View.OnClickListener openComments = v -> PostDialogHelper.showCommentsSheet(context, post, currentUser, this, position);
        holder.btnCommentArea.setOnClickListener(openComments);
        holder.tvCommentCount.setOnClickListener(openComments);

        // --- 8. Segnalazione ---
        holder.btnReportArea.setVisibility(View.VISIBLE);
        holder.btnReportArea.setOnClickListener(v -> PostDialogHelper.showReportDialog(context, post, currentUser));

        // --- 9. Condivisione Esterna ---
        holder.btnShareArea.setOnClickListener(v -> {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);

            String shareContent = "";
            if (post.getText() != null && !post.getText().isEmpty()) {
                shareContent += post.getText();
            }
            if (post.getMediaUri() != null && !post.getMediaUri().isEmpty()) {
                if (!shareContent.isEmpty()) shareContent += "\n\n";
                shareContent += "Guarda l'allegato: " + post.getMediaUri();
            }
            if (shareContent.isEmpty()) {
                // MODIFICA: Ora usa il VERO username grafico al posto dell'UID!
                shareContent = "Guarda questo post di " + holder.tvUser.getText().toString() + " su Spottio!";
            }

            sendIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
            sendIntent.setType("text/plain");

            Intent chooser = Intent.createChooser(sendIntent, "Condividi post tramite:");
            context.startActivity(chooser);
        });
    }

    // Metodo di appoggio per applicare i dati Firestore puliti all'interfaccia
    private void applicaDatiUtente(Context context, PostViewHolder holder, String cacheKey, DocumentSnapshot doc) {
        String realUsername = doc.getString("username");
        if(realUsername == null || realUsername.isEmpty()) realUsername = "Utente Sconosciuto";

        String realTimePfpUri = doc.getString("userPfUri");
        Boolean verifiedObj = doc.getBoolean("isVerified");
        boolean realTimeVerified = verifiedObj != null ? verifiedObj : false;

        // Salva in cache tutti i dati
        UserCache.putUser(cacheKey, realUsername, realTimePfpUri != null ? realTimePfpUri : "", realTimeVerified);

        // Aggiorna l'interfaccia
        holder.tvUser.setText(realUsername);
        UserVerificationManager.setupVerificationBadge(holder.ivVerifiedBadge, realTimeVerified);

        if (realTimePfpUri != null && !realTimePfpUri.isEmpty()) {
            Glide.with(context)
                    .load(realTimePfpUri)
                    .placeholder(android.R.drawable.sym_def_app_icon)
                    .error(android.R.drawable.sym_def_app_icon)
                    .circleCrop()
                    .into(holder.ivUserPfp);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    private Activity getActivitySafely(Context context) {
        while (context instanceof android.content.ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((android.content.ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}