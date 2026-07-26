package com.example.spottio

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminReportsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ReportAdapter
    private val reportList = mutableListOf<Report>()
    private lateinit var db: FirebaseFirestore
    private var reportsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_reports)

        recyclerView = findViewById(R.id.rvReports)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ReportAdapter(reportList)
        recyclerView.adapter = adapter

        db = FirebaseFirestore.getInstance()

        loadReports()
    } // <--- Questa era la parentesi mancante!

    private fun loadReports() {
        reportsListener = db.collection("reports")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("AdminReports", "Errore nel caricamento: ", error)
                    return@addSnapshotListener
                }

                if (value != null) {
                    reportList.clear()
                    for (doc in value.documents) {
                        try {
                            val r = doc.toObject(Report::class.java)
                            if (r != null) {
                                r.reportId = doc.id
                                reportList.add(r)
                            }
                        } catch (e: Exception) {
                            Log.e("AdminReports", "Documento ignorato: ${doc.id}", e)
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        reportsListener?.remove()
    }
}