package com.example.enjoybook.data

import java.util.UUID


data class Message(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val edited: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToMessageContent: String? = null,
    val translatedContent: String? = null,

    )

data class ChatItem(
    val partnerId: String,
    val partnerName: String,
    val lastMessage: String,
    val lastMessageTimestamp: Long,
    val profilePictureUrl: String = "",
    val unreadMessages: Int
)