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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Report
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.enjoybook.theme.primaryColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    navController: NavController,
) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var apiImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var showReportSuccessDialog by remember { mutableStateOf(false) }
    var isReporting by remember { mutableStateOf(false) }
    var reportOptions = listOf("Inappropriate content", "Fake account", "Harassment", "Spam", "Other")
    var selectedReportOption by remember { mutableStateOf(reportOptions[0]) }

    val context = LocalContext.current

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    var originalPassword by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var isCurrentUserProfile by remember { mutableStateOf(true) }

    val secondaryColor = Color(0xFF1A8A8F)
    val backgroundColor = (primaryColor.copy(alpha = 0.1f))


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
    fun fetchImagesFromApi() {
        isLoading = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sampleImages = mutableListOf<String>()

                // API: RoboHash - random avatars with high-quality SVGs
                val avatarTypes =
                    listOf("male", "female", "human", "identicon", "initials", "bottts")
                for (i in 0 until 6) {
                    val seed = "User${i + 100}"
                    val type = avatarTypes[i % avatarTypes.size]
                    sampleImages.add("https://robohash.org/$seed?set=$type&bgset=bg1&size=200x200") // Increased size for better quality
                }

                delay(800)

                withContext(Dispatchers.Main) {
                    apiImages = sampleImages
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "Failed to load avatars: ${e.message}"
                    showErrorDialog = true
                }
            }
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

        isSaving = true

        val userData = hashMapOf(
            "name" to name,
            "surname" to surname,
            "username" to username,
            "email" to email,
            "phone" to phone
        )


        if (password != originalPassword) {
            val user = FirebaseAuth.getInstance().currentUser
            user?.updatePassword(password)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                    } else {
                        errorMessage = "Failed to update authentication: ${task.exception?.message}"
                        showErrorDialog = true
                    }
                }
        }
        imageUri?.let {
            userData["profilePictureUrl"] = it.toString()
        }

        firestore.collection("users").document(currentUser.uid)
            .update(userData as Map<String, Any>)
            .addOnSuccessListener {
                isSaving = false
                isEditing = false

                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                isSaving = false
                errorMessage = "Failed to update profile: ${e.message}"
                showErrorDialog = true
            }
    }

    fun reportUser() {
        if (currentUser == null) {
            errorMessage = "You must be logged in to report accounts"
            showErrorDialog = true
            return
        }

        if (reportReason.isBlank() && selectedReportOption == "Other") {
            errorMessage = "Please provide a reason for the report"
            showErrorDialog = true
            return
        }

        isReporting = true

        // report document
        val reportData = hashMapOf(
            "reportedUserId" to userId,
            "reportedBy" to currentUser.uid,
            "reason" to if (selectedReportOption == "Other") reportReason else selectedReportOption,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("reports").add(reportData)
            .addOnSuccessListener {
                firestore.collection("reports")
                    .whereEqualTo("reportedUserId", userId)
                    .get()
                    .addOnSuccessListener { reports ->
                        if (reports.size() >= 5) {
                            firestore.collection("users").document(userId)
                                .update("isBanned", true)
                                .addOnSuccessListener {
                                    isReporting = false
                                    showReportDialog = false
                                    showReportSuccessDialog = true
                                }
                                .addOnFailureListener { e ->
                                    isReporting = false
                                    errorMessage = "Failed to ban user: ${e.message}"
                                    showErrorDialog = true
                                }
                        } else {
                            isReporting = false
                            showReportDialog = false
                            showReportSuccessDialog = true
                        }
                    }
                    .addOnFailureListener { e ->
                        isReporting = false
                        errorMessage = "Failed to count reports: ${e.message}"
                        showErrorDialog = true
                    }
            }
            .addOnFailureListener { e ->
                isReporting = false
                errorMessage = "Failed to submit report: ${e.message}"
                showErrorDialog = true
            }
    }

    val primaryColor = Color(0xFFA7E8EB)
    val accentColor = Color(0xFF4DB6AC)
    val primaryTextColor = Color(0xFF212121)
    val secondaryTextColor = Color(0xFF757575)
    val cardBackgroundColor = Color.White
    val buttonTextColor = Color(0xFF212121)
    val errorColor = Color(0xFFB00020)
    val warningColor = Color(0xFFFF6D00)

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val animationSpec: FiniteAnimationSpec<IntSize> = tween(durationMillis = 300)


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
                // Main content
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
                                    leadingIcon = Icons.Default.Email
                                )

                                ProfileField(
                                    label = "Phone",
                                    value = phone,
                                    onValueChange = { phone = it },
                                    isEditing = true,
                                    leadingIcon = Icons.Default.Phone,
                                    keyboardType = KeyboardType.Phone
                                )

                                ProfileField(
                                    label = "Password",
                                    value = password,
                                    onValueChange = { password = it },
                                    isEditing = true,
                                    leadingIcon = Icons.Default.Password
                                )

                            } else {
                                // View mode
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

                                ProfileField(
                                    label = "Password",
                                    value = password,
                                    onValueChange = { password = it },
                                    isEditing = false,
                                    leadingIcon = Icons.Default.Password
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
                                    if (isEditing && isCurrentUserProfile ) {
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

                            // Only show report button if this is NOT the current user's profile
                            if (!isCurrentUserProfile) {
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedButton(
                                    onClick = { showReportDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
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
                            }
                        }
                    }

                    if (isCurrentUserProfile && !isEditing) {
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

                                OutlinedButton(
                                    onClick = {
                                        //fare in seguito pagina dove ci sono le linee guida
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = accentColor
                                    ),
                                    border = BorderStroke(1.dp, accentColor),
                                    shape = RoundedCornerShape(25.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Help,
                                            contentDescription = "Help",
                                            tint = accentColor,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            "Community Guidelines",
                                            color = accentColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
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

    // Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Report,
                        contentDescription = "Report",
                        tint = warningColor,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        "Report Account",
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Why are you reporting this user?",
                        fontWeight = FontWeight.Medium,
                        color = primaryTextColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = {  },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            reportOptions.forEach { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable { selectedReportOption = option },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedReportOption == option,
                                        onClick = { selectedReportOption = option },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = accentColor
                                        )
                                    )
                                    Text(
                                        text = option,
                                        modifier = Modifier.padding(start = 8.dp),
                                        color = primaryTextColor
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = selectedReportOption == "Other") {
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            label = { Text("Please specify") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                focusedLabelColor = accentColor,
                                cursorColor = accentColor
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Text(
                        "Users will be automatically banned after receiving 5 reports.",
                        color = secondaryTextColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { reportUser() },
                    enabled = !isReporting,
                    colors = ButtonDefaults.buttonColors(containerColor = warningColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (isReporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit Report", color = Color.White)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReportDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryTextColor),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = cardBackgroundColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Report Success Dialog
    if (showReportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showReportSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = accentColor,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        "Report Submitted",
                        fontWeight = FontWeight.Bold,
                        color = primaryTextColor
                    )
                }
            },
            text = {
                Text(
                    "Thank you for your report. We take all reports seriously and will review this account. Users who receive multiple reports may be banned automatically.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showReportSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = cardBackgroundColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Text(
                    "Error",
                    fontWeight = FontWeight.Bold,
                    color = errorColor
                )
            },
            text = {
                Text(
                    errorMessage,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = cardBackgroundColor,
            shape = RoundedCornerShape(16.dp)
        )
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
    onTogglePasswordVisibility: () -> Unit = {}
) {
    val accentColor = Color(0xFF4DB6AC)
    var isPasswordVisible by remember { mutableStateOf(passwordVisible) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (isEditing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                singleLine = true,
                enabled = isEditing,
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
                            tint = accentColor
                        )
                    }
                } else null,
                trailingIcon = if (isPassword) {
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
                    cursorColor = accentColor
                ),
                shape = RoundedCornerShape(12.dp)
            )
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

                Divider(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}