package com.example.spottio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class GroupAdapter(context: Context, groups: List<GroupInfo>) : ArrayAdapter<GroupInfo>(context, 0, groups) {

    // pattern ViewHolder per non ripetere il findViewById del testo del gruppo
    private class GroupViewHolder(view: View) {
        val text: TextView = view.findViewById(android.R.id.text1)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: GroupViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
            holder = GroupViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as GroupViewHolder
        }

        val group = getItem(position)

        if (group != null) {
            holder.text.text = group.name
            holder.text.textSize = 18f
            holder.text.setPadding(24, 32, 24, 32)
        }

        return view
    }
}