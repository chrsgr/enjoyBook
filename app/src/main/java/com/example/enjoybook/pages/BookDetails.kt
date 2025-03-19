package com.example.enjoybook.pages

import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.Notification
import com.example.enjoybook.data.Review
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun BookDetails(navController: NavController, authViewModel: AuthViewModel, bookId: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserEmail = currentUser?.email ?: ""
    val db = FirebaseFirestore.getInstance()
    val isFrontCover = remember { mutableStateOf(true) }

    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)
    val errorColor = Color(0xFFD32F2F)

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

                        // Update availability state from the database
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



    // Toggle loan request status based on book availability
    fun toggleLoanRequest() {
        when {
            isLoanRequested -> {
                deleteRequest = true
                isLoanRequested = false
                buttonText = if (isBookAvailable) "available" else "not available"
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
                coroutineScope.launch {
                    delay(2000)
                    showLoanRequestDialog = false
                }
            }
            else -> {
                // This handles the "not available" case
                showLoanRequestDialog = false
                isLoanRequested = false
                buttonText = "not available"
                coroutineScope.launch {
                    delay(2000)
                    showLoanRequestDialog = false
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Back button in top right
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(30.dp, top = 30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = primaryColor
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = 100.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = book?.title ?: "TITLE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(20.dp))
//Cover
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp, 200.dp)
                        .background(Color(0xFFBDEBEE), shape = RoundedCornerShape(8.dp))
                        .clickable {
                            isFrontCover.value = !isFrontCover.value
                        }
                ) {
                    if ((isFrontCover.value && book?.frontCoverUrl != null) ||
                        (!isFrontCover.value && book?.backCoverUrl != null)) {

                        val imageUrl = if (isFrontCover.value) book?.frontCoverUrl else book?.backCoverUrl

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

                    // Favorite button
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
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
                                    )

                                    FavoritesManager.addFavorite(favoriteBook)
                                    isAnimating = true
                                } else {
                                    FavoritesManager.removeFavorite(bookId)
                                }
                                isFavorite = !isFavorite
                            },
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // heart animation
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

                Spacer(modifier = Modifier.width(30.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    BookInfoItem(label = "AUTHORS", value = book?.author ?: "", textColor = textColor)
                    BookInfoItem(label = "EDITION", value = book?.edition ?: "", textColor = textColor)
                    BookInfoItem(label = "YEAR", value = book?.year ?: "", textColor = textColor)
                    BookInfoItem(label = "GENRE", value = book?.type ?: "", textColor = textColor)
                    BookInfoItem(label = "CONDITION", value = book?.condition ?: "", textColor = textColor)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // description
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                if (book?.description?.isNotEmpty() == true) {
                    Text(
                        text = book?.description ?: "",
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No description available",
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { showContactDialog = true },
                    colors = ButtonDefaults.buttonColors(primaryColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "contact owner", color = Color.White)
                }

                Button(
                    onClick = { toggleLoanRequest() },
                    colors = ButtonDefaults.buttonColors(
                        when (buttonText) {
                            "requested" -> Color(0xFFFF9800)
                            "not available" -> errorColor
                            else -> Color(0xFF71F55E)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = buttonText,
                        color = if (buttonText == "requested" || buttonText == "not available") Color.White else Color.Black
                    )
                }


                Button(
                    onClick = { showReviewDialog = true },
                    colors = ButtonDefaults.buttonColors(primaryColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "review", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Review section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                if (submittedReviews.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No reviews yet",
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Reviews",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Displaying reviews
                            itemsIndexed(submittedReviews) { index, (authorEmail, review) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Review text with author's initials
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = authorEmail.split("@").first(),
                                            fontSize = 12.sp,
                                            color = textColor.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = review,  // Display the review text
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = textColor,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    if (authorEmail == currentUserEmail) {
                                        IconButton(
                                            onClick = {
                                                // Find the corresponding review in the database and delete it
                                                db.collection("reviews")
                                                    .whereEqualTo("bookId", bookId)
                                                    .whereEqualTo("userEmail", currentUserEmail)
                                                    .whereEqualTo("review", submittedReviews[index].second)
                                                    .get()
                                                    .addOnSuccessListener { documents ->
                                                        for (document in documents) {
                                                            // Delete the document
                                                            db.collection("reviews").document(document.id).delete()
                                                                .addOnSuccessListener {
                                                                    Log.d("ReviewDialog", "Review successfully deleted")

                                                                    // Remove from local state after successful deletion
                                                                    submittedReviews.removeAt(index)
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    Log.e("ReviewDialog", "Error deleting review", e)
                                                                }
                                                        }
                                                    }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete review",
                                                tint = errorColor
                                            )
                                        }
                                    }
                                }

                                // Add a divider between reviews, but not after the last one
                                if (index < submittedReviews.size - 1) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = primaryColor.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Contact Owner Dialog
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

                        // Content
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

        // Loan Request Confirmation Dialog
        if (showLoanRequestDialog) {
            Dialog(onDismissRequest = {  }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loan request sent",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Delete Request
        if (deleteRequest) {
            Dialog(onDismissRequest = {  }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Request Delete",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            color = textColor
                        )
                    }
                }
            }
        }

        // Review Dialog
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
                        // Close button (X) in top left
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

                        // Content
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

                            // Text field for review input
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

                                        // Add immediately to UI for responsiveness
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
}

@Composable
fun ContactInfoItem(label: String, value: String, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp),
            color = textColor
        )
        Text(
            text = value,
            color = textColor
        )
    }
}

@Composable
fun BookInfoItem(label: String, value: String, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp),
            color = textColor
        )
        Text(
            text = value,
            color = textColor
        )
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

    // Create notification data
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
4
    // Add to Firestore
    FirebaseFirestore.getInstance()
        .collection("notifications")
        .add(notification)
        .addOnSuccessListener {
            // Notification saved successfully
            Log.d("Notifications", "Request notification sent to owner")
        }
        .addOnFailureListener { e ->
            Log.e("Notifications", "Error sending notification", e)
        }
}