package com.example.enjoybook.pages

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.enjoybook.data.LentBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun LibraryPage(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var lentBooks by remember { mutableStateOf<List<LentBook>>(emptyList()) }
    var borrowedBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) }


    val primaryColor = Color(0xFFB4E4E8)
    val secondaryColor = Color(0xFF1A8A8F)
    val backgroundColor = (primaryColor.copy(alpha = 0.1f))

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
                        val bookId = borrowDoc.getString("bookId")
                        val borrowerId = borrowDoc.getString("borrowerId")

                        if (bookId != null && borrowerId != null) {
                            // Fetch book details
                            val bookDoc = db.collection("books").document(bookId).get().await()
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
                                borrowerName = borrowerDoc.getString("name") ?: "Unknown",
                                borrowerEmail = borrowerDoc.getString("email") ?: ""
                            )
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
                    .background(backgroundColor)
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
                    color = Color.Black
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
                    .basicMarquee() // adds a scrolling effect if text is too long
            )
        }
    }
}


@Composable
fun LentBookCard(lentBook: LentBook, navController: NavController) {
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
                    .weight(1f)
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
                    destinationRoute = "userDetails/${lentBook.book.userId}",
                    normalColor = Color.White.copy(alpha = 0.8f)
                )
            }
        }
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

