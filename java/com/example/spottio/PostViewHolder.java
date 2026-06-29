package com.example.spottio;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PostViewHolder extends RecyclerView.ViewHolder {
    public TextView tvUser, tvDate, tvText, tvLikeCount, tvCommentCount;
    public ImageView imageView, ivLikeIcon, ivUserPfp, ivVerifiedBadge, ivShareIcon;
    public VideoView videoView;
    public ImageButton btnDeletePost;
    public ImageButton btnEditPost;
    public LinearLayout btnLikeArea, btnCommentArea, btnReportArea, btnShareArea;

    public PostViewHolder(@NonNull View itemView) {
        super(itemView);
        tvUser = itemView.findViewById(R.id.tvUser);
        tvDate = itemView.findViewById(R.id.tvDate);
        tvText = itemView.findViewById(R.id.tvText);
        tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
        tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
        imageView = itemView.findViewById(R.id.imageViewPost);
        videoView = itemView.findViewById(R.id.videoViewPost);
        ivUserPfp = itemView.findViewById(R.id.ivUserPfp);
        ivLikeIcon = itemView.findViewById(R.id.ivLikeIcon);
        ivVerifiedBadge = itemView.findViewById(R.id.ivVerifiedBadge);

        btnDeletePost = itemView.findViewById(R.id.btnDeletePost);
        btnEditPost = itemView.findViewById(R.id.btnEditPost);
        btnLikeArea = itemView.findViewById(R.id.btnLikeArea);
        btnCommentArea = itemView.findViewById(R.id.btnCommentArea);
        btnReportArea = itemView.findViewById(R.id.btnReportArea);

        // --- COLLEGAMENTO AREA CONDIVISIONE ---
        btnShareArea = itemView.findViewById(R.id.btnShareArea);
        ivShareIcon = itemView.findViewById(R.id.ivShareIcon);
    }
}