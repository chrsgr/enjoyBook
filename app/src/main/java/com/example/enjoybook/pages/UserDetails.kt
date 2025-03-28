package com.example.enjoybook.pages

import android.icu.util.Calendar
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Report
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.enjoybook.data.Book
import com.example.enjoybook.theme.primaryColor
import com.example.enjoybook.theme.textColor
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.auth.User
import com.example.enjoybook.data.User
import com.example.enjoybook.utils.reportHandler
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import okhttp3.internal.platform.Jdk9Platform.Companion.isAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetails(navController: NavController, authViewModel: AuthViewModel, userId: String){

    val db = FirebaseFirestore.getInstance()

    val primaryColor = Color(0xFF2CBABE)
    val secondaryColor = Color(0xFF1A8A8F)
    val backgroundColor = (primaryColor.copy(alpha = 0.1f))

    val textColor = Color(0xFF333333)
    val errorColor = Color(0xFFD32F2F)
    val successColor = Color(0xFF4CAF50)
    val warningColor = Color(0xFFFF9800)
    val cardBackground = Color.White

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val authState = authViewModel.authState.observeAsState().value

    var showReportDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var favorites by remember { mutableStateOf<List<Book>>(emptyList()) }
    var booksAvailable by remember { mutableStateOf<List<Book>>(emptyList()) }

    var isAdmin by remember { mutableStateOf(false) }
    var isBanned by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            isLoading = true
            try {
                // Recupero dei dati utente
                val userDocument = db.collection("users").document(userId)
                    .get()
                    .await()

                if (userDocument != null && userDocument.exists()) {
                    user = userDocument.toObject(User::class.java)

                    val favoritesSnapshot = db.collection("favorites")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()

                    val userBooksSnapshot = db.collection("books")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()

                    val bookIds = favoritesSnapshot.documents.mapNotNull { it.getString("bookId") }

                    favorites = if (bookIds.isNotEmpty()) {
                        db.collection("books")
                            .whereIn("id", bookIds)
                            .get()
                            .await()
                            .documents.map { document ->
                                Book(
                                    id = document.id,
                                    title = document.getString("title") ?: "",
                                    author = document.getString("author") ?: "",
                                    type = document.getString("type") ?: "",
                                    isAvailable = document.getBoolean("isAvailable") ?: true,
                                    frontCoverUrl = document.getString("frontCoverUrl") ?: null,
                                )
                            }
                    }

                    else {
                        emptyList()
                    }

                    booksAvailable = userBooksSnapshot.documents.map { document ->
                        Book(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            author = document.getString("author") ?: "",
                            type = document.getString("type") ?: "",
                            timestamp = document.getTimestamp("timestamp") ?: Timestamp.now(),
                            isAvailable = document.getBoolean("isAvailable") ?: true,
                            frontCoverUrl = document.getString("frontCoverUrl") ?: null,
                        )
                    }


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

        val targetUserDoc = Firebase.firestore.collection("users").document(userId).get().await()
        isBanned = targetUserDoc.getBoolean("isBanned") ?: false
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
                // Controlla se l'utente è nullo
                if (user != null) {
                    // Sezione profilo utente
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                        ) {
                            // Immagine profilo
                            user?.profilePictureUrl?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, primaryColor, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ){
                                // Username
                                Text(
                                    text = user?.username ?: "",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Start
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Nome e Cognome
                                Text(
                                    text = "${user?.name ?: ""} ${user?.surname ?: ""}",
                                    fontSize = 18.sp,
                                    color = textColor,
                                    textAlign = TextAlign.Start
                                )

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
                                    } else {
                                        // Pulsante report
                                        OutlinedButton(
                                            onClick = { showReportDialog = true },
                                            modifier = Modifier
                                                .width(250.dp)
                                                .height(50.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = warningColor
                                            ),
                                            border = BorderStroke(1.dp, warningColor),
                                            shape = RoundedCornerShape(25.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Report,
                                                    contentDescription = "Report",
                                                    tint = warningColor,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Text(
                                                    "Report Account",
                                                    color = warningColor,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        // Dialog report
                                        if (showReportDialog) {
                                            reportHandler(userId, user?.username, showReportDialog)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Aggiungi questo come un nuovo item dopo i blocchi precedenti
                    item {
                        if (currentUser != null && currentUser.uid != userId) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        navController.navigate("messaging/$userId")
                                    },
                                    modifier = Modifier
                                        .width(250.dp)
                                        .height(50.dp),
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
                                            "Send Message",
                                            color = primaryColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        if (currentUser != null && currentUser.uid == userId) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        navController.navigate("chatList")
                                    },
                                    modifier = Modifier
                                        .width(250.dp)
                                        .height(50.dp),
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
                                            contentDescription = "Messaggi",
                                            tint = primaryColor,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            "Messages",
                                            color = primaryColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
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
                                    if (booksAvailable.isEmpty()) {
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
                                            booksAvailable.forEach { book ->
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
                                    if (favorites.isEmpty()) {
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
                                                "No favorites books here",
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
                                            favorites.forEach { book ->
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

@Composable
fun BookCardUser(
    book: Book,
    primaryColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    val sevenDaysAgo = Timestamp(calendar.time)

    val isNew = book.timestamp?.toDate()?.after(sevenDaysAgo.toDate()) == true

    val isAvailable = book?.isAvailable

    val isFrontCover = remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .padding(end = 16.dp)
            .width(140.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (isFrontCover.value && book?.frontCoverUrl != null)  {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                        .background(primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = book?.frontCoverUrl

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
                        contentDescription = "Front Cover",
                        modifier = Modifier.fillMaxSize(),
                        //contentScale = ContentScale.Crop
                    )

                    if (isNew && isAvailable != null && isAvailable == true) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(primaryColor, shape = RoundedCornerShape(bottomEnd = 8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    else if ((isNew && isAvailable != null && isAvailable == false) || (!isNew && isAvailable != null && isAvailable == false)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Red)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NOT AVAILABLE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            else {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                        .background(primaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(36.dp)
                    )

                    if (isNew && isAvailable != null && isAvailable == true) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .background(primaryColor, shape = RoundedCornerShape(bottomEnd = 8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    else if ((isNew && isAvailable != null && isAvailable == false) || (!isNew && isAvailable != null && isAvailable == false)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(primaryColor, shape = RoundedCornerShape(bottomEnd = 8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "NOT AVAILABLE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }



            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = book.title,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = book.author,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = book.type,
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            }
        }
    }
}