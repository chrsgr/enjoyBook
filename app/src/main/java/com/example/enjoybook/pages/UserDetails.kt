package com.example.enjoybook.pages

import android.icu.util.Calendar
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.FavoriteBook
import com.example.enjoybook.data.Notification
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.enjoybook.data.User
import com.example.enjoybook.utils.reportHandler
import com.example.enjoybook.utils.requestToFollowUser
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetails(navController: NavController, authViewModel: AuthViewModel, userId: String){

    val db = FirebaseFirestore.getInstance()

    val primaryColor = Color(0xFF2CBABE)
    val secondaryColor = Color(0xFF1A8A8F)

    val textColor = Color(0xFF333333)
    val errorColor = Color(0xFFD32F2F)
    val warningColor = Color(0xFFFF9800)
    val unavailableColor = Color(0xFFFFB74D)


    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val authState = authViewModel.authState.observeAsState().value

    var showReportDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showBannedDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val libraryBooks = remember { mutableStateOf<List<Book>>(emptyList()) }
    val favoritesBooks = remember { mutableStateOf<List<FavoriteBook>>(emptyList()) }
    val readsBooks = remember { mutableStateOf<List<Book>>(emptyList()) }

    var isAdmin by remember { mutableStateOf(false) }
    var isBanned by remember { mutableStateOf(false) }

    var isFollowing by remember { mutableStateOf(false) }
    var followerCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }

    var followRequestStatus by remember { mutableStateOf<String?>(null) }
    val approvedFollowers = remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(userId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .collection("approvedFollowers")
            .get()
            .addOnSuccessListener { documents ->
                val followerIds = documents.documents.map { it.id }
                approvedFollowers.value = followerIds
            }
    }

    LaunchedEffect(currentUser?.uid, userId, user?.isPrivate) {
        if (currentUser != null && userId != currentUser.uid && user?.isPrivate == true) {
            checkFollowRequestStatus(currentUser.uid, userId) { status ->
                followRequestStatus = status
            }
        }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            isLoading = true
            try {
                val userDocument = db.collection("users").document(userId)
                    .get()
                    .await()

                if (userDocument != null && userDocument.exists()) {
                    user = User(
                        userId = userDocument.id,
                        name = userDocument.getString("name") ?: "",
                        surname = userDocument.getString("surname") ?: "",
                        username = userDocument.getString("username") ?: "",
                        email = userDocument.getString("email") ?: "",
                        phone = userDocument.getString("phone") ?: "",
                        role = userDocument.getString("role") ?: "",
                        isBanned = userDocument.getBoolean("isBanned") ?: null,
                        isPrivate = userDocument.getBoolean("isPrivate") ?: null,
                        profilePictureUrl = userDocument.getString("profilePictureUrl") ?: "",
                        bio = userDocument.getString("bio") ?: ""
                    )
                } else {
                    errorMessage = "User not found"
                    showErrorDialog = true
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                showErrorDialog = true
            } finally {
                isLoading = false
            }
        } else {
            errorMessage = "No user ID specified"
            showErrorDialog = true
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        fetchLibraryBooks(userId) { books ->
            libraryBooks.value = books
        }
        fetchFavoriteBooksUser(userId) { books ->
            favoritesBooks.value = books
        }
        fetchReadsUser(userId) { books ->
            readsBooks.value = books
        }
        fetchFollowerCount(userId) { count ->
            followerCount = count
        }

        fetchFollowingCount(userId) { count ->
            followingCount = count
        }

        if (currentUser != null && userId != currentUser.uid) {
            checkFollowStatus(currentUser.uid, userId) { status ->
                isFollowing = status
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState !is AuthState.Authenticated) {
            navController.navigate("login")
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            val userDoc = Firebase.firestore.collection("users").document(uid).get().await()
            isAdmin = userDoc.getString("role") == "admin"
        }

        if (userId.isNotEmpty() && userId != "users") {
            val targetUserDoc = Firebase.firestore.collection("users").document(userId).get().await()
            isBanned = targetUserDoc.getBoolean("isBanned") ?: false
        } else {
            Log.e("Firestore", "Invalid userId: $userId")
        }
    }

    if(isBanned){
        errorMessage = "User banned"
        showBannedDialog = true
        isLoading = false
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    if (userId.isEmpty()) {
                        navController.popBackStack()
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showBannedDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    navController.popBackStack()
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = primaryColor)
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "USER DETAILS",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = secondaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        titleContentColor = textColor
                    ),
                    actions = {
                        if(currentUser != null){
                            if(user?.userId == currentUser.uid){
                                IconButton(
                                    onClick = {
                                        navController.navigate("profile") {
                                            launchSingleTop = true
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.Black
                                    )
                                }
                            }
                            else if (!isAdmin) {
                                IconButton(
                                    onClick = { showReportDialog = true },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Report,
                                        contentDescription = "Report",
                                        tint = warningColor
                                    )
                                }
                            }

                            // Dialog report
                            if (showReportDialog) {
                                reportHandler(
                                    userId = userId,
                                    username = user?.username,
                                    onDismiss = { showReportDialog = false }
                                )
                            }
                        }
                    },
                    windowInsets = WindowInsets(0)
                )
            },
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (user != null) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                        ) {
                            // Profile picture
                            user?.profilePictureUrl?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, primaryColor, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Username
                                Text(
                                    text = user?.username ?: "",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Start
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Name and Surname
                                Text(
                                    text = "${user?.name ?: ""} ${user?.surname ?: ""}",
                                    fontSize = 18.sp,
                                    color = textColor,
                                    textAlign = TextAlign.Start
                                )

                                // Bio section
                                if (user?.bio?.isNotEmpty() == true) {
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = user?.bio ?: "",
                                        fontSize = 16.sp,
                                        color = textColor.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Start,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp, end = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))


                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                        modifier = Modifier
                                            .clickable {
                                                navController.navigate("followers/$userId")
                                            }
                                            .padding(vertical = 8.dp, horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = followerCount.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = "Followers",
                                            fontSize = 14.sp,
                                            color = textColor.copy(alpha = 0.7f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(24.dp))

                                    Column(
                                        horizontalAlignment = Alignment.Start,
                                        modifier = Modifier
                                            .clickable {
                                                navController.navigate("following/$userId")
                                            }
                                            .padding(vertical = 8.dp, horizontal = 8.dp)
                                    ) {
                                        Text(
                                            text = followingCount.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = textColor
                                        )
                                        Text(
                                            text = "Following",
                                            fontSize = 14.sp,
                                            color = textColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }


                                // Controlli per admin/ban (solo se non è l'utente corrente)
                                if (currentUser != null && currentUser.uid != userId) {
                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (isAdmin) {
                                        // Pulsante ban/unban
                                        OutlinedButton(
                                            onClick = {
                                                val newStatus = !isBanned
                                                Firebase.firestore.collection("users").document(userId)
                                                    .update("isBanned", newStatus)
                                                    .addOnSuccessListener { isBanned = newStatus }
                                            },
                                            modifier = Modifier
                                                .width(250.dp)
                                                .height(50.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (isBanned) Color.Green else Color.Red
                                            ),
                                            border = BorderStroke(1.dp, if (isBanned) Color.Green else Color.Red),
                                            shape = RoundedCornerShape(25.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isBanned) Icons.Default.LockOpen else Icons.Default.Block,
                                                    contentDescription = "Ban/Unban",
                                                    tint = if (isBanned) Color.Green else Color.Red,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Text(
                                                    if (isBanned) "Unban Account" else "Ban Account",
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }


                                    }
                                }
                            }
                        }
                    }

                    item {
                        if (currentUser != null && currentUser.uid != userId) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Follow/Unfollow button
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (user?.isPrivate == true && !isFollowing) {
                                                if (followRequestStatus == "pending") {
                                                    cancelFollowRequest(currentUser.uid, userId) {
                                                        followRequestStatus = null
                                                    }
                                                } else {
                                                    requestToFollowUser(currentUser.uid, userId) {
                                                        followRequestStatus = "pending"
                                                    }
                                                }
                                            } else {
                                                if (isFollowing) {
                                                    unfollowUser(currentUser.uid, userId) {
                                                        isFollowing = false
                                                        followerCount--
                                                    }
                                                } else {
                                                    followUser(currentUser.uid, userId) {
                                                        isFollowing = true
                                                        followerCount++
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = when {
                                                isFollowing -> Color(0xFF81C784)
                                                followRequestStatus == "pending" -> Color(0xFFFFB74D)
                                                else -> primaryColor
                                            },
                                            containerColor = when {
                                                isFollowing -> Color.Transparent
                                                followRequestStatus == "pending" -> Color.Transparent
                                                else -> Color.Transparent
                                            }
                                        ),
                                        border = BorderStroke(1.dp, when {
                                            isFollowing -> Color(0xFF81C784)
                                            followRequestStatus == "pending" ->Color(0xFFFFB74D)
                                            else -> primaryColor
                                        }),
                                        shape = RoundedCornerShape(25.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    isFollowing -> Icons.Default.PersonRemove
                                                    followRequestStatus == "pending" -> Icons.Default.HourglassBottom
                                                    else -> Icons.Default.PersonAdd
                                                },
                                                contentDescription = when {
                                                    isFollowing -> "Unfollow"
                                                    followRequestStatus == "pending" -> "Requested"
                                                    else -> "Follow"
                                                },
                                                tint = when {
                                                    isFollowing -> Color(0xFF81C784)
                                                    followRequestStatus == "pending" -> Color(0xFFFFB74D)
                                                    else -> primaryColor
                                                },
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                when {
                                                    isFollowing -> "Following"
                                                    followRequestStatus == "pending" -> "Requested"
                                                    else -> "Follow"
                                                },
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

//MESSAGE

                                    OutlinedButton(
                                        onClick = {
                                            navController.navigate("messaging/$userId")
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = primaryColor
                                        ),
                                        border = BorderStroke(1.dp, primaryColor),
                                        shape = RoundedCornerShape(25.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Message,
                                                contentDescription = "Send Message",
                                                tint = primaryColor,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                "Message",
                                                color = primaryColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }

                    // Sezione libri caricati
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                SectionHeaderUser(
                                    icon = Icons.Default.Book,
                                    title = "${user?.username} library",
                                    primaryColor = primaryColor,
                                    textColor = textColor
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {



                                    if (libraryBooks.value.isEmpty()) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.Default.BookmarkBorder,
                                                contentDescription = null,
                                                tint = Color.LightGray,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "No favorites books yet",
                                                color = Color.Gray,
                                                fontSize = 16.sp
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .horizontalScroll(rememberScrollState())
                                                .fillMaxWidth()
                                        ) {
                                            libraryBooks.value.forEach { book ->

                                                    BookCardUser(
                                                        book = book,
                                                        primaryColor = primaryColor,
                                                        textColor = textColor,
                                                        onClick = {
                                                            navController.navigate("bookDetails/${book.id}")
                                                        }
                                                    )

                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }


                    // Sezione libri letti
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                SectionHeaderUser(
                                    icon = Icons.Default.BookmarkAdded,
                                    title = "${user?.username} reads",
                                    primaryColor = primaryColor,
                                    textColor = textColor
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                        if (currentUser != null) {
                                            val isOwner = currentUser.uid == userId
                                            val isApprovedFollower = approvedFollowers.value.contains(currentUser.uid)

                                            if (user?.isPrivate == true && !isOwner && !isApprovedFollower) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        tint = Color.LightGray,
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        "The list is private",
                                                        color = Color.Gray,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            }
                                            else{
                                            if (readsBooks.value.isEmpty()) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.BookmarkBorder,
                                                        contentDescription = null,
                                                        tint = Color.LightGray,
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        "No books here",
                                                        color = Color.Gray,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier
                                                        .horizontalScroll(rememberScrollState())
                                                        .fillMaxWidth()
                                                ) {
                                                    readsBooks.value.forEach { book ->
                                                        BookCardUser(
                                                            book = book,
                                                            primaryColor = primaryColor,
                                                            textColor = textColor,
                                                            onClick = {
                                                                navController.navigate("bookDetails/${book.id}")
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }


                    // Sezione libri preferiti
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                SectionHeaderUser(
                                    icon = Icons.Default.Favorite,
                                    title = "${user?.username} favorites",
                                    primaryColor = primaryColor,
                                    textColor = textColor
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentUser != null) {
                                        val isOwner = currentUser.uid == userId
                                        val isApprovedFollower = approvedFollowers.value.contains(currentUser.uid)

                                        if (user?.isPrivate == true && !isOwner && !isApprovedFollower) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = Color.LightGray,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    "The list is private",
                                                    color = Color.Gray,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }  else{
                                            if (favoritesBooks.value.isEmpty()) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    androidx.compose.material3.Icon(
                                                        imageVector = Icons.Default.BookmarkBorder,
                                                        contentDescription = null,
                                                        tint = Color.LightGray,
                                                        modifier = Modifier.size(48.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        "No favorites books yet",
                                                        color = Color.Gray,
                                                        fontSize = 16.sp
                                                    )
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier
                                                        .horizontalScroll(rememberScrollState())
                                                        .fillMaxWidth()
                                                ) {
                                                    favoritesBooks.value.forEach { book ->
                                                        FavoriteBookCardUser(
                                                            book = book,
                                                            primaryColor = primaryColor,
                                                            textColor = textColor,
                                                            onClick = {
                                                                navController.navigate("bookDetails/${book.bookId}")
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Messaggio se l'utente è nullo
                    item {
                        Text(
                            text = "No user available",
                            fontSize = 18.sp,
                            color = errorColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeaderUser(
    icon: ImageVector,
    title: String,
    primaryColor: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = primaryColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 16.sp
        )
    }
}


private fun fetchLibraryBooks(userId: String, onComplete: (List<Book>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("books")
        .whereEqualTo("userId", userId)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val booksList = mutableListOf<Book>()
            Log.d("bookslIST", booksList.toString());
            for (document in documents) {
                val book = document.toObject(Book::class.java).copy(
                    id = document.id
                )
                booksList.add(book)
            }
            onComplete(booksList)
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error getting featured books: ", exception)
            onComplete(emptyList())
        }
}

private fun fetchFavoriteBooksUser(userId: String, onComplete: (List<FavoriteBook>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("favorites")
        .orderBy("addedAt", Query.Direction.DESCENDING)
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { documents ->
            val booksList = mutableListOf<FavoriteBook>()
            for (document in documents) {
                val book = FavoriteBook(
                    bookId = document.getString("bookId") ?: "",
                    author = document.getString("author") ?: "",
                    addedAt = document.getTimestamp("addedAt") ?: Timestamp.now(),
                    title = document.getString("title") ?: "",
                    type = document.getString("type") ?: "",
                    userId = document.getString("userId") ?: "",
                    frontCoverUrl = document.getString("frontCoverUrl") ?: null,
                )
                booksList.add(book)
            }
            onComplete(booksList)
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error getting featured books: ", exception)
            onComplete(emptyList())
        }
}

private fun fetchReadsUser(userId: String, onComplete: (List<Book>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("borrows")
        .whereEqualTo("borrowerId", userId)
       // .whereEqualTo("status", "returned")
        .get()
        .addOnSuccessListener { userBorrowsSnapshot ->
            val bookBorrowIds = userBorrowsSnapshot.documents.mapNotNull { it.getString("bookId") }.toSet()

            if (bookBorrowIds.isEmpty()) {
                onComplete(emptyList())
                return@addOnSuccessListener
            }

            db.collection("books")
                .whereIn("id", bookBorrowIds.toList())
                .get()
                .addOnSuccessListener { documents ->
                    val booksList = mutableListOf<Book>()
                    for (document in documents) {
                        val book = document.toObject(Book::class.java).copy(
                            id = document.id
                        )
                        booksList.add(book)
                    }
                    onComplete(booksList)
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error getting featured books: ", exception)
                    onComplete(emptyList())
                }
        }
        .addOnFailureListener { exception ->
            Log.e("Firestore", "Error getting user borrows: ", exception)
            onComplete(emptyList())
        }
}

private fun fetchFollowerCount(userId: String, onComplete: (Int) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    db.collection("follows")
        .whereEqualTo("followingId", userId)
        .get()
        .addOnSuccessListener { documents ->
            onComplete(documents.size())
        }
        .addOnFailureListener { e ->
            Log.e("UserDetails", "Error fetching follower count: ", e)
            onComplete(0)
        }
}

private fun fetchFollowingCount(userId: String, onResult: (Int) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("follows")
        .whereEqualTo("followerId", userId)
        .get()
        .addOnSuccessListener { documents ->
            onResult(documents.size())
        }
        .addOnFailureListener { e ->
            Log.e("UserDetails", "Error getting following count: ", e)
            onResult(0)
        }
}

private fun checkFollowStatus(currentUserId: String, targetUserId: String, onResult: (Boolean) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("follows")
        .whereEqualTo("followerId", currentUserId)
        .whereEqualTo("followingId", targetUserId)
        .get()
        .addOnSuccessListener { documents ->
            onResult(!documents.isEmpty)
        }
        .addOnFailureListener { e ->
            Log.e("UserDetails", "Error checking follow status: ", e)
            onResult(false)
        }
}

private fun checkFollowRequestStatus(requesterId: String, targetId: String, onResult: (String?) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("followRequests")
        .whereEqualTo("requesterId", requesterId)
        .whereEqualTo("targetId", targetId)
        .whereEqualTo("status", "pending")
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                onResult("pending")
            } else {
                onResult(null)
            }
        }
        .addOnFailureListener { e ->
            Log.e("UserDetails", "Error checking follow request status: ", e)
            onResult(null)
        }
}

private fun cancelFollowRequest(requesterId: String, targetId: String, onComplete: () -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("followRequests")
        .whereEqualTo("requesterId", requesterId)
        .whereEqualTo("targetId", targetId)
        .whereEqualTo("status", "pending")
        .get()
        .addOnSuccessListener { documents ->
            for (document in documents) {
                document.reference.delete()
            }

            // Also delete the notification
            db.collection("notifications")
                .whereEqualTo("senderId", requesterId)
                .whereEqualTo("recipientId", targetId)
                .whereEqualTo("type", "FOLLOW_REQUEST")
                .get()
                .addOnSuccessListener { notifications ->
                    for (notification in notifications) {
                        notification.reference.delete()
                    }
                    onComplete()
                }
        }
}


