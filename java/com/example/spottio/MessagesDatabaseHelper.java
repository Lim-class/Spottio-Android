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
                                        targetId = p;
                                        nameToShow = p;
                                        break;
                                    }
                                }
                            }
                        }

                        if (nameToShow == null) nameToShow = (targetId != null) ? targetId : "Chat";
                        if (targetId == null) targetId = "";

                        ChatPreview preview = new ChatPreview(id, nameToShow, lastMessage, lastUpdate, isGroup, targetId, null, false);
                        masterList.add(preview);

                        // --- INIZIO LOGICA PULITA: Fetch Asincrono Centralizzato ---
                        if (!isGroup && targetId != null && !targetId.isEmpty()) {
                            final String finalTargetId = targetId;

                            // Ora affidiamo l'intero onere alla cache intelligente
                            if (UserCache.getUser(finalTargetId) == null) {
                                UserCache.fetchUserAsync(finalTargetId, () -> {
                                    if (adapter != null) {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                        // --- FINE LOGICA PULITA ---
                    }

                    displayedList.clear();
                    displayedList.addAll(masterList);

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    if (callback != null) {
                        callback.onDataLoaded();
                    }
                });
    }

    public void fetchAllOtherUsers(UsersLoadCallback callback) {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<User> allUsers = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String userId = doc.getId();
                    if (!userId.equals(currentUser)) {
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