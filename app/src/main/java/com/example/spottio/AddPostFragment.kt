package com.example.spottio

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore

class AddPostFragment : Fragment() {

    private var selectedMediaUri: Uri? = null
    private var isVideoSelected = false

    private lateinit var ivPreview: ImageView
    private lateinit var autoCompleteCategory: AutoCompleteTextView
    private lateinit var etText: EditText

    private val categoryList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    // Inizializza Firestore direttamente qui (AddPostHandler non serve più)
    private val db = FirebaseFirestore.getInstance()

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            // Uso context?. per evitare crash se il fragment viene chiuso mentre si sceglie il file
            val type = context?.contentResolver?.getType(uri)
            isVideoSelected = type != null && type.startsWith("video")
            ivPreview.setImageURI(uri)
            ivPreview.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_post, container, false)

        etText = view.findViewById(R.id.etPostText)
        val btnMedia = view.findViewById<Button>(R.id.btnAttachMedia)
        val btnPublish = view.findViewById<Button>(R.id.btnPublish)
        ivPreview = view.findViewById(R.id.ivMediaPreview)
        autoCompleteCategory = view.findViewById(R.id.autoCompleteCategory)

        // Setup Adapter in puro stile Kotlin
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryList).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        autoCompleteCategory.setAdapter(adapter)

        fetchCategories()

        btnMedia.setOnClickListener { galleryLauncher.launch("*/*") }

        // Non c'è bisogno di passare etText perché è già una variabile di classe
        btnPublish.setOnClickListener { handlePublish(autoCompleteCategory.text.toString()) }

        return view
    }

    private fun fetchCategories() {
        db.collection("categories")
            .orderBy("name")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    categoryList.clear()
                    for (document in task.result!!) {
                        document.getString("name")?.let { categoryList.add(it) }
                    }
                    // Aggiorniamo l'interfaccia solo se il fragment è ancora aperto
                    if (isAdded) {
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    Log.e("Firestore", "Errore nel caricamento categorie", task.exception)
                }
            }
    }

    private fun handlePublish(category: String) {
        val text = etText.text.toString().trim()

        if (text.isEmpty() && selectedMediaUri == null) {
            Toast.makeText(context, "Scrivi qualcosa o allega un file!", Toast.LENGTH_SHORT).show()
            return
        }

        // Recupero sicuro del Context per evitare crash con RequireContext()
        val safeContext = context ?: return
        val currentUid = AuthManager.getCurrentUserUid(safeContext)

        if (currentUid.isEmpty()) {
            Toast.makeText(safeContext, "Errore: sessione non valida.", Toast.LENGTH_SHORT).show()
            return
        }

        val mediaUriString = selectedMediaUri?.toString() ?: ""

        var finalCategory = category.trim()
        if (finalCategory.isEmpty()) {
            finalCategory = "Generale"
        }

        val newPost = Post(currentUid, text, mediaUriString, isVideoSelected, finalCategory)

        db.collection("posts")
            .add(newPost)
            .addOnSuccessListener {
                Toast.makeText(context, "Post pubblicato!", Toast.LENGTH_SHORT).show()

                // Svuotiamo il testo nel modo corretto in Kotlin per evitare il warning del setText
                etText.text.clear()
                ivPreview.visibility = View.GONE
                selectedMediaUri = null

                // Cambio pagina sicuro (se l'activity esiste ancora)
                activity?.findViewById<ViewPager2>(R.id.viewPager)?.setCurrentItem(0, true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Errore pubblicazione", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Err: ${e.message}")
            }
    }
}