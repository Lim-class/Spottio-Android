package com.example.spottio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth

class ProfileUIManager(private val context: Context) {

    fun configureProfileLayout(isMyProfile: Boolean, view: View) {
        view.findViewById<View>(R.id.btnLogout)?.visibility = if (isMyProfile) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.btnChatProfile)?.visibility = if (isMyProfile) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.btnFollow)?.visibility = if (isMyProfile) View.GONE else View.VISIBLE
    }

    fun updateFollowButton(isFollowing: Boolean, btnFollow: Button?) {
        btnFollow?.apply {
            if (isFollowing) {
                // FIX: Usiamo le stringhe originali del Java!
                text = context.getString(R.string.btn_following)
                setBackgroundColor(Color.LTGRAY)
            } else {
                text = context.getString(R.string.btn_follow_action)
                setBackgroundColor(Color.parseColor("#002D57"))
            }
        }
    }

    fun performLogout(activity: Activity) {
        // FIX: Usiamo stringhe testuali fisse per non dipendere da string.xml
        AlertDialog.Builder(activity)
            .setTitle("Logout")
            .setMessage("Vuoi davvero uscire?")
            .setPositiveButton("Sì") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                activity.getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE).edit().clear().apply()

                val intent = Intent(activity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                activity.startActivity(intent)
                activity.finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    fun showEditBioDialog(currentBio: String?, onBioEdited: (String) -> Unit) {
        val input = EditText(context).apply { setText(currentBio) }

        AlertDialog.Builder(context)
            .setTitle("Modifica Bio")
            .setView(input)
            .setPositiveButton(context.getString(R.string.btn_save)) { _, _ ->
                onBioEdited(input.text.toString().trim())
                Toast.makeText(context, context.getString(R.string.toast_saving), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(context.getString(R.string.btn_cancel), null)
            .show()
    }

    fun showEditProfileImageDialog(currentUrl: String?, onImageUrlEdited: (String) -> Unit) {
        val input = EditText(context).apply {
            setText(currentUrl)
            hint = "https://..."
        }

        AlertDialog.Builder(context)
            .setTitle("Modifica Foto Profilo")
            .setMessage("Inserisci il nuovo URL dell'immagine:")
            .setView(input)
            .setPositiveButton(context.getString(R.string.btn_save)) { _, _ ->
                onImageUrlEdited(input.text.toString().trim())
                Toast.makeText(context, context.getString(R.string.toast_saving), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(context.getString(R.string.btn_cancel), null)
            .show()
    }

    fun loadProfileImage(url: String?, ivPfp: ImageView?) {
        if (ivPfp == null) return
        if (!url.isNullOrEmpty()) {
            Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .placeholder(android.R.drawable.sym_def_app_icon)
                .error(android.R.drawable.sym_def_app_icon)
                .into(ivPfp)
        } else {
            ivPfp.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    fun showEditUsernameDialog(currentUsername: String?, onUsernameEdited: (String) -> Unit) {
        val input = EditText(context).apply { setText(currentUsername) }

        AlertDialog.Builder(context)
            .setTitle("Modifica Username")
            .setView(input)
            .setPositiveButton(context.getString(R.string.btn_save)) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    onUsernameEdited(newName)
                    Toast.makeText(context, context.getString(R.string.toast_saving), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "L'username non può essere vuoto", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(context.getString(R.string.btn_cancel), null)
            .show()
    }
}