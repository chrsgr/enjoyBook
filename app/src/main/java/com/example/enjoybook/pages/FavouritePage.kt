package com.example.enjoybook.pages

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.example.enjoybook.data.Book
import com.google.common.reflect.TypeToken
import com.google.gson.Gson


object FavoritesManager {
    private val _favorites = mutableStateListOf<Book>()
    val favorites: List<Book> = _favorites

    private val _favoritesFlow = MutableStateFlow<List<Book>>(_favorites.toList())
    val favoritesFlow: StateFlow<List<Book>> = _favoritesFlow

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("favorites_prefs", Context.MODE_PRIVATE)
        loadFavorites()
    }

    private fun loadFavorites() {
        val favoritesJson = sharedPreferences.getString("favorites", null)
        if (favoritesJson != null) {
            val type = object : TypeToken<List<Book>>() {}.type
            val loadedFavorites = gson.fromJson<List<Book>>(favoritesJson, type)
            _favorites.clear()
            _favorites.addAll(loadedFavorites)
            _favoritesFlow.value = _favorites.toList()
        }
    }

    private fun saveFavorites() {
        val favoritesJson = gson.toJson(_favorites.toList())
        sharedPreferences.edit().putString("favorites", favoritesJson).apply()
    }

    fun addFavorite(book: Book) {
        if (!isBookFavorite(book.id)) {
            _favorites.add(book)
            _favoritesFlow.value = _favorites.toList()
            saveFavorites()
        }
    }

    fun removeFavorite(bookId: String) {
        _favorites.removeIf { it.id == bookId }
        _favoritesFlow.value = _favorites.toList()
        saveFavorites()
    }

    fun isBookFavorite(bookId: String): Boolean {
        return _favorites.any { it.id == bookId }
    }

    fun getFavoriteBooks(): List<Book> {
        return _favorites.toList()
    }

    fun getFavoriteById(bookId: String): Book? {
        return _favorites.find { it.id == bookId }
    }
}
@Composable
fun FavouritePage(
    navController: NavController,
    onBookClick: (String) -> Unit = {}
) {
    val favorites by remember { mutableStateOf(FavoritesManager.favorites) }

    val refreshScope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var refreshOffset by remember { mutableStateOf(0f) }
    val maxRefreshOffset = 120.dp

    val density = LocalDensity.current
    val maxRefreshOffsetPx = with(density) { maxRefreshOffset.toPx() }

    fun refresh() = refreshScope.launch {
        refreshing = true
        delay(1500)
        refreshing = false

        val startTime = System.currentTimeMillis()
        val animationDuration = 300
        while (refreshOffset > 0) {
            val elapsedTime = System.currentTimeMillis() - startTime
            val fraction = (elapsedTime.toFloat() / animationDuration).coerceIn(0f, 1f)
            refreshOffset = maxRefreshOffsetPx * (1 - fraction)
            delay(16)
            if (elapsedTime >= animationDuration) break
        }
        refreshOffset = 0f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            if (refreshOffset > maxRefreshOffsetPx * 0.7f) {
                                refresh()
                            } else {
                                refreshScope.launch {
                                    val startTime = System.currentTimeMillis()
                                    val animationDuration = 200
                                    val startOffset = refreshOffset
                                    while (refreshOffset > 0) {
                                        val elapsedTime = System.currentTimeMillis() - startTime
                                        val fraction = (elapsedTime.toFloat() / animationDuration).coerceIn(0f, 1f)
                                        refreshOffset = startOffset * (1 - fraction)
                                        delay(16)
                                        if (elapsedTime >= animationDuration) break
                                    }
                                    refreshOffset = 0f
                                }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0) {
                                change.consume()
                                refreshOffset = (refreshOffset + dragAmount * 0.5f).coerceIn(
                                    0f,
                                    maxRefreshOffsetPx * 1.5f
                                )
                            }
                        }
                    )
                }
        ) {
            if (refreshOffset > 0 || refreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { refreshOffset.toDp() })
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            progress = if (refreshing) 1f else (refreshOffset / maxRefreshOffsetPx).coerceIn(0f, 1f),
                            color = Color(0xFF2CBABE) // Updated to use the primary color
                        )

                        if (refreshOffset > maxRefreshOffsetPx * 0.7f || refreshing) {
                            Text(
                                text = if (refreshing) "Aggiornamento..." else "Rilascia per aggiornare",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = with(density) { refreshOffset.toDp() })
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Titolo
                Text(
                    text = "FAVORITE BOOKS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333), // Using text color from login page
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (favorites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Favorite,
                                contentDescription = "No favorites",
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(bottom = 16.dp),
                                tint = Color(0xFF2CBABE) // Updated to use the primary color
                            )
                            Text(
                                text = "No favorites yet",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(favorites) { book ->
                            FavoriteBookItem(
                                book = book,
                                onClick = {
                                    navController.navigate("bookDetails/${book.id}")                                },
                                onRemove = {
                                    FavoritesManager.removeFavorite(book.id)
                                    refreshScope.launch {
                                        delay(100)
                                        refreshing = true
                                        delay(300)
                                        refreshing = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteBookItem(
    book: Book,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover
            Box(
                modifier = Modifier
                    .size(70.dp, 100.dp)
                    .background(Color(0xFF2CBABE).copy(alpha = 0.3f))
                    .clip(RoundedCornerShape(4.dp))

            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                    tint = Color(0xFF2CBABE)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF333333) // Using text color from login page
                )
                Text(
                    text = "di ${book.author}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${book.year} • ${book.type}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Button to remove from favorites
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.HeartBroken,
                    contentDescription = "Delete from favourite",
                    tint = Color.Red
                )
            }
        }
    }
}