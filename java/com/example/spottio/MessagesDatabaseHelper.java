package com.example.spottio;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagesDatabaseHelper {

    private final FirebaseFirestore db;
    private final String currentUser;
    private final ArrayList<ChatPreview> masterList;
    private final ArrayList<ChatPreview> displayedList;
    private final ChatPreviewAdapter adapter;

    public MessagesDatabaseHelper(FirebaseFirestore db, String currentUser, ArrayList<ChatPreview> masterList,
                                  ArrayList<ChatPreview> displayedList, ChatPreviewAdapter adapter) {
        this.db = db;
        this.currentUser = currentUser;
        this.masterList = masterList;
        this.displayedList = displayedList;
        this.adapter = adapter;
    }

    public interface DataLoadCallback {
        void onDataLoaded();
    }

    // Usiamo una lista di oggetti User completi
    public interface UsersLoadCallback {
        void onUsersLoaded(List<User> users);
    }

    public interface GroupCreateCallback {
        void onGroupCreated(String newGroupId);
    }

    public void loadUnifiedChats(DataLoadCallback callback) {
        db.collection("chat_previews")
                .whereArrayContains("participants", currentUser)
                .orderBy("lastUpdate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    masterList.clear();

                    for (QueryDocumentSnapshot doc : value) {
                        Boolean isGroupObj = doc.getBoolean("isGroup");
                        boolean isGroup = isGroupObj != null && isGroupObj;

                        String id = doc.getId();
                        String lastMessage = doc.getString("lastMessage");
                        Date lastUpdate = doc.getTimestamp("lastUpdate") != null ? doc.getTimestamp("lastUpdate").toDate() : new Date();

                        String nameToShow = null;
                        String targetId = null;

                        if (isGroup) {
                            nameToShow = doc.getString("groupName");
                            targetId = id;
                        } else {
                            List<String> participants = (List<String>) doc.get("participants");
                            if (participants != null) {
                                for (String p : participants) {
                                    if (!p.equals(currentUser)) {
                                        targetId = p; // Questo è l'UID dell'altro!
                                        nameToShow = p; // L'adapter penserà a tradurlo nel nome vero
                                        break;
                                    }
                                }
                            }
                        }

                        if (nameToShow == null) nameToShow = (targetId != null) ? targetId : "Chat";
                        if (targetId == null) targetId = "";

                        ChatPreview preview = new ChatPreview(id, nameToShow, lastMessage, lastUpdate, isGroup, targetId, null, false);
                        masterList.add(preview);
                    }

                    displayedList.clear();
                    displayedList.addAll(masterList);
                    adapter.notifyDataSetChanged();
                    callback.onDataLoaded();
                });
    }

    public void fetchAllOtherUsers(UsersLoadCallback callback) {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<User> allUsers = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String userId = doc.getId();
                    if (!userId.equals(currentUser)) {
                        // Recuperiamo il nome utente reale
                        String username = doc.getString("username");
                        User u = new User();
                        u.setUid(userId);
                        u.setUsername(username != null ? username : "Utente");
                        allUsers.add(u);
                    }
                }
                callback.onUsersLoaded(allUsers);
            }
        });
    }

    public void createNewGroup(String groupName, List<String> selectedMembers, GroupCreateCallback callback) {
        Map<String, Object> groupData = new HashMap<>();
        groupData.put("name", groupName);
        groupData.put("members", selectedMembers);
        groupData.put("createdBy", currentUser);

        db.collection("groups").add(groupData).addOnSuccessListener(documentReference -> {
            String newGroupId = documentReference.getId();

            // Creazione della preview ESATTAMENTE come nella foto
            Map<String, Object> previewData = new HashMap<>();
            previewData.put("isGroup", true);
            previewData.put("groupName", groupName);
            previewData.put("participants", selectedMembers);
            previewData.put("lastMessage", "Gruppo creato");
            previewData.put("lastUpdate", com.google.firebase.firestore.FieldValue.serverTimestamp());

            db.collection("chat_previews").document(newGroupId).set(previewData).addOnSuccessListener(aVoid -> {
                callback.onGroupCreated(newGroupId);
            });
        });
    }
}