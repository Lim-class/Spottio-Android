package com.example.spottio

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class PostAdapter(
    private var postList: List<Post>,
    private val currentUser: String,
    private val isAdmin: Boolean,
    private val interactionListener: PostInteractionListener
) : RecyclerView.Adapter<PostViewHolder>() {

    interface PostInteractionListener {
        fun onUserClick(userId: String)
        fun onEditClick(post: Post, position: Int)
        fun onCommentClick(post: Post, position: Int)
        fun onReportClick(post: Post)
        fun onShareClick(post: Post, shareContent: String)
        fun onImageZoomClick(imageView: View, mediaUri: String)
        fun onLikeClick(post: Post, position: Int)
        fun onDeleteClick(post: Post, position: Int)
    }

    private lateinit var context: Context

    fun setPostList(postList: List<Post>) {
        this.postList = postList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(post.timestamp)
        holder.tvDate.visibility = View.VISIBLE

        val postUidOrUsername = post.user
        val cachedUser = UserCache.getUser(postUidOrUsername)

        if (cachedUser != null) {
            UserGraphicsHelper.applicaGrafica(context, holder.tvUser, holder.ivUserPfp, holder.ivVerifiedBadge, cachedUser)
        } else {
            UserGraphicsHelper.applicaGrafica(context, holder.tvUser, holder.ivUserPfp, holder.ivVerifiedBadge, null)
            UserCache.fetchUserAsync(postUidOrUsername) {
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) notifyItemChanged(currentPos)
            }
        }

        val profileClickListener = View.OnClickListener { interactionListener.onUserClick(postUidOrUsername) }
        holder.ivUserPfp.setOnClickListener(profileClickListener)
        holder.tvUser.setOnClickListener(profileClickListener)

        holder.tvText.text = post.text
        holder.tvText.visibility = if (!post.text.isNullOrEmpty()) View.VISIBLE else View.GONE

        if (!post.mediaUri.isNullOrEmpty()) {
            val mediaUri = Uri.parse(post.mediaUri)
            if (post.isVideo) {
                holder.videoView.visibility = View.VISIBLE
                holder.imageView.visibility = View.GONE
                holder.videoView.setVideoURI(mediaUri)
                holder.videoView.setOnClickListener {
                    if (holder.videoView.isPlaying) holder.videoView.pause()
                    else holder.videoView.start()
                }
            } else {
                holder.imageView.visibility = View.VISIBLE
                holder.videoView.visibility = View.GONE
                Glide.with(context).load(post.mediaUri).into(holder.imageView)
                holder.imageView.setOnClickListener {
                    post.mediaUri?.let { uri -> interactionListener.onImageZoomClick(holder.imageView, uri) }
                }
            }
        } else {
            holder.imageView.visibility = View.GONE
            holder.videoView.visibility = View.GONE
        }

        val isMyPost = postUidOrUsername == currentUser
        if (isMyPost || isAdmin) {
            holder.btnDeletePost.visibility = View.VISIBLE
            holder.btnDeletePost.setOnClickListener { interactionListener.onDeleteClick(post, holder.adapterPosition) }
        } else {
            holder.btnDeletePost.visibility = View.GONE
        }

        if (isMyPost && post.mediaUri.isNullOrEmpty()) {
            holder.btnEditPost.visibility = View.VISIBLE
            holder.btnEditPost.setOnClickListener { interactionListener.onEditClick(post, holder.adapterPosition) }
        } else {
            holder.btnEditPost.visibility = View.GONE
        }

        val isLiked = post.likes.contains(currentUser)
        holder.ivLikeIcon.setColorFilter(if (isLiked) Color.RED else Color.GRAY)
        holder.tvLikeCount.text = post.likes.size.toString()
        holder.tvLikeCount.setTextColor(if (isLiked) Color.RED else Color.GRAY)

        holder.btnLikeArea.setOnClickListener { interactionListener.onLikeClick(post, holder.adapterPosition) }

        holder.tvCommentCount.text = post.comments.size.toString()
        val openComments = View.OnClickListener { interactionListener.onCommentClick(post, holder.adapterPosition) }
        holder.btnCommentArea.setOnClickListener(openComments)
        holder.tvCommentCount.setOnClickListener(openComments)

        holder.btnReportArea.visibility = View.VISIBLE
        holder.btnReportArea.setOnClickListener { interactionListener.onReportClick(post) }

        holder.btnShareArea.setOnClickListener {
            var shareContent = ""
            if (!post.text.isNullOrEmpty()) shareContent += post.text
            if (!post.mediaUri.isNullOrEmpty()) {
                if (shareContent.isNotEmpty()) shareContent += "\n\n"
                shareContent += "Guarda l'allegato: ${post.mediaUri}"
            }
            if (shareContent.isEmpty()) shareContent = "Guarda questo post di ${holder.tvUser.text} su Spottio!"
            interactionListener.onShareClick(post, shareContent)
        }
    }

    override fun getItemCount(): Int = postList.size
}