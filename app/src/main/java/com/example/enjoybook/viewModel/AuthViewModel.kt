package com.example.enjoybook.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        if (auth.currentUser == null) {
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value =
                        AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    fun signup(
        name: String,
        surname: String,
        username: String,
        email: String,
        password: String,
        phone: String,

    ) {
        // Validate all fields
        if (name.isEmpty() || surname.isEmpty() || username.isEmpty() ||
            email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }

        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    if (userId != null) {
                        val userMap = hashMapOf(
                            "name" to name,
                            "surname" to surname,
                            "username" to username,
                            "email" to email,
                            "phone" to phone,
                            "password" to password,
                        )

                        db.collection("users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                _authState.value = AuthState.Authenticated
                            }
                            .addOnFailureListener { e ->
                                auth.currentUser?.delete()
                                _authState.value = AuthState.Error(e.message ?: "Failed to save user data")
                            }
                    } else {
                        _authState.value = AuthState.Error("Failed to create user profile")
                    }
                } else {
                    _authState.value =
                        AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    fun signout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }


    fun updatePassword(newPassword: String) {
        val user = auth.currentUser
        if (user == null) {
            _authState.value = AuthState.Error("Utente non autenticato")
            return
        }

        _authState.value = AuthState.Loading

        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    updatePasswordInDatabase(user.uid, newPassword)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Errore nell'aggiornamento della password")
                }
            }
    }

    private fun updatePasswordInDatabase(userId: String, newPassword: String) {
        val userRef = db.collection("users").document(userId)

        userRef.update("password", newPassword)
            .addOnSuccessListener {
                _authState.value = AuthState.Authenticated
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error("Errore aggiornando la password nel database")
            }
    }

}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}