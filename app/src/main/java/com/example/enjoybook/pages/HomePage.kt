package com.example.enjoybook.pages


import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import com.example.enjoybook.data.NavItem
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import  androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import  androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Close

import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.enjoybook.data.Book
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.example.enjoybook.viewModel.SearchViewModel
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel, viewModel: SearchViewModel = viewModel()) {
    var query by remember { mutableStateOf("") }

    val authState = authViewModel.authState.observeAsState()

    var searchQuery by remember { mutableStateOf(query) }

    val favorites by FavoritesManager.favoritesFlow.collectAsState()


    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val searchFocusRequester = remember { FocusRequester() }

    var selectedGenre by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Adventure", "Classics", "Crime", "Folk", "Fantasy", "Historical",
        "Horror", "Literary fiction", "Mystery", "Poetry", "Plays", "Romance",
        "Science fiction", "Short stories", "Thrillers", "War", "Women’s fiction", "Young adult"
    )


    val featuredBooks = remember { mutableStateOf<List<Book>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        fetchFeaturedBooks { books ->
            featuredBooks.value = books
            isLoading.value = false
        }
    }

    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }


    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
        keyboardController?.show()
    }

    var unreadNotifications by remember { mutableStateOf(3) }
    var showNotificationPopup by remember { mutableStateOf(false) }

    val notifications = remember {
        listOf(
            "Richiesta di affitto per libro 'Il nome della rosa'",
            "Richiesta di affitto per libro 'La Divina Commedia'",
            "Richiesta di affitto per libro 'I Promessi Sposi'"
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(88.dp))

            // Top Bar

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                //notifications
                IconButton(onClick = {
                    showNotificationPopup = true
                    if (unreadNotifications > 0) {
                        unreadNotifications = 0
                    }
                }) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.Black
                        )
                    }
                    if (unreadNotifications > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(16.dp)
                                .offset(x = 8.dp, y = (-8).dp)
                                .clip(CircleShape)
                                .background(Color(0xFFA7E8EB))
                        ) {
                            Text(
                                text = unreadNotifications.toString(),
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Campo di ricerca
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (searchQuery.isNotEmpty()) {
                            viewModel.searchBooks(searchQuery)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Gray
                        )
                    },
                    modifier = Modifier
                        .width(250.dp)
                        .focusRequester(searchFocusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                keyboardController?.show()
                            }
                        },
                    placeholder = { Text("Search", color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFA7E8EB),
                        unfocusedContainerColor = Color(0xFFA7E8EB),
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color.Gray,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.searchBooks(query)
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                )

                //profile
                IconButton(onClick = { navController.navigate("profile") }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.Black
                    )
                }
            }

            //CONTAINER PAGINA
            Spacer(modifier = Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                // GENRE

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = "Book",
                        modifier = Modifier.size(30.dp),
                        tint = Color.Black

                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENRE", fontWeight = FontWeight.Bold)
                }

                Row(

                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    categories.forEach { category ->
                        var isSelected by remember { mutableStateOf(false) }
                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.5f,
                            animationSpec = tween(durationMillis = 200), label = "alphaAnimation"
                        )

                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .background(
                                    Color(0xFFA7E8EB).copy(alpha = animatedAlpha),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    isSelected = !isSelected
                                    selectedGenre = if (isSelected) category else null
                                    if (category.isNotEmpty()) {
                                        navController.navigate("filteredbooks/$category")
                                    }

                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        )

                        {
                            Text(category, color = Color.Black)
                        }
                    }
                }
                Text(
                    text = "see all",
                    color = Color.Blue,
                    modifier = Modifier
                        .clickable {
                            navController.navigate("search")
                        }
                        .padding(8.dp)
                )


                Spacer(modifier = Modifier.height(16.dp))

                //FEATURED BOOKS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Book,
                            contentDescription = "Book",
                            modifier = Modifier.size(30.dp),
                            tint = Color.Black
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Text
                        Text(
                            "FEATURED BOOKS",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    if (isLoading.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFA7E8EB))
                        }
                    } else if (featuredBooks.value.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No featured books available", color = Color.Gray)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 40.dp)
                        ) {
                            featuredBooks.value.forEach { book ->
                                var isSelected by remember { mutableStateOf(false) }
                                val animatedAlpha by animateFloatAsState(
                                    targetValue = if (isSelected) 1f else 0.5f,
                                    animationSpec = tween(durationMillis = 200),
                                    label = "alphaAnimation"
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .height(150.dp)
                                        .width(120.dp)
                                        .background(
                                            Color(0xFFA7E8EB).copy(alpha = animatedAlpha),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            isSelected = !isSelected
                                            selectedGenre = if (isSelected) book.type else null
                                            navController.navigate("bookDetails/${book.id}")
                                        }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = book.title,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = book.author,
                                            color = Color.DarkGray,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = book.type,
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .background(
                                                    Color.White.copy(alpha = 0.3f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))


                // FAVOURITE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favourite",
                            modifier = Modifier.size(30.dp),
                            tint = Color.Black
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Testo
                        Text(
                            "YOUR FAVOURITE",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    if (favorites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No favorites yet", color = Color.Gray)
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 40.dp)
                        ) {
                            favorites.forEach { book ->
                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .height(140.dp)
                                        .width(120.dp)
                                        .background(
                                            Color(0xFFA7E8EB),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            navController.navigate("book")
                                        }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // Title of the book
                                        Text(
                                            text = book.title,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Author of the book
                                        Text(
                                            text = book.author,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


                    // Notification popup
    if (showNotificationPopup) {
        Dialog(onDismissRequest = { showNotificationPopup = false }) {
            Surface(
                modifier = Modifier
                    .width(300.dp),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "notifications",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        IconButton(
                            onClick = { showNotificationPopup = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Black
                            )
                        }
                    }

                    // List of notifications
                    notifications.forEach { notification ->
                        NotificationItem(notification)
                    }
                }
            }
        }
    }
}


@Composable
fun NotificationItem(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),

        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {  },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD23333)
                        ,
                        contentColor = Color.Black
                    )
                ) {
                    Text("REJECT")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {  },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF98DD8E),
                        contentColor = Color.Black
                    )
                ) {
                    Text("ACCEPT")
                }
            }
        }
    }
}


private fun fetchFeaturedBooks(onComplete: (List<Book>) -> Unit) {
    val db = FirebaseFirestore.getInstance()

    db.collection("books")
        .limit(10)  // Limit to 10 books
        .get()
        .addOnSuccessListener { documents ->
            val booksList = mutableListOf<Book>()
            for (document in documents) {
                // Get the book data and ensure it has the document ID
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

@Composable
fun BookItemClickable(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Author: ${book.author}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Category: ${book.type}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}




