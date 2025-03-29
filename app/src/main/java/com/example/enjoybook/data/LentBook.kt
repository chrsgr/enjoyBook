package com.example.enjoybook.data

data class LentBook(
    val book: Book,
    val borrowId: String,
    val borrowerId: String,
    val borrowerName: String,
    val borrowerEmail: String
)