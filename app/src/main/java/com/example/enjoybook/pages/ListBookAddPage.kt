package com.example.enjoybook.pages

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.enjoybook.data.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ListBookAddPage(navController: NavController) {
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)
    val deleteColor = Color(0xFFE57373)
    val availableColor = Color(0xFF81C784)
    val requestedColor = Color(0xFFFF9800)
    val unavailableColor = Color(0xFFE57373)
    val secondaryColor = Color(0xFF1A8A8F)

    var booksList by remember { mutableStateOf<List<BookWithId>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = {
            isLoading = true
            scope.launch {
                loadUserBooks(db, userId) { books ->
                    booksList = books
                    isLoading = false
                }
            }
        }
    )

    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = true, key2 = refreshTrigger) {
        loadUserBooks(db, userId) { books ->
            booksList = books
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "YOUR ADD BOOK LIST",
                            color = textColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp
                        )
                    }
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
                    containerColor = Color.White,
                    titleContentColor = textColor
                ),
                windowInsets = WindowInsets(0),
                modifier = Modifier.shadow(elevation = 2.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_book_screen") },
                containerColor = primaryColor,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .wrapContentSize(Alignment.BottomEnd)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Book",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = backgroundColor,
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            if (isLoading && booksList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        strokeWidth = 3.dp
                    )
                }
            } else if (booksList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "No Books",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(bottom = 16.dp),
                        tint = primaryColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No books in your collection",
                        fontSize = 18.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the + button to add your first book",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(booksList) { bookWithId ->
                        BookItem(
                            book = bookWithId.book,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            deleteColor = deleteColor,
                            availableColor = availableColor,
                            requestedColor = requestedColor,
                            unavailableColor = unavailableColor,
                            onDeleteClick = {
                                scope.launch {
                                    deleteBook(db, bookWithId.id)
                                    loadUserBooks(db, userId) { books -> booksList = books }
                                }
                            },
                            onEditClick = {
                                navController.navigate("addPage?bookId=${bookWithId.id}&isEditing=true")
                            },
                            onClick = {
                                navController.navigate("bookDetails/${bookWithId.id}")
                            },
                            onRefresh = {
                                refreshTrigger++
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                contentColor = primaryColor,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}


data class BookWithId(val id: String, val book: Book)

private fun loadUserBooks(db: FirebaseFirestore, userId: String?, onComplete: (List<BookWithId>) -> Unit) {
    if (userId == null) {
        onComplete(emptyList())
        return
    }

    db.collection("books")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { documents ->
            val booksList = documents.map { doc ->
                val availabilityStatus = doc.getString("isAvailable") ?: "available"

                BookWithId(
                    id = doc.id,
                    book = Book(
                        id = doc.id,
                        isAvailable = availabilityStatus,
                        title = doc.getString("title") ?: "",
                        author = doc.getString("author") ?: "",
                        condition = doc.getString("condition") ?: "",
                        description = doc.getString("description") ?: "",
                        type = doc.getString("type") ?: "",
                        edition = doc.getString("edition") ?: "",
                        year = doc.getString("year") ?: "",
                        userUsername = doc.getString("userUsername") ?: "",
                        userId = doc.getString("userId") ?: "",
                        frontCoverUrl = doc.getString("frontCoverUrl") ?: null,
                        backCoverUrl = doc.getString("backCoverUrl") ?: null,
                    )
                )
            }
            onComplete(booksList)
        }
        .addOnFailureListener { exception ->
            println("Error getting documents: $exception")
            onComplete(emptyList())
        }
}

private suspend fun deleteBook(db: FirebaseFirestore, bookId: String) {
    try {
        db.collection("books").document(bookId).delete().await()
    } catch (e: Exception) {
        println("Error deleting book: $e")
    }
}

@Composable
fun BookItem(
    book: Book,
    primaryColor: Color,
    textColor: Color,
    deleteColor: Color,
    availableColor: Color,
    requestedColor: Color,
    unavailableColor: Color,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    var availabilityStatus by remember { mutableStateOf(book.isAvailable ?: "available") }
    var showDialog by remember { mutableStateOf(false) }
    var favoritesCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    LaunchedEffect(book.id) {
        fetchFavoritesCount(db, book.id) { count ->
            favoritesCount = count
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Update Availability") },
            text = {
                Text(
                    text = when(availabilityStatus) {
                        "available" -> "Mark this book as unavailable?"
                        "not available" -> "Mark this book as available again?"
                        "requested" -> "This book is currently requested. Change status?"
                        else -> "Update book availability status?"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        availabilityStatus = if(availabilityStatus == "available") "not available" else "available"
                        showDialog = false

                        scope.launch {
                            updateBookAvailability(
                                db, book.id, availabilityStatus, context, onUpdateComplete = { onRefresh() }
                            )
                        }
                    }
                ) {
                    Text("Confirm", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            titleContentColor = textColor,
            textContentColor = textColor.copy(alpha = 0.7f)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp, 100.dp)
                        .background(primaryColor.copy(alpha = 0.1f))
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!book.frontCoverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = book.frontCoverUrl,
                            contentDescription = "Book Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = primaryColor
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    // Title and Favorites section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = book.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Red
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$favoritesCount",
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = book.author,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = book.type,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }

                    if (book.year != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = book.year.toString(),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(30.dp)
                    ) {
                        when (availabilityStatus) {
                            "available" -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = availableColor.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .clickable { showDialog = true }
                                        .padding(end = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = availableColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Available",
                                            fontSize = 12.sp,
                                            color = availableColor
                                        )
                                    }
                                }
                            }
                            "requested" -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = requestedColor.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .clickable { showDialog = true }
                                        .padding(end = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Pending,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = requestedColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Requested",
                                            fontSize = 12.sp,
                                            color = requestedColor
                                        )
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = unavailableColor.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .clickable { showDialog = true }
                                        .padding(end = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = unavailableColor
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Unavailable",
                                            fontSize = 12.sp,
                                            color = unavailableColor
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(32.dp)
                                .background(primaryColor.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp),
                                tint = primaryColor
                            )
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(32.dp)
                                .background(deleteColor.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = deleteColor
                            )
                        }
                    }
                }
            }
        }
    }
}

suspend fun updateBookAvailability(
    db: FirebaseFirestore,
    bookId: String,
    newStatus: String,
    context: Context,
    onUpdateComplete: () -> Unit
) {
    try {
        val bookRef = db.collection("books").document(bookId)
        val bookDoc = bookRef.get().await()

        bookRef.update("isAvailable", newStatus).await()

        val borrowsQuery = db.collection("borrows")
            .whereEqualTo("bookId", bookId)
            .whereEqualTo("status", "accepted")
            .get()
            .await()

        for (borrowDoc in borrowsQuery.documents) {
            db.collection("borrows").document(borrowDoc.id)
                .update(
                    mapOf(
                        "status" to "returned",
                        "returnDate" to System.currentTimeMillis()
                    )
                ).await()
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                if (newStatus == "available") "Book marked as returned" else "Book status updated",
                Toast.LENGTH_SHORT
            ).show()
            onUpdateComplete()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Error updating book status: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun fetchFavoritesCount(
    db: FirebaseFirestore,
    bookId: String,
    onComplete: (Int) -> Unit
) {
    db.collection("favorites")
        .whereEqualTo("bookId", bookId)
        .get()
        .addOnSuccessListener { documents ->
            onComplete(documents.size())
        }
        .addOnFailureListener { exception ->
            println("Error getting favorites count: $exception")
            onComplete(0)
        }
}