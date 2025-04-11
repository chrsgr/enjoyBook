package com.example.enjoybook.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class SearchViewModel : ViewModel() {
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading


    fun searchBooks(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val lowerCaseQuery = query.trim().lowercase()

            try {
                val db = FirebaseFirestore.getInstance()
                val allBooks = db.collection("books").get().await()

                val bookList = allBooks.documents.mapNotNull { doc ->
                    val book = doc.toObject(Book::class.java)
                    if (book != null) {
                        val bookWithId = book.copy(id = doc.id)

                        if (bookWithId.title.lowercase().contains(lowerCaseQuery) ||
                            bookWithId.author.lowercase().contains(lowerCaseQuery)) {
                            bookWithId
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }

                _books.value = bookList
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching books", e)
                _books.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val lowerCaseQuery = query.trim().lowercase()

            try {
                val db = FirebaseFirestore.getInstance()
                val allUsers = db.collection("users").get().await()

                val userList = allUsers.documents.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        val userWithId = user.copy(userId = doc.id)

                        if (userWithId.username.lowercase().contains(lowerCaseQuery) ||
                            userWithId.name.lowercase().contains(lowerCaseQuery) ||
                            userWithId.surname.lowercase().contains(lowerCaseQuery)) {
                            userWithId
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }

                _users.value = userList
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching users", e)
                _users.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterForCategories(category: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val db = FirebaseFirestore.getInstance()
                val result = db.collection("books")
                    .whereEqualTo("type", category)
                    .get()
                    .await()

                val bookList = result.documents.mapNotNull { doc ->
                    doc.toObject(Book::class.java)?.copy(id = doc.id)
                }

                _books.value = bookList
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error filtering by category", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}