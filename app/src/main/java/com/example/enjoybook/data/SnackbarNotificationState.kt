package com.example.enjoybook.data


data class SnackbarNotificationState(
    val message: String,
    val actionLabel: String? = null,
    val duration: Long = 3000,
    val type: SnackbarType = SnackbarType.INFO,
    val onActionClick: () -> Unit = {}
)

enum class SnackbarType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}
