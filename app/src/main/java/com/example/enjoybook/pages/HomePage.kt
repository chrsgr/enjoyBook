package com.example.enjoybook.pages


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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import  androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import  androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.ScreenState
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.example.enjoybook.viewModel.SearchViewModel
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController, authViewModel: AuthViewModel) {
    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)
    val errorColor = Color(0xFFD32F2F)

    var query by remember { mutableStateOf("") }
    val authState = authViewModel.authState.observeAsState()
    var searchQuery by remember { mutableStateOf(query) }
    val favorites by FavoritesManager.favoritesFlow.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var selectedGenre by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "Adventure", "Classics", "Crime", "Folk", "Fantasy", "Historical",
        "Horror", "Literary fiction", "Mystery", "Poetry", "Plays", "Romance",
        "Science fiction", "Short stories", "Thrillers", "War", "Women's fiction", "Young adult"
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

    var unreadNotifications by remember { mutableStateOf(3) }
    var showNotificationPopup by remember { mutableStateOf(false) }

    val notifications = remember {
        listOf(
            "Richiesta di affitto per libro 'Il nome della rosa'",
            "Richiesta di affitto per libro 'La Divina Commedia'",
            "Richiesta di affitto per libro 'I Promessi Sposi'"
        )
    }

    Scaffold(

    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 16.dp)
        ){
            // Genre
            item {
                //Spacer(modifier = Modifier.height(60.dp))
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
                        SectionHeader(
                            icon = Icons.Filled.Book,
                            title = "FEATURED BOOKS",
                            primaryColor = primaryColor,
                            textColor = textColor,

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
                            if (favorites.isEmpty()) {
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
                                    favorites.forEach { book ->
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

                // space  scrolling
                Spacer(modifier = Modifier.height(90.dp))
            }
        }
    }

    // Notification popup
    if (showNotificationPopup) {
        Dialog(onDismissRequest = { showNotificationPopup = false }) {
            Surface(
                modifier = Modifier.width(320.dp),
                shape = RoundedCornerShape(16.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textColor
                            )
                        }

                        IconButton(
                            onClick = { showNotificationPopup = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // List of notifications
                    notifications.forEach { notification ->
                        NotificationItem(
                            message = notification,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            errorColor = errorColor
                        )
                    }
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

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 Informazioni sul libro
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
fun NotificationItem(
    message: String,
    primaryColor: Color,
    textColor: Color,
    errorColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = errorColor.copy(alpha = 0.1f),
                        contentColor = errorColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("REJECT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("ACCEPT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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