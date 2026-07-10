package com.example.spottio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _chatItems = MutableLiveData<List<ChatItem>>()
    val chatItems: LiveData<List<ChatItem>> get() = _chatItems

    init {
        loadMessages()
    }

    private fun loadMessages() {
        repository.listenForMessages { rawMessages ->
            _chatItems.value = formatMessagesWithDateHeaders(rawMessages)
        }
    }

    fun sendMessage(text: String) {
        if (text.isNotBlank()) {
            repository.sendMessage(text.trim())
        }
    }

    fun editMessage(docId: String, newText: String) {
        repository.editMessage(docId, newText)
    }

    fun deleteMessage(docId: String, deleteForEveryone: Boolean) {
        repository.deleteMessage(docId, deleteForEveryone) {
            // Ricarica la lista localmente quando nascondiamo un messaggio per noi
            loadMessages()
        }
    }

    private fun formatMessagesWithDateHeaders(messages: List<ChatMessage>): List<ChatItem> {
        val result = mutableListOf<ChatItem>()
        var ultimaDataMostrata = ""

        for (msg in messages) {
            val dataCorrente = formattaDataDivisore(msg.timestamp)
            if (dataCorrente != ultimaDataMostrata) {
                result.add(ChatItem.DateHeader(dataCorrente))
                ultimaDataMostrata = dataCorrente
            }
            result.add(ChatItem.Message(msg))
        }
        return result
    }

    private fun formattaDataDivisore(date: Date): String {
        val calOggi = Calendar.getInstance()
        val calIeri = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val calMsg = Calendar.getInstance().apply { time = date }

        return when {
            calMsg.get(Calendar.YEAR) == calOggi.get(Calendar.YEAR) &&
                    calMsg.get(Calendar.DAY_OF_YEAR) == calOggi.get(Calendar.DAY_OF_YEAR) -> "Oggi"
            calMsg.get(Calendar.YEAR) == calIeri.get(Calendar.YEAR) &&
                    calMsg.get(Calendar.DAY_OF_YEAR) == calIeri.get(Calendar.DAY_OF_YEAR) -> "Ieri"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(date)
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopListening()
    }
}