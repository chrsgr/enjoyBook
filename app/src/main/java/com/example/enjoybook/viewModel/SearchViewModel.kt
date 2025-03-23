package com.example.enjoybook.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.enjoybook.data.Book
import com.example.enjoybook.data.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SearchViewModel : ViewModel() {
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books

    // Add StateFlow for users
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

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
                Log.e("SearchViewModel", "Errore durante la ricerca dei libri", exception)
            }
    }

    // Funzione di ricerca degli utenti per nome o username
    fun searchUsers(query: String) {
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
                        Log.e("SearchViewModel", "Errore durante la ricerca per nome", exception)
                        // In case of failure, still show username results
                        _users.value = usernameList
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("SearchViewModel", "Errore durante la ricerca per username", exception)
                _users.value = emptyList()
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
                Log.e("SearchViewModel", "Errore durante la ricerca per categoria", exception)
            }
    }
}