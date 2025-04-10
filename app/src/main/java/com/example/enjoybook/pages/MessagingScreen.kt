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
import com.google.firebase.firestore.FieldValue
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

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var newMessageText by remember { mutableStateOf("") }
    var targetUserName by remember { mutableStateOf("Unknown User") }

    var messageToEdit by remember { mutableStateOf<Message?>(null) }
    var messageToReply by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()

    Log.d("Navigation", "message: ${targetUserId}")

    LaunchedEffect(targetUserId) {
        val currentUserId = currentUser?.uid ?: return@LaunchedEffect

        // First, make sure this chat isn't marked as deleted for the current user
        val chatDocumentId = listOf(currentUserId, targetUserId).sorted().joinToString("_")
        db.collection("chats")
            .document(chatDocumentId)
            .get()
            .addOnSuccessListener { chatDoc ->
                if (chatDoc.exists()) {
                    val deletedFor = chatDoc.get("deletedFor") as? List<String> ?: emptyList()

                    // If chat was deleted for current user, restore it
                    if (deletedFor.contains(currentUserId)) {
                        val updatedDeletedFor = deletedFor.filter { it != currentUserId }
                        db.collection("chats")
                            .document(chatDocumentId)
                            .update("deletedFor", updatedDeletedFor)
                            .addOnSuccessListener {
                                Log.d("Chat", "Chat restored for current user")
                            }
                    }
                }
            }

        // Then proceed with marking messages as read
        markMessagesAsRead(
            db,
            currentUserId,
            targetUserId,
            onSuccess = { Log.d("Chat", "Messaggio marcato come letto all'apertura della chat") },
            onError = { e -> Log.e("Chat", "Errore nel marcare i messaggi: ${e.message}") }
        )

        fetchMessages(db, currentUserId, targetUserId) { fetchedMessages ->
            messages = fetchedMessages.toList()
        }
    }

    LaunchedEffect(messages) {
        listState.animateScrollToItem(0)
        Log.d("ChatMessage", "ListState")
    }

    LaunchedEffect(targetUserId) {
        Log.d("ChatMessage", "TargetUserId")
        db.collection("users").document(targetUserId)
            .get()
            .addOnSuccessListener { document ->
                targetUserName = document.getString("username") ?: "Unknown User"
            }
    }


    // Helper function to update chat metadata
    fun updateChatMetadata(
        db: FirebaseFirestore,
        chatDocumentId: String,
        currentUserId: String,
        targetUserId: String,
        messageContent: String
    ) {
        val chatUpdates = hashMapOf<String, Any>(
            "participants" to listOf(currentUserId, targetUserId),
            "lastMessageTimestamp" to System.currentTimeMillis(),
            "lastMessage" to messageContent,
            "lastMessageSenderId" to currentUserId
        )

        db.collection("chats")
            .document(chatDocumentId)
            .set(chatUpdates, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("MessagingSystem", "Chat document updated")
            }
            .addOnFailureListener { e ->
                Log.e("MessagingSystem", "Error updating chat document", e)
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
        if (currentUser.uid.isBlank() || targetUserId.isBlank()) {
            return
        }

        if (messageContent.isBlank()) {
            return
        }

        val chatDocumentId = listOf(currentUser.uid, targetUserId).sorted().joinToString("_")

        if (editedMessage != null) {
            // Get reference to the existing message document
            db.collection("messages")
                .whereEqualTo("id", editedMessage.id)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val messageDoc = querySnapshot.documents[0]

                        // Update the message
                        messageDoc.reference.update(
                            mapOf(
                                "content" to messageContent,
                                "edited" to true,
                                "timestamp" to System.currentTimeMillis()
                            )
                        ).addOnSuccessListener {
                            // Update chat metadata
                            updateChatMetadata(db, chatDocumentId, currentUser.uid, targetUserId, messageContent)
                            Log.d("MessagingSystem", "Message edited successfully")
                        }.addOnFailureListener { e ->
                            Log.e("MessagingSystem", "Error editing message", e)
                        }
                    } else {
                        Log.e("MessagingSystem", "Message to edit not found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MessagingSystem", "Error finding message to edit", e)
                }
            return
        }

        val messageRef = when {
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
                    read = false
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
                    read = false
                )
            }
        }

        // First check if chat exists and handle deletion status
        db.collection("chats")
            .document(chatDocumentId)
            .get()
            .addOnSuccessListener { chatDoc ->
                if (chatDoc.exists()) {
                    val deletedFor = chatDoc.get("deletedFor") as? List<String> ?: emptyList()
                    val updatedDeletedFor = deletedFor.toMutableList()

                    // If current user had deleted this chat, remove them from deletedFor
                    if (updatedDeletedFor.contains(currentUser.uid)) {
                        updatedDeletedFor.remove(currentUser.uid)

                        // Don't remove the deletion timestamp - we'll still filter based on it
                    }

                    // Save the message first
                    db.collection("messages")
                        .document(messageRef.id)
                        .set(messageRef)
                        .addOnSuccessListener {
                            // Then update the chat metadata
                            val chatUpdates = hashMapOf<String, Any>(
                                "participants" to listOf(currentUser.uid, targetUserId),
                                "lastMessageTimestamp" to System.currentTimeMillis(),
                                "lastMessage" to messageContent,
                                "lastMessageSenderId" to currentUser.uid,
                                "deletedFor" to updatedDeletedFor  // Updated deletedFor list
                            )

                            db.collection("chats")
                                .document(chatDocumentId)
                                .set(chatUpdates, SetOptions.merge())
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
                } else {
                    // Create new chat - no deletion history
                    db.collection("messages")
                        .document(messageRef.id)
                        .set(messageRef)
                        .addOnSuccessListener {
                            val chatData = hashMapOf(
                                "participants" to listOf(currentUser.uid, targetUserId),
                                "lastMessageTimestamp" to System.currentTimeMillis(),
                                "lastMessage" to messageContent,
                                "lastMessageSenderId" to currentUser.uid,
                                "deletedFor" to emptyList<String>()
                            )

                            db.collection("chats")
                                .document(chatDocumentId)
                                .set(chatData)
                                .addOnSuccessListener {
                                    Log.d("MessagingSystem", "New chat created")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("MessagingSystem", "Error creating chat", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("MessagingSystem", "Error sending first message", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MessagingSystem", "Error checking chat status", e)
                // Fallback to just sending message
                db.collection("messages")
                    .document(messageRef.id)
                    .set(messageRef)
                    .addOnSuccessListener {
                        Log.d("MessagingSystem", "Message sent (fallback)")
                    }
                    .addOnFailureListener { e2 ->
                        Log.e("MessagingSystem", "Error sending message (fallback)", e2)
                    }
            }
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
                },
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                state = listState,
                reverseLayout = true

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
                                    messages = messages.filter {
                                        it != message && it.replyToMessageId != message.id
                                    }
                                }
                            }
                        }
                    )
                }
            }

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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


                IconButton(
                    onClick = {
                        if (currentUser != null) {
                            when {
                                messageToEdit != null -> {
                                    // Handle edit
                                    sendMessage(db, currentUser, targetUserId, newMessageText,
                                        replyToMessage = null, editedMessage = messageToEdit)
                                    messageToEdit = null
                                }
                                messageToReply != null -> {
                                    // Handle reply
                                    sendMessage(db, currentUser, targetUserId, newMessageText,
                                        replyToMessage = messageToReply)
                                    messageToReply = null
                                }
                                else -> {
                                    // Normal message
                                    sendMessage(db, currentUser, targetUserId, newMessageText)
                                }
                            }
                            newMessageText = ""
                        }
                    },
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
    val replyingMessages = messages.filter { it.replyToMessageId == message.id }

    val batch = db.batch()

    db.collection("messages")
        .whereEqualTo("senderId", message.senderId)
        .whereEqualTo("receiverId", message.receiverId)
        .whereEqualTo("content", message.content)
        .whereEqualTo("timestamp", message.timestamp)
        .get()
        .addOnSuccessListener { snapshot ->
            snapshot.documents.firstOrNull()?.let { document ->
                batch.delete(document.reference)

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
            message.replyToMessageContent?.let { replyContent ->
                Card(
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .padding(bottom = 4.dp)
                        .align(if (isCurrentUser) Alignment.End else Alignment.Start),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.LightGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .background(Color(0xFF2CBABE), shape = RoundedCornerShape(2.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "Reply to message:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Bold
                            )

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
            }

            Card(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .align(if (isCurrentUser) Alignment.End else Alignment.Start)
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

                    if (message.edited) {
                        Text(
                            text = "Edited",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .align(Alignment.End)
                        )
                    }
                }
            }

            if (expanded) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(if (isCurrentUser) Alignment.End else Alignment.Start),
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

fun markMessagesAsRead(
    db: FirebaseFirestore,
    currentUserId: String,
    targetUserId: String,
    onSuccess: () -> Unit = {},
    onError: (Exception) -> Unit = {}
) {
    Log.d("Chat", "Cercando messaggi non letti da $targetUserId a $currentUserId")

    db.collection("messages")
        .whereEqualTo("receiverId", currentUserId)
        .whereEqualTo("senderId", targetUserId)
        .whereEqualTo("read", false)
        .get()
        .addOnSuccessListener { snapshot ->
            val count = snapshot.documents.size
            Log.d("Chat", "Trovati $count messaggi da marcare come letti")

            if (count == 0) {
                onSuccess()
                return@addOnSuccessListener
            }

            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                Log.d("Chat", "Marcando messaggio ID: ${doc.id} come letto")
                batch.update(doc.reference, "read", true)
            }

            batch.commit()
                .addOnSuccessListener {
                    Log.d("Chat", "Aggiornati con successo $count messaggi a read=true")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("Chat", "Errore nel batch update: ${e.message}")
                    onError(e)
                }
        }
        .addOnFailureListener { e ->
            Log.e("Chat", "Errore nella query per messaggi non letti: ${e.message}")
            onError(e)
        }
}


fun fetchMessages(
    db: FirebaseFirestore,
    currentUserId: String,
    targetUserId: String,
    onMessagesReceived: (List<Message>) -> Unit
) {
    val chatDocumentId = listOf(currentUserId, targetUserId).sorted().joinToString("_")

    // First get the chat document to check deletion status
    db.collection("chats")
        .document(chatDocumentId)
        .get()
        .addOnSuccessListener { chatDoc ->
            var deletionTimestamp: Long = 0

            if (chatDoc.exists()) {
                // Check if current user previously deleted this chat
                val deletedFor = chatDoc.get("deletedFor") as? List<String> ?: emptyList()

                // Get the user-specific deletion timestamp field
                val deletionField = "deletedTimestamp_$currentUserId"
                deletionTimestamp = chatDoc.getLong(deletionField) ?: 0

                Log.d("MessagingSystem", "Deletion timestamp for $currentUserId: $deletionTimestamp")
            }

            // Now fetch messages with appropriate filtering
            val messageQuery = db.collection("messages")
                .where(
                    Filter.or(
                        Filter.and(
                            Filter.equalTo("senderId", currentUserId),
                            Filter.equalTo("receiverId", targetUserId)
                        ),
                        Filter.and(
                            Filter.equalTo("senderId", targetUserId),
                            Filter.equalTo("receiverId", currentUserId)
                        )
                    )
                )
                .orderBy("timestamp", Query.Direction.ASCENDING)

            messageQuery.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MessagingSystem", "Error fetching messages: ${e.message}")
                    return@addSnapshotListener
                }

                val fetchedMessages = snapshot?.toObjects(Message::class.java) ?: emptyList()

                // Filter out messages before deletion timestamp if needed
                val filteredMessages = if (deletionTimestamp > 0) {
                    Log.d("MessagingSystem", "Filtering messages before $deletionTimestamp")
                    fetchedMessages.filter { it.timestamp > deletionTimestamp }
                } else {
                    fetchedMessages
                }

                Log.d("MessagingSystem",
                    "Received ${fetchedMessages.size} messages, showing ${filteredMessages.size} after filtering")

                onMessagesReceived(filteredMessages)
            }
        }
        .addOnFailureListener { e ->
            Log.e("MessagingSystem", "Error checking chat deletion status: ${e.message}")
            // Fallback to fetching all messages if we can't determine deletion status
            defaultFetchMessages(db, currentUserId, targetUserId, onMessagesReceived)
        }
}

// Fallback function to fetch all messages if deletion check fails
private fun defaultFetchMessages(
    db: FirebaseFirestore,
    currentUserId: String,
    targetUserId: String,
    onMessagesReceived: (List<Message>) -> Unit
) {
    db.collection("messages")
        .where(
            Filter.or(
                Filter.and(
                    Filter.equalTo("senderId", currentUserId),
                    Filter.equalTo("receiverId", targetUserId)
                ),
                Filter.and(
                    Filter.equalTo("senderId", targetUserId),
                    Filter.equalTo("receiverId", currentUserId)
                )
            )
        )
        .orderBy("timestamp", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("MessagingSystem", "Error fetching messages: ${e.message}")
                return@addSnapshotListener
            }

            val fetchedMessages = snapshot?.toObjects(Message::class.java) ?: emptyList()
            onMessagesReceived(fetchedMessages)
        }
}

