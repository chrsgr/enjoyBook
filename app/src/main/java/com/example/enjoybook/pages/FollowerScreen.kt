package com.example.enjoybook.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.enjoybook.data.User
import com.google.firebase.firestore.FirebaseFirestore


@Composable
fun FollowersScreen(navController: NavController, userId: String) {
    val db = FirebaseFirestore.getInstance()
    var followers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        db.collection("follows")
            .whereEqualTo("followingId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val followerIds = documents.map { it.getString("followerId") ?: "" }
                if (followerIds.isNotEmpty()) {
                    fetchUsersFromIds(followerIds) { usersList ->
                        followers = usersList
                        isLoading = false
                    }
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    UserFollowList(
        navController = navController,
        users = followers,
        isLoading = isLoading,
        title = "FOLLOWERS"
    )
}

@Composable
fun FollowingScreen(navController: NavController, userId: String) {
    val db = FirebaseFirestore.getInstance()
    var following by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        db.collection("follows")
            .whereEqualTo("followerId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val followingIds = documents.map { it.getString("followingId") ?: "" }
                if (followingIds.isNotEmpty()) {
                    fetchUsersFromIds(followingIds) { usersList ->
                        following = usersList
                        isLoading = false
                    }
                } else {
                    isLoading = false
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    UserFollowList(
        navController = navController,
        users = following,
        isLoading = isLoading,
        title = "FOLLOWING"

    )
}

private fun fetchUsersFromIds(userIds: List<String>, onComplete: (List<User>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val users = mutableListOf<User>()
    var completedQueries = 0

    if (userIds.isEmpty()) {
        onComplete(emptyList())
        return
    }

    for (userId in userIds) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = User(
                        userId = document.id,
                        name = document.getString("name") ?: "",
                        surname = document.getString("surname") ?: "",
                        username = document.getString("username") ?: "",
                        email = document.getString("email") ?: "",
                        phone = document.getString("phone") ?: "",
                        role = document.getString("role") ?: "",
                        isBanned = document.getBoolean("isBanned"),
                        isPrivate = document.getBoolean("isPrivate"),
                        profilePictureUrl = document.getString("profilePictureUrl") ?: "",
                        bio = document.getString("bio") ?: ""
                    )
                    users.add(user)
                }

                completedQueries++
                if (completedQueries == userIds.size) {
                    onComplete(users)
                }
            }
            .addOnFailureListener {
                completedQueries++
                if (completedQueries == userIds.size) {
                    onComplete(users)
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFollowList(
    navController: NavController,
    users: List<User>,
    isLoading: Boolean,
    title: String
) {
    val primaryColor = Color(0xFF2CBABE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0),


        ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else if (users.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("NO $title")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(users.size) { index ->
                    val user = users[index]
                    UserListItem(user, navController)
                    Divider()
                }
            }
        }
    }
}
@Composable
fun UserListItem(user: User, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navController.navigate("userDetails/${user.userId}")
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.profilePictureUrl,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = user.username,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "${user.name} ${user.surname}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

