package com.example.enjoybook.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.enjoybook.data.Book
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


open class SearchViewModel : ViewModel() {
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    // Funzione di ricerca dei libri per titolo
    fun searchBooks(query: String) {
        val lowerCaseQuery = query.trim().lowercase()

        FirebaseFirestore.getInstance().collection("books")
            .whereGreaterThanOrEqualTo("titleLower", lowerCaseQuery)
            .whereLessThanOrEqualTo("titleLower", lowerCaseQuery + "\uf8ff")
            .get()
            .addOnSuccessListener { result ->
                val booksList = result.toObjects(Book::class.java)
                _books.value = booksList
            }
            .addOnFailureListener { exception ->
                Log.e("SearchViewModel", "Errore durante la ricerca", exception)
            }
    }

    // Funzione per filtrare per categoria
    fun filterForCategories(query: String) {
        Log.d("SearchViewModel", "Filtrando per categoria: $query")
        FirebaseFirestore.getInstance().collection("books")
            .whereEqualTo("type", query)
            .get()
            .addOnSuccessListener { result ->
                val booksList = result.toObjects(Book::class.java)
                _books.value = booksList
                Log.d("SearchViewModel", "Libri trovati: ${booksList.size}")
            }
            .addOnFailureListener { exception ->
                Log.e("SearchViewModel", "Errore durante la ricerca", exception)
            }
    }
}