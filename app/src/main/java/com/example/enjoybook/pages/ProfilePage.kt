package com.example.enjoybook.pages
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.enjoybook.theme.primaryColor
import com.example.enjoybook.viewModel.AuthViewModel
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.BuildConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    navController: NavController,
    viewModel: AuthViewModel
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var isPrivate by remember {mutableStateOf(false)}
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var apiImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    val isGoogleAccount = remember { mutableStateOf(false) }

    val context = LocalContext.current

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var isCurrentUserProfile by remember { mutableStateOf(true) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val secondaryColor = Color(0xFF1A8A8F)
    val backgroundColor = (primaryColor.copy(alpha = 0.1f))
    val primaryColor = Color(0xFFA7E8EB)
    val accentColor = Color(0xFF4DB6AC)
    val primaryTextColor = Color(0xFF212121)
    val secondaryTextColor = Color(0xFF757575)
    val cardBackgroundColor = Color.White
    val buttonTextColor = Color(0xFF212121)
    val warningColor = Color(0xFFFF6D00)


    LaunchedEffect(Unit) {
        if (currentUser != null) {
            firestore.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        name = document.getString("name") ?: ""
                        surname = document.getString("surname") ?: ""
                        username = document.getString("username") ?: ""
                        email = document.getString("email") ?: ""
                        phone = document.getString("phone") ?: ""
                        password = document.getString("password") ?: ""
                        bio = document.getString("bio") ?: ""
                        isPrivate = document.getBoolean("isPrivate") ?: false

                        document.getString("profilePictureUrl")?.let {
                            imageUri = Uri.parse(it)
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error loading profile: ${e.message}"
                    showErrorDialog = true
                    isLoading = false
                }
        } else {
            navController.navigate("login") {
                popUpTo("main") { inclusive = true }
            }
        }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    isGoogleAccount.value = document.getBoolean("isGoogleAuth") ?: false
                }
        }
    }


    fun fetchImagesFromApi() {
        isLoading = true

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val baseUrl = "${ApiConfig.getBaseUrl()}/api/images"
                val url = URL(baseUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val images = json.getJSONArray("images")

                    val fetchedImages = mutableListOf<String>()
                    for (i in 0 until images.length()) {
                        val fileName = images.getString(i)
                        fetchedImages.add("${ApiConfig.getBaseUrl()}/api/images/$fileName")
                    }

                    withContext(Dispatchers.Main) {
                        apiImages = fetchedImages
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Server error $responseCode"
                        showErrorDialog = true
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Failed to load avatars: ${e.message}"
                    showErrorDialog = true
                    isLoading = false
                }
            }
        }
    }

    fun isPasswordValid(password: String): Pair<Boolean, String> {
        val minLength = 8
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        return when {
            password.length < minLength ->
                Pair(false, "The password must contain at least $minLength characters")
            !hasUpperCase ->
                Pair(false, "The password must contain at least one uppercase letter")
            !hasDigit ->
                Pair(false, "The password must contain at least one number")
            !hasSpecialChar ->
                Pair(false, "The password must contain at least one special character (@, #, $, etc...)")
            else ->
                Pair(true, "Password valid")
        }
    }

    fun saveUserData() {
        if (currentUser == null) {
            errorMessage = "You must be logged in to save profile"
            showErrorDialog = true
            return
        }

        if (name.isBlank() || surname.isBlank() || username.isBlank()) {
            errorMessage = "Name, surname and username cannot be empty"
            showErrorDialog = true
            return
        }

        val isPasswordChangeRequested = newPassword.isNotBlank() || confirmNewPassword.isNotBlank()

        if (isPasswordChangeRequested) {
            if (currentPassword.isBlank()) {
                errorMessage = "Current password is required to change password"
                showErrorDialog = true
                return
            }

            if (newPassword != confirmNewPassword) {
                errorMessage = "New passwords do not match"
                showErrorDialog = true
                return
            }

            val (isValid, validationMessage) = isPasswordValid(newPassword)
            if (!isValid) {
                errorMessage = validationMessage
                showErrorDialog = true
                return
            }
        }

        isSaving = true

        firestore.collection("users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val otherUsersWithUsername = querySnapshot.documents
                    .filter { it.id != currentUser.uid }

                if (otherUsersWithUsername.isNotEmpty()) {
                    isSaving = false
                    errorMessage = "Username is already taken. Please choose a different username."
                    showErrorDialog = true
                } else {
                    val userData = hashMapOf(
                        "name" to name,
                        "surname" to surname,
                        "username" to username,
                        "email" to email,
                        "phone" to phone,
                        "bio" to bio,
                        "lastUpdated" to FieldValue.serverTimestamp(),
                        "isPrivate" to isPrivate
                    )

                    imageUri?.let {
                        userData["profilePictureUrl"] = it.toString()
                    }

                    val updateFirestore = {
                        firestore.collection("users").document(currentUser.uid)
                            .update(userData as Map<String, Any>)
                            .addOnSuccessListener {
                                isSaving = false
                                isEditing = false
                                Toast.makeText(
                                    context,
                                    "Profile updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                isSaving = false
                                errorMessage = "Failed to update profile: ${e.message}"
                                showErrorDialog = true
                                Toast.makeText(
                                    context,
                                    "Failed to update profile: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }

                    firestore.collection("users").document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { document ->
                            val isGoogleAuth = document.getBoolean("isGoogleAuth") ?: false

                            if (isPasswordChangeRequested) {
                                if (isGoogleAuth) {
                                    isSaving = false
                                    errorMessage =
                                        "Google accounts cannot change their password through the app. Please manage your Google account settings instead."
                                    showErrorDialog = true
                                } else {
                                    val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)

                                    currentUser.reauthenticate(credential)
                                        .addOnSuccessListener {
                                            // Dopo la riautenticazione, aggiorna la password
                                            currentUser.updatePassword(newPassword)
                                                .addOnSuccessListener {
                                                    currentPassword = ""
                                                    newPassword = ""
                                                    confirmNewPassword = ""

                                                    updateFirestore()

                                                    Toast.makeText(
                                                        context,
                                                        "Password updated successfully",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    isSaving = false
                                                    errorMessage = "Failed to update password: ${e.message}"
                                                    showErrorDialog = true
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            isSaving = false
                                            errorMessage = "Authentication failed: Current password is incorrect"
                                            showErrorDialog = true
                                        }
                                }
                            } else {
                                updateFirestore()
                            }
                        }
                        .addOnFailureListener { e ->
                            isSaving = false
                            errorMessage = "Failed to retrieve account information: ${e.message}"
                            showErrorDialog = true
                        }
                }
            }
            .addOnFailureListener { e ->
                isSaving = false
                errorMessage = "Failed to check username uniqueness: ${e.message}"
                showErrorDialog = true
            }
    }


    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val animationSpec: FiniteAnimationSpec<IntSize> = tween(durationMillis = 300)
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                color = backgroundColor,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = secondaryColor
                        )
                    }

                    Text(
                        text = if (isEditing) "EDIT PROFILE" else "PROFILE",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = buttonTextColor
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = accentColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading profile...",
                            color = primaryTextColor
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    color = accentColor,
                                    shape = CircleShape
                                )
                                .shadow(
                                    elevation = 4.dp,
                                    shape = CircleShape
                                )
                                .background(if (imageUri == null) primaryColor else Color.Transparent)
                                .clickable(enabled = isEditing) {
                                    fetchImagesFromApi()
                                    showBottomSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            imageUri?.let {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        ImageRequest.Builder(context)
                                            .data(it)
                                            .crossfade(true)
                                            .build()
                                    ),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (isEditing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(0.6f)
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Change Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(

                        modifier = Modifier
                            .fillMaxWidth()

                            .animateContentSize(animationSpec),

                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isEditing) {
                                // Edit mode

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ){
                                    Checkbox(
                                        checked = isPrivate,
                                        onCheckedChange = { isPrivate = it }
                                    )
                                    Text("Private profile"
                                    )
                                }

                                ProfileField(
                                    label = "Bio",
                                    value = bio,
                                    onValueChange = { bio = it },
                                    isEditing = true,
                                    leadingIcon = Icons.Default.Info,
                                    isBio = true
                                )

                                ProfileField(
                                    label = "Name",
                                    value = name,
                                    onValueChange = { name = it },
                                    isEditing = true,
                                    leadingIcon = Icons.Default.Person
                                )

                                ProfileField(
                                    label = "Surname",
                                    value = surname,
                                    onValueChange = { surname = it },
                                    isEditing = true,
                                    leadingIcon = Icons.Default.Person
                                )

                                ProfileField(
                                    label = "Username",
                                    value = username,
                                    onValueChange = { username = it },
                                    isEditing = true,
                                    leadingIcon = Icons.Default.AccountCircle
                                )

                                ProfileField(
                                    label = "Email",
                                    value = email,
                                    onValueChange = { email = it },
                                    isEditing = false,
                                    leadingIcon = Icons.Default.Email,
                                    isEnabled = !isGoogleAccount.value,
                                )

                                ProfileField(
                                    label = "Phone",
                                    value = phone,
                                    onValueChange = { phone = it },
                                    isEditing = true,
                                    leadingIcon = Icons.Default.Phone,
                                    keyboardType = KeyboardType.Phone
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(top = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = { showInfoDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Password requirements",
                                            tint = Color(0xFF4DB6AC)
                                        )
                                    }
                                    Text(
                                        text = " Reset password",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                }

                                ProfileField(
                                    label = "Current password",
                                    value = currentPassword,
                                    onValueChange = { currentPassword = it },
                                    isEditing = isEditing,
                                    leadingIcon = Icons.Default.Password,
                                    isPassword = true,
                                    passwordVisible = currentPasswordVisible,
                                    onTogglePasswordVisibility = { currentPasswordVisible = !currentPasswordVisible },
                                    isEnabled = !isGoogleAccount.value,
                                    disabledMessage = if (isGoogleAccount.value)
                                        "Password management is handled through your Google account"
                                    else null
                                )

                                ProfileField(
                                    label = "New password",
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    isEditing = isEditing,
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true,
                                    passwordVisible = newPasswordVisible,
                                    onTogglePasswordVisibility = { newPasswordVisible = !newPasswordVisible },
                                    isEnabled = !isGoogleAccount.value,
                                    disabledMessage = if (isGoogleAccount.value)
                                        "Password management is handled through your Google account"
                                    else null
                                )

                                ProfileField(
                                    label = "Confirm new password",
                                    value = confirmNewPassword,
                                    onValueChange = { confirmNewPassword = it },
                                    isEditing = isEditing,
                                    leadingIcon = Icons.Default.Lock,
                                    isPassword = true,
                                    passwordVisible = confirmPasswordVisible,
                                    onTogglePasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                                    isEnabled = !isGoogleAccount.value,
                                    disabledMessage = if (isGoogleAccount.value)
                                        "Password management is handled through your Google account"
                                    else null
                                )

                            } else {
                                // View mode
                                ProfileField(
                                    label = "Bio",
                                    value = bio,
                                    onValueChange = { bio = it },
                                    isEditing = false,
                                    leadingIcon = Icons.Default.Info,
                                    isBio = true
                                )

                                ProfileField(
                                    label = "Name",
                                    value = name,
                                    isEditing = false,
                                    leadingIcon = Icons.Default.Person
                                )

                                ProfileField(
                                    label = "Surname",
                                    value = surname,
                                    isEditing = false,
                                    leadingIcon = Icons.Default.Person
                                )

                                ProfileField(
                                    label = "Username",
                                    value = username,
                                    isEditing = false,
                                    leadingIcon = Icons.Default.AccountCircle
                                )

                                ProfileField(
                                    label = "Email",
                                    value = email,
                                    isEditing = false,
                                    leadingIcon = Icons.Default.Email
                                )

                                ProfileField(
                                    label = "Phone",
                                    value = phone,
                                    isEditing = false,
                                    leadingIcon = Icons.Default.Phone
                                )


                                var stringPrivate = if(isPrivate) "Private" else "Public"

                                ProfileField(
                                    label = "Status profile",
                                    value = stringPrivate,
                                    onValueChange = { stringPrivate = it },
                                    isEditing = false,
                                    leadingIcon = Icons.Default.Lock
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandHorizontally()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isEditing && isCurrentUserProfile) {
                                        Button(
                                            onClick = { saveUserData() },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                                .height(50.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                            shape = RoundedCornerShape(25.dp),
                                            enabled = !isSaving,
                                            elevation = ButtonDefaults.buttonElevation(
                                                defaultElevation = 4.dp
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                if (isSaving) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        color = Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Done,
                                                        contentDescription = "Save",
                                                        tint = Color.White,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                    Text(
                                                        "Save",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = { isEditing = false },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 8.dp)
                                                .height(50.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                                            shape = RoundedCornerShape(25.dp),
                                            enabled = !isSaving,
                                            elevation = ButtonDefaults.buttonElevation(
                                                defaultElevation = 4.dp
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Cancel",
                                                    tint = Color.DarkGray,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Text(
                                                    "Cancel",
                                                    color = Color.DarkGray,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = { isEditing = true },
                                            modifier = Modifier
                                                .fillMaxWidth(0.7f)
                                                .height(50.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                            shape = RoundedCornerShape(25.dp),
                                            elevation = ButtonDefaults.buttonElevation(
                                                defaultElevation = 4.dp
                                            )
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = Color.White,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                Text(
                                                    "Edit Profile",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showInfoDialog) {
                        AlertDialog(
                            onDismissRequest = { showInfoDialog = false },
                            title = { Text("Password requirements") },
                            text = {
                                Column {
                                    Text("Your password must contains::")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    BulletPoint("At least 8 characters")
                                    BulletPoint("At least one uppercase letter")
                                    BulletPoint("At least one digit")
                                    BulletPoint("At least one special character (@, #, $, etc.)")
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showInfoDialog = false }) {
                                    Text("I understand")
                                }
                            }
                        )
                    }

                    if (isCurrentUserProfile && !isEditing) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Delete Account button
                        Button(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(25.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Account",
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    "Delete Account",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (showDeleteConfirmation) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirmation = false },
                                title = { Text("Confirm Deletion") },
                                text = { Text("Proceeding will permanently delete your account and all your activity.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            // Call a function to delete the account from the database
                                            deleteUserAccount( navController, viewModel )
                                            showDeleteConfirmation = false
                                        }
                                    ) {
                                        Text("Yes", color = Color.Red)
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showDeleteConfirmation = false }
                                    ) {
                                        Text("No")
                                    }
                                }
                            )
                        }



                        Spacer(modifier = Modifier.height(16.dp))




                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec),
                            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Safety",
                                        tint = warningColor,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                    Text(
                                        "Account Safety",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = primaryTextColor
                                    )
                                }

                                Text(
                                    "If you find inappropriate content or want to report a user, visit their profile and use the Report Account button.",
                                    fontSize = 14.sp,
                                    color = secondaryTextColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )


                            }
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = cardBackgroundColor,
            dragHandle = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.LightGray, RoundedCornerShape(2.dp))
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Choose Your Avatar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = primaryTextColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Select one of our styled avatars",
                    color = secondaryTextColor,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading avatars...",
                                color = secondaryTextColor
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Avatar Collection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.height(350.dp)
                        ) {
                            items(apiImages.size) { index ->
                                val imageUrl = apiImages[index]

                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()

                                val borderColor by animateColorAsState(
                                    targetValue = if (isPressed) accentColor else Color.LightGray,
                                    label = "borderColor"
                                )

                                val scale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.92f else 1f,
                                    label = "scale",
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                                )

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.dp,
                                            color = borderColor,
                                            shape = CircleShape
                                        )
                                        .shadow(elevation = 4.dp, shape = CircleShape)
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = LocalIndication.current,
                                            onClick = {
                                                imageUri = Uri.parse(imageUrl)
                                                showBottomSheet = false
                                            }
                                        )
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            ImageRequest.Builder(context)
                                                .data(imageUrl)
                                                .crossfade(true)
                                                .build()
                                        ),
                                        contentDescription = "Avatar Option",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    if (isPressed) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Select Avatar",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedButton(
                            onClick = { showBottomSheet = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            border = BorderStroke(1.dp, accentColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = accentColor
                            )
                        ) {
                            Text(
                                "Cancel",
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    isEditing: Boolean,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = label.equals("Password", ignoreCase = true),
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
    isEnabled: Boolean = true,
    disabledMessage: String? = null,
    isBio: Boolean = false
) {
    val accentColor = Color(0xFF4DB6AC)
    var isPasswordVisible by remember { mutableStateOf(passwordVisible) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (isEditing) {

            if (isBio) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .animateContentSize(),
                    singleLine = false,
                    maxLines = 4,
                    enabled = isEditing && isEnabled,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    leadingIcon = if (leadingIcon != null) {
                        {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = if (isEnabled) accentColor else accentColor.copy(alpha = 0.5f)
                            )
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        focusedLabelColor = accentColor,
                        cursorColor = accentColor,
                        disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                        disabledLabelColor = Color.Gray.copy(alpha = 0.5f),
                        disabledTextColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Write something about yourself...") }
                )
            } else {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text(label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    singleLine = true,
                    enabled = isEditing && isEnabled,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType
                    ),
                    visualTransformation = if (isPassword && !isPasswordVisible)
                        PasswordVisualTransformation() else VisualTransformation.None,
                    leadingIcon = if (leadingIcon != null) {
                        {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = if (isEnabled) accentColor else accentColor.copy(alpha = 0.5f)
                            )
                        }
                    } else null,
                    trailingIcon = if (isPassword && isEnabled) {
                        {
                            IconButton(onClick = {
                                isPasswordVisible = !isPasswordVisible
                                onTogglePasswordVisibility()
                            }) {
                                Icon(
                                    imageVector = if (isPasswordVisible)
                                        Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (isPasswordVisible)
                                        "Hide password" else "Show password",
                                    tint = accentColor
                                )
                            }
                        }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        focusedLabelColor = accentColor,
                        cursorColor = accentColor,
                        disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                        disabledLabelColor = Color.Gray.copy(alpha = 0.5f),
                        disabledTextColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            if (!isEnabled && disabledMessage != null) {
                Text(
                    text = disabledMessage,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(start = 16.dp, top = 4.dp)
                        .fillMaxWidth()
                )
            }

        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = label,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = accentColor
                    )
                }

                if (isBio) {
                    Text(
                        text = if (value.isEmpty()) "No bio added yet" else value,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (value.isEmpty()) Color.Gray else Color.Black,
                        modifier = Modifier
                            .padding(start = 22.dp, bottom = 12.dp)
                            .fillMaxWidth(),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = if (isPassword) {
                            if (value.isEmpty()) "Not set" else "••••••••"
                        } else {
                            if (value.isEmpty()) "Not set" else value
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (value.isEmpty()) Color.Gray else Color.Black,
                        modifier = Modifier.padding(start = 22.dp, bottom = 12.dp)
                    )

                    if (isPassword && !isEnabled && disabledMessage != null) {
                        Text(
                            text = disabledMessage,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 22.dp, bottom = 8.dp)
                        )
                    }

                    Divider(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}


fun deleteUserAccount(navController: NavController, viewModel: AuthViewModel) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    user?.let { currentUser ->
        val userId = currentUser.uid
        val collectionsToDelete =
            listOf("books", "reviews", "users", "messages", "favorites", "chats")
        val tasks = mutableListOf<Task<Void>>()

        for (collection in collectionsToDelete) {
            val query = db.collection(collection).whereEqualTo("userId", userId)
            query.get().addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    tasks.add(document.reference.delete())
                }

                Tasks.whenAllComplete(tasks).addOnSuccessListener {
                    currentUser.delete().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            println("Success: account deleted.")

                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true // Per evitare duplicati
                            }
                            viewModel.signout()

                        } else {
                            println("Error in the delete action: ${task.exception?.message}")
                        }
                    }
                }.addOnFailureListener { e ->
                    println("Error in the data delete: ${e.message}")
                }
            }.addOnFailureListener { e ->
                println("Errore nel recupero dei documenti: ${e.message}")
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = "• ",
            fontWeight = FontWeight.Bold
        )
        Text(text = text)
    }
}

