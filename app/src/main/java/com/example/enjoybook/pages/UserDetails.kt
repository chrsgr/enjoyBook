package com.example.enjoybook.pages

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import coil.compose.AsyncImage
import com.example.enjoybook.data.Book
import com.example.enjoybook.theme.primaryColor
import com.example.enjoybook.theme.textColor
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.auth.User
import com.example.enjoybook.data.User
import com.example.enjoybook.utils.reportHandler
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetails(navController: NavController, authViewModel: AuthViewModel, userId: String){

    val db = FirebaseFirestore.getInstance()

    val primaryColor = Color(0xFF2CBABE)
    val secondaryColor = Color(0xFF1A8A8F)
    val backgroundColor = (primaryColor.copy(alpha = 0.1f))

    val textColor = Color(0xFF333333)
    val errorColor = Color(0xFFD32F2F)
    val successColor = Color(0xFF4CAF50)
    val warningColor = Color(0xFFFF9800)
    val cardBackground = Color.White

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val authState = authViewModel.authState.observeAsState().value

    var showReportDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var favorites by remember { mutableStateOf<List<Book>>(emptyList()) }

    var isAdmin by remember { mutableStateOf(false) }
    var isBanned by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            isLoading = true
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        user = document.toObject(User::class.java)
                        isLoading = false
                    } else {
                        errorMessage = "User not found"
                        showErrorDialog = true
                        isLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error: ${e.message}"
                    showErrorDialog = true
                    isLoading = false
                }

        } else {
            errorMessage = "No user ID specified"
            showErrorDialog = true
            isLoading = false
        }
    }


    LaunchedEffect(authState) {
        if (authState !is AuthState.Authenticated) {
            navController.navigate("login")
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            val userDoc = Firebase.firestore.collection("users").document(uid).get().await()
            isAdmin = userDoc.getString("role") == "admin"
        }

        val targetUserDoc = Firebase.firestore.collection("users").document(userId).get().await()
        isBanned = targetUserDoc.getBoolean("isBanned") ?: false
    }


    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showErrorDialog = false
                    if (userId.isEmpty()) {
                        navController.popBackStack()
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }


    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = primaryColor)
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "USER DETAILS",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = secondaryColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        titleContentColor = textColor
                    ),
                    windowInsets = WindowInsets(0)
                )
            },
            contentWindowInsets = WindowInsets(0)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (user != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp)
                    ) {
                        user?.profilePictureUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, primaryColor, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Username
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ){
                            Text(
                                text = user?.username ?: "",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Start
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Nome e Cognome
                            Text(
                                text = "${user?.name ?: ""} ${user?.surname ?: ""}",
                                fontSize = 18.sp,
                                color = textColor,
                                textAlign = TextAlign.Start
                            )

                            //show only if is not the current user
                            if (currentUser != null && currentUser.uid != userId) {
                                Spacer(modifier = Modifier.height(16.dp))

                                if (isAdmin) {
                                    // Pulsante per bannare/sbannare
                                    OutlinedButton(
                                        onClick = {
                                            val newStatus = !isBanned
                                            Firebase.firestore.collection("users").document(userId)
                                                .update("isBanned", newStatus)
                                                .addOnSuccessListener { isBanned = newStatus }
                                        },
                                        modifier = Modifier
                                            .width(250.dp)
                                            .height(50.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (isBanned) Color.Green else Color.Red
                                        ),
                                        border = BorderStroke(1.dp, if (isBanned) Color.Green else Color.Red),
                                        shape = RoundedCornerShape(25.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isBanned) Icons.Default.LockOpen else Icons.Default.Block,
                                                contentDescription = "Ban/Unban",
                                                tint = if (isBanned) Color.Green else Color.Red,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                if (isBanned) "Unban Account" else "Ban Account",
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { showReportDialog = true },
                                        modifier = Modifier
                                            .width(250.dp)
                                            .height(50.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = warningColor
                                        ),
                                        border = BorderStroke(1.dp, warningColor),
                                        shape = RoundedCornerShape(25.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Report,
                                                contentDescription = "Report",
                                                tint = warningColor,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                "Report Account",
                                                color = warningColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (showReportDialog) {
                                        reportHandler(userId, user?.username, showReportDialog)
                                    }
                                }
                            }
                        }
                    }

                    //Spacer(modifier = Modifier.height(24.dp))
                    if (currentUser != null && currentUser.uid != userId) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Aggiungi questo blocco
                        OutlinedButton(
                            onClick = {
                                navController.navigate("messaging/$userId")
                            },
                            modifier = Modifier
                                .width(250.dp)
                                .height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = primaryColor
                            ),
                            border = BorderStroke(1.dp, primaryColor),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = "Send Message",
                                    tint = primaryColor,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    "Send Message",
                                    color = primaryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (currentUser != null && currentUser.uid == userId) {
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = {
                                navController.navigate("chatList")
                            },
                            modifier = Modifier
                                .width(250.dp)
                                .height(50.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = primaryColor
                            ),
                            border = BorderStroke(1.dp, primaryColor),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = "Messaggi",
                                    tint = primaryColor,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    "Messaggi",
                                    color = primaryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Altre informazioni dell'utente
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = cardBackground
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            // Email
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = user?.email ?: "",
                                    fontSize = 16.sp,
                                    color = textColor
                                )
                            }

                            // Telefono (se disponibile)
                            user?.phone?.let { phone ->
                                if (phone.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = "Phone",
                                            tint = primaryColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = phone,
                                            fontSize = 16.sp,
                                            color = textColor
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Book,
                                    contentDescription = "Libri",
                                    tint = primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${favorites.size}",
                                    fontSize = 16.sp,
                                    color = textColor
                                )
                            }

                        }
                    }

                    Log.d("Libri preferiti:", "${favorites.size}")

                } else {
                    Text(
                        text = "No user available",
                        fontSize = 18.sp,
                        color = errorColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 32.dp)
                    )
                }

            }
        }
    }

}