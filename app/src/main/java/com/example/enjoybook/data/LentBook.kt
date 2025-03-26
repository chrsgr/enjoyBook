package com.example.enjoybook.data

data class LentBook(
    val book: Book,
    val borrowerName: String,
    val borrowerEmail: String
)