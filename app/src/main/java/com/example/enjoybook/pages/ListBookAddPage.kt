package com.example.enjoybook.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text("YOUR BOOK LIST") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addPage") }
            ) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(booksList) { bookWithId ->
                        BookItem(
                            book = bookWithId.book,
                            onDeleteClick = {
                                scope.launch {
                                    deleteBook(db, bookWithId.id)
                                    loadUserBooks(db, userId) { books -> booksList = books }
                                }
                            },
                            onEditClick = {
                                navController.navigate("addPage?isEditing=true")
                            }
                        )
                    }
                }
                if (isLoading && booksList.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                PullRefreshIndicator(
                    refreshing = isLoading,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )

            }     }
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
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    var isAvailable by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = book.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "By: ${book.author}",
                fontSize = 16.sp
            )

            Text(
                text = "Type: ${book.type}",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Delete button (trash icon)
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Edit button
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EDIT")
                    }
                }

                // Available toggle button
                Button(
                    onClick = { isAvailable = !isAvailable },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAvailable) Color(0xFF8BC34A) else Color(0xFFFF5252)
                    )
                ) {
                    Text(if (isAvailable) "available" else "not available")
                }
            }
        }
    }
}