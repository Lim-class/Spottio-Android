package com.example.spottio;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostViewHolder> {

    public interface PostInteractionListener {
        void onUserClick(String userId);
        void onEditClick(Post post, int position);
        void onCommentClick(Post post, int position);
        void onReportClick(Post post);
        void onShareClick(Post post, String shareContent);
        void onImageZoomClick(View imageView, String mediaUri);
    }

    private List<Post> postList;
    private final String currentUser;
    private final boolean isAdmin;
    private PostActionHandler actionHandler;
    private final PostInteractionListener interactionListener;

    public PostAdapter(List<Post> postList, String currentUser, boolean isAdmin, PostInteractionListener listener) {
        this.postList = postList;
        this.currentUser = currentUser;
        this.isAdmin = isAdmin;
        this.interactionListener = listener;
    }

    public void setPostList(List<Post> newPosts) {
        this.postList = newPosts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        var post = postList.get(position);
        var context = holder.itemView.getContext();

        if (actionHandler == null) {
            actionHandler = new PostActionHandler(context, this, currentUser);
        }

        // --- 1. Data di pubblicazione ---
        var timestamp = post.getTimestamp();
        if (timestamp != null) {
            var sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvDate.setText(sdf.format(timestamp));
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        // --- 2. Dati Dinamici (Username, Foto Profilo) ---
        var postUidOrUsername = post.getUser();

        if (postUidOrUsername != null && !postUidOrUsername.isEmpty()) {
            var cachedUser = UserCache.getUser(postUidOrUsername);
            if (cachedUser != null) {
                UserGraphicsHelper.applicaGrafica(context, holder.tvUser, holder.ivUserPfp, holder.ivVerifiedBadge, cachedUser);
            } else {
                UserGraphicsHelper.applicaGrafica(context, holder.tvUser, holder.ivUserPfp, holder.ivVerifiedBadge, null);
                UserCache.fetchUserAsync(postUidOrUsername, () -> {
                    var currentPos = holder.getAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        notifyItemChanged(currentPos);
                    }
                });
            }

            // Nota: Le espressioni lambda tipizzate come listener non possono usare var a sinistra
            View.OnClickListener profileClickListener = v -> {
                if (interactionListener != null) interactionListener.onUserClick(postUidOrUsername);
            };
            holder.ivUserPfp.setOnClickListener(profileClickListener);
            holder.tvUser.setOnClickListener(profileClickListener);
        }

        // --- 3. Contenuto Post (Testo e Media) ---
        holder.tvText.setText(post.getText());
        holder.tvText.setVisibility(post.getText() != null && !post.getText().isEmpty() ? View.VISIBLE : View.GONE);

        if (post.getMediaUri() != null && !post.getMediaUri().isEmpty()) {
            try {
                var mediaUri = Uri.parse(post.getMediaUri());
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

                    holder.imageView.setOnClickListener(v -> {
                        if (interactionListener != null) interactionListener.onImageZoomClick(holder.imageView, post.getMediaUri());
                    });
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
        var isMyPost = post.getUser() != null && post.getUser().equals(currentUser);
        if (isAdmin || isMyPost) {
            holder.btnDeletePost.setVisibility(View.VISIBLE);
            holder.btnDeletePost.setOnClickListener(v -> actionHandler.deletePost(post));
        } else {
            holder.btnDeletePost.setVisibility(View.GONE);
        }

        // --- 5. Modifica Post ---
        if (isMyPost) {
            holder.btnEditPost.setVisibility(View.VISIBLE);
            holder.btnEditPost.setOnClickListener(v -> {
                if (interactionListener != null) interactionListener.onEditClick(post, position);
            });
        } else {
            holder.btnEditPost.setVisibility(View.GONE);
        }

        // --- 6. Gestione Like ---
        var isLiked = post.getLikes().contains(currentUser);
        holder.ivLikeIcon.setColorFilter(isLiked ? Color.RED : Color.GRAY);
        holder.tvLikeCount.setText(String.valueOf(post.getLikes().size()));
        holder.tvLikeCount.setTextColor(isLiked ? Color.RED : Color.GRAY);
        holder.btnLikeArea.setOnClickListener(v -> actionHandler.toggleLike(post, position));

        // --- 7. Commenti ---
        var commentCount = post.getComments().size();
        holder.tvCommentCount.setText(String.valueOf(commentCount));
        View.OnClickListener openComments = v -> {
            if (interactionListener != null) interactionListener.onCommentClick(post, position);
        };
        holder.btnCommentArea.setOnClickListener(openComments);
        holder.tvCommentCount.setOnClickListener(openComments);

        // --- 8. Segnalazione ---
        holder.btnReportArea.setVisibility(View.VISIBLE);
        holder.btnReportArea.setOnClickListener(v -> {
            if (interactionListener != null) interactionListener.onReportClick(post);
        });

        // --- 9. Condivisione Esterna ---
        holder.btnShareArea.setOnClickListener(v -> {
            String shareContent = "";
            if (post.getText() != null && !post.getText().isEmpty()) shareContent += post.getText();
            if (post.getMediaUri() != null && !post.getMediaUri().isEmpty()) {
                if (!shareContent.isEmpty()) shareContent += "\n\n";
                shareContent += "Guarda l'allegato: " + post.getMediaUri();
            }
            if (shareContent.isEmpty()) shareContent = "Guarda questo post di " + holder.tvUser.getText().toString() + " su Spottio!";

            if (interactionListener != null) interactionListener.onShareClick(post, shareContent);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }
}