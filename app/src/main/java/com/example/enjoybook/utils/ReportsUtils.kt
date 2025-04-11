package com.example.enjoybook.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.enjoybook.theme.errorColor
import com.example.enjoybook.theme.primaryColor
import com.example.enjoybook.theme.warningColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun reportHandler(userId: String, username: String?, onDismiss: () -> Unit) {
    val accentColor = Color(0xFF4DB6AC)
    val primaryTextColor = Color(0xFF212121)
    val secondaryTextColor = Color(0xFF757575)
    val cardBackgroundColor = Color.White
    val errorColor = Color(0xFFB00020)
    val warningColor = Color(0xFFFF6D00)

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var reportReason by remember { mutableStateOf("") }
    var showReportSuccessDialog by remember { mutableStateOf(false) }
    var isReporting by remember { mutableStateOf(false) }
    var reportOptions = listOf("Inappropriate content", "Fake account", "Harassment", "Spam", "Other")
    var selectedReportOption by remember { mutableStateOf(reportOptions[0]) }

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun reportUser() {
        if (currentUser == null) {
            errorMessage = "You must be logged in to report accounts"
            showErrorDialog = true
            return
        }
        if (reportReason.isBlank() && selectedReportOption == "Other") {
            errorMessage = "Please provide a reason for the report"
            showErrorDialog = true
            return
        }

        isReporting = true

        val reportData = hashMapOf(
            "reportedUserId" to userId,
            "reportedBy" to currentUser.uid,
            "reportedUsername" to username,
            "reason" to if (selectedReportOption == "Other") reportReason else selectedReportOption,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("reports").add(reportData)
            .addOnSuccessListener {
                // Rimosso il codice per contare i report e bannare l'utente
                isReporting = false
                onDismiss()
                showReportSuccessDialog = true
            }
            .addOnFailureListener { e ->
                isReporting = false
                errorMessage = "Failed to submit report: ${e.message}"
                showErrorDialog = true
            }
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Report,
                    contentDescription = "Report",
                    tint = warningColor,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    "Report Account",
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Why are you reporting this user?",
                    fontWeight = FontWeight.Medium,
                    color = primaryTextColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = {  },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        reportOptions.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable { selectedReportOption = option },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedReportOption == option,
                                    onClick = { selectedReportOption = option },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = accentColor
                                    )
                                )
                                Text(
                                    text = option,
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = primaryTextColor
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = selectedReportOption == "Other") {
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = { Text("Please specify") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            focusedLabelColor = accentColor,
                            cursorColor = accentColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text(
                    "Reports are reviewed by our moderation team.",
                    color = secondaryTextColor,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { reportUser() },
                enabled = !isReporting,
                colors = ButtonDefaults.buttonColors(containerColor = warningColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (isReporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Submit Report", color = Color.White)
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onDismiss() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryTextColor),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Text("Cancel")
            }
        },
        containerColor = cardBackgroundColor,
        shape = RoundedCornerShape(16.dp)
    )

    if (showReportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showReportSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = accentColor,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        "Report Submitted",
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )
                }
            },
            text = {
                Text(
                    "Thank you for your report. We take all reports seriously and will review this account.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showReportSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = cardBackgroundColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Text(
                    "Error",
                    fontWeight = FontWeight.Bold,
                    color = errorColor
                )
            },
            text = {
                Text(
                    errorMessage,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = cardBackgroundColor,
            shape = RoundedCornerShape(16.dp)
        )
    }
}