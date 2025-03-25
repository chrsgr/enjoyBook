package com.example.enjoybook.data

import com.google.firebase.Timestamp

data class Report(
    val id: String = "",
    val reportedBy: String = "",
    val reportedUserId: String = "",
    val reportedUsername: String? = "",
    val reason: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
