package com.example.spottio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SettingsAdapter(
    list: List<SettingItem>,
    private val bindListener: OnBindLayoutListener?
) : RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    enum class SettingType {
        PASSWORD, CAMBIO_EMAIL, COLORE_SFONDO, SCARICA_APK, PRIVACY_PROFILO, ESPORTA_DATI, STORIA, INFORMATIVA_PRIVACY, POLICY_COMMUNITY, ELIMINA_ACCOUNT
    }

    data class SettingItem(
        val title: String,
        val type: SettingType
    )

    private val originalList: List<SettingItem> = list
    private val filteredList: MutableList<SettingItem> = ArrayList(list)
    private var expandedPosition = -1

    interface OnBindLayoutListener {
        fun onBindInnerLayout(type: SettingType, innerView: View, item: SettingItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_setting_accordion, parent, false)
        return SettingsViewHolder(view)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val item = filteredList[position]
        holder.textTitle.text = item.title

        // Previene la sovrapposizione visiva ripulendo i vecchi componenti riciclati
        holder.layoutContent.removeAllViews()

        val innerLayoutRes = when (item.type) {
            SettingType.PASSWORD -> R.layout.layout_inner_password
            SettingType.CAMBIO_EMAIL -> R.layout.layout_inner_cambio_email
            SettingType.PRIVACY_PROFILO -> R.layout.layout_inner_privacy_profilo
            SettingType.ESPORTA_DATI -> R.layout.layout_inner_esporta_dati
            else -> R.layout.layout_inner_info
        }

        val innerView = LayoutInflater.from(holder.itemView.context).inflate(innerLayoutRes, holder.layoutContent, false)
        holder.layoutContent.addView(innerView)

        bindListener?.onBindInnerLayout(item.type, innerView, item)

        // Logica di espansione esclusiva (Accordion pattern)
        val isExpanded = (position == expandedPosition)
        holder.layoutContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.imageArrow.rotation = if (isExpanded) 180f else 0f

        holder.layoutHeader.setOnClickListener {
            @Suppress("DEPRECATION")
            val currentPos = holder.adapterPosition // SOLUZIONE
            if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener

            if (isExpanded) {
                expandedPosition = -1
                notifyItemChanged(currentPos)
            } else {
                val prevExpanded = expandedPosition
                expandedPosition = currentPos
                if (prevExpanded != -1) {
                    notifyItemChanged(prevExpanded)
                }
                notifyItemChanged(expandedPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    // Filtraggio dei nodi in tempo reale
    fun filter(text: String) {
        filteredList.clear()
        if (text.isEmpty()) {
            filteredList.addAll(originalList)
        } else {
            val query = text.lowercase().trim()
            for (item in originalList) {
                if (item.title.lowercase().contains(query)) {
                    filteredList.add(item)
                }
            }
        }
        expandedPosition = -1 // Reset dello stato di espansione per evitare artefatti grafici
        notifyDataSetChanged()
    }

    class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutHeader: LinearLayout = itemView.findViewById(R.id.layoutHeader)
        val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        val imageArrow: ImageView = itemView.findViewById(R.id.imageArrow)
        val layoutContent: FrameLayout = itemView.findViewById(R.id.layoutContent)
    }
}