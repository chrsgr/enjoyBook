package com.example.enjoybook.pages

import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.Review
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun BookDetails(navController: NavController, authViewModel: AuthViewModel, bookId: String) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentUserEmail = currentUser?.email ?: ""
    val db = FirebaseFirestore.getInstance()

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

    var bookReviews by remember { mutableStateOf<List<Review>>(emptyList()) }


    val (submittedReviews, setSubmittedReviews) = remember {
        mutableStateOf(mutableStateListOf<Pair<String, String>>()) //(email autore, testo)
    }

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

    LaunchedEffect(bookId) {
        if (bookId.isNotEmpty()) {
            Log.d("BookDetails", "Recupero dettagli libro e recensioni per bookId: $bookId")

            // Recupera i dettagli del libro usando il bookId effettivo (quello che esiste nel DB)
            db.collection("books").document(bookId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Assegna il libro recuperato al nostro stato
                        book = document.toObject(Book::class.java)

                        // Recupera anche le recensioni per lo stesso bookId
                        fetchReviewsForBook(bookId) { newReviews ->
                            val formattedReviews = newReviews.map { it.userEmail to it.review }
                            submittedReviews.addAll(formattedReviews)  // Aggiungi le nuove recensioni
                            Log.d("BookDetails", "Recuperate ${newReviews.size} recensioni per bookId: $bookId")
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Errore nel recupero del libro: ", exception)
                    isLoading = false
                }
        } else {
            isLoading = false
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

    var isBookAvailable by remember { mutableStateOf(true) }  // Change to false if book is not available
    var buttonText by remember { mutableStateOf("available") }

    // Toggle loan request status based on book availability
    fun toggleLoanRequest() {
        when {
            isLoanRequested -> {
                deleteRequest = true
                isLoanRequested = false
                buttonText = "available"
                coroutineScope.launch {
                    delay(2000) // Simulate network or database request delay
                    deleteRequest = false
                }
            }
            isBookAvailable -> {
                showLoanRequestDialog = true
                isLoanRequested = true
                buttonText = "requested"
                coroutineScope.launch {
                    delay(2000) // Simulate network or database request delay
                    showLoanRequestDialog = false
                }
            }
            else -> {
                showLoanRequestDialog = false
                isLoanRequested = false
                buttonText = "not available"
                coroutineScope.launch {
                    delay(2000) // Simulate network or database request delay
                    showLoanRequestDialog = false
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Back button in top right
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(30.dp, top = 30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
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
            // Titolo
            Text(
                text = book?.title ?: "TITLE",
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Copertina e dettagli libro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp, 200.dp)
                        .background(Color((0xFFA7E8EB)), shape = RoundedCornerShape(8.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopStart
                    ) {
                        IconButton(
                            onClick = {
                                if (!isFavorite) {
                                    // Add to favorites
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
                                tint = if (isFavorite) Color(0xFFF100F1) else Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // animazione cuore
                    if (isAnimating) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFC00FC).copy(alpha = alpha),
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
                    BookInfoItem(label = "AUTHORS", value = book?.author ?: "")
                    BookInfoItem(label = "EDITION", value = book?.edition ?: "")
                    BookInfoItem(label = "YEAR", value = book?.year ?: "")
                    BookInfoItem(label = "GENRE", value = book?.type ?: "")
                    BookInfoItem(label = "CONDITION", value = book?.condition ?: "")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Riassunto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.White, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "short description", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Pulsanti
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { showContactDialog = true },
                    colors = ButtonDefaults.buttonColors(Color(0xFFA7E8EB))
                ) {
                    Text(text = "contact owner", color = Color.Black)
                }

                Button(
                    onClick = { toggleLoanRequest() },
                    colors = ButtonDefaults.buttonColors(
                        // Set button color based on the availability status
                        when (buttonText) {
                            "requested" -> Color(0xFFFF9800) // Orange for "requested"
                            "not available" -> Color.Red // Red for "not available"
                            else -> Color(0xFF71F55E) // Green for "available"
                        }
                    )
                ) {
                    Text(
                        text = buttonText,
                        color = if (buttonText == "requested" || buttonText == "not available") Color.White else Color.Black
                    )
                }

                Button(
                    onClick = { showReviewDialog = true },
                    colors = ButtonDefaults.buttonColors(Color(0xFFA7E8EB))
                ) {
                    Text(text = "review", color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Review section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .padding(16.dp)
            ) {
                if (submittedReviews.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No reviews yet", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Reviews",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
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
                                            text = authorEmail.split("@")
                                                .first(),  // Displaying only the part before "@"
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = review,  // Display the review text
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    // Optionally add a delete icon for the current user's review
                                    if (authorEmail == currentUserEmail) {
                                        IconButton(
                                            onClick = {
                                                // Delete the review
                                                val newList = submittedReviews.toMutableList()
                                                newList.removeAt(index)
                                                setSubmittedReviews(newList.toMutableStateList())
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete review",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }

                                // Add a divider between reviews, but not after the last one
                                if (index < submittedReviews.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
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
                                contentDescription = "Close"
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
                                fontSize = 20.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ContactInfoItem(label = "Name", value = name)
                            ContactInfoItem(label = "Surname", value = surname)
                            ContactInfoItem(label = "Email", value = book?.userEmail ?: "")
                            ContactInfoItem(label = "Phone", value = phone)

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showContactDialog = false },
                                colors = ButtonDefaults.buttonColors(Color(0xFFA7E8EB))
                            ) {
                                Text("Close", color = Color.Black)
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
                            textAlign = TextAlign.Center
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
                            textAlign = TextAlign.Center
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
                                contentDescription = "Close"
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
                                fontSize = 20.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Text field for review input
                            OutlinedTextField(
                                value = review,
                                onValueChange = { review = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                placeholder = { Text("Share your thoughts about this book...") },
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (review.isNotBlank() && currentUserEmail.isNotEmpty()) {
                                        // Save to local state
                                        submittedReviews.add(Pair(currentUserEmail, review))

                                        // Save to database - aggiungendo bookId come primo parametro
                                        saveReviewToDatabase(book!!.id, currentUserEmail, review) {
                                            // Ricarica le recensioni aggiornate dopo il salvataggio
                                            fetchReviewsForBook(book!!.id) { newReviews ->
                                                // Formatta le recensioni recuperate e aggiorna lo stato
                                                val formattedReviews = newReviews.map { it.userEmail to it.review }
                                                submittedReviews.addAll(formattedReviews) // Aggiunge le recensioni aggiornate
                                            }
                                        }

                                        // Reset e chiusura del dialog
                                        review = ""
                                        showReviewDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(Color(0xFFA7E8EB))
                            ) {
                                Text("Submit", color = Color.Black)
                            }


                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp)
        )
        Text(text = value)
    }
}

@Composable
fun BookInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(text = value)
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
            onComplete()  // Chiamata dopo il salvataggio
        }
        .addOnFailureListener { e ->
            Log.e("ReviewDialog", "Error adding review", e)
        }
}

private fun fetchReviewsForBook(bookId: String, callback: (List<Review>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("reviews")
        .whereEqualTo("bookId", bookId)  // Usa il bookId per cercare le recensioni correlate
        .orderBy("timestamp", Query.Direction.DESCENDING) // Ordina per timestamp (dal più recente al più vecchio)
        .get()
        .addOnSuccessListener { documents ->
            val reviewsList = documents.map { document ->
                document.toObject(Review::class.java).copy(id = document.id)  // Recupera la review e aggiungi l'ID del documento
            }
            Log.d("Firestore", "Fetched ${reviewsList.size} reviews for bookId: $bookId")
            callback(reviewsList)  // Passa la lista delle recensioni alla callback
        }
        .addOnFailureListener { e ->
            Log.e("ReviewDialog", "Errore nel recupero delle recensioni", e)
            callback(emptyList())  // In caso di errore, restituisci una lista vuota
        }
}

