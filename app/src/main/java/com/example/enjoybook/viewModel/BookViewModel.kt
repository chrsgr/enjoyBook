package com.example.enjoybook.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enjoybook.data.Book
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
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

    private val allLetters = ('A'..'Z').toList()

    // Job to track current loading operation
    private var loadingJob: Job? = null

    init {
        loadBooks()
    }

    fun loadBooks() {
        // Cancel any existing loading job
        loadingJob?.cancel()

        // Start a new loading job
        loadingJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val db = FirebaseFirestore.getInstance()

                // First get banned users
                val bannedUsersQuery = db.collection("users")
                    .whereEqualTo("isBanned", true)
                    .get()
                    .await()

                val bannedUserIds = bannedUsersQuery.documents.mapNotNull { it.id }.toSet()

                // Then get all books
                val result = db.collection("books")
                    .orderBy("title")
                    .get()
                    .await()

                val booksList = result.documents.mapNotNull { doc ->
                    val book = doc.toObject(Book::class.java)?.copy(id = doc.id)

                    // Only include books from non-banned users
                    if (book != null && !bannedUserIds.contains(book.userId)) {
                        book
                    } else {
                        null
                    }
                }

                val grouped = booksList.groupBy { book ->
                    if (book.title.isNotEmpty()) book.title[0].uppercaseChar() else '#'
                }.toSortedMap()

                val completeMap = mutableMapOf<Char, List<Book>>()

                for (letter in allLetters) {
                    completeMap[letter] = grouped[letter] ?: emptyList()
                }

                _booksGrouped.value = completeMap.toSortedMap()
            } catch (e: Exception) {
                Log.e("BooksByInitialViewModel", "Error loading books", e)
                _error.value = "Impossibile caricare i libri: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Call this method for immediate refresh
    fun refreshBooks() {
        loadBooks()
    }
}