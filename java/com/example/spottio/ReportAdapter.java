package com.example.spottio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<Report> reportList;
    private Context context;
    private FirebaseFirestore db;

    public ReportAdapter(List<Report> reportList) {
        this.reportList = reportList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.tvReason.setText("Motivo: " + report.getReason());
        holder.tvReporter.setText("Segnalato da: " + report.getReporterUser());
        holder.tvReportedUser.setText("Autore post: " + report.getPostAuthor());

        // GESTIONE DEI NUOVI CAMPI
        // Controllo se sono null nel caso ci siano vecchi documenti sul database senza questi campi
        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText("Dettaglio: " + report.getDescription());
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        if (report.getPostText() != null && !report.getPostText().isEmpty()) {
            holder.tvPostText.setVisibility(View.VISIBLE);
            holder.tvPostText.setText("Contenuto: \n" + report.getPostText());
        } else {
            holder.tvPostText.setVisibility(View.GONE);
        }

        // AZIONE: Ignora (Cancella solo la segnalazione)
        holder.btnIgnore.setOnClickListener(v -> {
            db.collection("reports").document(report.getReportId())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Segnalazione rimossa", Toast.LENGTH_SHORT).show());
        });

        // AZIONE: Elimina Post (Cancella il Post E la segnalazione)
        holder.btnDeletePost.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Attenzione")
                    .setMessage("Sei sicuro di voler eliminare definitivamente questo post?")
                    .setPositiveButton("Sì, elimina", (dialog, which) -> deletePostAndReport(report))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void deletePostAndReport(Report report) {
        // 1. Elimina il post dalla collezione "posts"
        db.collection("posts").document(report.getPostId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // 2. Se ha successo, elimina anche la segnalazione
                    db.collection("reports").document(report.getReportId()).delete();
                    Toast.makeText(context, "Post e segnalazione eliminati!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Errore eliminazione post: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvReason, tvReporter, tvReportedUser, tvDescription, tvPostText;
        Button btnIgnore, btnDeletePost;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReason = itemView.findViewById(R.id.tvReason);
            tvReporter = itemView.findViewById(R.id.tvReporter);
            tvReportedUser = itemView.findViewById(R.id.tvReportedUser);

            // Inizializzazione nuovi campi
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvPostText = itemView.findViewById(R.id.tvPostText);

            btnIgnore = itemView.findViewById(R.id.btnIgnore);
            btnDeletePost = itemView.findViewById(R.id.btnDeletePostAdmin);
        }
    }
}