package com.example.enjoybook.pages


import android.icu.util.Calendar
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import  androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import  androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MenuBook

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.FavoriteBook
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController, authViewModel: AuthViewModel) {
    val primaryColor = Color(0xFF2CBABE)
    val textColor = Color(0xFF333333)

    val authState = authViewModel.authState.observeAsState()
    val favorites by FavoritesManager.favoritesFlow.collectAsState()

    var selectedGenre by remember { mutableStateOf<String?>(null) }
    val refreshing = remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(refreshing.value)

    val categories = listOf(
        "Adventure", "Classics", "Crime", "Folk", "Fantasy", "Historical",
        "Horror", "Literary fiction", "Mystery", "Poetry", "Plays", "Romance",
        "Science fiction", "Short stories", "Thrillers", "War", "Women's fiction", "Young adult"
    )

    val featuredBooks = remember { mutableStateOf<List<Book>>(emptyList()) }
    val favoritesBooks = remember { mutableStateOf<List<FavoriteBook>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    val refreshData = {
        refreshing.value = true
        isLoading.value = true

        fetchFeaturedBooks { books ->
            featuredBooks.value = books
            isLoading.value = false
        }

        fetchFavoriteBooks { books ->
            favoritesBooks.value = books
            refreshing.value = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = refreshData,
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    contentColor = primaryColor
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(top = 16.dp)
            ){
                // Genre
                item {
                    Box(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        SectionHeader(
                            icon = Icons.Filled.MenuBook,
                            title = "GENRE",
                            primaryColor = primaryColor,
                            textColor = textColor,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 8.dp)
                    ) {
                        categories.forEach { category ->
                            var isSelected by remember { mutableStateOf(false) }
                            val animatedAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.7f,
                                animationSpec = tween(durationMillis = 200),
                                label = "alphaAnimation"
                            )

                            Box(
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 2.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        primaryColor.copy(alpha = animatedAlpha),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        isSelected = !isSelected
                                        selectedGenre = if (isSelected) category else null
                                        if (category.isNotEmpty()) {
                                            navController.navigate("filteredbooks/$category")
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // "See all" link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "See all genres",
                            color = primaryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { navController.navigate("search") }
                                .padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Featured books
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ){
                                SectionHeader(
                                    icon = Icons.Filled.Book,
                                    title = "FEATURED BOOKS",
                                    primaryColor = primaryColor,
                                    textColor = textColor,
                                )
                                ClickableTextWithNavigation(
                                    fullText = "All books",
                                    clickableWord = "All books",
                                    navController = navController,
                                    destinationRoute = "allBooks",
                                    normalColor = textColor
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading.value) {
                                    CircularProgressIndicator(color = primaryColor)
                                } else if (featuredBooks.value.isEmpty()) {
                                    Text("No featured books available", color = Color.Gray)
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .fillMaxWidth()
                                    ) {
                                        featuredBooks.value.forEach { book ->
                                            FeatureBookCard(
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

                // Favorites section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            SectionHeader(
                                icon = Icons.Default.Favorite,
                                title = "YOUR FAVORITES",
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
                                if (isLoading.value) {
                                    CircularProgressIndicator(color = primaryColor)
                                } else if (favoritesBooks.value.isEmpty()) {
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
                                            "No favorites books yet",
                                            color = Color.Gray,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Add books to your favorites to see them here",
                                            color = Color.Gray,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .horizontalScroll(rememberScrollState())
                                            .fillMaxWidth()
                                    ) {
                                        favoritesBooks.value.forEach { book ->
                                            Log.d("FavoritesHome", "${book.bookId}")
                                            FavoriteBookCard(
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

                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }
}
@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    primaryColor: Color,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
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
fun FeatureBookCard(
    book: Book,
    primaryColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    val sevenDaysAgo = Timestamp(calendar.time)

    val isNew = book.timestamp?.toDate()?.after(sevenDaysAgo.toDate()) == true

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

                    if (isNew) {
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
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(36.dp)
                    )

                    if (isNew) {
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

@Composable
fun FavoriteBookCard(
    book: FavoriteBook,
    primaryColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {

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
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(36.dp)
                    )
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



private fun fetchFeaturedBooks(onComplete: (List<Book>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("users")
        .whereEqualTo("isBanned", true)
        .get()
        .addOnSuccessListener { bannedUsersSnapshot ->
            val bannedUserIds = bannedUsersSnapshot.documents.map { it.id }

            db.collection("books")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { documents ->
                    val booksList = mutableListOf<Book>()
                    for (document in documents) {
                        val book = document.toObject(Book::class.java).copy(
                            id = document.id
                        )
                        if (book.userId !in bannedUserIds) {
                            booksList.add(book)
                        }
                    }
                    onComplete(booksList)
                }
                .addOnFailureListener { exception ->
                    onComplete(emptyList())
                }
        }
        .addOnFailureListener { exception ->
            fetchBooksWithoutBanCheck(onComplete)
        }
}

private fun fetchBooksWithoutBanCheck(onComplete: (List<Book>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("books")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(10)
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


private fun fetchFavoriteBooks(onComplete: (List<FavoriteBook>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser != null) {
        db.collection("users")
            .whereEqualTo("isBanned", true)
            .get()
            .addOnSuccessListener { bannedUsersSnapshot ->
                val bannedUserIds = bannedUsersSnapshot.documents.map { it.id }

                db.collection("favorites")
                    .whereEqualTo("userId", currentUser.uid)
                    .orderBy("addedAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        val favoriteBooks = documents.mapNotNull { document ->
                            FavoriteBook(
                                bookId = document.getString("bookId") ?: "",
                                author = document.getString("author") ?: "",
                                addedAt = document.getTimestamp("addedAt") ?: Timestamp.now(),
                                title = document.getString("title") ?: "",
                                type = document.getString("type") ?: "",
                                userId = document.getString("userId") ?: "",
                                frontCoverUrl = document.getString("frontCoverUrl") ?: null
                            )
                        }

                        if (favoriteBooks.isEmpty()) {
                            onComplete(emptyList())
                            return@addOnSuccessListener
                        }

                        val bookIds = favoriteBooks.map { it.bookId }

                        val filteredBooks = mutableListOf<FavoriteBook>()
                        var booksChecked = 0

                        for (bookId in bookIds) {
                            db.collection("books").document(bookId).get()
                                .addOnSuccessListener { bookDoc ->
                                    val bookOwnerId = bookDoc.getString("userId") ?: ""
                                    val favoriteBook = favoriteBooks.find { it.bookId == bookId }

                                    if (bookOwnerId !in bannedUserIds && favoriteBook != null) {
                                        filteredBooks.add(favoriteBook)
                                    }

                                    booksChecked++
                                    if (booksChecked >= bookIds.size) {
                                        onComplete(filteredBooks)
                                    }
                                }
                                .addOnFailureListener { exception ->

                                    booksChecked++
                                    if (booksChecked >= bookIds.size) {
                                        onComplete(filteredBooks)
                                    }
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        onComplete(emptyList())
                    }
            }
            .addOnFailureListener { exception ->
                fetchFavoritesWithoutBanCheck(onComplete)
            }
    } else {
        onComplete(emptyList())
    }
}

private fun fetchFavoritesWithoutBanCheck(onComplete: (List<FavoriteBook>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    if (currentUser != null) {
        db.collection("favorites")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("addedAt", Query.Direction.DESCENDING)
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
                onComplete(emptyList())
            }
    } else {
        onComplete(emptyList())
    }
}