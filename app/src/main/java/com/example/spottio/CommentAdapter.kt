package com.example.spottio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.spottio.CommentAdapter.CommentViewHolder

class CommentAdapter(private val commentList: List<Comment>?) :
    RecyclerView.Adapter<CommentViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList!![position]

        holder.tvUser.text = comment.author
        holder.tvText.text = comment.text
        holder.tvDate.text = comment.formattedDate
    }

    override fun getItemCount(): Int {
        return if ((commentList != null)) commentList.size else 0
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvUser: TextView = itemView.findViewById(R.id.tvCommentUser)
        var tvText: TextView = itemView.findViewById(R.id.tvCommentText)
        var tvDate: TextView = itemView.findViewById(R.id.tvCommentDate)
    }
}