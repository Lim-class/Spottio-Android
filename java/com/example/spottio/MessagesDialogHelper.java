package com.example.spottio;

import android.content.Context;
import android.graphics.Color;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

public class MessagesDialogHelper {

    private final Context context;
    private final FragmentManager fragmentManager;
    private final MessagesDatabaseHelper dbHelper;
    private final String currentUser;

    public MessagesDialogHelper(Context context, FragmentManager fragmentManager, MessagesDatabaseHelper dbHelper, String currentUser) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.dbHelper = dbHelper;
        this.currentUser = currentUser;
    }

    public void mostraDialogoNuovaChat() {
        dbHelper.fetchAllOtherUsers(allUsers -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Nuova Chat");

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);

            Button btnCreateGroup = new Button(context);
            btnCreateGroup.setText("👥 Crea Nuovo Gruppo");
            btnCreateGroup.setBackgroundColor(Color.TRANSPARENT);
            btnCreateGroup.setTextColor(Color.parseColor("#1DB954"));
            layout.addView(btnCreateGroup);

            // Estraiamo SOLO i nomi per farli vedere nella grafica
            List<String> displayNames = new ArrayList<>();
            for (User u : allUsers) {
                displayNames.add(u.getUsername());
            }

            ListView userListView = new ListView(context);
            ArrayAdapter<String> userAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, displayNames);
            userListView.setAdapter(userAdapter);
            layout.addView(userListView);

            builder.setView(layout);
            AlertDialog dialog = builder.create();

            btnCreateGroup.setOnClickListener(v -> {
                dialog.dismiss();
                mostraDialogoCreazioneGruppo(allUsers);
            });

            userListView.setOnItemClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                // Passiamo l'UID (targetUser) come chiave, e l'Username per il titolo in alto!
                String selectedUid = allUsers.get(position).getUid();
                String selectedName = allUsers.get(position).getUsername();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ChatFragment.newInstance(selectedUid, false, selectedName))
                        .addToBackStack(null)
                        .commit();
            });

            dialog.show();
        });
    }

    private void mostraDialogoCreazioneGruppo(List<User> allUsers) {
        String[] usersArray = new String[allUsers.size()];
        for (int i = 0; i < allUsers.size(); i++) {
            usersArray[i] = allUsers.get(i).getUsername();
        }

        boolean[] checkedUsers = new boolean[usersArray.length];
        List<String> selectedMemberUids = new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Nuovo Gruppo");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText etGroupName = new EditText(context);
        etGroupName.setHint("Nome del gruppo...");
        layout.addView(etGroupName);

        Button btnSelectMembers = new Button(context);
        btnSelectMembers.setText("Seleziona Membri (0)");
        layout.addView(btnSelectMembers);

        btnSelectMembers.setOnClickListener(v -> {
            AlertDialog.Builder membersBuilder = new AlertDialog.Builder(context);
            membersBuilder.setTitle("Partecipanti");
            membersBuilder.setMultiChoiceItems(usersArray, checkedUsers, (d, which, isChecked) -> checkedUsers[which] = isChecked);
            membersBuilder.setPositiveButton("OK", (d, which) -> {
                selectedMemberUids.clear();
                for (int i = 0; i < checkedUsers.length; i++) {
                    // SELEZIONIAMO I LORO UID PER IL DATABASE!
                    if (checkedUsers[i]) selectedMemberUids.add(allUsers.get(i).getUid());
                }
                btnSelectMembers.setText("Seleziona Membri (" + selectedMemberUids.size() + ")");
            });
            membersBuilder.show();
        });

        builder.setView(layout);
        builder.setPositiveButton("Crea", (dialog, which) -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) return;

            // Aggiungi anche te stesso al gruppo
            selectedMemberUids.add(currentUser);

            dbHelper.createNewGroup(groupName, selectedMemberUids, newGroupId -> {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ChatFragment.newInstance(newGroupId, true, groupName))
                        .addToBackStack(null)
                        .commit();
            });
        });

        builder.setNegativeButton("Annulla", null);
        builder.show();
    }
}