package com.example.enjoybook.data

data class Book(
    val id: String = "",
    val isAvailable: Boolean? = true,
    val author: String = "",
    val condition: String = "",
    val description: String = "",
    val edition : String = "",
    val review: String = "",
    val title: String = "",
    val titleLower: String = "",
    val type: String = "",
    val userEmail: String? = "",
    val userId: String? = "",
    val year : String = "",
    val frontCoverUrl: String? = null,
    val backCoverUrl: String? = null,

)



