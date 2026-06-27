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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;

public class PostDialogHelper {

    // --- Gestione Dialog Segnalazione ---
    public static void showReportDialog(Context context, Post post, String currentUser) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_report, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Spinner spinnerReason = view.findViewById(R.id.spinnerReason);
        EditText etDescription = view.findViewById(R.id.etDescription);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnSubmit = view.findViewById(R.id.btnSubmit);

        String[] reasons = {"Seleziona un motivo", "Spam", "Contenuto inappropriato", "Violenza o incitamento all'odio", "Informazioni false", "Altro"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, reasons);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReason.setAdapter(spinnerAdapter);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            if (spinnerReason.getSelectedItemPosition() == 0) {
                Toast.makeText(context, "Seleziona un motivo per continuare", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedReason = spinnerReason.getSelectedItem().toString();
            String description = etDescription.getText().toString().trim();
            String postContent = post.getText() != null ? post.getText() : "";

            Report report = new Report(
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Modifica Post");

        final EditText input = new EditText(context);
        input.setText(post.getText());

        LinearLayout layout = new LinearLayout(context);
        layout.setPadding(50, 20, 50, 0);
        layout.addView(input);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        input.setLayoutParams(params);

        builder.setView(layout);

        builder.setPositiveButton("Salva", (dialog, which) -> {
            String newText = input.getText().toString().trim();

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
        BottomSheetDialog sheet = new BottomSheetDialog(context);

        // Creazione del layout principale
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 32, 0, 32);

        // Titolo
        TextView title = new TextView(context);
        title.setText("Commenti");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(32, 0, 32, 16);
        layout.addView(title);

        // 1. INSERIMENTO RECYCLER VIEW PER I COMMENTI
        RecyclerView commentsRecyclerView = new RecyclerView(context);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        // 2. UTILIZZO DEL TUO COMMENT ADAPTER
        CommentAdapter commentAdapter = new CommentAdapter(post.getComments());
        commentsRecyclerView.setAdapter(commentAdapter);

        // Imposta un peso per far scrollare la lista se ci sono molti commenti
        LinearLayout.LayoutParams rvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        commentsRecyclerView.setLayoutParams(rvParams);
        layout.addView(commentsRecyclerView);

        // Area di input in basso
        LinearLayout inputArea = new LinearLayout(context);
        inputArea.setOrientation(LinearLayout.HORIZONTAL);
        inputArea.setPadding(32, 16, 32, 0);

        EditText input = new EditText(context);
        input.setHint("Aggiungi un commento...");
        input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        inputArea.addView(input);

        Button btnSend = new Button(context);
        btnSend.setText("Invia");
        btnSend.setOnClickListener(v -> {
            String txt = input.getText().toString().trim();
            if (!txt.isEmpty()) {
                Comment newComment = new Comment(currentUser, txt);
                post.addComment(newComment);

                if (post.getPostId() != null) {
                    FirebaseFirestore.getInstance().collection("posts")
                            .document(post.getPostId())
                            .update("comments", post.getComments())
                            .addOnSuccessListener(a -> {
                                // Aggiorna l'interfaccia in tempo reale senza chiudere la BottomSheet
                                commentAdapter.notifyItemInserted(post.getComments().size() - 1);
                                commentsRecyclerView.scrollToPosition(post.getComments().size() - 1);
                                input.setText("");
                                adapter.notifyItemChanged(postPos); // Aggiorna il contatore dei commenti nel feed
                            });
                }
            }
        });
        inputArea.addView(btnSend);
        layout.addView(inputArea);

        sheet.setContentView(layout);

        // Imposta un'altezza minima per la BottomSheet in modo che la RecyclerView abbia spazio
        View bottomSheetInternal = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels / 2);
        }

        sheet.show();
    }
}