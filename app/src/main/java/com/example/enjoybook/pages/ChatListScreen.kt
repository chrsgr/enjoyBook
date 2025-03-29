package com.example.enjoybook.pages

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.enjoybook.viewModel.AuthViewModel

import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var chatList by remember { mutableStateOf<List<ChatItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val primaryColor = Color(0xFFB4E4E8)
    val textColor = Color(0xFF333333)

    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()

    // Function to load chats
    fun loadChats() {
        coroutineScope.launch {
            if (currentUser == null) {
                isLoading = false
                return@launch
            }

            val db = FirebaseFirestore.getInstance()
            try {
                // Fetch chats collection using a more flexible query
                val chatsSnapshot = db.collection("chats")
                    .whereArrayContains("participants", currentUser.uid)
                    .get()
                    .await()

                // Process chat documents
                val chats = chatsSnapshot.documents.mapNotNull { chatDoc ->
                    val participants = chatDoc.get("participants") as? List<String> ?: return@mapNotNull null

                    // Find the partner (the other user in the chat)
                    val partnerId = participants.first { it != currentUser.uid }

                    // Fetch partner's user details
                    val partnerSnapshot = db.collection("users")
                        .document(partnerId)
                        .get()
                        .await()

                    // If partner details don't exist, skip this chat
                    if (!partnerSnapshot.exists()) return@mapNotNull null

                    ChatItem(
                        partnerId = partnerId,
                        partnerName = partnerSnapshot.getString("username") ?: "Unknown User",
                        lastMessage = chatDoc.getString("lastMessage") ?: "",
                        lastMessageTimestamp = chatDoc.getLong("lastMessageTimestamp") ?: 0L,
                        profilePicUrl = partnerSnapshot.getString("profilePicUrl") ?: ""
                    )
                }

                // Update chat list
                chatList = chats
                isLoading = false

            } catch (e: Exception) {
                Log.e("ChatListScreen", "Error fetching chats", e)
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }
    }


    fun deleteChat(chatItem: ChatItem) {
        coroutineScope.launch {
            val db = FirebaseFirestore.getInstance()
            val currentUserId = currentUser?.uid ?: return@launch

            try {
                // Find the chat document
                val chatQuery = db.collection("chats")
                    .whereArrayContains("participants", currentUserId)
                    .whereEqualTo("participants", listOf(currentUserId, chatItem.partnerId))
                    .get()
                    .await()

                chatQuery.documents.forEach { chatDoc ->
                    // Mark messages as deleted for the current user
                    val messagesQuery = db.collection("chats")
                        .document(chatDoc.id)
                        .collection("messages")
                        .get()
                        .await()

                    // Batch write to update messages and chat
                    val batch = db.batch()

                    // Update each message to mark as deleted for the current user
                    messagesQuery.documents.forEach { messageDoc ->
                        val deletedForUsers = (messageDoc.get("deletedFor") as? List<String> ?: emptyList()).toMutableList()
                        if (!deletedForUsers.contains(currentUserId)) {
                            deletedForUsers.add(currentUserId)
                            batch.update(
                                messageDoc.reference,
                                "deletedFor",
                                deletedForUsers
                            )
                        }
                    }

                    // Update the chat document to mark as deleted for the current user
                    val deletedChatForUsers = (chatDoc.get("deletedFor") as? List<String> ?: emptyList()).toMutableList()
                    if (!deletedChatForUsers.contains(currentUserId)) {
                        deletedChatForUsers.add(currentUserId)
                        batch.update(
                            chatDoc.reference,
                            "deletedFor",
                            deletedChatForUsers
                        )
                    }

                    // Commit the batch
                    batch.commit().await()

                    // Update local chat list
                    chatList = chatList.filter { it.partnerId != chatItem.partnerId }
                }

            } catch (e: Exception) {
                Log.e("ChatListScreen", "Error deleting chat", e)
                errorMessage = "Error deleting chat: ${e.message}"
            }
        }
    }

    // Trigger chat loading
    LaunchedEffect(currentUser) {
        loadChats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats", color = textColor) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = textColor
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(50.dp),
                        color = primaryColor
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red
                        )
                        Button(onClick = { loadChats() }) {
                            Text("Retry")
                        }
                    }
                }
                chatList.isEmpty() -> {
                    Text(
                        text = "No chats available",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn {
                        items(
                            items = chatList,
                            key = { it.partnerId }
                        ) { chatItem ->
                            SwipeToDeleteItem(
                                item = chatItem,
                                onDelete = { deletedItem ->
                                    deleteChat(deletedItem)
                                }
                            ) { currentItem ->
                                ChatListItem(
                                    chatItem = currentItem,
                                    onClick = {
                                        navController.navigate("messaging/${currentItem.partnerId}")
                                    }
                                )
                            }
                        }

                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun <T> SwipeToDeleteItem(
    item: T,
    onDelete: (T) -> Unit,
    content: @Composable (T) -> Unit
) {
    var isDeleted by remember { mutableStateOf(false) }

    val dismissState = rememberDismissState()

    if (dismissState.isDismissed(DismissDirection.EndToStart)) {
        LaunchedEffect(key1 = true) {
            onDelete(item)
            isDeleted = true
        }
    }

    if (isDeleted) {
        return
    }

    SwipeToDismiss(
        state = dismissState,
        background = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.Default -> Color.Transparent
                    DismissValue.DismissedToEnd -> Color.Transparent
                    DismissValue.DismissedToStart -> Color.Red
                },
                label = "Dismiss Background Color"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        },
        dismissContent = { content(item) },
        directions = setOf(DismissDirection.EndToStart)
    )
}
data class ChatItem(
    val partnerId: String,
    val partnerName: String,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val profilePicUrl: String = ""
)

@Composable
fun ChatListItem(
    chatItem: ChatItem,
    onClick: () -> Unit
) {
    val primaryColor = Color(0xFFB4E4E8)
    val textColor = Color(0xFF333333)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chatItem.profilePicUrl.isNotEmpty()) {
            AsyncImage(
                model = chatItem.profilePicUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(2.dp, primaryColor, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chatItem.partnerName.first().uppercase(),
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Chat Information
        Column {
            Text(
                text = chatItem.partnerName,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = chatItem.lastMessage,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Timestamp
        Text(
            text = formatTimestamp(chatItem.lastMessageTimestamp),
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "Now"
        diff < 3600000 -> "${diff / 60000}m"
        diff < 86400000 -> "${diff / 3600000}h"
        else -> "${diff / 86400000}d"
    }
}