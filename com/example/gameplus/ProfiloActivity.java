package com.example.gameplus;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class ProfiloActivity extends AppCompatActivity {

    private TextInputEditText etNickname;
    private Button btnSalva;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profilo);

        etNickname = findViewById(R.id.etNicknameProfilo);
        btnSalva = findViewById(R.id.btnSalvaProfilo);

        btnSalva.setOnClickListener(v -> {
            String nick = etNickname.getText().toString().trim();
            if (!nick.isEmpty()) {
                // ECCO DOVE VIENE USATO
                salvaNickname(nick);
            } else {
                etNickname.setError("Inserisci un nickname!");
            }
        });
    }

    private void salvaNickname(String nickname) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("uid", uid);

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(user, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfiloActivity.this, "Profilo Salvato!", Toast.LENGTH_SHORT).show();
                    // Dopo il salvataggio, chiude l'activity e torna alla Home
                    finish();
                });
    }
}