package com.example.spottio

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView

// Aggiunto @JvmField ovunque per rendere i campi visibili direttamente da Java
class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @JvmField val tvUser: TextView = itemView.findViewById(R.id.tvUser)
    @JvmField val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    @JvmField val tvText: TextView = itemView.findViewById(R.id.tvText)
    @JvmField val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
    @JvmField val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)

    @JvmField val imageView: ImageView = itemView.findViewById(R.id.imageViewPost)
    @JvmField val ivLikeIcon: ImageView = itemView.findViewById(R.id.ivLikeIcon)
    @JvmField val ivUserPfp: ImageView = itemView.findViewById(R.id.ivUserPfp)
    @JvmField val ivVerifiedBadge: ImageView = itemView.findViewById(R.id.ivVerifiedBadge)
    @JvmField val ivShareIcon: ImageView = itemView.findViewById(R.id.ivShareIcon)

    @JvmField val videoView: VideoView = itemView.findViewById(R.id.videoViewPost)

    @JvmField val btnDeletePost: ImageButton = itemView.findViewById(R.id.btnDeletePost)
    @JvmField val btnEditPost: ImageButton = itemView.findViewById(R.id.btnEditPost)

    @JvmField val btnLikeArea: LinearLayout = itemView.findViewById(R.id.btnLikeArea)
    @JvmField val btnCommentArea: LinearLayout = itemView.findViewById(R.id.btnCommentArea)
    @JvmField val btnReportArea: LinearLayout = itemView.findViewById(R.id.btnReportArea)
    @JvmField val btnShareArea: LinearLayout = itemView.findViewById(R.id.btnShareArea)
}