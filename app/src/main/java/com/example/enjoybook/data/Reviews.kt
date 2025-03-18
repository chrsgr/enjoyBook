package com.example.enjoybook.data

import java.security.Timestamp

data class Review(
    val id: String = "",
    val bookId: String = "",
    val userEmail: String = "",
    val review: String = "",
    val timestamp: Timestamp? = null
)