package com.example.enjoybook.pages

import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotTouch
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.Notification
import com.example.enjoybook.data.Review
import com.example.enjoybook.data.SnackbarNotificationState
import com.example.enjoybook.data.SnackbarType
import com.example.enjoybook.utils.NotificationHost
import com.example.enjoybook.utils.ScrollableTextWithScrollbar
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun BookDetails(navController: NavController, authViewModel: AuthViewModel, bookId: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserEmail = currentUser?.email ?: ""
    val db = FirebaseFirestore.getInstance()
    val isFrontCover = remember { mutableStateOf(true) }

    val primaryColor = Color(0xFF2CBABE)
    val secondaryColor = Color(0xFF1A8A8F)
    val backgroundColor = (primaryColor.copy(alpha = 0.1f))

    val textColor = Color(0xFF333333)
    val errorColor = Color(0xFFD32F2F)
    val successColor = Color(0xFF4CAF50)
    val warningColor = Color(0xFFFF9800)
    val cardBackground = Color.White

    var book by remember { mutableStateOf<Book?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val firestore = FirebaseFirestore.getInstance()



    LaunchedEffect(Unit) {
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        name = document.getString("name") ?: ""
                        surname = document.getString("surname") ?: ""
                        username = document.getString("username") ?: ""
                        email = document.getString("email") ?: ""
                        phone = document.getString("phone") ?: ""

                        document.getString("profilePictureUrl")?.let {
                            imageUri = Uri.parse(it)
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error loading profile: ${e.message}"
                    showErrorDialog = true
                    isLoading = false
                }
        } else {
            navController.navigate("login") {
                popUpTo("main") { inclusive = true }
            }
        }
    }

    var bookReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    val submittedReviews = remember { mutableStateListOf<Pair<String, String>>() }

    var isBookAvailable by remember { mutableStateOf(true) }
    var buttonText by remember { mutableStateOf("available") }

    LaunchedEffect(bookId) {
        if (bookId.isNotEmpty()) {
            db.collection("books").document(bookId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        book = document.toObject(Book::class.java)

                        isBookAvailable = document.getBoolean("isAvailable") ?: true
                        buttonText = if (isBookAvailable) "available" else "not available"

                        val documentId = document.id

                        submittedReviews.clear()

                        fetchReviewsForBook(documentId) { reviews ->
                            bookReviews = reviews

                            val formattedReviews = reviews.map {
                                it.userEmail to it.review
                            }

                            submittedReviews.addAll(formattedReviews)
                        }
                    } else {
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    isLoading = false
                }
        }
    }

    val authState = authViewModel.authState.observeAsState().value

    LaunchedEffect(authState) {
        if (authState !is AuthState.Authenticated) {
            navController.navigate("login")
        }
    }

    var review by remember { mutableStateOf("") }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showLoanRequestDialog by remember { mutableStateOf(false) }
    var deleteRequest by remember { mutableStateOf(false) }
    var isLoanRequested by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var isFavorite by remember { mutableStateOf(FavoritesManager.isBookFavorite(bookId)) }
    var isAnimating by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 20f else 1f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        finishedListener = {
            if (isAnimating) {
                isAnimating = false
            }
        },
        label = "heart scale animation"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isAnimating) 0f else 1f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "heart alpha animation"
    )


    fun toggleFavorite(book: Book) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        if (!FavoritesManager.isBookFavorite(book.id)) {
            val favoriteBook = hashMapOf(
                "bookId" to book.id,
                "userId" to currentUser.uid,
                "title" to book.title,
                "type" to book.type,
                "author" to book.author,
                "frontCoverUrl" to book.frontCoverUrl,
                "addedAt" to FieldValue.serverTimestamp()
            )

            db.collection("favorites")
                .add(favoriteBook)
                .addOnSuccessListener { documentReference ->
                    Log.d("FavoriteBook", "Book added to favorites with ID: ${documentReference.id}")
                    FavoritesManager.addFavorite(book)
                }
                .addOnFailureListener { e ->
                    Log.e("FavoriteBook", "Error adding book to favorites", e)
                }
        } else {
            db.collection("favorites")
                .whereEqualTo("bookId", book.id)
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        db.collection("favorites").document(document.id).delete()
                    }
                    FavoritesManager.removeFavorite(book.id)
                }
                .addOnFailureListener { e ->
                    Log.e("FavoriteBook", "Error removing book from favorites", e)
                }
        }
    }


    fun toggleLoanRequest() {
        when {
            isLoanRequested -> {
                deleteRequest = true
                isLoanRequested = false
                buttonText = if (isBookAvailable) "available" else "not available"

                NotificationManager.showNotification(
                    message = "Loan request canceled successfully",
                    type = SnackbarType.INFO,
                    duration = 3000
                )

                coroutineScope.launch {
                    delay(2000)
                    deleteRequest = false
                }
            }

            isBookAvailable -> {
                showLoanRequestDialog = true
                isLoanRequested = true
                buttonText = "requested"

                sendBookRequestNotification(book?.userId.toString(), book!!.title, book!!.id)

                NotificationManager.showNotification(
                    message = "Loan request sent successfully",
                    type = SnackbarType.SUCCESS,
                    duration = 3000,

                )

                coroutineScope.launch {
                    delay(2000)
                    showLoanRequestDialog = false
                }
            }

            else -> {
                // "not available" case
                showLoanRequestDialog = false
                isLoanRequested = false
                buttonText = "not available"

                // Show on-screen notification
                NotificationManager.showNotification(
                    message = "Sorry, this book is currently unavailable",
                    type = SnackbarType.ERROR,
                    duration = 3000
                )

                coroutineScope.launch {
                    delay(2000)
                    showLoanRequestDialog = false
                }
            }
        }
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
                            "BOOK DETAILS",
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
                        containerColor = backgroundColor,
                        titleContentColor = textColor
                    ),
                    windowInsets = WindowInsets(0)
                )
            },
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->
            NotificationHost {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = book?.title ?: "TITLE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    ClickableTextWithNavigation(
                        fullText = "pubblicated by ${book?.userUsername}",
                        clickableWord = "${book?.userUsername}",
                        navController = navController,
                        destinationRoute = "userDetails/${book?.userId}",
                        normalColor = textColor
                    )



                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp, 200.dp)
                            .shadow(8.dp, RoundedCornerShape(8.dp))
                            .background(Color(0xFFBDEBEE), shape = RoundedCornerShape(8.dp))
                            .clickable { isFrontCover.value = !isFrontCover.value }
                    ) {
                        if ((isFrontCover.value && book?.frontCoverUrl != null) ||
                            (!isFrontCover.value && book?.backCoverUrl != null)
                        ) {
                            val imageUrl =
                                if (isFrontCover.value) book?.frontCoverUrl else book?.backCoverUrl

                            val painter = rememberAsyncImagePainter(imageUrl)

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (painter.state is AsyncImagePainter.State.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                }
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
                                    .background(
                                        Color.Black.copy(alpha = 0.7f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isFrontCover.value) "Front" else "Back",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
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
                                    tint = primaryColor,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (!isFavorite) {
                                        val favoriteBook = Book(
                                            id = bookId,
                                            author = book!!.author,
                                            condition = book!!.condition,
                                            edition = book!!.edition,
                                            title = book!!.title,
                                            titleLower = book!!.titleLower,
                                            type = book!!.type,
                                            userEmail = book!!.userEmail,
                                            userId = book!!.userId,
                                            year = book!!.year,
                                            frontCoverUrl = book?.frontCoverUrl,
                                            backCoverUrl = book?.backCoverUrl
                                        )
                                        toggleFavorite(favoriteBook)
                                        isAnimating = true
                                    } else {
                                        toggleFavorite(book!!)
                                    }
                                    isFavorite = !isFavorite
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isAnimating) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = Color.Red.copy(alpha = alpha),
                                    modifier = Modifier
                                        .scale(scale)
                                        .size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        BookInfoItem(
                            label = "AUTHORS",
                            value = book?.author ?: "",
                            textColor = textColor,
                        )

                        BookInfoItem(
                            label = "EDITION",
                        value = if (book?.edition.isNullOrEmpty()) "no edition available" else book!!.edition,
                        textColor = textColor,
                        )
                        BookInfoItem(
                            label = "YEAR",
                            value = book?.year ?: "",
                            textColor = textColor,
                        )
                        BookInfoItem(
                            label = "GENRE",
                            value = book?.type ?: "",
                            textColor = textColor,
                        )
                        BookInfoItem(
                            label = "CONDITION",
                            value = book?.condition ?: "",
                            textColor = textColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Description",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = primaryColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (book?.description?.isNotEmpty() == true) {
                                ScrollableTextWithScrollbar(
                                    text = book?.description?: "",
                                    textColor = textColor,
                                    scrollbarColor = primaryColor
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No description available",
                                        fontWeight = FontWeight.Medium,
                                        color = textColor.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))


                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    if (currentUserEmail != book?.userEmail) {
                    // Contact button
                    Button(
                        onClick = { navController.navigate("messaging/${book?.userId}")},
                        colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {


                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Chat with owner",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Available/Unavailable button
                    Button(
                        onClick = { toggleLoanRequest() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (buttonText) {
                                "requested" -> warningColor
                                "not available" -> errorColor
                                else -> successColor
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val icon = when (buttonText) {
                            "requested" -> Icons.Default.Pending
                            "not available" -> Icons.Default.DoNotTouch
                            else -> Icons.Default.CheckCircle
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (buttonText) {
                                    "requested" -> "Requested"
                                    "not available" -> "Unavailable"
                                    else -> "Available"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Review button
                    Button(
                        onClick = { showReviewDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Review",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBackground),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        if (submittedReviews.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Comment,
                                        contentDescription = null,
                                        tint = textColor.copy(alpha = 0.3f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No reviews yet",
                                        fontWeight = FontWeight.Medium,
                                        color = textColor.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Be the first to share your thoughts!",
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.4f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Comment,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Reviews",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = primaryColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${submittedReviews.size})",
                                        fontSize = 14.sp,
                                        color = textColor.copy(alpha = 0.6f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = primaryColor.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    submittedReviews.forEachIndexed { index, (authorEmail, reviewText) ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (authorEmail == currentUserEmail)
                                                    primaryColor.copy(alpha = 0.1f)
                                                else
                                                    cardBackground
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                // Review text
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .background(
                                                                    primaryColor.copy(alpha = 0.2f),
                                                                    CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = authorEmail.firstOrNull()
                                                                    ?.uppercase() ?: "?",
                                                                color = primaryColor,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                text = authorEmail.split("@")
                                                                    .first(),
                                                                fontSize = 14.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = textColor
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Text(
                                                        text = reviewText,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = textColor,
                                                        modifier = Modifier.padding(start = 40.dp)
                                                    )
                                                }

                                                // Delete button
                                                if (authorEmail == currentUserEmail) {
                                                    IconButton(
                                                        onClick = {
                                                            db.collection("reviews")
                                                                .whereEqualTo("bookId", bookId)
                                                                .whereEqualTo(
                                                                    "userEmail",
                                                                    currentUserEmail
                                                                )
                                                                .whereEqualTo(
                                                                    "review",
                                                                    submittedReviews[index].second
                                                                )
                                                                .get()
                                                                .addOnSuccessListener { documents ->
                                                                    for (document in documents) {
                                                                        // Delete the document
                                                                        db.collection("reviews")
                                                                            .document(document.id)
                                                                            .delete()
                                                                            .addOnSuccessListener {
                                                                                Log.d(
                                                                                    "ReviewDialog",
                                                                                    "Review successfully deleted"
                                                                                )
                                                                                // Remove from local state after successful deletion
                                                                                submittedReviews.removeAt(
                                                                                    index
                                                                                )
                                                                            }
                                                                            .addOnFailureListener { e ->
                                                                                Log.e(
                                                                                    "ReviewDialog",
                                                                                    "Error deleting review",
                                                                                    e
                                                                                )
                                                                            }
                                                                    }
                                                                }
                                                        },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .padding(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete review",
                                                            tint = errorColor,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (index < submittedReviews.size - 1) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
                }

            if (showReviewDialog) {
                Dialog(onDismissRequest = { showReviewDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { showReviewDialog = false },
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = primaryColor
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .padding(top = 48.dp, bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Write a Review",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = textColor
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = review,
                                    onValueChange = { review = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    placeholder = { Text("Share your thoughts about this book...", color = textColor.copy(alpha = 0.5f)) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryColor,
                                        unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (review.isNotBlank() && currentUserEmail.isNotEmpty() && book != null) {
                                            val documentId = bookId
                                            Log.d("ReviewSubmit", "Submitting review for document ID: $documentId")

                                            submittedReviews.add(0, currentUserEmail to review)

                                            saveReviewToDatabase(documentId, currentUserEmail, review) {
                                                Log.d("ReviewSubmit", "Review saved to database")
                                            }

                                            review = ""
                                            showReviewDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(primaryColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Submit", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showContactDialog) {
            Dialog(onDismissRequest = { showContactDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = { showContactDialog = false },
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = primaryColor
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .padding(top = 48.dp, bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Owner Contact Information",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = textColor
                            )

                            Spacer(modifier = Modifier.height(8.dp))


                            ContactInfoItem(label = "Email", value = book?.userEmail ?: "", textColor = textColor)
                            ContactInfoItem(label = "Phone", value = phone, textColor = textColor)

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showContactDialog = false },
                                colors = ButtonDefaults.buttonColors(primaryColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Close", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

    }
}

private fun saveReviewToDatabase(bookId: String, userEmail: String, review: String, onComplete: () -> Unit) {
    val db = FirebaseFirestore.getInstance()

    val reviewData = hashMapOf(
        "bookId" to bookId,
        "userEmail" to userEmail,
        "review" to review,
        "timestamp" to FieldValue.serverTimestamp()
    )

    db.collection("reviews")
        .add(reviewData)
        .addOnSuccessListener { documentReference ->
            Log.d("ReviewDialog", "Review saved with ID: ${documentReference.id}")
            onComplete()
        }
        .addOnFailureListener { e ->
            Log.e("ReviewDialog", "Error adding review", e)
        }
}

private fun fetchReviewsForBook(bookId: String, callback: (List<Review>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("reviews")
        .whereEqualTo("bookId", bookId)
        .get()
        .addOnSuccessListener { documents ->

            if (documents.isEmpty) {
                callback(emptyList())
                return@addOnSuccessListener
            }

            val reviewsList = mutableListOf<Review>()
            for (document in documents) {
                try {
                    val review = Review(
                        id = document.id,
                        bookId = document.getString("bookId") ?: "",
                        userEmail = document.getString("userEmail") ?: "",
                        review = document.getString("review") ?: ""
                    )
                    reviewsList.add(review)
                } catch (e: Exception) {
                }
            }

            callback(reviewsList)
        }
        .addOnFailureListener { e ->
            callback(emptyList())
        }
}
fun sendBookRequestNotification(userId: String, title: String, bookId: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return

    val notification = Notification(
        recipientId = userId,
        senderId = currentUser.uid,
        message = "Richiesta di affitto per libro '$title'",
        timestamp = System.currentTimeMillis(),
        isRead = false,
        type = "LOAN_REQUEST",
        bookId = bookId,
        title = title
    )

    FirebaseFirestore.getInstance()
        .collection("notifications")
        .add(notification)
        .addOnSuccessListener {
            Log.d("Notifications", "Request notification sent to owner")
        }
        .addOnFailureListener { e ->
            Log.e("Notifications", "Error sending notification", e)
        }
}






object NotificationManager {
    private val _notificationState = MutableStateFlow<SnackbarNotificationState?>(null)
    val notificationState: StateFlow<SnackbarNotificationState?> = _notificationState.asStateFlow()

    fun showNotification(
        message: String,
        type: SnackbarType = SnackbarType.INFO,
        duration: Long = 3000,
        actionLabel: String? = null,
        onActionClick: () -> Unit = {}
    ) {
        _notificationState.value = SnackbarNotificationState(
            message = message,
            type = type,
            duration = duration,
            actionLabel = actionLabel,
            onActionClick = onActionClick
        )

        CoroutineScope(Dispatchers.Main).launch {
            delay(duration)
            dismissNotification()
        }
    }

    fun dismissNotification() {
        _notificationState.value = null
    }
}








