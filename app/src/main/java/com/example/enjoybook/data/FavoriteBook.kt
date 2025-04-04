package com.example.enjoybook.data

import com.google.firebase.Timestamp

data class FavoriteBook(
    val bookId: String = "",
    val author: String = "",
    val addedAt: Timestamp = Timestamp.now(),
    val title: String = "",
    val type: String = "",
    val userId: String? = "",
    val frontCoverUrl: String? = null,
)