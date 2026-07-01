package com.example.spottio;

import android.widget.ListView;

import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChatDatabaseHelper {

    private final FirebaseFirestore db;
    private final String currentUser;
    private final String targetUser;
    private final boolean isGroup;
    private final String conversationId;
    private final ChatAdapter adapter;
    private final ArrayList<ChatMessage> messageList;
    private final ListView lvMessages;

    public ChatDatabaseHelper(FirebaseFirestore db, String currentUser, String targetUser, boolean isGroup,
                              String conversationId, ChatAdapter adapter, ArrayList<ChatMessage> messageList, ListView lvMessages) {
        this.db = db;
        this.currentUser = currentUser;
        this.targetUser = targetUser;
        this.isGroup = isGroup;
        this.conversationId = conversationId;
        this.adapter = adapter;
        this.messageList = messageList;
        this.lvMessages = lvMessages;
    }

    public void listenForMessages(Fragment fragment) {
        if (currentUser == null || targetUser == null) return;

        Query query;
        if (isGroup) {
            query = db.collection("groups").document(targetUser).collection("chats");
        } else {
            query = db.collection("chats").document(conversationId).collection("messages");
        }

        query.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || !fragment.isAdded()) return;

                    messageList.clear();
                    String ultimaDataMostrata = "";

                    for (QueryDocumentSnapshot doc : value) {
                        String sender = doc.getString("sender");
                        String txt = doc.getString("text");
                        if (txt == null) txt = "";

                        Timestamp ts = doc.getTimestamp("timestamp");
                        Date date = (ts != null) ? ts.toDate() : new Date();

                        String dataCorrente = formattaDataDivisore(date);
                        boolean mostraData = !dataCorrente.equals(ultimaDataMostrata);
                        if (mostraData) {
                            ultimaDataMostrata = dataCorrente;
                        }

                        boolean isMe = sender != null && sender.equals(currentUser);

                        String cachedUri = null;
                        boolean isVerified = false;

                        if (sender != null) {
                            UserCache.UserData cachedData = UserCache.getUser(sender);
                            if (cachedData != null) {
                                cachedUri = cachedData.pfpUri;
                                isVerified = cachedData.isVerified;
                            }
                        }

                        ChatMessage msg = new ChatMessage(
                                doc.getId(), txt, isMe, date, mostraData, dataCorrente, sender != null ? sender : "Sistema", isGroup, cachedUri, isVerified
                        );
                        messageList.add(msg);

                        // --- LOGICA PULITA: Usiamo il metodo asincrono centralizzato ---
                        if (sender != null && UserCache.getUser(sender) == null) {
                            UserCache.fetchUserAsync(sender, () -> {
                                if (fragment.isAdded()) {
                                    UserCache.UserData data = UserCache.getUser(sender);
                                    if (data != null) {
                                        // Aggiorniamo i messaggi già caricati nella lista a cui mancava la foto
                                        for (ChatMessage m : messageList) {
                                            if (m.senderName != null && m.senderName.equals(sender)) {
                                                m.profilePicUri = data.pfpUri;
                                                m.isVerified = data.isVerified;
                                            }
                                        }
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                        // --- FINE LOGICA PULITA ---
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        lvMessages.setSelection(messageList.size() - 1);
                    }
                });
    }

    public void sendMessage(String text) {
        if (currentUser == null) return;

        Map<String, Object> msg = new HashMap<>();
        msg.put("sender", currentUser);
        msg.put("text", text);
        msg.put("timestamp", FieldValue.serverTimestamp());

        String previewId;

        if (isGroup) {
            previewId = targetUser;
            db.collection("groups").document(targetUser).collection("chats").add(msg);
        } else {
            msg.put("receiver", targetUser);
            msg.put("conversationId", conversationId);
            previewId = conversationId;
            db.collection("chats").document(conversationId).collection("messages").add(msg);
        }

        aggiornaPreview(previewId, text);
    }

    private void aggiornaPreview(String previewId, String text) {
        Map<String, Object> previewData = new HashMap<>();
        previewData.put("lastMessage", text);
        previewData.put("lastUpdate", FieldValue.serverTimestamp());

        if (!isGroup) {
            previewData.put("participants", java.util.Arrays.asList(currentUser, targetUser));
            previewData.put("isGroup", false);
        }

        db.collection("chat_previews").document(previewId).set(previewData, com.google.firebase.firestore.SetOptions.merge());
    }

    private String formattaDataDivisore(Date date) {
        Calendar calOggi = Calendar.getInstance();
        Calendar calIeri = Calendar.getInstance();
        calIeri.add(Calendar.DAY_OF_YEAR, -1);
        Calendar calMsg = Calendar.getInstance();
        calMsg.setTime(date);

        if (calMsg.get(Calendar.YEAR) == calOggi.get(Calendar.YEAR) &&
                calMsg.get(Calendar.DAY_OF_YEAR) == calOggi.get(Calendar.DAY_OF_YEAR)) {
            return "Oggi";
        } else if (calMsg.get(Calendar.YEAR) == calIeri.get(Calendar.YEAR) &&
                calMsg.get(Calendar.DAY_OF_YEAR) == calIeri.get(Calendar.DAY_OF_YEAR)) {
            return "Ieri";
        } else {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(date);
        }
    }
}