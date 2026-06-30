package com.example.spottio;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;

public class PostDialogHelper {

    // --- Gestione Dialog Segnalazione ---
    public static void showReportDialog(Context context, Post post, String currentUser) {
        var builder = new AlertDialog.Builder(context);
        var inflater = LayoutInflater.from(context);
        var view = inflater.inflate(R.layout.dialog_report, null);
        builder.setView(view);

        var dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        var spinnerReason = (Spinner) view.findViewById(R.id.spinnerReason);
        var etDescription = (EditText) view.findViewById(R.id.etDescription);
        var btnCancel = (Button) view.findViewById(R.id.btnCancel);
        var btnSubmit = (Button) view.findViewById(R.id.btnSubmit);

        var reasons = new String[]{"Seleziona un motivo", "Spam", "Contenuto inappropriato", "Violenza o incitamento all'odio", "Informazioni false", "Altro"};

        // JAVA 15: Con 'var', il tipo tra parentesi angolari a destra è obbligatorio
        var spinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, reasons);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReason.setAdapter(spinnerAdapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            if (spinnerReason.getSelectedItemPosition() == 0) {
                Toast.makeText(context, "Seleziona un motivo per continuare", Toast.LENGTH_SHORT).show();
                return;
            }

            var selectedReason = spinnerReason.getSelectedItem().toString();
            var description = etDescription.getText().toString().trim();
            var postContent = post.getText() != null ? post.getText() : "";

            var report = new Report(
                    post.getPostId(),
                    post.getUser(),
                    currentUser,
                    selectedReason,
                    description,
                    postContent
            );

            FirebaseFirestore.getInstance().collection("reports")
                    .add(report)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(context, "Segnalazione inviata", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Errore invio segnalazione", Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    // --- Gestione Dialog Modifica Post ---
    public static void showEditPostDialog(Context context, Post post, RecyclerView.Adapter<?> adapter, int postPos) {
        var builder = new AlertDialog.Builder(context);
        builder.setTitle("Modifica Post");

        var input = new EditText(context);
        input.setText(post.getText());

        var layout = new LinearLayout(context);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(input);

        var params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(params);

        builder.setView(layout);

        builder.setPositiveButton("Salva", (dialog, which) -> {
            var newText = input.getText().toString().trim();

            if (!newText.isEmpty() && post.getPostId() != null && !newText.equals(post.getText())) {
                FirebaseFirestore.getInstance().collection("posts")
                        .document(post.getPostId())
                        .update("text", newText)
                        .addOnSuccessListener(aVoid -> {
                            post.setText(newText);
                            adapter.notifyItemChanged(postPos);
                            Toast.makeText(context, "Post modificato", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Errore nella modifica", Toast.LENGTH_SHORT).show()
                        );
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // --- Gestione BottomSheet Commenti ---
    public static void showCommentsSheet(Context context, Post post, String currentUser, RecyclerView.Adapter<?> adapter, int postPos) {
        var sheet = new BottomSheetDialog(context);

        var layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 32, 0, 32);

        var title = new TextView(context);
        title.setText("Commenti");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(32, 0, 32, 16);
        layout.addView(title);

        var commentsRecyclerView = new RecyclerView(context);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        var commentAdapter = new CommentAdapter(post.getComments());
        commentsRecyclerView.setAdapter(commentAdapter);

        var rvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        commentsRecyclerView.setLayoutParams(rvParams);
        layout.addView(commentsRecyclerView);

        var inputArea = new LinearLayout(context);
        inputArea.setOrientation(LinearLayout.HORIZONTAL);
        inputArea.setPadding(32, 16, 32, 0);

        var input = new EditText(context);
        input.setHint("Aggiungi un commento...");
        input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        inputArea.addView(input);

        var btnSend = new Button(context);
        btnSend.setText("Invia");
        btnSend.setOnClickListener(v -> {
            var txt = input.getText().toString().trim();
            if (!txt.isEmpty()) {
                var newComment = new Comment(currentUser, txt);
                post.addComment(newComment);

                if (post.getPostId() != null) {
                    FirebaseFirestore.getInstance().collection("posts")
                            .document(post.getPostId())
                            .update("comments", post.getComments())
                            .addOnSuccessListener(a -> {
                                commentAdapter.notifyItemInserted(post.getComments().size() - 1);
                                commentsRecyclerView.scrollToPosition(post.getComments().size() - 1);
                                input.setText("");
                                adapter.notifyItemChanged(postPos);
                            });
                }
            }
        });
        inputArea.addView(btnSend);
        layout.addView(inputArea);

        sheet.setContentView(layout);

        var bottomSheetInternal = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels / 2);
        }

        sheet.show();
    }
}