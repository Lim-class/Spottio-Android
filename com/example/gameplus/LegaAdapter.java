package com.example.gameplus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LegaAdapter extends RecyclerView.Adapter<LegaAdapter.LegaViewHolder> {

    private List<Lega> listaLeghe;
    private OnLegaClickListener listener;

    // Interfaccia per gestire i click (Entra o Apri)
    public interface OnLegaClickListener {
        void onLegaClick(Lega lega);
    }

    public LegaAdapter(List<Lega> listaLeghe, OnLegaClickListener listener) {
        this.listaLeghe = listaLeghe;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LegaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lega, parent, false);
        return new LegaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LegaViewHolder holder, int position) {
        Lega lega = listaLeghe.get(position);

        holder.tvNome.setText(lega.getNome());
        holder.tvDettagli.setText("Budget: " + lega.getCreditiIniziali());

        // Gestisce il click sul pulsante o sull'intera riga
        holder.btnEntra.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLegaClick(lega);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLegaClick(lega);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaLeghe.size();
    }

    public static class LegaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNome, tvDettagli;
        Button btnEntra;

        public LegaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNome = itemView.findViewById(R.id.tvNomeLegaItem);
            tvDettagli = itemView.findViewById(R.id.tvDettagliLega);
            btnEntra = itemView.findViewById(R.id.btnEntraLega);
        }
    }
}