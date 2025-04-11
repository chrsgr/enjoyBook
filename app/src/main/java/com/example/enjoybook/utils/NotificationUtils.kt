package com.example.enjoybook.utils

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.enjoybook.data.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.enjoybook.pages.CustomSnackbar
import com.example.enjoybook.pages.NotificationManager
import com.google.firebase.firestore.FieldValue

@Composable
fun NotificationItem(
    notification: Notification,
    primaryColor: Color,
    textColor: Color,
    errorColor: Color,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onDelete: () -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onAcceptFollow: () -> Unit = {},
    onRejectFollow: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (notification.type == "FOLLOW" || notification.type == "FOLLOW_REQUEST") {
                    val message = notification.message
                    val usernameEndIndex = if (notification.type == "FOLLOW") {
                        message.indexOf(" started following you")
                    } else {
                        message.indexOf(" requested to follow you")
                    }

                    if (usernameEndIndex > 0) {
                        val username = message.substring(0, usernameEndIndex)
                        val remainingText = if (notification.type == "FOLLOW") {
                            " started following you"
                        } else {
                            " requested to follow you"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = username,
                                fontSize = 14.sp,
                                color = Color.Blue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onUserClick(notification.senderId) }
                            )
                            Text(
                                text = remainingText,
                                fontSize = 14.sp,
                                color = textColor
                            )
                        }
                    } else {
                        Text(
                            text = notification.message,
                            fontSize = 14.sp,
                            color = textColor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = notification.message,
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        notification.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Delete button
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete notification",
                    tint = Color.Red
                )
            }
        }

        when (notification.type) {
            "LOAN_REQUEST" -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onReject() },
                        colors = ButtonDefaults.textButtonColors(contentColor = errorColor)
                    ) {
                        Text("Reject")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onAccept() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Accept", color = Color.White)
                    }
                }
            }

            "FOLLOW_REQUEST" -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onRejectFollow() },
                        colors = ButtonDefaults.textButtonColors(contentColor = errorColor)
                    ) {
                        Text("Reject")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onAcceptFollow() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Accept", color = Color.White)
                    }
                }
            }
        }

        Divider(
            color = Color.LightGray.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

fun markNotificationsAsRead() {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val db = FirebaseFirestore.getInstance()

    db.collection("notifications")
        .whereEqualTo("recipientId", currentUser.uid)
        .whereEqualTo("isRead", false)
        .get()
        .addOnSuccessListener { documents ->
            val batch = db.batch()
            for (document in documents) {
                batch.update(document.reference, "isRead", true)
            }
            batch.commit().addOnSuccessListener {
            }
        }
}

fun handleAcceptLoanRequest(notification: Notification) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    db.collection("notifications").document(notification.id)
        .delete()
        .addOnSuccessListener {
            Log.d("Notifications", "Original notification deleted immediately")

            db.collection("books").document(notification.bookId)
                .update("isAvailable", "not available")
                .addOnSuccessListener {
                    Log.d("Notifications", "Book marked as unavailable")

                    val requesterId = notification.senderId

                    val borrowData = hashMapOf(
                        "bookId" to notification.bookId,
                        "ownerId" to currentUser?.uid,
                        "borrowerId" to requesterId,
                        "borrowDate" to System.currentTimeMillis(),
                        "status" to "accepted",
                        "title" to notification.title
                    )

                    db.collection("borrows").add(borrowData)
                        .addOnSuccessListener { documentReference ->
                            Log.d("Notifications", "Borrow record created with ID: ${documentReference.id}")

                            val confirmationNotification = Notification(
                                recipientId = requesterId,
                                senderId = currentUser?.uid ?: "",
                                message = "Your book request '${notification.title}' is accepted",
                                timestamp = System.currentTimeMillis(),
                                isRead = false,
                                type = "LOAN_ACCEPTED",
                                bookId = notification.bookId,
                                title = notification.title
                            )

                            db.collection("notifications").add(confirmationNotification)
                                .addOnSuccessListener {
                                    Log.d("Notifications", "Acceptance notification sent")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Notifications", "Error sending acceptance notification", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Notifications", "Error creating borrow record", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Notifications", "Error updating book status", e)
                }
        }
        .addOnFailureListener { e ->
            Log.e("Notifications", "Error deleting notification", e)
        }
}


fun handleRejectLoanRequest(notification: Notification) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return

    db.collection("books").document(notification.bookId)
        .update(mapOf("isAvailable" to "available"))
        .addOnSuccessListener {
            Log.d("Notifications", "Book status updated to available")

            db.collection("notifications").document(notification.id)
                .delete()
                .addOnSuccessListener {
                    Log.d("Notifications", "Notification deleted successfully")

                    val rejectionNotification = Notification(
                        recipientId = notification.senderId,
                        senderId = currentUser.uid,
                        message = "Your book request '${notification.title}' is rejected",
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        type = "LOAN_REJECTED",
                        bookId = notification.bookId,
                        title = notification.title,

                    )

                    db.collection("notifications").add(rejectionNotification)
                        .addOnSuccessListener {
                            Log.d("Notifications", "Rejection notification sent")
                        }
                }
        }
}

fun deleteNotification(notificationId: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("notifications").document(notificationId)
        .delete()
        .addOnSuccessListener {
            Log.d("Notifications", "Notification deleted successfully")
        }
        .addOnFailureListener { e ->
            Log.e("Notifications", "Error deleting notification", e)
        }
}


@Composable
fun NotificationHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val notificationState by NotificationManager.notificationState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        content()

        notificationState?.let { state ->
            CustomSnackbar(
                state = state,
                onDismiss = { NotificationManager.dismissNotification() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
        }
    }
}



@Composable
fun NotificationsDialog(
    notifications: List<Notification>,
    showNotificationPopup: Boolean,
    onDismiss: () -> Unit,
    navController: NavController,
    primaryColor: Color,
    textColor: Color,
    errorColor: Color,
    removeNotificationLocally: (String) -> Unit
) {
    if (showNotificationPopup) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier.width(320.dp),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textColor
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }

                    androidx.compose.material.Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    var selectedTabIndex by remember { mutableStateOf(0) }
                    val tabs = listOf("Books", "Follows")

                    val bookNotifications = notifications.filter {
                        it.type.contains("LOAN") || (it.bookId.isNotEmpty() && !it.type.contains("FOLLOW"))
                    }

                    val followNotifications = notifications.filter {
                        it.type == "FOLLOW" || it.type == "FOLLOW_REQUEST" ||
                                it.type == "FOLLOW_ACCEPTED"
                    }

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = Color.Black,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                height = 2.dp,
                                color = primaryColor
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (selectedTabIndex) {
                        0 -> {
                            // Books tab
                            if (bookNotifications.isEmpty()) {
                                EmptyNotificationsMessage()
                            } else {
                                NotificationsList(
                                    notifications = bookNotifications,
                                    primaryColor = primaryColor,
                                    textColor = textColor,
                                    errorColor = errorColor,
                                    navController = navController,
                                    removeNotificationLocally = removeNotificationLocally,
                                    onUserClick = { userId ->
                                        navController.navigate("userDetails/$userId")
                                        onDismiss()
                                    }
                                )
                            }
                        }
                        1 -> {
                            if (followNotifications.isEmpty()) {
                                EmptyNotificationsMessage()
                            } else {
                                NotificationsList(
                                    notifications = followNotifications,
                                    primaryColor = primaryColor,
                                    textColor = textColor,
                                    errorColor = errorColor,
                                    navController = navController,
                                    removeNotificationLocally = removeNotificationLocally,
                                    onUserClick = { userId ->
                                        navController.navigate("userDetails/$userId")
                                        onDismiss()
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

@Composable
fun EmptyNotificationsMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No notifications",
            color = Color.Gray,
            fontSize = 16.sp
        )
    }
}

@Composable
fun NotificationsList(
    notifications: List<Notification>,
    primaryColor: Color,
    textColor: Color,
    errorColor: Color,
    navController: NavController,
    removeNotificationLocally: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        items(notifications.size) { index ->
            val notification = notifications[index]
            NotificationItem(
                notification = notification,
                primaryColor = primaryColor,
                textColor = textColor,
                errorColor = errorColor,
                onAccept = {
                    handleAcceptLoanRequest(notification)
                    removeNotificationLocally(notification.id)
                },
                onReject = {
                    handleRejectLoanRequest(notification)
                    removeNotificationLocally(notification.id)
                },
                onAcceptFollow = {
                    handleAcceptFollowRequest(notification)
                    removeNotificationLocally(notification.id)
                },
                onRejectFollow = {
                    handleRejectFollowRequest(notification)
                    removeNotificationLocally(notification.id)
                },
                onDelete = {
                    deleteNotification(notification.id)
                    removeNotificationLocally(notification.id)
                },
                onUserClick = onUserClick
            )
        }
    }
}

 fun requestToFollowUser(currentUserId: String, targetUserId: String, onComplete: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val followRequestData = hashMapOf(
        "requesterId" to currentUserId,
        "targetId" to targetUserId,
        "status" to "pending",
        "timestamp" to FieldValue.serverTimestamp()
    )

    db.collection("followRequests")
        .add(followRequestData)
        .addOnSuccessListener { documentReference ->
            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { userDocument ->
                    val username = userDocument.getString("username") ?: ""

                    val notification = Notification(
                        recipientId = targetUserId,
                        senderId = currentUserId,
                        message = "$username requested to follow you",
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        type = "FOLLOW_REQUEST",
                        bookId = "",
                        title = ""
                    )

                    db.collection("notifications").add(notification)
                        .addOnSuccessListener {
                            Log.d("UserDetails", "Follow request notification sent")
                            onComplete()
                        }
                        .addOnFailureListener { e ->
                            Log.e("UserDetails", "Error sending follow request notification", e)
                            onComplete()
                        }
                }
        }
        .addOnFailureListener { e ->
            Log.e("UserDetails", "Error creating follow request: ", e)
        }
}

fun handleAcceptFollowRequest(notification: Notification) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    db.collection("notifications").document(notification.id)
        .delete()
        .addOnSuccessListener {
            db.collection("followRequests")
                .whereEqualTo("requesterId", notification.senderId)
                .whereEqualTo("targetId", currentUser?.uid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { requests ->
                    for (request in requests) {
                        request.reference.delete()
                    }

                    val followData = hashMapOf(
                        "followerId" to notification.senderId,
                        "followingId" to currentUser?.uid,
                        "timestamp" to FieldValue.serverTimestamp()
                    )

                    db.collection("follows").add(followData)
                        .addOnSuccessListener {
                            val approvedFollowerData = hashMapOf(
                                "userId" to notification.senderId,
                                "approvedAt" to FieldValue.serverTimestamp()
                            )

                            db.collection("users").document(currentUser?.uid ?: "")
                                .collection("approvedFollowers")
                                .document(notification.senderId)
                                .set(approvedFollowerData)
                                .addOnSuccessListener {
                                    Log.d("UserDetails", "Added user to approved followers")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("UserDetails", "Error adding to approved followers", e)
                                }

                            db.collection("users").document(currentUser?.uid ?: "")
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    val username = userDoc.getString("username") ?: ""

                                    val acceptNotification = Notification(
                                        recipientId = notification.senderId,
                                        senderId = currentUser?.uid ?: "",
                                        message = "$username accept your follow request",
                                        timestamp = System.currentTimeMillis(),
                                        isRead = false,
                                        type = "FOLLOW_ACCEPTED",
                                        bookId = "",
                                        title = ""
                                    )

                                    db.collection("notifications").add(acceptNotification)
                                }
                        }
                }
        }
}

fun handleRejectFollowRequest(notification: Notification) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    db.collection("notifications").document(notification.id)
        .delete()
        .addOnSuccessListener {
            db.collection("followRequests")
                .whereEqualTo("requesterId", notification.senderId)
                .whereEqualTo("targetId", currentUser?.uid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener { requests ->
                    for (request in requests) {
                        request.reference.delete()
                    }
                }
        }
}
