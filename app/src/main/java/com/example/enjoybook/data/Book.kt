package com.example.enjoybook.data

import com.google.firebase.Timestamp

data class Book(
    val id: String = "",
    val isAvailable: String = "",
    val author: String = "",
    val condition: String = "",
    val description: String = "",
    val edition : String = "",
    val review: String = "",
    val title: String = "",
    val titleLower: String = "",
    val type: String = "",
    val userEmail: String? = "",
    val userUsername: String = "",
    val userId: String? = "",
    val year : String = "",
    val frontCoverUrl: String? = null,
    val backCoverUrl: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp? = null,
    val favoritesCount: Int = 0
)



