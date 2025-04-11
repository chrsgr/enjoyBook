package com.example.enjoybook.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enjoybook.data.Book
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BooksViewModel : ViewModel() {
    private val _booksGrouped = MutableStateFlow<Map<Char, List<Book>>>(emptyMap())
    val booksGrouped: StateFlow<Map<Char, List<Book>>> = _booksGrouped

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Lista completa di tutte le lettere dell'alfabeto
    private val allLetters = ('A'..'Z').toList()

    init {
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val db = FirebaseFirestore.getInstance()
                val result = db.collection("books")
                    .orderBy("title")
                    .get()
                    .await()

                val booksList = result.documents.mapNotNull { doc ->
                    doc.toObject(Book::class.java)?.copy(id = doc.id)
                }

                // Raggruppiamo i libri per l'iniziale del titolo
                val grouped = booksList.groupBy { book ->
                    if (book.title.isNotEmpty()) book.title[0].uppercaseChar() else '#'
                }.toSortedMap()

                // Creiamo una mappa con tutte le lettere, anche quelle senza libri
                val completeMap = mutableMapOf<Char, List<Book>>()

                // Aggiungiamo tutte le lettere dell'alfabeto
                for (letter in allLetters) {
                    completeMap[letter] = grouped[letter] ?: emptyList()
                }

                // Ordiniamo la mappa
                _booksGrouped.value = completeMap.toSortedMap()
            } catch (e: Exception) {
                Log.e("BooksByInitialViewModel", "Error loading books", e)
                _error.value = "Impossibile caricare i libri: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}