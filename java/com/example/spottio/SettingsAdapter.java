package com.example.spottio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder> {

    public enum SettingType {
        PASSWORD, CAMBIO_EMAIL, COLORE_SFONDO, SCARICA_APK, PRIVACY_PROFILO, ESPORTA_DATI, STORIA, INFORMATIVA_PRIVACY, POLICY_COMMUNITY, ELIMINA_ACCOUNT
    }

    public static class SettingItem {
        public String title;
        public SettingType type;

        public SettingItem(String title, SettingType type) {
            this.title = title;
            this.type = type;
        }
    }

    private final List<SettingItem> originalList;
    private final List<SettingItem> filteredList;
    private int expandedPosition = -1;
    private final OnBindLayoutListener bindListener;

    public interface OnBindLayoutListener {
        void onBindInnerLayout(SettingType type, View innerView, SettingItem item);
    }

    public SettingsAdapter(List<SettingItem> list, OnBindLayoutListener bindListener) {
        this.originalList = list;
        this.filteredList = new ArrayList<>(list);
        this.bindListener = bindListener;
    }

    @NonNull
    @Override
    public SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_setting_accordion, parent, false);
        return new SettingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsViewHolder holder, int position) {
        var item = filteredList.get(position);
        holder.textTitle.setText(item.title);

        // Previene la sovrapposizione visiva ripulendo i vecchi componenti riciclati
        holder.layoutContent.removeAllViews();

        // MAGIA JAVA 14+: Switch Expression (Restituisce direttamente un valore)
        var innerLayoutRes = switch (item.type) {
            case PASSWORD -> R.layout.layout_inner_password;
            case CAMBIO_EMAIL -> R.layout.layout_inner_cambio_email;
            case PRIVACY_PROFILO -> R.layout.layout_inner_privacy_profilo;
            case ESPORTA_DATI -> R.layout.layout_inner_esporta_dati;
            default -> R.layout.layout_inner_info;
        };

        var innerView = LayoutInflater.from(holder.itemView.getContext()).inflate(innerLayoutRes, holder.layoutContent, false);
        holder.layoutContent.addView(innerView);

        if (bindListener != null) {
            bindListener.onBindInnerLayout(item.type, innerView, item);
        }

        // Logica di espansione esclusiva (Accordion pattern)
        var isExpanded = (position == expandedPosition);
        holder.layoutContent.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.imageArrow.setRotation(isExpanded ? 180f : 0f);

        holder.layoutHeader.setOnClickListener(v -> {
            var currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            if (isExpanded) {
                expandedPosition = -1;
                notifyItemChanged(currentPos);
            } else {
                var prevExpanded = expandedPosition;
                expandedPosition = currentPos;
                if (prevExpanded != -1) {
                    notifyItemChanged(prevExpanded);
                }
                notifyItemChanged(expandedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // Filtraggio dei nodi in tempo reale
    public void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            var query = text.toLowerCase().trim();
            for (var item : originalList) {
                if (item.title.toLowerCase().contains(query)) {
                    filteredList.add(item);
                }
            }
        }
        expandedPosition = -1; // Reset dello stato di espansione per evitare artefatti grafici
        notifyDataSetChanged();
    }

    static class SettingsViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutHeader;
        TextView textTitle;
        ImageView imageArrow;
        FrameLayout layoutContent;

        public SettingsViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutHeader = itemView.findViewById(R.id.layoutHeader);
            textTitle = itemView.findViewById(R.id.textTitle);
            imageArrow = itemView.findViewById(R.id.imageArrow);
            layoutContent = itemView.findViewById(R.id.layoutContent);
        }
    }
}