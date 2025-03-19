package com.example.enjoybook.pages

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.enjoybook.data.Book
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

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
    initialCategory: String = ""
) {
    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)

    val db = FirebaseFirestore.getInstance()

    val title = remember { mutableStateOf(initialTitle) }
    val author = remember { mutableStateOf(initialAuthor) }
    val condition = remember { mutableStateOf("") }
    val description = remember { mutableStateOf(initialDescription) }
    val edition = remember { mutableStateOf("") }
    val year = remember { mutableStateOf(initialYear) }
    val frontCoverUri = remember { mutableStateOf<Uri?>(null) }
    val backCoverUri = remember { mutableStateOf<Uri?>(null) }

    val frontCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        frontCoverUri.value = uri
    }

    val backCoverLauncher = rememberLauncherForActivityResult(
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

    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember {
        mutableStateOf(
            if (initialCategory.isNotEmpty() && bookTypes.contains(initialCategory)) {
                initialCategory
            } else {
                bookTypes.first()
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
                            condition.value = it.condition
                            description.value = it.description
                            edition.value = it.edition
                            year.value = it.year
                            if (bookTypes.contains(it.type)) {
                                selectedType = it.type
                            }

                            it.frontCoverUrl?.let { url ->
                                // Convert URL to URI if needed for display
                            }
                            it.backCoverUrl?.let { url ->
                                // Convert URL to URI if needed for display
                            }
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { e ->
                    Log.e("AddPage", "Error loading book data", e)
                    Toast.makeText(context, "Error loading book data", Toast.LENGTH_SHORT).show()
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
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor)
        ) {
            if (isLoading) {
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
                                text = "SCAN BOOK COVER",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Book Cover Image Upload Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Front Cover Upload Box
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
                                .shadow(4.dp, RoundedCornerShape(8.dp), clip = false)
                                .clickable { frontCoverLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                frontCoverUri.value != null -> {
                                    AsyncImage(
                                        model = frontCoverUri.value,
                                        contentDescription = "Book Front Cover",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddAPhoto,
                                            contentDescription = "Add Front Cover",
                                            tint = primaryColor,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Front Cover",
                                            color = textColor,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        // Back Cover Upload Box
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
                                .shadow(4.dp, RoundedCornerShape(8.dp), clip = false)
                                .clickable { backCoverLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                backCoverUri.value != null -> {
                                    AsyncImage(
                                        model = backCoverUri.value,
                                        contentDescription = "Book Back Cover",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddAPhoto,
                                            contentDescription = "Add Back Cover",
                                            tint = primaryColor,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Back Cover",
                                            color = textColor,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title field
                    OutlinedTextField(
                        value = title.value,
                        onValueChange = { title.value = it },
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
                        )
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
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Condition field
                    OutlinedTextField(
                        value = condition.value,
                        onValueChange = { condition.value = it },
                        label = { Text("Condition") },
                        placeholder = { Text("Enter the condition of the book") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = primaryColor
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                            focusedLabelColor = primaryColor,
                            cursorColor = primaryColor
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description field
                    OutlinedTextField(
                        value = description.value,
                        onValueChange = { description.value = it },
                        label = { Text("Description") },
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
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Edition field
                    OutlinedTextField(
                        value = edition.value,
                        onValueChange = { edition.value = it },
                        label = { Text("Edition") },
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
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Year field
                    OutlinedTextField(
                        value = year.value,
                        onValueChange = { year.value = it },
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
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
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
                                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
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
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
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
                                        expanded = false
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
                                Toast.makeText(context, "Please enter title", Toast.LENGTH_SHORT).show()
                            } else if (author.value.isEmpty()) {
                                Toast.makeText(context, "Please enter author", Toast.LENGTH_SHORT).show()
                            } else if (selectedType.isEmpty()) {
                                Toast.makeText(context, "Please enter type", Toast.LENGTH_SHORT).show()
                            } else if (condition.value.isEmpty()) {
                                Toast.makeText(context, "Please enter condition", Toast.LENGTH_SHORT).show()
                            } else if (description.value.isEmpty()) {
                                Toast.makeText(context, "Please enter description", Toast.LENGTH_SHORT).show()
                            } else if (edition.value.isEmpty()) {
                                Toast.makeText(context, "Please enter edition", Toast.LENGTH_SHORT).show()
                            } else if (year.value.isEmpty()) {
                                Toast.makeText(context, "Please enter year", Toast.LENGTH_SHORT).show()
                            } else {
                                if (isEditing && bookId != null) {
                                    updateBookWithImages(
                                        bookId, title.value, author.value, selectedType,
                                        condition.value, description.value, edition.value,
                                        year.value, frontCoverUri.value, backCoverUri.value,
                                        context, navController
                                    )
                                } else {
                                    addDataToFirebaseWithImages(
                                        title.value, author.value, selectedType,
                                        condition.value, description.value, edition.value,
                                        year.value, frontCoverUri.value, backCoverUri.value,
                                        context, navController
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
    navController: NavController
) {
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
                "year" to year
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
                        updateDocumentWithChanges(docRef, updates, context, navController)
                    }
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
                        updateDocumentWithChanges(docRef, updates, context, navController)
                    }
                }
            }

            if (uploadTasks.isEmpty()) {
                updateDocumentWithChanges(docRef, updates, context, navController)
            }
        } else {
            Toast.makeText(context, "Book not found", Toast.LENGTH_SHORT).show()
        }
    }.addOnFailureListener { e ->
        Toast.makeText(context, "Error getting book data: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun updateDocumentWithChanges(
    docRef: DocumentReference,
    updates: HashMap<String, Any>,
    context: Context,
    navController: NavController
) {
    docRef.update(updates)
        .addOnSuccessListener {
            Toast.makeText(context, "Book updated successfully", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
        .addOnFailureListener { e ->
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
    frontCoverUri: Uri?,
    backCoverUri: Uri?,
    context: Context,
    navController: NavController
) {
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
        put("title", title)
        put("titleLower", titleLower)
        put("type", type)
        put("userEmail", currentUser?.email.toString())
        put("userId", currentUser?.uid.toString())
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
                saveBookToFirestore(docRef, bookData, context, navController)
            }
        }.addOnFailureListener { e ->
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
                saveBookToFirestore(docRef, bookData, context, navController)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to upload back cover: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    if (uploadTasks.isEmpty()) {
        saveBookToFirestore(docRef, bookData, context, navController)
    }
}

private fun saveBookToFirestore(
    docRef: DocumentReference,
    bookData: HashMap<String, Any>,
    context: Context,
    navController: NavController
) {
    docRef.set(bookData)
        .addOnSuccessListener {
            Toast.makeText(
                context,
                "Your book has been added to Firebase Firestore",
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Failed to add book: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}