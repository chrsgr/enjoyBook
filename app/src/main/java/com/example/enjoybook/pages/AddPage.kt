package com.example.enjoybook.pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.enjoybook.data.Book
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.firestore.FieldValue

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddPage(
    navController: NavController,
    context: Context,
    isEditing: Boolean = false,
    bookId: String? = null,
    initialTitle: String = "",
    initialAuthor: String = "",
    initialYear: String = "",
    initialDescription: String = "",
    initialCategory: String = "",
    onNextField: (() -> Unit)? = null
) {
    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)

    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }

    val localContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val title = remember { mutableStateOf(initialTitle) }
    val author = remember { mutableStateOf(initialAuthor) }
    val description = remember { mutableStateOf(initialDescription) }
    val edition = remember { mutableStateOf("") }
    val year = remember { mutableStateOf(initialYear) }
    val frontCoverUri = remember { mutableStateOf<Uri?>(null) }
    val backCoverUri = remember { mutableStateOf<Uri?>(null) }

    var showFrontCameraView by remember { mutableStateOf(false) }
    var showBackCameraView by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                localContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(
                localContext,
                "Camera permission is required to take photos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        username = document.getString("username") ?: ""
                        email = document.getString("email") ?: ""
                        userId = currentUser?.uid.toString()
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error: ${e.message}"
                }
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(localContext) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    val frontCoverGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        frontCoverUri.value = uri
    }

    val backCoverGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        backCoverUri.value = uri
    }

    val bookTypes = listOf(
        "Adventure", "Classics", "Crime", "Folk", "Fantasy", "Historical",
        "Horror", "Literary fiction", "Mystery", "Poetry", "Plays",
        "Romance", "Science fiction", "Short stories", "Thrillers",
        "War", "Women's fiction", "Young adult"
    )

    val bookConditions = listOf(
        "New", "Fine", "As New", "Very good", "Good", "Fair", "Poor"
    )

    var expandedType by remember { mutableStateOf(false) }
    var expandedCondition by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var selectedType by remember {
        mutableStateOf(
            if (initialCategory.isNotEmpty() && bookTypes.contains(initialCategory)) {
                initialCategory
            } else {
                bookTypes.first()
            }
        )
    }

    var selectedCondition by remember {
        mutableStateOf(
            if (initialCategory.isNotEmpty() && bookConditions.contains(initialCategory)) {
                initialCategory
            } else {
                bookConditions.first()
            }
        )
    }

    var isLoading by remember { mutableStateOf(isEditing) }

    LaunchedEffect(bookId) {
        if (isEditing && bookId != null) {
            db.collection("books").document(bookId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val book = document.toObject(Book::class.java)
                        book?.let {
                            title.value = it.title
                            author.value = it.author
                            if (bookConditions.contains(it.condition)) {
                                selectedCondition = it.condition
                            }
                            description.value = it.description
                            edition.value = it.edition
                            year.value = it.year
                            if (bookTypes.contains(it.type)) {
                                selectedType = it.type
                            }

                            it.frontCoverUrl?.let { url ->
                            }
                            it.backCoverUrl?.let { url ->
                            }
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e("AddPage", "Error loading book data", e)
                    Toast.makeText(localContext, "Error loading book data", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "EDIT BOOK" else "ADD BOOK",
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor
                ),
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0),
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            if (showFrontCameraView && hasCameraPermission) {
                CameraView(
                    onPhotoCaptured = { uri ->
                        frontCoverUri.value = uri
                        showFrontCameraView = false
                    },
                    onClose = { showFrontCameraView = false },
                    cameraProviderFuture = cameraProviderFuture,
                    imageCapture = imageCapture,
                    executor = executor,
                    localContext = localContext,
                    lifecycleOwner = lifecycleOwner,
                    primaryColor = primaryColor
                )
            }
            else if (showBackCameraView && hasCameraPermission) {
                CameraView(
                    onPhotoCaptured = { uri ->
                        backCoverUri.value = uri
                        showBackCameraView = false
                    },
                    onClose = { showBackCameraView = false },
                    cameraProviderFuture = cameraProviderFuture,
                    imageCapture = imageCapture,
                    executor = executor,
                    localContext = localContext,
                    lifecycleOwner = lifecycleOwner,
                    primaryColor = primaryColor
                )
            }
            else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        strokeWidth = 3.dp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Scan Book Button
                    Button(
                        onClick = {
                            navController.navigate("book_scan_screen")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scan Book",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SCAN IBSN CODE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = primaryColor.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .shadow(4.dp, RoundedCornerShape(8.dp), clip = false),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                frontCoverUri.value != null -> {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = frontCoverUri.value,
                                            contentDescription = "Book Front Cover",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .clickable {
                                                    frontCoverUri.value = null
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Change Image",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Front Cover",
                                            color = textColor,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (hasCameraPermission) {
                                                        showFrontCameraView = true
                                                    } else {
                                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryColor.copy(alpha = 0.2f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = "Take Photo",
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { frontCoverGalleryLauncher.launch("image/*") },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryColor.copy(alpha = 0.2f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoLibrary,
                                                    contentDescription = "Choose from Gallery",
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(
                                    width = 1.dp,
                                    color = primaryColor.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .shadow(4.dp, RoundedCornerShape(8.dp), clip = false),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                backCoverUri.value != null -> {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = backCoverUri.value,
                                            contentDescription = "Book Back Cover",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .clickable {
                                                    backCoverUri.value = null
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Change Image",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Back Cover",
                                            color = textColor,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (hasCameraPermission) {
                                                        showBackCameraView = true
                                                    } else {
                                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryColor.copy(alpha = 0.2f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CameraAlt,
                                                    contentDescription = "Take Photo",
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { backCoverGalleryLauncher.launch("image/*") },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(primaryColor.copy(alpha = 0.2f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoLibrary,
                                                    contentDescription = "Choose from Gallery",
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title field
                    OutlinedTextField(
                        value = title.value,
                        onValueChange = { newValue ->
                            title.value = newValue.capitalize()
                        },
                        label = { Text("Book Title") },
                        placeholder = { Text("Enter the title of the book") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Title,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            cursorColor = primaryColor
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                keyboardController?.hide()
                                focusManager.moveFocus(FocusDirection.Next)
                                onNextField?.invoke()
                            }
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Author field
                    OutlinedTextField(
                        value = author.value,
                        onValueChange = { author.value = it },
                        label = { Text("Book Author") },
                        placeholder = { Text("Enter the author's name") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            cursorColor = primaryColor
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                keyboardController?.hide()
                                focusManager.moveFocus(FocusDirection.Next)
                                onNextField?.invoke()
                            }
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedCondition,
                        onExpandedChange = { expandedCondition = !expandedCondition },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCondition,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Book Condition") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedCondition) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                                focusedLabelColor = primaryColor,
                                cursorColor = primaryColor,
                                focusedContainerColor = backgroundColor,
                                unfocusedContainerColor = backgroundColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expandedCondition,
                            onDismissRequest = { expandedCondition = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                        ) {
                            bookConditions.forEach { condition ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = condition,
                                            color = textColor,
                                            fontWeight = if (condition == selectedCondition) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedCondition = condition
                                        expandedCondition = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = textColor
                                    ),
                                    leadingIcon = {
                                        if (condition == selectedCondition) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = primaryColor
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (condition == selectedCondition) primaryColor.copy(alpha = 0.1f) else Color.Transparent
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description field
                    OutlinedTextField(
                        value = description.value,
                        onValueChange = { description.value = it },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("Enter a description") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                        singleLine = false,
                        maxLines = 3,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            cursorColor = primaryColor
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                keyboardController?.hide()
                                focusManager.moveFocus(FocusDirection.Next)
                                onNextField?.invoke()
                            }
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Edition field
                    OutlinedTextField(
                        value = edition.value,
                        onValueChange = { edition.value = it },
                        label = { Text("Edition (optional)") },
                        placeholder = { Text("Enter the edition") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            cursorColor = primaryColor
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                keyboardController?.hide()
                                focusManager.moveFocus(FocusDirection.Next)
                                onNextField?.invoke()
                            }
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Year field
                    OutlinedTextField(
                        value = year.value,
                        onValueChange = { newValue ->
                            // Accetta solo numeri e limita a 4 caratteri
                            if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                                year.value = newValue

                                val anno = newValue.toIntOrNull()
                                errorMessage = when {
                                    anno == null -> ""
                                    anno < 100 || anno > 2024 -> "Year is not valid"
                                    else -> ""
                                }
                            }
                        },
                        label = { Text("Year") },
                        placeholder = { Text("Enter the year") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            cursorColor = primaryColor
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                keyboardController?.hide()
                                focusManager.moveFocus(FocusDirection.Next)
                                onNextField?.invoke()
                            } // Chiude la tastiera
                        ),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedType,
                        onExpandedChange = { expandedType = !expandedType },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Book Type") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (expandedType) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                                focusedLabelColor = primaryColor,
                                cursorColor = primaryColor,
                                focusedContainerColor = backgroundColor,
                                unfocusedContainerColor = backgroundColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(backgroundColor)
                        ) {
                            bookTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = type,
                                            color = textColor,
                                            fontWeight = if (type == selectedType) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        selectedType = type
                                        expandedType = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = textColor
                                    ),
                                    leadingIcon = {
                                        if (type == selectedType) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = primaryColor
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (type == selectedType) primaryColor.copy(alpha = 0.1f) else Color.Transparent
                                        )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    // Save Button
                    Button(
                        onClick = {
                            if (title.value.isEmpty()) {
                                Toast.makeText(localContext, "Please enter title", Toast.LENGTH_SHORT).show()
                            } else if (author.value.isEmpty()) {
                                Toast.makeText(localContext, "Please enter author", Toast.LENGTH_SHORT).show()
                            } else if (selectedType.isEmpty()) {
                                Toast.makeText(localContext, "Please enter type", Toast.LENGTH_SHORT).show()
                            } else if (selectedCondition.isEmpty()) {
                                Toast.makeText(localContext, "Please enter condition", Toast.LENGTH_SHORT).show()
                            } else if (year.value.isEmpty()) {
                                Toast.makeText(localContext, "Please enter year", Toast.LENGTH_SHORT).show()
                            } else {
                                if (isEditing && bookId != null) {
                                    updateBookWithImages(
                                        bookId, title.value, author.value, selectedType,
                                        selectedCondition, description.value, edition.value,
                                        year.value, frontCoverUri.value, backCoverUri.value,
                                        localContext, navController,
                                        onLoadingChanged = { loading ->
                                            isLoading = loading
                                        }
                                    )
                                } else {
                                    addDataToFirebaseWithImages(
                                        title.value, author.value, selectedType,
                                        selectedCondition, description.value, edition.value,
                                        year.value, userId, username, frontCoverUri.value, backCoverUri.value,
                                        localContext, navController,
                                        onLoadingChanged = { loading ->
                                            isLoading = loading
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Text(
                            text = if (isEditing) "SAVE CHANGES" else "ADD BOOK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }
    }
}

// Camera view composable that handles the camera UI and photo capture
@Composable
fun CameraView(
    onPhotoCaptured: (Uri) -> Unit,
    onClose: () -> Unit,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    imageCapture: ImageCapture,
    executor: Executor,
    localContext: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    primaryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                try {
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("CameraPreview", "Binding failed", e)
                            }
                        } catch (e: Exception) {
                            Log.e("CameraPreview", "Camera provider future failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Failed to add listener", e)
                }

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Camera controls overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Camera",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Capture button
            IconButton(
                onClick = {
                    takePhoto(
                        context = localContext,
                        imageCapture = imageCapture,
                        executor = executor,
                        onPhotoTaken = onPhotoCaptured
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = "Take Photo",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

// Function to capture a photo and return the URI
private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    executor: Executor,
    onPhotoTaken: (Uri) -> Unit
) {
    val photoFile = File(
        context.cacheDir,
        SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.ITALY)
            .format(System.currentTimeMillis()) + ".jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onPhotoTaken(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraCapture", "Error taking photo", exception)
                Toast.makeText(
                    context,
                    "Error taking photo: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    )
}

fun updateBookWithImages(
    bookId: String,
    title: String,
    author: String,
    type: String,
    condition: String,
    description: String,
    edition: String,
    year: String,
    frontCoverUri: Uri?,
    backCoverUri: Uri?,
    context: Context,
    navController: NavController,
    onLoadingChanged: (Boolean) -> Unit
) {
    onLoadingChanged(true)

    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val docRef = db.collection("books").document(bookId)
    val titleLower = title.lowercase()

    docRef.get().addOnSuccessListener { document ->
        if (document != null && document.exists()) {
            val updates = hashMapOf<String, Any>(
                "title" to title,
                "titleLower" to titleLower,
                "author" to author,
                "type" to type,
                "condition" to condition,
                "description" to description,
                "edition" to edition,
                "year" to year,
                "lastUpdated" to FieldValue.serverTimestamp(),
                "isAvailable" to true
            )

            val uploadTasks = mutableListOf<Task<Uri>>()

            frontCoverUri?.let { uri ->
                val frontCoverRef = storage.reference.child("book_covers/${bookId}_front")

                val uploadTask = frontCoverRef.putFile(uri).continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    frontCoverRef.downloadUrl
                }
                uploadTasks.add(uploadTask)

                uploadTask.addOnSuccessListener { downloadUri ->
                    updates["frontCoverUrl"] = downloadUri.toString()
                    if (backCoverUri == null || uploadTasks.size == 2) {
                        updateDocumentWithChanges(docRef, updates, context, navController, onLoadingChanged)
                    }
                }.addOnFailureListener {
                    onLoadingChanged(false)
                }
            }

            backCoverUri?.let { uri ->
                val backCoverRef = storage.reference.child("book_covers/${bookId}_back")
                val uploadTask = backCoverRef.putFile(uri).continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    backCoverRef.downloadUrl
                }
                uploadTasks.add(uploadTask)

                uploadTask.addOnSuccessListener { downloadUri ->
                    updates["backCoverUrl"] = downloadUri.toString()
                    if (frontCoverUri == null || uploadTasks.size == 2) {
                        updateDocumentWithChanges(docRef, updates, context, navController, onLoadingChanged)
                    }
                }.addOnFailureListener {
                    onLoadingChanged(false)
                }
            }

            if (uploadTasks.isEmpty()) {
                updateDocumentWithChanges(docRef, updates, context, navController, onLoadingChanged)
            }
        } else {
            onLoadingChanged(false)
            Toast.makeText(context, "Book not found", Toast.LENGTH_SHORT).show()
        }
    }.addOnFailureListener { e ->
        onLoadingChanged(false)
        Toast.makeText(context, "Error getting book data: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun updateDocumentWithChanges(
    docRef: DocumentReference,
    updates: HashMap<String, Any>,
    context: Context,
    navController: NavController,
    onLoadingChanged: (Boolean) -> Unit
) {
    docRef.update(updates)
        .addOnSuccessListener {
            onLoadingChanged(false)
            Toast.makeText(context, "Book updated successfully", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
        .addOnFailureListener { e ->
            onLoadingChanged(false)
            Toast.makeText(context, "Failed to update book: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun addDataToFirebaseWithImages(
    title: String,
    author: String,
    type: String,
    condition: String,
    description: String,
    edition: String,
    year: String,
    userId: String,
    userUsername: String,
    frontCoverUri: Uri?,
    backCoverUri: Uri?,
    context: Context,
    navController: NavController,
    onLoadingChanged: (Boolean) -> Unit
) {

    onLoadingChanged(true)

    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val dbCourses: CollectionReference = db.collection("books")
    val currentUser = FirebaseAuth.getInstance().currentUser
    val titleLower = title.lowercase()

    val docRef = dbCourses.document()
    val documentId = docRef.id


    val bookData = HashMap<String, Any>().apply {
        put("id", documentId)
        put("author", author)
        put("condition", condition)
        put("description", description)
        put("edition", edition)
        put("isAvailable", true)
        put("title", title)
        put("titleLower", titleLower)
        put("type", type)
        put("timestamp", FieldValue.serverTimestamp())
        put("userUsername", userUsername)
        put("userEmail", currentUser?.email.toString())
        put("userId", userId)
        put("year", year)
    }

    val uploadTasks = mutableListOf<Task<Uri>>()

    frontCoverUri?.let { uri ->
        val frontCoverRef = storage.reference.child("book_covers/${documentId}_front")
        val uploadTask = frontCoverRef.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            frontCoverRef.downloadUrl
        }
        uploadTasks.add(uploadTask)

        uploadTask.addOnSuccessListener { downloadUri ->
            bookData["frontCoverUrl"] = downloadUri.toString()
            if (backCoverUri == null || (backCoverUri != null && bookData.containsKey("backCoverUrl"))) {
                saveBookToFirestore(docRef, bookData, context, navController, onLoadingChanged)
            }
        }.addOnFailureListener { e ->
            onLoadingChanged(false)
            Toast.makeText(context, "Failed to upload front cover: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    backCoverUri?.let { uri ->
        val backCoverRef = storage.reference.child("book_covers/${documentId}_back")
        val uploadTask = backCoverRef.putFile(uri).continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            backCoverRef.downloadUrl
        }
        uploadTasks.add(uploadTask)

        uploadTask.addOnSuccessListener { downloadUri ->
            bookData["backCoverUrl"] = downloadUri.toString()
            if (frontCoverUri == null || (frontCoverUri != null && bookData.containsKey("frontCoverUrl"))) {
                saveBookToFirestore(docRef, bookData, context, navController, onLoadingChanged)
            }
        }.addOnFailureListener { e ->
            onLoadingChanged(false)
            Toast.makeText(context, "Failed to upload back cover: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    if (uploadTasks.isEmpty()) {
        saveBookToFirestore(docRef, bookData, context, navController, onLoadingChanged)
    }
}

private fun saveBookToFirestore(
    docRef: DocumentReference,
    bookData: HashMap<String, Any>,
    context: Context,
    navController: NavController,
    onLoadingChanged: (Boolean) -> Unit
) {
    docRef.set(bookData)
        .addOnSuccessListener {
            onLoadingChanged(false)
            Toast.makeText(
                context,
                "Your book has been added in the library!",
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }
        .addOnFailureListener { e ->
            onLoadingChanged(false)
            Toast.makeText(context, "Failed to add book: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

private fun String.capitalize(): String {
    return if (isNotEmpty())
        this[0].uppercase() + substring(1).lowercase()
    else
        this
}