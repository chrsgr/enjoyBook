package com.example.enjoybook.pages

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.enjoybook.data.Book
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.common.reflect.TypeToken
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        val json = sharedPreferences.getString("favorites", null)
        Log.d("SharedPreferences", "Initialize, Favorites JSON: $json")
        loadFavorites()
    }

    private fun loadFavorites() {
        val favoritesJson = sharedPreferences.getString("favorites", null)
        Log.d("SharedPreferences", "Load, Favorites JSON: $favoritesJson")
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
        Log.d("SharedPreferences", "Save, Favorites JSON: $favoritesJson")
        sharedPreferences.edit().putString("favorites", favoritesJson).apply()
    }

    fun addFavorite(book: Book) {
        Log.d("FavoritesManager", "Aggiungendo: ${book.title}, Cover: ${book.frontCoverUrl ?: "No Cover"}")
        if (!isBookFavorite(book.id)) {
            Log.d("FavoritesManager", "Aggiungendo: ${book.title}, Cover: ${book.frontCoverUrl}")
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

}
@Composable
fun FavouritePage(
    navController: NavController,
) {
    val favorites by remember { mutableStateOf(FavoritesManager.favorites) }

    val refreshing = remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(refreshing.value)

    val refreshScope = rememberCoroutineScope()

    fun refresh() {
        refreshScope.launch {
            refreshing.value = true
            delay(500)
            refreshing.value = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = { refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                                tint = Color(0xFF2CBABE)
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
                                    navController.navigate("bookDetails/${book.id}")
                                },
                                onRemove = {
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser != null) {
                                        val db = FirebaseFirestore.getInstance()
                                        db.collection("favorites")
                                            .whereEqualTo("bookId", book.id)
                                            .whereEqualTo("userId", currentUser.uid)
                                            .get()
                                            .addOnSuccessListener { documents ->
                                                for (document in documents) {
                                                    db.collection("favorites").document(document.id).delete()
                                                }
                                                FavoritesManager.removeFavorite(book.id)

                                                refreshScope.launch {
                                                    delay(100)
                                                    refreshing.value = true
                                                    delay(300)
                                                    refreshing.value = false
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                            }
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

    val isFrontCover = remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover
            if (isFrontCover.value && book?.frontCoverUrl != null)  {
                Box(
                    modifier = Modifier
                        .size(70.dp, 100.dp)
                        .background(Color(0xFF2CBABE).copy(alpha = 0.3f))
                        .clip(RoundedCornerShape(4.dp)),
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
                        contentScale = ContentScale.Crop
                    )
                }
            }

            else {
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
                    color = Color(0xFF333333)
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