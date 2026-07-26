package com.example.spottio

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ReportAdapter(private val reportList: MutableList<Report>) : RecyclerView.Adapter<ReportAdapter.ReportViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reportList[position]

        holder.tvReason.text = "Motivo: ${report.reason}"
        holder.tvReporter.text = "Segnalato da: ${report.reporterUser}"
        holder.tvReportedUser.text = "Autore Post: ${report.postAuthor}"
        holder.tvDescription.text = "Dettagli: ${report.description}"
        holder.tvPostText.text = "Testo Post: ${report.postText}"

        // Click per ignorare la segnalazione
        holder.btnIgnore.setOnClickListener {
            report.reportId?.let { id ->
                db.collection("reports").document(id).delete()
                    .addOnSuccessListener {
                        // FIX: Usiamo adapterPosition per compatibilità con la tua versione
                        val currentPos = holder.adapterPosition
                        if (currentPos != RecyclerView.NO_POSITION) {
                            reportList.removeAt(currentPos)
                            notifyItemRemoved(currentPos)
                            Toast.makeText(context, "Segnalazione ignorata", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // Click per eliminare il Post (e a cascata la segnalazione)
        holder.btnDeletePost.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Elimina Post")
                .setMessage("Sei sicuro di voler eliminare il post segnalato?")
                // FIX: Passiamo l'adapterPosition
                .setPositiveButton("Elimina") { _, _ -> deletePostAndReport(report, holder.adapterPosition) }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    private fun deletePostAndReport(report: Report, position: Int) {
        // FIX: Se postId è nullo o vuoto, esce dalla funzione invece di crashare (salva la vita!)
        val pId = report.postId
        if (pId.isNullOrEmpty()) return

        db.collection("posts").document(pId).delete()
            .addOnSuccessListener {
                report.reportId?.let { id -> db.collection("reports").document(id).delete() }
                if (position != RecyclerView.NO_POSITION) {
                    reportList.removeAt(position)
                    notifyItemRemoved(position)
                }
                Toast.makeText(context, "Post e segnalazione eliminati!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Errore eliminazione post: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int = reportList.size

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReason: TextView = itemView.findViewById(R.id.tvReason)
        val tvReporter: TextView = itemView.findViewById(R.id.tvReporter)
        val tvReportedUser: TextView = itemView.findViewById(R.id.tvReportedUser)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvPostText: TextView = itemView.findViewById(R.id.tvPostText)
        val btnIgnore: Button = itemView.findViewById(R.id.btnIgnore)
        val btnDeletePost: Button = itemView.findViewById(R.id.btnDeletePostAdmin)
    }
}