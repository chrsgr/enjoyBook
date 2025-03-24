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


    // Funzione di ricerca dei libri per titolo
    /*fun searchBooks(query: String) {
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
                Log.e("SearchViewModel", "Errore durante la ricerca dei libri", exception)
            }
    }*/

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

    // Funzione di ricerca degli utenti per nome o username
    /*fun searchUsers(query: String) {
        val lowerCaseQuery = query.trim().lowercase()

        // First search by username
        FirebaseFirestore.getInstance().collection("users")
            .whereGreaterThanOrEqualTo("username", lowerCaseQuery)
            .whereLessThanOrEqualTo("username", lowerCaseQuery + "\uf8ff")
            .get()
            .addOnSuccessListener { usernameResults ->
                val usernameList = usernameResults.toObjects(User::class.java)

                // Then search by name
                FirebaseFirestore.getInstance().collection("users")
                    .whereGreaterThanOrEqualTo("name", lowerCaseQuery)
                    .whereLessThanOrEqualTo("name", lowerCaseQuery + "\uf8ff")
                    .get()
                    .addOnSuccessListener { nameResults ->
                        val nameList = nameResults.toObjects(User::class.java)

                        // Combine results and remove duplicates by ID
                        val combinedList = (usernameList + nameList).distinctBy { it.userId }
                        _users.value = combinedList
                    }
                    .addOnFailureListener { exception ->
                        // In case of failure, still show username results
                        _users.value = usernameList
                    }
            }
            .addOnFailureListener { exception ->
                _users.value = emptyList()
            }
    }*/

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

    // Funzione per filtrare per categoria
    /*fun filterForCategories(query: String) {
        FirebaseFirestore.getInstance().collection("books")
            .whereEqualTo("type", query)
            .get()
            .addOnSuccessListener { result ->
                val booksList = result.toObjects(Book::class.java)
                _books.value = booksList
            }
            .addOnFailureListener { exception ->
                Log.e("SearchViewModel", "Errore durante la ricerca per categoria", exception)
            }
    }*/

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