package com.example.enjoybook.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.enjoybook.data.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ListBookAddPage(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    // Define theme colors to match rest of app
    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)
    val deleteColor = Color(0xFFE57373)
    val availableColor = Color(0xFF81C784)
    val unavailableColor = Color(0xFFFFB74D)

    // State to hold book list
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

    LaunchedEffect(key1 = true) {
        loadUserBooks(db, userId) { books ->
            booksList = books
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "YOUR BOOK LIST",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addPage") },
                containerColor = primaryColor,
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Book",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        containerColor = backgroundColor
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
                // Empty state
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
                            unavailableColor = unavailableColor,
                            onDeleteClick = {
                                scope.launch {
                                    deleteBook(db, bookWithId.id)
                                    loadUserBooks(db, userId) { books -> booksList = books }
                                }
                            },
                            onEditClick = {
                                // Pass the book ID for editing
                                navController.navigate("addPage?bookId=${bookWithId.id}&isEditing=true")
                            },
                            onClick = {
                                // Navigate to book detail
                                navController.navigate("book/${bookWithId.id}")
                            }
                        )
                    }
                    // Add some bottom padding for better UX with FAB
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
                BookWithId(
                    id = doc.id,
                    book = doc.toObject(Book::class.java)
                )
            }
            onComplete(booksList)
        }
        .addOnFailureListener { exception ->
            println("Error getting documents: $exception")
            onComplete(emptyList())
        }
}

// Function to delete a book
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
    unavailableColor: Color,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onClick: () -> Unit
) {
    var isAvailable by remember { mutableStateOf(book.isAvailable ?: true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover placeholder
            Box(
                modifier = Modifier
                    .size(70.dp, 100.dp)
                    .background(primaryColor.copy(alpha = 0.1f))
                    .clip(RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = primaryColor
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = book.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

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
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isAvailable) availableColor.copy(alpha = 0.2f) else unavailableColor.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clickable { isAvailable = !isAvailable }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAvailable) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isAvailable) availableColor else unavailableColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAvailable) "Available" else "Not Available",
                                fontSize = 12.sp,
                                color = if (isAvailable) availableColor else unavailableColor
                            )
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