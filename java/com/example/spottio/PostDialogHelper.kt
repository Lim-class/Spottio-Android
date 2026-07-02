package com.example.spottio

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore

object PostDialogHelper {

    // --- Gestione Dialog Segnalazione ---
    @JvmStatic
    fun showReportDialog(context: Context, post: Post, currentUser: String) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_report, null)
        builder.setView(view)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val spinnerReason = view.findViewById<Spinner>(R.id.spinnerReason)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val btnCancel = view.findViewById<Button>(R.id.btnCancel)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)

        val reasons = arrayOf("Seleziona un motivo", "Spam", "Contenuto inappropriato", "Violenza o incitamento all'odio", "Informazioni false", "Altro")

        val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, reasons)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReason.adapter = spinnerAdapter

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            if (spinnerReason.selectedItemPosition == 0) {
                Toast.makeText(context, "Seleziona un motivo per continuare", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedReason = spinnerReason.selectedItem.toString()
            val description = etDescription.text.toString().trim()
            val postContent = post.text ?: ""

            val report = Report(
                post.postId ?: "",
                post.user,
                currentUser,
                selectedReason,
                description,
                postContent
            )

            FirebaseFirestore.getInstance().collection("reports")
                .add(report)
                .addOnSuccessListener {
                    Toast.makeText(context, "Segnalazione inviata", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Errore invio segnalazione", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    // --- Gestione Dialog Modifica Post ---
    @JvmStatic
    fun showEditPostDialog(context: Context, post: Post, adapter: RecyclerView.Adapter<*>, postPos: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Modifica Post")

        val input = EditText(context)
        input.setText(post.text)

        val layout = LinearLayout(context).apply {
            setPadding(50, 20, 50, 0)
            addView(input)
        }

        input.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )

        builder.setView(layout)

        builder.setPositiveButton("Salva") { _, _ ->
            val newText = input.text.toString().trim()

            if (newText.isNotEmpty() && newText != post.text) {
                post.postId?.let { pId ->
                    FirebaseFirestore.getInstance().collection("posts")
                        .document(pId)
                        .update("text", newText)
                        .addOnSuccessListener {
                            post.text = newText
                            adapter.notifyItemChanged(postPos)
                            Toast.makeText(context, "Post modificato", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Errore nella modifica", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }

        builder.setNegativeButton("Annulla") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // --- Gestione BottomSheet Commenti ---
    @JvmStatic
    fun showCommentsSheet(context: Context, post: Post, currentUser: String, adapter: RecyclerView.Adapter<*>, postPos: Int) {
        val sheet = BottomSheetDialog(context)

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 32, 0, 32)
        }

        val title = TextView(context).apply {
            text = "Commenti"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(32, 0, 32, 16)
        }
        layout.addView(title)

        val commentsRecyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
        }

        val commentAdapter = CommentAdapter(post.comments)
        commentsRecyclerView.adapter = commentAdapter

        commentsRecyclerView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f
        )
        layout.addView(commentsRecyclerView)

        val inputArea = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(32, 16, 32, 0)
        }

        val input = EditText(context).apply {
            hint = "Aggiungi un commento..."
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        inputArea.addView(input)

        val btnSend = Button(context).apply {
            text = "Invia"
            setOnClickListener {
                val txt = input.text.toString().trim()
                if (txt.isNotEmpty()) {
                    val newComment = Comment(currentUser, txt)
                    post.addComment(newComment)

                    post.postId?.let { pId ->
                        FirebaseFirestore.getInstance().collection("posts")
                            .document(pId)
                            .update("comments", post.comments)
                            .addOnSuccessListener {
                                commentAdapter.notifyItemInserted(post.comments.size - 1)
                                commentsRecyclerView.scrollToPosition(post.comments.size - 1)
                                input.setText("")
                                adapter.notifyItemChanged(postPos)
                            }
                    }
                }
            }
        }
        inputArea.addView(btnSend)
        layout.addView(inputArea)

        sheet.setContentView(layout)

        val bottomSheetInternal = sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheetInternal?.minimumHeight = Resources.getSystem().displayMetrics.heightPixels / 2

        sheet.show()
    }
}