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
import com.example.enjoybook.data.ChatItem
import com.example.enjoybook.viewModel.AuthViewModel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Filter

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    val coroutineScope = rememberCoroutineScope()


     suspend fun addChatItemToList(
        db: FirebaseFirestore,
        currentUserId: String,
        partnerId: String,
        chatItems: MutableList<ChatItem>
    ) {
        try {
            val partnerSnapshot = db.collection("users")
                .document(partnerId)
                .get()
                .await()

            if (!partnerSnapshot.exists()) return

            val unreadMessagesCount = db.collection("messages")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("senderId", partnerId)
                .whereEqualTo("read", false)
                .get()
                .await()
                .size()

            val recentMessageQuery = db.collection("messages")
                .where(
                    Filter.or(
                        Filter.and(
                            Filter.equalTo("senderId", currentUserId),
                            Filter.equalTo("receiverId", partnerId)
                        ),
                        Filter.and(
                            Filter.equalTo("senderId", partnerId),
                            Filter.equalTo("receiverId", currentUserId)
                        )
                    )
                )
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val lastMessageContent: String
            val lastMessageTimestamp: Long

            if (recentMessageQuery.documents.isNotEmpty()) {
                val lastMessageDoc = recentMessageQuery.documents[0]
                lastMessageContent = lastMessageDoc.getString("content") ?: ""
                lastMessageTimestamp = lastMessageDoc.getLong("timestamp") ?: System.currentTimeMillis()
            } else {
                lastMessageContent = ""
                lastMessageTimestamp = System.currentTimeMillis()
            }

            chatItems.add(ChatItem(
                partnerId = partnerId,
                partnerName = partnerSnapshot.getString("username") ?: "Unknown User",
                lastMessage = lastMessageContent,
                lastMessageTimestamp = lastMessageTimestamp,
                profilePictureUrl = partnerSnapshot.getString("profilePictureUrl") ?: "",
                unreadMessages = unreadMessagesCount
            ))
        } catch (e: Exception) {
            Log.e("ChatListScreen", "Error adding chat item for partner $partnerId", e)
        }
    }

    fun loadChats() {
        coroutineScope.launch {
            if (currentUser == null) {
                isLoading = false
                return@launch
            }

            val db = FirebaseFirestore.getInstance()
            try {
                val chatsSnapshot = db.collection("chats")
                    .whereArrayContains("participants", currentUser.uid)
                    .get()
                    .await()

                val chatItems = mutableListOf<ChatItem>()

                for (chatDoc in chatsSnapshot.documents) {
                    val participants = chatDoc.get("participants") as? List<String> ?: continue
                    val deletedFor = chatDoc.get("deletedFor") as? List<String> ?: emptyList()

                    if (deletedFor.contains(currentUser.uid)) {
                        continue
                    }

                    val partnerId = participants.firstOrNull { it != currentUser.uid } ?: continue
                    addChatItemToList(db, currentUser.uid, partnerId, chatItems)
                }

                val unrespondedMessagesQuery = db.collection("messages")
                    .whereEqualTo("receiverId", currentUser.uid)
                    .whereEqualTo("read", false)
                    .get()
                    .await()

                val senderIds = unrespondedMessagesQuery.documents
                    .mapNotNull { it.getString("senderId") }
                    .distinct()

                for (senderId in senderIds) {
                    if (chatItems.none { it.partnerId == senderId }) {
                        addChatItemToList(db, currentUser.uid, senderId, chatItems)
                    }
                }

                chatList = chatItems.sortedByDescending { it.lastMessageTimestamp }
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
                val chatDocumentId = listOf(currentUserId, chatItem.partnerId).sorted().joinToString("_")
                val chatDocRef = db.collection("chats").document(chatDocumentId)
                val chatDoc = chatDocRef.get().await()

                if (chatDoc.exists()) {
                    Log.d("ChatListScreen", "Found chat document: ${chatDoc.id}")

                    val deletedFor = (chatDoc.get("deletedFor") as? List<String> ?: emptyList()).toMutableList()

                    if (!deletedFor.contains(currentUserId)) {
                        deletedFor.add(currentUserId)

                        val currentTime = System.currentTimeMillis()
                        val fieldName = "deletedTimestamp_$currentUserId"

                        val updates = mapOf(
                            "deletedFor" to deletedFor,
                            fieldName to currentTime
                        )

                        chatDocRef.update(updates)
                            .addOnSuccessListener {
                                Log.d("ChatListScreen", "Successfully updated deletedFor and timestamp")
                                chatList = chatList.filter { it.partnerId != chatItem.partnerId }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ChatListScreen", "Error updating deletedFor", e)
                                errorMessage = "Error deleting chat: ${e.message}"
                            }
                    }
                } else {
                    Log.e("ChatListScreen", "Could not find chat document")
                    errorMessage = "Error: Chat document not found"
                }
            } catch (e: Exception) {
                Log.e("ChatListScreen", "Error in deleteChat", e)
                errorMessage = "Error deleting chat: ${e.message}"
            }
        }
    }

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


@Composable
fun ChatListItem(
    chatItem: ChatItem,
    onClick: () -> Unit
) {
    val primaryColor = Color(0xFFB4E4E8)
    val textColor = Color(0xFF333333)
    val colorBadge = Color(0xFF2CBABE)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (chatItem.profilePictureUrl.isNotEmpty()) {
            AsyncImage(
                model = chatItem.profilePictureUrl,
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

        Column{
            Text(
                text = formatTimestamp(chatItem.lastMessageTimestamp),
                color = Color.Gray,
                fontSize = 12.sp
            )
            if (chatItem.unreadMessages > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge(
                    containerColor = colorBadge
                ) {
                    Text(
                        text = chatItem.unreadMessages.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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