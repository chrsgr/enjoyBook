package com.example.enjoybook.pages


import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.ScreenState
import com.example.enjoybook.viewModel.SearchViewModel

@Composable
fun QueryBooks(query: String, navController: NavController, viewModel: SearchViewModel) {

    Log.d("QueryBook", "Pagina caricata correttamente!")
    val books by viewModel.books.collectAsState() // Osserva lo stato dei libri
    val coroutineScope = rememberCoroutineScope()

    Log.d("QueryBook", "Query ricevuta: $query")

    // Carica i libri filtrati per categoria all'apertura della schermata
    LaunchedEffect(query) {
        viewModel.searchBooks(query)
    }

    // Layout della pagina con la lista di libri filtrati
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(), // Centra verticalmente i contenuti
            horizontalAlignment = Alignment.CenterHorizontally // Opzionale per centrare anche orizzontalmente
        ) {
            //Spacer(modifier = Modifier.height(88.dp))
            Text("Results for: \"$query\"", style = MaterialTheme.typography.headlineSmall)

            if (books.isEmpty()) {
                Text("No books found.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn {
                    items(books) { book ->
                        BookListQuery(book)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                navController.navigate("search"){
                    popUpTo("search") { inclusive = true }
                }
            }) {
                Text("Back to Search")
            }
        }
    }

}

@Composable
fun BookListQuery(book: Book) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = book.title)
            Text(text = "Autore: ${book.author}")
        }
    }
}