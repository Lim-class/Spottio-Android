package com.example.spottio;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupSettingsActivity extends AppCompatActivity {

    private String currentUser;
    private String groupId;
    private String groupAdminId;
    private boolean isAdmin = false;

    private ImageView ivGroupIcon;
    private TextView tvGroupName, tvGroupDesc, tvEditIconHint;
    private ImageButton btnEditGroupName, btnEditGroupDesc;
    private ListView lvGroupMembers;
    private Button btnAddMember, btnLeaveGroup;

    private FirebaseFirestore db;
    private GroupMemberAdapter memberAdapter;
    private List<String> memberIds;

    private final ActivityResultLauncher<String> pickGroupIcon = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadGroupIcon(uri);
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_settings);

        SharedPreferences prefs = getSharedPreferences("SpottioPrefs", Context.MODE_PRIVATE);
        currentUser = prefs.getString("username_attivo", null);

        groupId = getIntent().getStringExtra("group_id");
        String passedGroupName = getIntent().getStringExtra("group_name");

        if (groupId == null || currentUser == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        memberIds = new ArrayList<>();

        ivGroupIcon = findViewById(R.id.ivGroupIcon);
        tvGroupName = findViewById(R.id.tvGroupName);
        tvGroupDesc = findViewById(R.id.tvGroupDesc);
        tvEditIconHint = findViewById(R.id.tvEditIconHint);
        btnEditGroupName = findViewById(R.id.btnEditGroupName);
        btnEditGroupDesc = findViewById(R.id.btnEditGroupDesc);
        lvGroupMembers = findViewById(R.id.lvGroupMembers);
        btnAddMember = findViewById(R.id.btnAddMember);
        btnLeaveGroup = findViewById(R.id.btnLeaveGroup);

        if (passedGroupName != null && !passedGroupName.isEmpty()) {
            tvGroupName.setText(passedGroupName);
        }

        // --- INIZIO FIX: CREAZIONE DEL LISTENER PER L'ADAPTER ---
        GroupMemberAdapter.GroupMemberListener listener = new GroupMemberAdapter.GroupMemberListener() {
            @Override
            public void onRemoveMemberRequest(String memberId) {
                // Il popup ora è generato dall'Activity (Sicuro e senza leak!)
                new AlertDialog.Builder(GroupSettingsActivity.this)
                        .setTitle("Rimuovi partecipante")
                        .setMessage("Vuoi rimuovere " + memberId + " dal gruppo?")
                        .setPositiveButton("Rimuovi", (d, w) -> removeMemberFromGroup(memberId))
                        .setNegativeButton("Annulla", null)
                        .show();
            }

            @Override
            public void onMemberClick(String memberId) {
                // Opzionale: Se vuoi che cliccando un partecipante si apra il suo profilo,
                // puoi implementare l'Intent qui. Altrimenti lascialo vuoto.
            }
        };

        // PASSIAMO IL LISTENER AL COSTRUTTORE (Ora gli argomenti coincidono)
        memberAdapter = new GroupMemberAdapter(this, memberIds, currentUser, db, groupId, listener);
        lvGroupMembers.setAdapter(memberAdapter);
        // --- FINE FIX ---

        loadGroupData();

        btnEditGroupName.setOnClickListener(v -> showEditDialog("Nome", "name", tvGroupName.getText().toString()));
        btnEditGroupDesc.setOnClickListener(v -> showEditDialog("Descrizione", "groupDescription", tvGroupDesc.getText().toString()));
        ivGroupIcon.setOnClickListener(v -> pickGroupIcon.launch("image/*"));

        btnAddMember.setOnClickListener(v -> showAddMemberDialog());
        btnLeaveGroup.setOnClickListener(v -> leaveGroup());
    }

    private void loadGroupData() {
        db.collection("groups").document(groupId).addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) {
                checkAdminPermissions();
                return;
            }

            String name = snapshot.getString("name");
            String desc = snapshot.getString("groupDescription");
            String iconUri = snapshot.getString("groupIconUri");
            groupAdminId = snapshot.getString("createdBy");

            List<String> members = (List<String>) snapshot.get("members");

            if (name != null && !name.isEmpty()) tvGroupName.setText(name);

            if (desc != null && !desc.isEmpty()) {
                tvGroupDesc.setText(desc);
            } else {
                tvGroupDesc.setText("Nessuna descrizione");
            }

            if (iconUri != null && !iconUri.isEmpty()) {
                Glide.with(this).load(iconUri).into(ivGroupIcon);
            }

            if (members != null) {
                memberIds.clear();
                memberIds.addAll(members);
                memberAdapter.setAdminId(groupAdminId);
                memberAdapter.notifyDataSetChanged();
            }

            checkAdminPermissions();
        });
    }

    private void checkAdminPermissions() {
        if (groupAdminId == null) {
            isAdmin = true;
        } else {
            isAdmin = currentUser.equals(groupAdminId);
        }

        btnEditGroupName.setVisibility(View.VISIBLE);
        btnEditGroupDesc.setVisibility(View.VISIBLE);
        tvEditIconHint.setVisibility(View.VISIBLE);

        btnAddMember.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        memberAdapter.setCurrentUserIsAdmin(isAdmin);
    }

    private void showEditDialog(String titolo, String fieldName, String testoAttuale) {
        EditText input = new EditText(this);
        input.setText(testoAttuale.equals("Nessuna descrizione") ? "" : testoAttuale);
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Modifica " + titolo)
                .setView(input)
                .setPositiveButton("Salva", (dialog, which) -> {
                    String newText = input.getText().toString().trim();

                    Map<String, Object> data = new HashMap<>();
                    data.put(fieldName, newText);
                    db.collection("groups").document(groupId).set(data, SetOptions.merge());

                    if (fieldName.equals("name")) {
                        Map<String, Object> previewData = new HashMap<>();
                        previewData.put("name", newText);
                        db.collection("chat_previews").document(groupId).set(previewData, SetOptions.merge());
                    }
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void showAddMemberDialog() {
        EditText input = new EditText(this);
        input.setHint("Es: MarioRossi");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("Aggiungi Partecipante")
                .setMessage("Inserisci l'username esatto dell'utente:")
                .setView(input)
                .setPositiveButton("Aggiungi", (dialog, which) -> {
                    String username = input.getText().toString().trim();
                    if (!username.isEmpty() && !memberIds.contains(username)) {
                        verificaEAggiungiUtente(username);
                    } else if (memberIds.contains(username)) {
                        Toast.makeText(this, "L'utente è già nel gruppo!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void verificaEAggiungiUtente(String username) {
        db.collection("users").document(username).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                db.collection("groups").document(groupId).update("members", FieldValue.arrayUnion(username))
                        .addOnSuccessListener(a -> Toast.makeText(this, username + " aggiunto al gruppo!", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Utente non trovato.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void leaveGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Abbandona Gruppo")
                .setMessage("Sei sicuro di voler uscire da questo gruppo?")
                .setPositiveButton("Esci", (dialog, which) -> {
                    if (isAdmin && memberIds.size() > 1) {
                        String nuovoAdmin = memberIds.get(0).equals(currentUser) ? memberIds.get(1) : memberIds.get(0);

                        db.collection("groups").document(groupId).update(
                                "members", FieldValue.arrayRemove(currentUser),
                                "createdBy", nuovoAdmin
                        ).addOnSuccessListener(a -> chiudiETornaAllaHome());
                    } else {
                        db.collection("groups").document(groupId).update("members", FieldValue.arrayRemove(currentUser))
                                .addOnSuccessListener(a -> chiudiETornaAllaHome());
                    }
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void chiudiETornaAllaHome() {
        Toast.makeText(this, "Hai abbandonato il gruppo", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    public void removeMemberFromGroup(String memberId) {
        db.collection("groups").document(groupId)
                .update("members", FieldValue.arrayRemove(memberId))
                .addOnSuccessListener(a -> Toast.makeText(this, "Membro rimosso", Toast.LENGTH_SHORT).show());
    }

    private void uploadGroupIcon(Uri uri) {
        Toast.makeText(this, "Aggiornamento icona...", Toast.LENGTH_SHORT).show();
        String uriString = uri.toString();

        Map<String, Object> iconData = new HashMap<>();
        iconData.put("groupIconUri", uriString);

        db.collection("groups").document(groupId).set(iconData, SetOptions.merge())
                .addOnSuccessListener(a -> {
                    Map<String, Object> previewData = new HashMap<>();
                    previewData.put("profilePicUri", uriString);
                    db.collection("chat_previews").document(groupId).set(previewData, SetOptions.merge());
                    Toast.makeText(this, "Icona aggiornata", Toast.LENGTH_SHORT).show();
                });
    }
}