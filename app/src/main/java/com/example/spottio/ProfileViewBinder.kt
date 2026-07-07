package com.example.spottio

import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProfileViewBinder(private val view: View) {

    val ivPfp: ImageView = view.findViewById(R.id.ivProfilePic)
    val tvBio: TextView = view.findViewById(R.id.tvBio)
    val tvFollowersCount: TextView = view.findViewById(R.id.tvFollowersCount)
    val tvFollowingCount: TextView = view.findViewById(R.id.tvFollowingCount)
    val btnFollow: Button? = view.findViewById(R.id.btnFollow)
    val btnSettings: ImageButton? = view.findViewById(R.id.btnSettings)
    val btnEditBioPencil: ImageButton? = view.findViewById(R.id.btnEditBioPencil)
    val rvMyPosts: RecyclerView = view.findViewById(R.id.rvMyPosts)
    val btnViewReports: Button? = view.findViewById(R.id.btnViewReports)
    val ivVerifiedBadge: ImageView? = view.findViewById(R.id.ivVerifiedBadge)
    val btnEditUsernamePencil: ImageButton? = view.findViewById(R.id.btnEditUsernamePencil)

    private val layoutFollowers: LinearLayout? = view.findViewById(R.id.layoutFollowers)
    private val layoutFollowing: LinearLayout? = view.findViewById(R.id.layoutFollowing)

    fun configureForUser(
        isMyProfile: Boolean,
        coordinator: ProfileCoordinator,
        viewModel: ProfileViewModel,
        currentUser: String,
        targetUser: String,
        uiManager: ProfileUIManager
    ) {
        val btnChat = view.findViewById<Button>(R.id.btnChatProfile)

        // RECUPERIAMO IL TASTO LOGOUT
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        if (isMyProfile) {
            btnFollow?.visibility = View.GONE
            btnChat?.visibility = View.GONE

            // FIX: RIATTIVIAMO IL CLICK PER IL LOGOUT!
            btnLogout?.setOnClickListener {
                coordinator.handleLogout(uiManager)
            }

            btnSettings?.visibility = View.VISIBLE
            btnSettings?.setOnClickListener { coordinator.navigateToSettings() }

            btnEditBioPencil?.visibility = View.VISIBLE
            btnEditBioPencil?.setOnClickListener {
                uiManager.showEditBioDialog(viewModel.bio.value) { newBio ->
                    viewModel.updateBio(currentUser, newBio)
                }
            }

            btnEditUsernamePencil?.visibility = View.VISIBLE
            btnEditUsernamePencil?.setOnClickListener {
                uiManager.showEditUsernameDialog(viewModel.username.value) { newUsername ->
                    viewModel.updateUsername(currentUser, newUsername)
                }
            }
        } else {
            btnSettings?.visibility = View.GONE
            btnEditBioPencil?.visibility = View.GONE
            btnEditUsernamePencil?.visibility = View.GONE

            btnChat?.setOnClickListener {
                val tvUsername = view.findViewById<TextView>(R.id.tvProfileName)
                val targetName = tvUsername?.text?.toString() ?: "Chat"
                coordinator.navigateToChat(targetName)
            }

            btnFollow?.setOnClickListener {
                viewModel.toggleFollow(targetUser, currentUser, viewModel.isFollowing.value == true)
            }
        }
    }

    fun setupClickListeners(coordinator: ProfileCoordinator, viewModel: ProfileViewModel) {
        layoutFollowers?.setOnClickListener {
            coordinator.navigateToFollowers(viewModel.followersList.value)
        }

        layoutFollowing?.setOnClickListener {
            coordinator.navigateToFollowing(viewModel.followingList.value)
        }
    }
}