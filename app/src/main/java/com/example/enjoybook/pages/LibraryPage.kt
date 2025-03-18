package com.example.enjoybook.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.enjoybook.data.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.lazy.grid.items


@Composable
fun LibraryPage(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var ownedBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var borrowedBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }

    val primaryColor = Color(0xFFB4E4E8)


    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val db = FirebaseFirestore.getInstance()

            db.collection("borrows")
                .whereEqualTo("borrowerId", currentUser.uid)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener { borrowDocs ->
                    val borrowedBookIds = borrowDocs.mapNotNull { it.getString("bookId") }

                    if (borrowedBookIds.isEmpty()) {
                        isLoading = false
                        return@addOnSuccessListener
                    }

                    db.collection("books")
                        .whereIn(FieldPath.documentId(), borrowedBookIds)
                        .get()
                        .addOnSuccessListener { bookDocs ->
                            ownedBooks = bookDocs.map { doc ->
                                Book(
                                    id = doc.id,
                                    title = doc.getString("title") ?: "",
                                    author = doc.getString("author") ?: "",
                                    type = doc.getString("type") ?: "",
                                    userId = doc.getString("userId") ?: "",
                                    isAvailable = false,
                                    userEmail = doc.getString("userEmail") ?: ""
                                )
                            }
                            isLoading = false
                        }
                        .addOnFailureListener {
                            isLoading = false
                        }
                }
                .addOnFailureListener {
                    isLoading = false
                }

            // Recupera i libri che l'utente ha dato in prestito
            db.collection("books")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    borrowedBooks = documents.map { doc ->
                        Book(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            author = doc.getString("author") ?: "",
                            type = doc.getString("type") ?: "",
                            userId = doc.getString("userId") ?: "",
                            isAvailable = doc.getBoolean("isAvailable") ?: true,
                            userEmail = doc.getString("userEmail") ?: ""
                        )
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        }
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
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                Text(
                    text = "YOUR LIBRARY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            TabRow(
                selectedTabIndex = selectedTab,
                backgroundColor =  Color(0xFF2CBABE),
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
                        Text(
                            "My Books (${ownedBooks.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Borrowed (${borrowedBooks.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else {
                when (selectedTab) {
                    0 -> BookGrid(ownedBooks, navController, "No books in your library yet")
                    1 -> BookGrid(borrowedBooks, navController, "You haven't borrowed any books")
                }
            }
        }
    }
}

@Composable
fun BookGrid(books: List<Book>, navController: NavController, emptyMessage: String) {
    if (books.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.LightGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = emptyMessage,
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(books) { book ->
                BookCard(book, navController)
            }
        }
    }
}
@Composable
fun BookCard(book: Book, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable { navController.navigate("bookDetails/${book.id}") },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2CBABE)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Immagine del libro
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .background(Color(0xFFBDEBEE))
            ) {
                // immagine copertina
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = book.title,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 30.dp, vertical = 15.dp)
            )
        }
    }
}

