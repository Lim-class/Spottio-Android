package com.example.spottio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class GroupAdapter(context: Context, groups: List<GroupInfo>) : ArrayAdapter<GroupInfo>(context, 0, groups) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // L'operatore Elvis (?:) dice: usa convertView, se è nullo inflata la vista
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)

        val group = getItem(position)
        val text = view.findViewById<TextView>(android.R.id.text1)

        if (group != null && text != null) {
            text.text = group.name
            text.textSize = 18f
            text.setPadding(24, 32, 24, 32)
        }

        return view
    }
}