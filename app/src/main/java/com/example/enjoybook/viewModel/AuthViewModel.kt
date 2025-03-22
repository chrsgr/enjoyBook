package com.example.enjoybook.viewModel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enjoybook.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class AuthViewModel(val context: Context): ViewModel() {

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
                    val user = auth.currentUser
                    if (user != null && user.isEmailVerified) {
                        _authState.value = AuthState.Authenticated
                    } else {
                        auth.signOut()
                        _authState.value =
                            AuthState.Error("Please verify your email before logging in")
                    }
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

                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {

                                val userId = auth.currentUser?.uid

                                if (userId != null) {
                                    val userMap = hashMapOf(
                                        "name" to name,
                                        "surname" to surname,
                                        "username" to username,
                                        "email" to email,
                                        "phone" to phone,
                                        "password" to password,
                                        "emailVerified" to false
                                    )

                                    db.collection("users").document(userId)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            auth.signOut()
                                            _authState.value = AuthState.WaitingForVerification
                                        }
                                        .addOnFailureListener { e ->
                                            user.delete()
                                            _authState.value = AuthState.Error(
                                                e.message ?: "Failed to save user data"
                                            )
                                        }
                                } else {
                                    user.delete()
                                    _authState.value =
                                        AuthState.Error("Failed to create user profile")
                                }
                            } else {
                                user.delete()  // Se l'email di verifica non viene inviata, elimina l'account
                                _authState.value =
                                    AuthState.Error("Failed to send verification email")
                            }
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

    fun resetPassword(email: String) {
        if (email.isEmpty()) {
            _authState.value = AuthState.Error("Insert a valid email")
            return
        }

        _authState.value = AuthState.Loading

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.PasswordResetSent
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Errore durante l'invio della mail")
                }
            }
    }

    //PROVA AUTENTICAZIONE GOOGLE

    private fun createNonce(): String{
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return digest.fold("") { str, it ->
            str + "%02x".format(it)
        }

    }

    fun signInWithGoogle() {
        _authState.value = AuthState.Loading

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.web_client_id))
            .setAutoSelectEnabled(false)
            .setNonce(createNonce())
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential) {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            val googleIdTokenCredential = GoogleIdTokenCredential
                                .createFrom(credential.data)
                            val firebaseCredential = GoogleAuthProvider
                                .getCredential(
                                    googleIdTokenCredential.idToken,
                                    null
                                )
                            auth.signInWithCredential(firebaseCredential)
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        val user = auth.currentUser

                                        // Aggiorna solo i campi che vuoi sempre mantenere aggiornati con Google
                                        val googleUpdates = hashMapOf(
                                            "uid" to user?.uid,
                                            "email" to user?.email,
                                            "profilePictureUrl" to user?.photoUrl?.toString(),
                                            "lastLogin" to FieldValue.serverTimestamp()
                                        )

                                        // Salva i dati in Firestore (nella collezione "users")
                                        user?.uid?.let { uid ->
                                            db.collection("users").document(uid).get()
                                                .addOnSuccessListener { document ->
                                                    if (document.exists()) {
                                                        // L'utente esiste, aggiorna solo i campi di Google
                                                        db.collection("users").document(uid)
                                                            .update(googleUpdates as Map<String, Any>)
                                                            .addOnSuccessListener {
                                                                _authState.value = AuthState.Authenticated
                                                            }
                                                            .addOnFailureListener { e ->
                                                                _authState.value = AuthState.Error(message = e.message ?: "Error updating user data")
                                                            }
                                                    } else {
                                                        // Primo accesso: crea un nuovo utente con tutti i dati
                                                        val displayName = user.displayName ?: ""
                                                        val nameParts = displayName.split(" ", limit = 2)
                                                        val firstName = nameParts.getOrNull(0) ?: ""
                                                        val lastName = if (nameParts.size > 1) nameParts[1] else ""

                                                        val fullUserData = googleUpdates.apply {
                                                            put("name", firstName)
                                                            put("surname", lastName)
                                                            put("username","")
                                                            put("phone","")
                                                            put("emailVerified", false)
                                                            put("isGoogleAuth", true)
                                                            put("createdAt", FieldValue.serverTimestamp())
                                                        }

                                                        db.collection("users").document(uid)
                                                            .set(fullUserData)
                                                            .addOnSuccessListener {
                                                                _authState.value = AuthState.Authenticated
                                                            }
                                                            .addOnFailureListener { e ->
                                                                _authState.value = AuthState.Error(message = e.message ?: "Error saving user data")
                                                            }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    _authState.value = AuthState.Error(message = e.message ?: "Error checking user data")
                                                }
                                        } ?: run {
                                            _authState.value = AuthState.Authenticated
                                        }
                                    } else {
                                        _authState.value = AuthState.Error(message = it.exception?.message ?: "Unknown error")
                                    }
                                }
                        } catch (e: GoogleIdTokenParsingException) {
                            _authState.value = AuthState.Error(message = e.message ?: "Error parsing Google token")
                        }
                    } else {
                        _authState.value = AuthState.Error("Unsupported credential type")
                    }
                } else {
                    _authState.value = AuthState.Error("Invalid credential")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(message = e.message ?: "Authentication failed")
            }
        }
    }
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    object WaitingForVerification: AuthState()
    object PasswordResetSent: AuthState()
    data class Error(val message: String) : AuthState()
}