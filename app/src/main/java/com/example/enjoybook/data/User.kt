package com.example.enjoybook.data

import com.google.type.DateTime

data class User(
    val userId: String = "",
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email : String = "",
    val phone: String? = "",
    val role: String = "",
    val isBanned: Boolean? = false,
    val emailVerified: Boolean = false,
    val isGoogleAuth: Boolean = false,
    val profilePictureUrl: String = "",
    )
