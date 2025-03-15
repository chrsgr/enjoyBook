package com.example.enjoybook.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.enjoybook.data.Book
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class BookViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun getBookById(bookId: String) {
        Log.d("FirestoreDebug", "Sto cercando il libro con ID: $bookId")

        db.collection("books").document(bookId).get()
            .addOnSuccessListener { snapshot ->
                Log.d("FirestoreDebug", "Documento ricevuto: ${snapshot.data}")

                if (snapshot.exists()) {
                    val book = snapshot.toObject(Book::class.java)
                    Log.d("FirestoreDebug", "Libro trovato: ${book?.title}")
                    _book.value = book
                } else {
                    Log.e("FirestoreDebug", "Libro non trovato")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDebug", "Errore Firestore: ${e.message}")
            }

    }

}


