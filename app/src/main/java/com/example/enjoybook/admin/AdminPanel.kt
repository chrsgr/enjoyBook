package com.example.enjoybook.admin

import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.LentBook
import com.example.enjoybook.data.Report
import com.example.enjoybook.data.User
import com.example.enjoybook.theme.primaryColor
import com.example.enjoybook.theme.secondaryColor
import com.example.enjoybook.theme.textColor
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanel(navController: NavController, authViewModel: AuthViewModel){

    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var reportsCollection = remember { mutableStateOf<List<Report>>(emptyList()) }
    val usersList = remember { mutableStateOf<List<User>>(emptyList()) }

    val primaryColor = Color(0xFFB4E4E8)
    val secondaryColor = Color(0xFF1A8A8F)
    val secondaryBackgroundColor = (primaryColor.copy(alpha = 0.1f))


    val authState = authViewModel.authState.observeAsState().value

    LaunchedEffect(authState) {
        if (authState !is AuthState.Authenticated) {
            navController.navigate("login")
        }
    }

    LaunchedEffect(Unit) {
        fetchReports { reports ->
            reportsCollection.value = reports
            isLoading = false
        }
        fetchBannedUsers { users ->
            usersList.value = users
            isLoading = false
        }
    }


    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    if (currentUser == null) {
                        navController.popBackStack()
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }


    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    //.background(secondaryBackgroundColor)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material.IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = secondaryColor
                    )
                }

                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                androidx.compose.material.Text(
                    text = "ADMIN PANEL",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                backgroundColor = Color(0xFF2CBABE),
                contentColor = primaryColor,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.White,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        androidx.compose.material.Text("Reports",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        androidx.compose.material.Text(
                            "Banned",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material.CircularProgressIndicator(color = primaryColor)
                }
            } else {
                when (selectedTab) {
                    0 -> ListReportsGrid(reportsCollection.value, navController, "You haven't lent any reports yet")
                    1 -> UsersBannedGrid(usersList.value, navController, "No user banned")
                }
            }
        }
    }
}

fun fetchReports(onComplete: (List<Report>) -> Unit) {
    FirebaseFirestore.getInstance().collection("reports")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val reportsList = mutableListOf<Report>()

            for (document in documents) {
                val report = document.toObject(Report::class.java).copy(
                    id = document.id
                )
                reportsList.add(report)
            }
            onComplete(reportsList)
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error getting featured reports: ", exception)
            onComplete(emptyList())
        }

}

fun fetchBannedUsers(onComplete: (List<User>) -> Unit) {
    FirebaseFirestore.getInstance().collection("users")
        .whereEqualTo("isBanned", true)
        //.orderBy("username", Query.Direction.ASCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val usersList = mutableListOf<User>()

            for (document in documents) {
                val user = document.toObject(User::class.java).copy(
                    userId = document.id
                )
                usersList.add(user)
            }
            onComplete(usersList)
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error getting featured users: ", exception)
            onComplete(emptyList())
        }

}

@Composable
fun ListReportsGrid(
    reports: List<Report>,
    navController: NavController,
    emptyMessage: String
) {
    if (reports.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Report,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material.Text(
                    text = emptyMessage,
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                //.weight(1f)
        ) {
            items(reports) { report ->
                ReportUserCard(report, navController)
            }
        }
    }
}

@Composable
fun UsersBannedGrid(
    users: List<User>,
    navController: NavController,
    emptyMessage: String
) {
    if (users.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material.Text(
                    text = emptyMessage,
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
            //.weight(1f)
        ) {
            items(users) { user ->
                UserBannedCard(user, navController)
            }
        }
    }
}

@Composable
fun ReportUserCard(report: Report, navController: NavController) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickable {
                    // Navigate to BookDetails screen when the book is clicked
                    navController.navigate("userDetails/${report.reportedUserId}")
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = "Reported user: ${report.reportedUsername}",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Reason: ${report.reason}",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Date: ${
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(
                            report.timestamp.toDate()
                        )
                    }",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}


@Composable
fun UserBannedCard(user: User, navController: NavController) {

    val isAvatar = remember { mutableStateOf(true) }

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickable {
                    navController.navigate("userDetails/${user.userId}")
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar placeholder
            if (isAvatar.value && user?.profilePictureUrl != null) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = user?.profilePictureUrl

                    val painter = rememberAsyncImagePainter(imageUrl)
                    val state = painter.state

                    if (state is AsyncImagePainter.State.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }

                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(primaryColor, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(primaryColor.copy(alpha = 0.2f), CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // User details
            Column {
                Text(
                    text = user.username,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${user.name} ${user.surname}",
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

}