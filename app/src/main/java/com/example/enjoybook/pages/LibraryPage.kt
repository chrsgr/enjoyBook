package com.example.enjoybook.pages

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.enjoybook.data.LentBook
import com.example.enjoybook.theme.primaryColor
import com.example.enjoybook.theme.textColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun LibraryPage(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var lentBooks by remember { mutableStateOf<List<LentBook>>(emptyList()) }
    var borrowedBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }
    var idBorrow by remember {mutableStateOf("")}


    val primaryColor = Color(0xFFB4E4E8)
    val secondaryColor = Color(0xFF1A8A8F)
    val secondaryBackgroundColor = (primaryColor.copy(alpha = 0.1f))


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
                        borrowedBooks = emptyList()
                    } else {
                        db.collection("books")
                            .whereIn(FieldPath.documentId(), borrowedBookIds)
                            .get()
                            .addOnSuccessListener { bookDocs ->
                                borrowedBooks = bookDocs.map { doc ->
                                    Book(
                                        id = doc.id,
                                        title = doc.getString("title") ?: "",
                                        author = doc.getString("author") ?: "",
                                        type = doc.getString("type") ?: "",
                                        userId = doc.getString("userId") ?: "",
                                        isAvailable = false,
                                        userEmail = doc.getString("userEmail") ?: "",
                                        frontCoverUrl = doc.getString("frontCoverUrl") ?: null,
                                        backCoverUrl = doc.getString("backCoverUrl") ?: null
                                    )
                                }
                            }
                            .addOnFailureListener {  }
                    }
                }
                .addOnFailureListener { }

            withContext(Dispatchers.IO) {
                try {
                    val borrowQuery = db.collection("borrows")
                        .whereEqualTo("ownerId", currentUser.uid)
                        .whereEqualTo("status", "accepted")
                        .get()
                        .await()

                    val lentBookDetails = borrowQuery.documents.mapNotNull { borrowDoc ->
                        val borrowId = borrowDoc.id
                        val bookId = borrowDoc.getString("bookId")
                        val borrowerId = borrowDoc.getString("borrowerId")

                        if (bookId != null && borrowerId != null) {
                            val bookDoc = db.collection("books").document(bookId).get().await()

                            if (bookDoc.getBoolean("isAvailable") != true) {
                                val book = Book(
                                    id = bookId,
                                    title = bookDoc.getString("title") ?: "",
                                    author = bookDoc.getString("author") ?: "",
                                    type = bookDoc.getString("type") ?: "",
                                    userId = bookDoc.getString("userId") ?: "",
                                    isAvailable = false,
                                    userEmail = bookDoc.getString("userEmail") ?: "",
                                    frontCoverUrl = bookDoc.getString("frontCoverUrl") ?: null,
                                    backCoverUrl = bookDoc.getString("backCoverUrl") ?: null
                                )

                                val borrowerDoc = db.collection("users").document(borrowerId).get().await()

                                LentBook(
                                    book = book,
                                    borrowId = borrowId,
                                    borrowerId = borrowerId,
                                    borrowerName = borrowerDoc.getString("name") ?: "Unknown",
                                    borrowerEmail = borrowerDoc.getString("email") ?: ""
                                )
                            } else null
                        } else null
                    }

                    withContext(Dispatchers.Main) {
                        lentBooks = lentBookDetails
                        isLoading = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
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
                    .background(secondaryBackgroundColor)
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go back",
                        tint = secondaryColor
                    )
                }

                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                Text(
                    text = "YOUR LIBRARY",
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
                        Text(
                            "Lent Books (${lentBooks.size})",
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
                    0 -> LentBookGrid(lentBooks, navController, "You haven't lent any books yet")
                    1 -> BookGrid(borrowedBooks, navController, "You haven't borrowed any books")
                }
            }
        }
    }
}



@Composable
fun BookGrid(
    books: List<Book>,
    navController: NavController,
    emptyMessage: String
) {
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
fun LentBookGrid(
    lentBooks: List<LentBook>,
    navController: NavController,
    emptyMessage: String
) {
    if (lentBooks.isEmpty()) {
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
            items(lentBooks) { lentBook ->
                LentBookCard(lentBook, navController)
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
            val isFrontCover = remember { mutableStateOf(true) }

            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .background(Color(0xFFBDEBEE))
                    .clickable {
                        isFrontCover.value = !isFrontCover.value
                    }
            ) {
                if ((isFrontCover.value && book.frontCoverUrl != null) ||
                    (!isFrontCover.value && book.backCoverUrl != null)) {

                    val imageUrl = if (isFrontCover.value) book.frontCoverUrl else book.backCoverUrl

                    val painter = rememberAsyncImagePainter(imageUrl)
                    val state = painter.state

                    if (state is AsyncImagePainter.State.Loading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }

                    AsyncImage(
                        model = imageUrl,
                        contentDescription = if (isFrontCover.value) "Front Cover" else "Back Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isFrontCover.value) "Front" else "Back",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "No Cover Available",
                            tint = Color(0xFF2CBABE),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = book.title,
                fontSize = 15.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 30.dp, vertical = 19.dp)
                    .basicMarquee()
            )
        }
    }
}


@Composable
fun LentBookCard(lentBook: LentBook, navController: NavController) {

    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable { navController.navigate("bookDetails/${lentBook.book.id}") },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2CBABE)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isFrontCover = remember { mutableStateOf(true) }

            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .background(Color(0xFFBDEBEE))
                    .clickable {
                        isFrontCover.value = !isFrontCover.value
                    }
            ) {
                if ((isFrontCover.value && lentBook.book.frontCoverUrl != null) ||
                    (!isFrontCover.value && lentBook.book.backCoverUrl != null)) {

                    val imageUrl = if (isFrontCover.value) lentBook.book.frontCoverUrl else lentBook.book.backCoverUrl

                    AsyncImage(
                        model = imageUrl,
                        contentDescription = if (isFrontCover.value) "Front Cover" else "Back Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isFrontCover.value) "Front" else "Back",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "No Cover Available",
                            tint = Color(0xFF2CBABE),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = lentBook.book.title,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                ClickableTextWithNavigation(
                    fullText = "Borrowed by: ${lentBook.borrowerName}",
                    clickableWord = lentBook.borrowerName,
                    navController = navController,
                    destinationRoute = "userDetails/${lentBook.borrowerId}",
                    normalColor = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    //color = availableColor.copy(alpha = 0.2f),
                    color = primaryColor.copy(alpha = 0.2f),
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
                            tint = primaryColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Returned?",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { androidx.compose.material3.Text("Update Borrow") },
            text = {
                androidx.compose.material3.Text(
                    text = "Do you want sign the borrow as concluded?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false

                        scope.launch {
                            updateBorrow(db, lentBook.borrowId, lentBook.book.id, context)
                        }
                    }
                ) {
                    androidx.compose.material3.Text("Confirm", color = primaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    androidx.compose.material3.Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            titleContentColor = textColor,
            textContentColor = textColor.copy(alpha = 0.7f)
        )
    }

}

@Composable
fun ClickableTextWithNavigation(
    fullText: String,
    clickableWord: String,
    navController: NavController,
    destinationRoute: String,
    normalColor: Color
) {
    val annotatedString = buildAnnotatedString {
        append(fullText)

        val startIndex = fullText.indexOf(clickableWord)
        val endIndex = startIndex + clickableWord.length

        addStyle(
            style = SpanStyle(
                color = normalColor,
                textDecoration = TextDecoration.Underline
            ),
            start = startIndex,
            end = endIndex
        )

        addStringAnnotation(
            tag = "route",
            annotation = destinationRoute,
            start = startIndex,
            end = endIndex
        )
    }

    ClickableText(
        text = annotatedString,
        style = TextStyle(
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "route", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    navController.navigate(annotation.item)
                }
        }
    )
}

private suspend fun updateBorrow(
    db: FirebaseFirestore,
    borrowId: String,
    bookId: String,
    context: Context)
{
    try {
        db.collection("borrows").document(borrowId)
            .update("status", "concluded")
            .await()

        db.collection("books").document(bookId)
            .update("isAvailable", true)
            .await()

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Book is now updated and available again",
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Failed to update book availability: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

