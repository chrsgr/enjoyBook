package com.example.enjoybook.data

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
    val status: String = "PENDING"
)

