package com.example.enjoybook.pages

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.enjoybook.data.Message
import com.example.enjoybook.viewModel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.UUID





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMessagingScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    targetUserId: String,
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var newMessageText by remember { mutableStateOf("") }
    var targetUserName by remember { mutableStateOf("Unknown User") }

    // Nuovo stato per la modifica e la risposta
    var messageToEdit by remember { mutableStateOf<Message?>(null) }
    var messageToReply by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()

    // Recupera i messaggi
    LaunchedEffect(targetUserId) {
        val currentUserId = currentUser?.uid ?: return@LaunchedEffect

        fetchMessages(db, currentUserId, targetUserId) { fetchedMessages ->
            messages = fetchedMessages.toList()
            Log.d("MessagingSystem", "Fetched messages: ${fetchedMessages.size}")
            markMessagesAsRead(db, currentUserId, targetUserId)
        }

    }

    LaunchedEffect(messages) {
        listState.animateScrollToItem(0)
    }

    // Recupera i dettagli dell'utente di destinazione
    LaunchedEffect(targetUserId) {
        db.collection("users").document(targetUserId)
            .get()
            .addOnSuccessListener { document ->
                targetUserName = document.getString("username") ?: "Unknown User"
            }
    }

    fun sendMessage(
        db: FirebaseFirestore,
        currentUser: FirebaseUser,
        targetUserId: String,
        messageContent: String,
        replyToMessage: Message? = null,
        editedMessage: Message? = null
    ) {
        // Validate inputs
        if (currentUser.uid.isBlank() || targetUserId.isBlank()) {
            Log.e("MessagingSystem", "Invalid user IDs: Current User ID: ${currentUser.uid}, Target User ID: $targetUserId")
            return
        }

        if (messageContent.isBlank()) {
            Log.e("MessagingSystem", "Cannot send empty message")
            return
        }

        // Create a unique chat document ID that is always the same regardless of sender/receiver order
        val chatDocumentId = listOf(currentUser.uid, targetUserId).sorted().joinToString("_")

        // Prepare the message object
        val messageRef = when {
            editedMessage != null -> {
                editedMessage.copy(
                    content = messageContent,
                    isEdited = true,
                    timestamp = System.currentTimeMillis()
                )
            }
            replyToMessage != null -> {
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUser.uid,
                    senderName = currentUser.displayName ?: "Anonymous",
                    receiverId = targetUserId,
                    content = messageContent,
                    timestamp = System.currentTimeMillis(),
                    replyToMessageId = replyToMessage.id,
                    replyToMessageContent = replyToMessage.content,
                    isRead = false
                )
            }
            else -> {
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = currentUser.uid,
                    senderName = currentUser.displayName ?: "Anonymous",
                    receiverId = targetUserId,
                    content = messageContent,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            }
        }

        // Detailed logging
        Log.d("MessagingSystem", "Sending message:")
        Log.d("MessagingSystem", "Message ID: ${messageRef.id}")
        Log.d("MessagingSystem", "Sender ID: ${messageRef.senderId}")
        Log.d("MessagingSystem", "Receiver ID: ${messageRef.receiverId}")
        Log.d("MessagingSystem", "Content: ${messageRef.content}")

        // Save message to Firestore
        db.collection("messages")
            .document(messageRef.id)
            .set(messageRef)
            .addOnSuccessListener {
                Log.d("MessagingSystem", "Message saved successfully")

                // Update chat document
                db.collection("chats")
                    .document(chatDocumentId)
                    .set(mapOf(
                        "participants" to listOf(currentUser.uid, targetUserId),
                        "lastMessageTimestamp" to System.currentTimeMillis(),
                        "lastMessage" to messageContent,
                        "lastMessageSenderId" to currentUser.uid
                    ), SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("MessagingSystem", "Chat document updated")
                    }
                    .addOnFailureListener { e ->
                        Log.e("MessagingSystem", "Error updating chat document", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("MessagingSystem", "Error sending message", e)
            }
    }
    val sendMessage = sendMessage@{
        if (auth.currentUser == null || newMessageText.isBlank()) return@sendMessage

        val currentUser = auth.currentUser!!

        // Prepara il messaggio da inviare
        val messageToSend = when {
            // Editing an existing message
            messageToEdit != null -> {
                messageToEdit!!.copy(
                    content = newMessageText,
                    isEdited = true
                )
            }
            // Replying to a message
            messageToReply != null -> {
                Message(
                    senderId = currentUser.uid,
                    senderName = currentUser.displayName ?: "Anonymous",
                    receiverId = targetUserId,
                    content = newMessageText,
                    replyToMessageId = messageToReply?.id,
                    replyToMessageContent = messageToReply?.content
                )
            }
            // New message
            else -> {
                Message(
                    senderId = currentUser.uid,
                    senderName = currentUser.displayName ?: "Anonymous",
                    receiverId = targetUserId,
                    content = newMessageText
                )
            }
        }

        // Chiama il nuovo metodo sendMessage
        sendMessage(
            db = db,
            currentUser = currentUser,
            targetUserId = targetUserId,
            messageContent = newMessageText,
            replyToMessage = messageToReply,
            editedMessage = messageToEdit
        )

        // Aggiorna lo stato locale dei messaggi
        messages = messages + messageToSend
        newMessageText = ""
        messageToReply = null
        messageToEdit = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = targetUserName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Lista dei messaggi
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                state = listState, // Usa lo stato della lista qui
                reverseLayout = true // Per mostrare i messaggi dal basso verso l'alto

            ) {
                items(messages.reversed()) { message ->
                    MessageItem(
                        message = message,
                        isCurrentUser = message.senderId == currentUser?.uid,
                        onReply = { messageToReply = message },
                        onEdit = {
                            if (message.senderId == currentUser?.uid) {
                                messageToEdit = message
                                newMessageText = message.content
                            }
                        },
                        onDelete = {
                            if (message.senderId == currentUser?.uid) {
                                deleteMessage(
                                    db,
                                    message,
                                    messages
                                ) {
                                    // Rimuovi il messaggio e i suoi reply dalla lista locale
                                    messages = messages.filter {
                                        it != message && it.replyToMessageId != message.id
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Indicatore di risposta o modifica
            if (messageToReply != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Replying to: ${messageToReply?.content}",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { messageToReply = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Reply")
                    }
                }
            }

            if (messageToEdit != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Editing message", modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        messageToEdit = null
                        newMessageText = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Edit")
                    }
                }
            }

            // Area di input del messaggio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TextField per il messaggio
                TextField(
                    value = newMessageText,
                    onValueChange = { newMessageText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    placeholder = { Text("Message...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.LightGray.copy(alpha = 0.2f),
                        focusedContainerColor = Color.White
                    )
                )

                // Pulsante di invio
                IconButton(
                    onClick = { sendMessage() },
                    modifier = Modifier
                        .background(Color(0xFF2CBABE), shape = CircleShape)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White
                    )
                }
            }
        }
    }
}


fun deleteMessage(
    db: FirebaseFirestore,
    message: Message,
    messages: List<Message>,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    // Trova i messaggi che rispondono a questo messaggio
    val replyingMessages = messages.filter { it.replyToMessageId == message.id }

    // Batch per eliminare il messaggio principale e i messaggi di risposta
    val batch = db.batch()

    // Trova e aggiungi il documento del messaggio principale al batch
    db.collection("messages")
        .whereEqualTo("senderId", message.senderId)
        .whereEqualTo("receiverId", message.receiverId)
        .whereEqualTo("content", message.content)
        .whereEqualTo("timestamp", message.timestamp)
        .get()
        .addOnSuccessListener { snapshot ->
            snapshot.documents.firstOrNull()?.let { document ->
                batch.delete(document.reference)

                // Elimina anche i messaggi di risposta
                replyingMessages.forEach { replyMessage ->
                    db.collection("messages")
                        .whereEqualTo("senderId", replyMessage.senderId)
                        .whereEqualTo("receiverId", replyMessage.receiverId)
                        .whereEqualTo("content", replyMessage.content)
                        .whereEqualTo("timestamp", replyMessage.timestamp)
                        .get()
                        .addOnSuccessListener { replySnapshot ->
                            replySnapshot.documents.firstOrNull()?.let { replyDoc ->
                                batch.delete(replyDoc.reference)
                            }
                        }
                }

                // Esegui il batch
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("MessagingSystem", "Messaggio e risposte eliminati con successo")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "MessagingSystem",
                            "Errore nell'eliminazione dei messaggi: ${e.message}"
                        )
                        onFailure(e)
                    }
            } ?: run {
                Log.d("MessagingSystem", "Nessun messaggio trovato per la cancellazione")
                onFailure(Exception("Messaggio non trovato"))
            }
        }
        .addOnFailureListener { e ->
            Log.e("MessagingSystem", "Errore nella query per cancellare il messaggio: ${e.message}")
            onFailure(e)
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column {
            // Mostra il contesto della risposta se esiste
            message.replyToMessageContent?.let { replyContent ->
                Card(
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .padding(bottom = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.LightGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Linea verticale di demarcazione
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .background(Color(0xFF2CBABE), shape = RoundedCornerShape(2.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = replyContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .combinedClickable(
                        onClick = { expanded = !expanded },
                        onLongClick = { expanded = true }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentUser) Color(0xFF2CBABE) else Color.LightGray.copy(
                        alpha = 0.5f
                    )
                ),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                )
            ) {
                Column {
                    Text(
                        text = message.content,
                        color = if (isCurrentUser) Color.White else Color.Black,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 16.sp
                    )

                    // Mostra l'indicatore di modificato
                    if (message.isEdited) {
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .align(Alignment.End)
                        )
                    }
                }
            }

            // Menu a discesa per le azioni
            if (expanded) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        onReply()
                        expanded = false
                    }) {
                        Text("Reply")
                    }

                    if (isCurrentUser) {
                        TextButton(onClick = {
                            onEdit()
                            expanded = false
                        }) {
                            Text("Edit")
                        }

                        TextButton(
                            onClick = {
                                onDelete()
                                expanded = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Red
                            )
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}




    // Funzione per marcare i messaggi come letti
fun markMessagesAsRead(
    db: FirebaseFirestore,
    currentUserId: String,
    targetUserId: String
) {
        db.collection("messages")
            .where(
                Filter.and(
                    Filter.equalTo("receiverId", currentUserId),
                    Filter.equalTo("senderId", targetUserId),
                    Filter.equalTo("isRead", false)
                )
            )
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit()
            }
    }


fun fetchMessages(
    db: FirebaseFirestore,
    currentUserId: String,
    targetUserId: String,
    onMessagesReceived: (List<Message>) -> Unit
) {
    // Create a chat document ID that is consistent for both users
    val chatDocumentId = listOf(currentUserId, targetUserId).sorted().joinToString("_")

    db.collection("messages")
        .whereIn("senderId", listOf(currentUserId, targetUserId))
        .whereIn("receiverId", listOf(currentUserId, targetUserId))
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("MessagingSystem", "Error fetching messages: ${e.message}")
                return@addSnapshotListener
            }

            val fetchedMessages = snapshot?.toObjects(Message::class.java) ?: emptyList()

            val filteredMessages = fetchedMessages.filter { message ->
                (message.senderId == currentUserId && message.receiverId == targetUserId) ||
                        (message.senderId == targetUserId && message.receiverId == currentUserId)
            }

            Log.d("MessagingSystem", "Fetched messages for chat $chatDocumentId:")
            filteredMessages.forEach { message ->
                Log.d("MessagingSystem", "Message: ${message.id}, Sender: ${message.senderId}, Receiver: ${message.receiverId}, Content: ${message.content}")
            }

            onMessagesReceived(filteredMessages)
        }
}




