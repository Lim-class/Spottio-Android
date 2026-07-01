package com.example.gameplus;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginEstremoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_estremo);

        EditText editPassword = findViewById(R.id.edit_password);
        Button btnConfirm = findViewById(R.id.btn_login_confirm);

        btnConfirm.setOnClickListener(v -> {
            String password = editPassword.getText().toString();

            // Imposta qui la tua password personalizzata
            if (password.equals("1234")) {
                Intent intent = new Intent(this, GamePlayActivity.class);
                intent.putExtra("MODALITA", "Estremo");
                startActivity(intent);
                finish(); // Chiude il login così non si torna indietro qui
            } else {
                Toast.makeText(this, "Password Errata!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}