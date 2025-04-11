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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

        val chatDocumentId = listOf(currentUserId, targetUserId).sorted().joinToString("_")
        db.collection("chats")
            .document(chatDocumentId)
            .get()
            .addOnSuccessListener { chatDoc ->
                if (chatDoc.exists()) {
                    val deletedFor = chatDoc.get("deletedFor") as? List<String> ?: emptyList()

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
            db.collection("messages")
                .whereEqualTo("id", editedMessage.id)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val messageDoc = querySnapshot.documents[0]

                        messageDoc.reference.update(
                            mapOf(
                                "content" to messageContent,
                                "edited" to true
                            )
                        ).addOnSuccessListener {
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

        db.collection("chats")
            .document(chatDocumentId)
            .get()
            .addOnSuccessListener { chatDoc ->
                if (chatDoc.exists()) {
                    val deletedFor = chatDoc.get("deletedFor") as? List<String> ?: emptyList()
                    val updatedDeletedFor = deletedFor.toMutableList()

                    if (updatedDeletedFor.contains(currentUser.uid)) {
                        updatedDeletedFor.remove(currentUser.uid)

                    }

                    db.collection("messages")
                        .document(messageRef.id)
                        .set(messageRef)
                        .addOnSuccessListener {
                            val chatUpdates = hashMapOf<String, Any>(
                                "participants" to listOf(currentUser.uid, targetUserId),
                                "lastMessageTimestamp" to System.currentTimeMillis(),
                                "lastMessage" to messageContent,
                                "lastMessageSenderId" to currentUser.uid,
                                "deletedFor" to updatedDeletedFor
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
                                    sendMessage(db, currentUser, targetUserId, newMessageText,
                                        replyToMessage = null, editedMessage = messageToEdit)
                                    messageToEdit = null
                                }
                                messageToReply != null -> {
                                    sendMessage(db, currentUser, targetUserId, newMessageText,
                                        replyToMessage = messageToReply)
                                    messageToReply = null
                                }
                                else -> {
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

    val timeString = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 250.dp)
                .padding(horizontal = 8.dp)
        ) {
            message.replyToMessageContent?.let { replyContent ->
                Card(
                    modifier = Modifier
                        .align(if (isCurrentUser) Alignment.End else Alignment.Start)
                        .padding(bottom = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.LightGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(30.dp)
                                .background(Color(0xFF2CBABE), shape = RoundedCornerShape(1.dp))
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Text(
                                text = "Reply to message:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            Text(
                                text = replyContent,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .wrapContentWidth()
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
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isCurrentUser) 12.dp else 4.dp,
                    bottomEnd = if (isCurrentUser) 4.dp else 12.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 50.dp)
                ) {
                    Text(
                        text = message.content,
                        color = if (isCurrentUser) Color.White else Color.Black,
                        modifier = Modifier.padding(8.dp),
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp, bottom = 4.dp, top = 0.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Time
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            fontSize = 9.sp
                        )

                        Spacer(modifier = Modifier.width(3.dp))

                        if (isCurrentUser) {
                            if (message.read) {
                                // Double check mark for read
                                Row {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Read",
                                        modifier = Modifier.size(10.dp),
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(10.dp)
                                            .offset(x = (-5).dp),
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                // Single check mark for sent but unread
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Sent",
                                    modifier = Modifier.size(10.dp),
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    if (message.edited) {
                        Text(
                            text = "Edited",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 1.dp)
                                .align(Alignment.End),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            if (expanded) {
                Row(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .align(if (isCurrentUser) Alignment.End else Alignment.Start),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            onReply()
                            expanded = false
                        },
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Reply", fontSize = 12.sp)
                    }

                    if (isCurrentUser) {
                        TextButton(
                            onClick = {
                                onEdit()
                                expanded = false
                            },
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Edit", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = {
                                onDelete()
                                expanded = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Red
                            ),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Delete", fontSize = 12.sp)
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

    db.collection("chats")
        .document(chatDocumentId)
        .get()
        .addOnSuccessListener { chatDoc ->
            var deletionTimestamp: Long = 0

            if (chatDoc.exists()) {

                val deletionField = "deletedTimestamp_$currentUserId"
                deletionTimestamp = chatDoc.getLong(deletionField) ?: 0

                Log.d("MessagingSystem", "Deletion timestamp for $currentUserId: $deletionTimestamp")
            }

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
            defaultFetchMessages(db, currentUserId, targetUserId, onMessagesReceived)
        }
}

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

