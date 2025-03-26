package com.example.enjoybook.utils

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
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

@Composable
fun NotificationItem(
    notification: Notification,
    primaryColor: Color,
    textColor: Color,
    errorColor: Color,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onDelete: () -> Unit = {}
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
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth()
                )

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

        if (notification.type == "LOAN_REQUEST") {
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
                .update("isAvailable", false)
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
                                message = "La tua richiesta per il libro '${notification.title}' è stata accettata",
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
    db.collection("notifications").document(notification.id)
        .delete()
        .addOnSuccessListener {
            Log.d("Notifications", "Original notification deleted immediately")

            val requesterId = notification.senderId

            val rejectionNotification = Notification(
                recipientId = requesterId,
                senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                message = "La tua richiesta per il libro '${notification.title}' è stata rifiutata",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                type = "LOAN_REJECTED",
                bookId = notification.bookId,
                title = notification.title
            )

            db.collection("notifications").add(rejectionNotification)
                .addOnSuccessListener {
                    Log.d("Notifications", "Rejection notification sent")
                }
                .addOnFailureListener { e ->
                    Log.e("Notifications", "Error sending rejection notification", e)
                }
        }
        .addOnFailureListener { e ->
            Log.e("Notifications", "Error deleting notification", e)
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