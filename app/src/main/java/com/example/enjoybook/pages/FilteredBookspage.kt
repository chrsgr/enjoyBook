package com.example.enjoybook.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.example.enjoybook.data.Book
import com.example.enjoybook.theme.primaryColor
import com.example.enjoybook.theme.secondaryColor
import com.example.enjoybook.theme.textColor
import com.example.enjoybook.viewModel.SearchViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredBooksPage(category: String, navController: NavController, viewModel: SearchViewModel) {

    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(category) {
        viewModel.filterForCategories(category)
    }

    when {
        isLoading -> {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.size(50.dp)
                    )}
            }
        }

        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "BOOKS IN ${category.uppercase()}",
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                androidx.compose.material.Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = secondaryColor
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            titleContentColor = textColor
                        ),
                        windowInsets = WindowInsets(0)
                    )
                },
                contentWindowInsets = WindowInsets(0)
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (books.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No books available in this category.",
                                    color = textColor.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .weight(1f)
                            ) {
                                items(books) { book ->
                                    BookListItem(book, navController)
                                }
                            }
                        }

                    }
                }
            }
        }
    }

}


@Composable
fun BookListItem(book: Book, navController: NavController) {
    val isFrontCover = remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickable {
                    navController.navigate("bookDetails/${book.id}")
                },
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (isFrontCover.value && book?.frontCoverUrl != null)
            {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(com.example.enjoybook.theme.primaryColor),
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
                        .size(80.dp)
                        .background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Book details
            Column {
                Text(
                    text = book.title,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "by ${book.author}",
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.type,
                    color = primaryColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


