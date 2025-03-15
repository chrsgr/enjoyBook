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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.enjoybook.data.Book
import com.example.enjoybook.viewModel.SearchViewModel

/*Questa pagina serve per il SearchPage: la mia idea è che quando clicchiamo sul cercata o cerchiamo
dei libri ordinandoli per categoria, spariscano le categorie mostrare in SearchPage e mostri il risultato

TO DO: un'iconcina per il caricamento dei libri*/

@Composable
fun FilteredBooksPage(category: String, navController: NavController, viewModel: SearchViewModel) {

    Log.d("FilteredBooksPage", "Pagina caricata correttamente!")
    val books by viewModel.books.collectAsState() // Osserva lo stato dei libri
    Log.d("FilteredBooksPage", "Stato UI aggiornato: ${books.size} libri")
    val coroutineScope = rememberCoroutineScope()

    Log.d("FilteredBooksPage", "Libri ricevuti: ${books.size}")

    // Carica i libri filtrati per categoria all'apertura della schermata
    LaunchedEffect(category) {
        viewModel.filterForCategories(category)
    }

    // Debug per vedere se i libri sono presenti
    Log.d("FilteredBooksPage", "Libri caricati: ${books.size}")

    // Layout della pagina con la lista di libri
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(), // Centra verticalmente i contenuti
            horizontalAlignment = Alignment.CenterHorizontally // Opzionale per centrare anche orizzontalmente
        ) {
            Spacer(modifier = Modifier.height(88.dp))
            Text(
                text = "Books in $category",
            )

            // Se non ci sono libri, mostra un messaggio
            if (books.isEmpty()) {
                Text(text = "No books available in this category.")
            } else {
                LazyColumn {
                    items(books) { book ->
                        BookListItem(book)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Pulsante per tornare alla pagina di ricerca
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA7E8EB))
            ) {
                Text("Back to Search", color = Color.Black)
            }

        }
    }

}


@Composable
fun BookListItem(book: Book) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = book.title)
            Text(text = "Autore: ${book.author}")
        }
    }
}