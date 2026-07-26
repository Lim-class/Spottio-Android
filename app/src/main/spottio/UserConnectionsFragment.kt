package com.example.spottio

import android.os.Bundle
import android.view.View

class UserConnectionsFragment : BaseUserListFragment() {

    companion object {
        // Passiamo sia il titolo che la lista
        @JvmStatic
        fun newInstance(title: String, users: ArrayList<String>) = UserConnectionsFragment().apply {
            arguments = Bundle().apply {
                putString("TITLE", title)
                putStringArrayList("USER_LIST", users)
            }
        }
    }

    override fun setupFragment() {
        etSearch.visibility = View.GONE // Nasconde la barra di ricerca

        // Recupero sicuro degli argomenti usando 'let'
        arguments?.let { args ->

            // Imposta il titolo dinamicamente
            tvTitle.text = args.getString("TITLE", "")

            // Carica la lista
            val userList = args.getStringArrayList("USER_LIST")
            if (userList != null) {
                displayedList.clear()
                displayedList.addAll(userList)
                adapter.notifyDataSetChanged()
            }
        }
    }
}