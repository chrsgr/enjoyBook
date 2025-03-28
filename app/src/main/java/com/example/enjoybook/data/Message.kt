package com.example.enjoybook.data

import java.util.UUID



// Data classes for messaging
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isEdited: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToMessageContent: String? = null,
    val translatedContent: String? = null,

    )

