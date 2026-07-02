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
    private val interactionListener: PostInteractionListener?
) : RecyclerView.Adapter<PostViewHolder>() {

    interface PostInteractionListener {
        fun onUserClick(userId: String)
        fun onEditClick(post: Post, position: Int)
        fun onCommentClick(post: Post, position: Int)
        fun onReportClick(post: Post)
        fun onShareClick(post: Post, shareContent: String)
        fun onImageZoomClick(imageView: View, mediaUri: String)
    }

    private lateinit var actionHandler: PostActionHandler
    private lateinit var context: Context

    fun setPostList(postList: List<Post>) {
        this.postList = postList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        context = parent.context
        actionHandler = PostActionHandler(context, this, currentUser)
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        // 1. Data Formattata (Gestione sicura senza if multipli)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvDate.text = sdf.format(post.timestamp)
        holder.tvDate.visibility = View.VISIBLE

        // 2. Utente e Foto Profilo
        val postUidOrUsername = post.user
        val cachedUser = UserCache.getUser(postUidOrUsername)

        if (cachedUser != null) {
            UserGraphicsHelper.applicaGrafica(context, holder.tvUser, holder.ivUserPfp, holder.ivVerifiedBadge, cachedUser)
        } else {
            UserGraphicsHelper.applicaGrafica(context, holder.tvUser, holder.ivUserPfp, holder.ivVerifiedBadge, null)
            UserCache.fetchUserAsync(postUidOrUsername) {
                // Notifica aggiornamento specifico per non ricaricare tutta la lista e farla laggare
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) notifyItemChanged(currentPos)
            }
        }

        val profileClickListener = View.OnClickListener {
            interactionListener?.onUserClick(postUidOrUsername)
        }
        holder.ivUserPfp.setOnClickListener(profileClickListener)
        holder.tvUser.setOnClickListener(profileClickListener)

        // 3. Testo del post
        holder.tvText.text = post.text
        holder.tvText.visibility = if (!post.text.isNullOrEmpty()) View.VISIBLE else View.GONE

        // 4. Media (Foto o Video)
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
                    post.mediaUri?.let { uri ->
                        interactionListener?.onImageZoomClick(holder.imageView, uri)
                    }
                }
            }
        } else {
            holder.imageView.visibility = View.GONE
            holder.videoView.visibility = View.GONE
        }

        // 5. Opzioni Proprietario / Admin
        val isMyPost = postUidOrUsername == currentUser
        if (isMyPost || isAdmin) {
            holder.btnDeletePost.visibility = View.VISIBLE
            holder.btnDeletePost.setOnClickListener { actionHandler.deletePost(post) }
        } else {
            holder.btnDeletePost.visibility = View.GONE
        }

        // Mostra tasto Edit solo se è il mio post E non ha allegati
        if (isMyPost && post.mediaUri.isNullOrEmpty()) {
            holder.btnEditPost.visibility = View.VISIBLE
            holder.btnEditPost.setOnClickListener {
                interactionListener?.onEditClick(post, holder.adapterPosition)
            }
        } else {
            holder.btnEditPost.visibility = View.GONE
        }

        // 6. Like
        val isLiked = post.likes.contains(currentUser)
        holder.ivLikeIcon.setColorFilter(if (isLiked) Color.RED else Color.GRAY)
        holder.tvLikeCount.text = post.likes.size.toString()
        holder.tvLikeCount.setTextColor(if (isLiked) Color.RED else Color.GRAY)
        holder.btnLikeArea.setOnClickListener { actionHandler.toggleLike(post, holder.adapterPosition) }

        // 7. Commenti
        holder.tvCommentCount.text = post.comments.size.toString()
        val openComments = View.OnClickListener {
            interactionListener?.onCommentClick(post, holder.adapterPosition)
        }
        holder.btnCommentArea.setOnClickListener(openComments)
        holder.tvCommentCount.setOnClickListener(openComments)

        // 8. Segnalazione
        holder.btnReportArea.visibility = View.VISIBLE
        holder.btnReportArea.setOnClickListener {
            interactionListener?.onReportClick(post)
        }

        // 9. Condivisione Esterna
        holder.btnShareArea.setOnClickListener {
            var shareContent = ""
            if (!post.text.isNullOrEmpty()) shareContent += post.text
            if (!post.mediaUri.isNullOrEmpty()) {
                if (shareContent.isNotEmpty()) shareContent += "\n\n"
                shareContent += "Guarda l'allegato: ${post.mediaUri}"
            }
            if (shareContent.isEmpty()) {
                shareContent = "Guarda questo post di ${holder.tvUser.text} su Spottio!"
            }

            interactionListener?.onShareClick(post, shareContent)
        }
    }

    override fun getItemCount(): Int = postList.size
}