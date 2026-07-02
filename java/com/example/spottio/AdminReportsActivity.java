package com.example.spottio;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration; // Aggiunto per il Memory Leak
import java.util.ArrayList;
import java.util.List;

public class AdminReportsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReportAdapter adapter;
    private List<Report> reportList;
    private FirebaseFirestore db;
    private ListenerRegistration reportsListener; // Riferimento al listener

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_reports);

        recyclerView = findViewById(R.id.rvReports);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        reportList = new ArrayList<>();
        adapter = new ReportAdapter(reportList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        loadReports();
    }

    private void loadReports() {
        // Salviamo il listener nella variabile
        reportsListener = db.collection("reports")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("AdminReports", "Errore nel caricamento: ", error);
                        return;
                    }

                    if (value != null) {
                        reportList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                Report r = doc.toObject(Report.class);
                                if (r != null) {
                                    r.reportId = doc.getId();
                                    reportList.add(r);
                                }
                            } catch (Exception e) {
                                Log.e("AdminReports", "Documento ignorato per errore di formato: " + doc.getId(), e);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Disconnettiamo il listener quando l'activity viene chiusa per evitare il Memory Leak
        if (reportsListener != null) {
            reportsListener.remove();
        }
    }
}