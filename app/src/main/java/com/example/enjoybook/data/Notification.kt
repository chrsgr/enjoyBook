package com.example.enjoybook.data

import android.R

// Notification data class (add this to your models)
data class Notification(
    val recipientId: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val type: String = "",
    val bookId: String = "",
    val id: String = "",
    val senderId: String = "",
    val title: String = "",
)