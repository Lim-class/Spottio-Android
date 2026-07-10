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

object PostDialogHelper {

    @JvmStatic
    fun showDeletePostDialog(context: Context, onConfirm: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle("Elimina Post")
            .setMessage("Vuoi eliminare definitivamente questo post?")
            .setPositiveButton("Sì") { _, _ -> onConfirm() }
            .setNegativeButton("No", null)
            .show()
    }

    @JvmStatic
    fun showReportDialog(context: Context, onSubmit: (String, String) -> Unit) {
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
            onSubmit(spinnerReason.selectedItem.toString(), etDescription.text.toString().trim())
            dialog.dismiss()
        }

        dialog.show()
    }

    @JvmStatic
    fun showEditPostDialog(context: Context, currentText: String?, onSave: (String) -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Modifica Post")

        val input = EditText(context)
        input.setText(currentText)

        val layout = LinearLayout(context).apply {
            setPadding(50, 20, 50, 0)
            addView(input)
        }

        input.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        builder.setView(layout)

        builder.setPositiveButton("Salva") { _, _ ->
            val newText = input.text.toString().trim()
            if (newText.isNotEmpty() && newText != currentText) {
                onSave(newText)
            }
        }

        builder.setNegativeButton("Annulla") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    @JvmStatic
    fun showCommentsSheet(context: Context, comments: List<Comment>, onCommentAdded: (String) -> Unit) {
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

        val commentAdapter = CommentAdapter(comments.toMutableList())
        commentsRecyclerView.adapter = commentAdapter
        commentsRecyclerView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f)
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
                    onCommentAdded(txt)
                    input.setText("")
                    // L'aggiornamento visivo della lista all'interno del Dialog avviene ricaricando i dati passati dal ViewModel
                    commentAdapter.notifyDataSetChanged()
                    commentsRecyclerView.scrollToPosition(comments.size)
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