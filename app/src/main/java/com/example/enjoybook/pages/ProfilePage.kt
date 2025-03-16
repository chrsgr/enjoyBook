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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.enjoybook.viewModel.AuthViewModel
import com.example.enjoybook.viewModel.SearchViewModel
import com.google.firebase.auth.FirebaseAuth
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
    var isEditing by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var apiImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) } // Start with loading state
    var isSaving by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

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

    val primaryColor = Color(0xFFA7E8EB)
    val accentColor = Color(0xFF4DB6AC)
    val primaryTextColor = Color(0xFF212121)
    val secondaryTextColor = Color(0xFF757575)
    val cardBackgroundColor = Color.White
    val buttonTextColor = Color(0xFF212121)
    val errorColor = Color(0xFFB00020)

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val animationSpec: FiniteAnimationSpec<IntSize> = tween(durationMillis = 300)


    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                color = primaryColor,
                shadowElevation = 4.dp
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
                            tint = buttonTextColor
                        )
                    }

                    Text(
                        text = if (isEditing) "Edit Profile" else "Profile",
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
                                    isEditing = false, // Email is read-only
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
                                    if (isEditing) {
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
    keyboardType: KeyboardType = KeyboardType.Text
) {
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
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                leadingIcon = if (leadingIcon != null) {


                    {
                        val accentColor = Color(0xFF4DB6AC)

                        Icon(
                            imageVector = leadingIcon as ImageVector,
                            contentDescription = null,
                            tint = accentColor
                        )
                    }
                } else null,

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4DB6AC) ,
                    focusedLabelColor = Color(0xFF4DB6AC) ,
                    cursorColor = Color(0xFF4DB6AC)
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
                            tint = Color.Blue,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = label,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF4DB6AC)
                    )
                }

                Text(
                    text = value.ifEmpty { "Not set" },
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

