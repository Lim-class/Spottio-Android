package com.example.spottio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment

abstract class BaseUserListFragment : Fragment() {

    protected lateinit var adapter: UserAdapter
    protected val displayedList = ArrayList<String>()
    protected lateinit var listView: ListView
    protected lateinit var etSearch: EditText
    protected lateinit var tvTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_list, container, false)

        listView = view.findViewById(R.id.lvUsers)
        etSearch = view.findViewById(R.id.etSearchUser)
        tvTitle = view.findViewById(R.id.tvListTitle)

        adapter = UserAdapter(requireContext(), displayedList)

        // FIX: Usiamo un'implementazione esplicita per evitare l'errore del ritorno "Int"
        adapter.setOnUserClickListener(object : UserAdapter.OnUserClickListener {
            override fun onUserClick(uidOrUsername: String) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment.newInstance(uidOrUsername))
                    .addToBackStack(null)
                    .commit()
            }
        })

        listView.adapter = adapter
        setupFragment()

        return view
    }

    protected abstract fun setupFragment()
}