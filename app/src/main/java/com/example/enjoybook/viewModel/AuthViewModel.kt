package com.example.enjoybook.viewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.enjoybook.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.net.URLEncoder
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
        _authState.value = AuthState.Loading

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: ""

                    if (username.isBlank()) {
                        _authState.value = AuthState.WaitingForUsername
                    } else {
                        _authState.value = AuthState.Authenticated
                    }
                } else {
                    auth.signOut()
                    _authState.value = AuthState.Unauthenticated
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Error checking user data")
            }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading

        db.collection("users")
            .whereEqualTo("email", email)
            .whereEqualTo("isGoogleAuth", true)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    _authState.value = AuthState.Error("This email is already used with Google authentication. Please sign in with Google.")
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { userDocs ->
                        val userDoc = userDocs.documents.firstOrNull()
                        val isBanned = userDoc?.getBoolean("isBanned") ?: false

                        if (isBanned) {
                            FirebaseAuth.getInstance().signOut()
                            Toast.makeText(
                                context,
                                "Banned account. Denied access.",
                                Toast.LENGTH_LONG
                            ).show()
                            _authState.value = AuthState.Error("Banned account. Denied access.")
                        } else {

                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->

                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        if (user != null && user.isEmailVerified) {
                                            checkAuthStatus()
                                        } else {
                                            auth.signOut()
                                            _authState.value = AuthState.Error("Please verify your email before logging in")
                                        }
                                    } else {
                                        _authState.value =
                                            AuthState.Error(
                                                task.exception?.message ?: "Something went wrong"
                                            )
                                    }
                                }
                            }
                        }
                    .addOnFailureListener { e ->
                        _authState.value = AuthState.Error(
                            e.message ?: "Error checking email authentication method"
                        )
                    }
            }
    }


    fun signup(
        name: String,
        surname: String,
        username: String,
        email: String,
        password: String,
        phone: String?,
    ) {
        // Validate all fields
        if (name.isEmpty() || surname.isEmpty() || username.isEmpty() ||
            email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }

        _authState.value = AuthState.Loading

        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val isGoogleAuth = document.getBoolean("isGoogleAuth") ?: false
                        if (isGoogleAuth) {
                            _authState.value = AuthState.Error("This email is already used with Google authentication. Please sign in with Google.")
                            return@addOnSuccessListener
                        }

                    }
                }

                isUsernameTaken(username) { isTaken ->
                    if (isTaken) {
                        _authState.value = AuthState.Error("This username is already.")
                        return@isUsernameTaken

                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    user?.sendEmailVerification()
                                        ?.addOnCompleteListener { verificationTask ->
                                            if (verificationTask.isSuccessful) {
                                                val userId = auth.currentUser?.uid

                                                if (userId != null) {

                                                    val displayName = "$name $surname"
                                                    val encodedName =
                                                        URLEncoder.encode(displayName, "UTF-8")
                                                    val profilePictureUrl =
                                                        "https://ui-avatars.com/api/?name=$encodedName&background=random&color=fff&size=200"

                                                    val profileUpdates = userProfileChangeRequest {
                                                        photoUri = Uri.parse(profilePictureUrl)
                                                    }
                                                    user.updateProfile(profileUpdates)

                                                    val userMap = hashMapOf(
                                                        "userId" to userId,
                                                        "name" to name,
                                                        "surname" to surname,
                                                        "username" to username,
                                                        "email" to email,
                                                        "phone" to phone,
                                                        "emailVerified" to false,
                                                        "isBanned" to false,
                                                        "isGoogleAuth" to false,
                                                        "role" to "user",
                                                        "profilePictureUrl" to profilePictureUrl,
                                                        "createdAt" to FieldValue.serverTimestamp()
                                                    )

                                                    db.collection("users").document(userId)
                                                        .set(userMap)
                                                        .addOnSuccessListener {
                                                            auth.signOut()
                                                            _authState.value =
                                                                AuthState.WaitingForVerification
                                                        }
                                                        .addOnFailureListener { e ->
                                                            user.delete()
                                                            _authState.value = AuthState.Error(
                                                                e.message
                                                                    ?: "Failed to save user data"
                                                            )
                                                        }
                                                } else {
                                                    user.delete()
                                                    _authState.value =
                                                        AuthState.Error("Failed to create user profile")
                                                }
                                            } else {
                                                user.delete()
                                                _authState.value =
                                                    AuthState.Error("Failed to send verification email")
                                            }
                                        }
                                } else {
                                    _authState.value =
                                        AuthState.Error(
                                            task.exception?.message ?: "Something went wrong"
                                        )
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Error checking email authentication method")
            }
    }

    fun signout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }


    fun isUsernameTaken(username: String, onComplete: (Boolean) -> Unit) {
        val usersRef = db.collection("users")
        val query = usersRef.whereEqualTo("username", username)

        query.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val documents = task.result
                onComplete(documents?.isEmpty == false)
            } else {
                Log.w("Signup", "Error checking username", task.exception)
                onComplete(false)
            }
        }
    }


//    fun updatePassword(newPassword: String) {
//        val user = auth.currentUser
//        if (user == null) {
//            _authState.value = AuthState.Error("Unauthenticated user")
//            return
//        }
//
//        _authState.value = AuthState.Loading
//
//        user.updatePassword(newPassword)
//            .addOnCompleteListener { task ->
//                if (task.isSuccessful) {
//
//                    updatePasswordInDatabase(user.uid, newPassword)
//                } else {
//                    _authState.value = AuthState.Error(task.exception?.message ?: "Errore nell'aggiornamento della password")
//                }
//            }
//    }

//    private fun updatePasswordInDatabase(userId: String, newPassword: String) {
//        val userRef = db.collection("users").document(userId)
//
//        userRef.update("password", newPassword)
//            .addOnSuccessListener {
//                _authState.value = AuthState.Authenticated
//            }
//            .addOnFailureListener { e ->
//                _authState.value = AuthState.Error("Errore aggiornando la password nel database")
//            }
//    }

    fun resetPassword(email: String) {
        if (email.isEmpty()) {
            _authState.value = AuthState.Error("Insert a valid email")
            return
        }

        _authState.value = AuthState.Loading

        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val isGoogleAuth = document.getBoolean("isGoogleAuth") ?: false
                        if (isGoogleAuth) {
                            _authState.value = AuthState.Error("This email is registered with Google. Please sign in with Google instead.")
                            return@addOnSuccessListener
                        }
                    }
                }

                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _authState.value = AuthState.PasswordResetSent
                        } else {
                            _authState.value = AuthState.Error(task.exception?.message ?: "Error during the send of the email")
                        }
                    }
            }
            .addOnFailureListener { e ->
                _authState.value = AuthState.Error(e.message ?: "Error in the check of the authentication method")
            }
    }

    fun saveUsername(username: String, callback: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val currentUser = auth.currentUser

            if (currentUser == null) {
                callback(false, "User not logged in")
                return@launch
            }

            if (username.isBlank()) {
                callback(false, "Username cannot be empty")
                return@launch
            }

            db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        callback(false, "Username already in use")
                        return@addOnSuccessListener
                    }

                    db.collection("users").document(currentUser.uid)
                        .update("username", username)
                        .addOnSuccessListener {
                            _authState.value = AuthState.Authenticated
                            callback(true, null)
                        }
                        .addOnFailureListener { e ->
                            callback(false, e.message ?: "Error saving username")
                        }
                }
                .addOnFailureListener { e ->
                    callback(false, e.message ?: "Error checking username availability")
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

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                        val email = googleIdTokenCredential.id

                        db.collection("users")
                            .whereEqualTo("email", email)
                            .whereEqualTo("isGoogleAuth", false)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    _authState.value = AuthState.Error("This email is already registered with email/password. Please use that method to sign in.")
                                    return@addOnSuccessListener
                                }

                                db.collection("users")
                                    .whereEqualTo("email", email)
                                    .get()
                                    .addOnSuccessListener { userDocs ->
                                        val userDoc = userDocs.documents.firstOrNull()
                                        val isBanned = userDoc?.getBoolean("isBanned") ?: false

                                        if (isBanned) {
                                            FirebaseAuth.getInstance().signOut()
                                            Toast.makeText(context, "Banned account. Denied access.", Toast.LENGTH_LONG).show()
                                            _authState.value = AuthState.Error("Banned account. Denied access.")
                                        } else {

                                            auth.signInWithCredential(firebaseCredential)
                                                .addOnCompleteListener { task ->
                                                    if (task.isSuccessful) {
                                                        val user = auth.currentUser
                                                        val userId = user?.uid

                                                        val googleUpdates = hashMapOf(
                                                            "userId" to userId,
                                                            "email" to user?.email,
                                                            "profilePictureUrl" to user?.photoUrl?.toString(),
                                                            "lastLogin" to FieldValue.serverTimestamp()
                                                        )

                                                        userId?.let { uid ->
                                                            db.collection("users").document(uid).get()
                                                                .addOnSuccessListener { document ->
                                                                    if (document.exists()) {
                                                                        db.collection("users").document(uid)
                                                                            .update(googleUpdates as Map<String, Any>)
                                                                            .addOnSuccessListener {
                                                                                checkAuthStatus()
                                                                            }
                                                                            .addOnFailureListener { e ->
                                                                                _authState.value = AuthState.Error(e.message ?: "Error updating user data")
                                                                            }
                                                                    } else {
                                                                        val displayName = user.displayName ?: ""
                                                                        val nameParts = displayName.split(" ", limit = 2)
                                                                        val firstName = nameParts.getOrNull(0) ?: ""
                                                                        val lastName = nameParts.getOrNull(1) ?: ""

                                                                        val fullUserData = googleUpdates.apply {
                                                                            put("name", firstName)
                                                                            put("surname", lastName)
                                                                            put("role", "user")
                                                                            put("username", "")
                                                                            put("phone", "")
                                                                            put("emailVerified", false)
                                                                            put("isGoogleAuth", true)
                                                                            put("isBanned", false)
                                                                            put("createdAt", FieldValue.serverTimestamp())
                                                                        }

                                                                        db.collection("users").document(uid)
                                                                            .set(fullUserData)
                                                                            .addOnSuccessListener {
                                                                                _authState.value = AuthState.WaitingForUsername

                                                                            }
                                                                            .addOnFailureListener { e ->
                                                                                _authState.value = AuthState.Error(e.message ?: "Error saving user data")
                                                                            }
                                                                    }
                                                                }
                                                                .addOnFailureListener { e ->
                                                                    _authState.value = AuthState.Error(e.message ?: "Error checking user data")
                                                                }
                                                        }
                                                    } else {
                                                        _authState.value = AuthState.Error(task.exception?.message ?: "Unknown error")
                                                    }
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        _authState.value = AuthState.Error(e.message ?: "Error checking banned status")
                                    }
                            }
                            .addOnFailureListener { e ->
                                _authState.value = AuthState.Error(e.message ?: "Error checking email authentication method")
                            }

                    } catch (e: GoogleIdTokenParsingException) {
                        _authState.value = AuthState.Error(e.message ?: "Error parsing Google token")
                    }
                } else {
                    _authState.value = AuthState.Error("Unsupported credential type")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            }
        }
    }
}



sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    object WaitingForVerification: AuthState()
    object WaitingForUsername: AuthState()
    object PasswordResetSent: AuthState()
    data class Error(val message: String) : AuthState()
}