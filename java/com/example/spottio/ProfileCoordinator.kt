package com.example.spottio

import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment

class ProfileCoordinator(
    private val fragment: Fragment,
    private val currentUser: String,
    private val targetUser: String?
) {

    fun isMyProfile(): Boolean = targetUser != null && targetUser.equals(currentUser, ignoreCase = true)

    fun navigateToFollowers(followers: List<String>?) {
        if (!followers.isNullOrEmpty()) {
            val titolo = fragment.context?.getString(R.string.label_followers) ?: "Followers"
            replaceFragment(UserConnectionsFragment.newInstance(titolo, ArrayList(followers)))
        } else {
            Toast.makeText(fragment.context, "Nessun follower", Toast.LENGTH_SHORT).show()
        }
    }

    fun navigateToFollowing(following: List<String>?) {
        if (!following.isNullOrEmpty()) {
            val titolo = fragment.context?.getString(R.string.label_following) ?: "Seguiti"
            replaceFragment(UserConnectionsFragment.newInstance(titolo, ArrayList(following)))
        } else {
            Toast.makeText(fragment.context, "Nessun utente seguito", Toast.LENGTH_SHORT).show()
        }
    }

    fun navigateToChat(titleName: String?) {
        val chatFrag = ChatFragment.newInstance(targetUser ?: "", false, titleName)
        replaceFragment(chatFrag)
    }

    fun navigateToSettings() {
        replaceFragment(SettingsFragment())
    }

    fun changeLanguage(langTag: String, flag: String, name: String) {
        LanguageHelper.setLanguage(fragment.requireContext(), langTag, flag, name)
        fragment.activity?.recreate()
    }

    fun handleLogout(uiManager: ProfileUIManager) {
        fragment.activity?.let { uiManager.performLogout(it) }
    }

    fun pickProfileImage(launcher: ActivityResultLauncher<String>) {
        launcher.launch("image/*")
    }

    private fun replaceFragment(newFragment: Fragment) {
        fragment.parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, newFragment)
            .addToBackStack(null)
            .commit()
    }

    fun navigateToAdminReports() {
        fragment.activity?.let {
            it.startActivity(Intent(it, AdminReportsActivity::class.java))
        }
    }
}