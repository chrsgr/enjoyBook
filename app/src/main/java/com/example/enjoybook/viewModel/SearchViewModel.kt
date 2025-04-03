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

    // Add StateFlow for users
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
                val result = db.collection("books")
                    .whereGreaterThanOrEqualTo("titleLower", lowerCaseQuery)
                    .whereLessThanOrEqualTo("titleLower", lowerCaseQuery + "\uf8ff")
                    .get()
                    .await()

                val bookList = result.documents.mapNotNull { doc ->
                    doc.toObject(Book::class.java)?.copy(id = doc.id)
                }

                _books.value = bookList
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching books", e)
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
                val resultUsername = db.collection("users")
                    .whereGreaterThanOrEqualTo("username", lowerCaseQuery)
                    .whereLessThanOrEqualTo("username", lowerCaseQuery + "\uf8ff")
                    .get()
                    .await()

                val usernameList = resultUsername.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(userId = doc.id)
                }

                val resultName = db.collection("users")
                    .whereGreaterThanOrEqualTo("name", lowerCaseQuery)
                    .whereLessThanOrEqualTo("name", lowerCaseQuery + "\uf8ff")
                    .get()
                    .await()

                val nameList = resultName.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(userId = doc.id)
                }

                val combinedList = (usernameList + nameList).distinctBy { it.userId }

                _users.value = combinedList
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching users", e)
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