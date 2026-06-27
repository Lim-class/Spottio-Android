package com.example.spottio;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class AddPostFragment extends Fragment {

    private Uri selectedMediaUri = null;
    private boolean isVideoSelected = false;
    private ImageView ivPreview;
    private AutoCompleteTextView autoCompleteCategory;

    private List<String> categoryList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private AddPostHandler addPostHandler;

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedMediaUri = uri;
                    String type = requireContext().getContentResolver().getType(uri);
                    isVideoSelected = type != null && type.startsWith("video");
                    ivPreview.setImageURI(uri);
                    ivPreview.setVisibility(View.VISIBLE);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_post, container, false);

        addPostHandler = new AddPostHandler();

        EditText etText = view.findViewById(R.id.etPostText);
        Button btnMedia = view.findViewById(R.id.btnAttachMedia);
        Button btnPublish = view.findViewById(R.id.btnPublish);
        ivPreview = view.findViewById(R.id.ivMediaPreview);
        autoCompleteCategory = view.findViewById(R.id.autoCompleteCategory);

        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categoryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoCompleteCategory.setAdapter(adapter);

        fetchCategories();

        btnMedia.setOnClickListener(v -> galleryLauncher.launch("*/*"));

        btnPublish.setOnClickListener(v -> handlePublish(etText, autoCompleteCategory.getText().toString()));

        return view;
    }

    private void fetchCategories() {
        addPostHandler.loadCategories(new AddPostHandler.OnCategoriesLoadedListener() {
            @Override
            public void onSuccess(List<String> categories) {
                categoryList.clear();
                categoryList.addAll(categories);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("Firestore", "Errore nel caricamento categorie", e);
            }
        });
    }

    private void handlePublish(EditText etText, String category) {
        String text = etText.getText().toString().trim();

        // Verifica che l'utente non stia tentando di pubblicare un post completamente vuoto
        if (text.isEmpty() && selectedMediaUri == null) {
            Toast.makeText(getContext(), "Scrivi qualcosa o allega un file!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Recupero UID centralizzato e sicuro tramite AuthManager
        String currentUid = AuthManager.getCurrentUserUid(requireContext());

        // Blocco di sicurezza nel caso in cui la sessione sia scaduta o non valida
        if (currentUid.isEmpty()) {
            Toast.makeText(getContext(), "Errore: sessione non valida.", Toast.LENGTH_SHORT).show();
            return;
        }

        String mediaUriString = (selectedMediaUri != null) ? selectedMediaUri.toString() : "";

        // Validazione e assegnazione di un valore di default alla categoria
        String finalCategory = category.trim();
        if (finalCategory.isEmpty()) {
            finalCategory = "Generale"; // Evita l'invio di stringhe vuote a Firestore
        }

        // Creazione dell'oggetto Post con i parametri verificati e puliti
        Post newPost = new Post(currentUid, text, mediaUriString, isVideoSelected, finalCategory);

        addPostHandler.publishPost(newPost, new AddPostHandler.OnPostPublishedListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Post pubblicato!", Toast.LENGTH_SHORT).show();
                etText.setText("");
                ivPreview.setVisibility(View.GONE);
                selectedMediaUri = null;

                // Ritorna automaticamente alla tab principale del feed (HomeFragment)
                ViewPager2 vp = requireActivity().findViewById(R.id.viewPager);
                if (vp != null) vp.setCurrentItem(0, true);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Errore pubblicazione", Toast.LENGTH_SHORT).show();
                Log.e("Firestore", "Err: " + e.getMessage());
            }
        });
    }
}