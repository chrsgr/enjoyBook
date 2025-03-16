package com.example.enjoybook.pages

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.enjoybook.data.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddPage(
    navController: NavController,
    context: Context,
    isEditing: Boolean = false,
    bookId: String? = null
) {
    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)

    val db = FirebaseFirestore.getInstance()

    val title = remember { mutableStateOf("") }
    val author = remember { mutableStateOf("") }
    val condition = remember { mutableStateOf("") }
    val description = remember { mutableStateOf("") }
    val edition = remember { mutableStateOf("") }
    val year = remember { mutableStateOf("") }

    val bookTypes = listOf(
        "Adventure", "Classics", "Crime", "Folk", "Fantasy", "Historical",
        "Horror", "Literary fiction", "Mystery", "Poetry", "Plays",
        "Romance", "Science fiction", "Short stories", "Thrillers",
        "War", "Women's fiction", "Young adult"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(bookTypes.first()) }

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
                            // Verifica che il tipo del libro sia nella lista di tipi disponibili
                            if (bookTypes.contains(it.type)) {
                                selectedType = it.type
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
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(30.dp))


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
                                    updateBook(bookId, title.value, author.value, selectedType, condition.value, description.value, edition.value, year.value, context, navController)
                                } else {
                                    addDataToFirebase(title.value, author.value, selectedType, condition.value, description.value, edition.value, year.value, context, navController)
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
                }
            }
        }
    }
}

fun updateBook(
    bookId: String,
    title: String,
    author: String,
    type: String,
    condition: String,
    description: String,
    edition: String,
    year: String,
    context: Context,
    navController: NavController
) {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
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

            docRef.update(updates)
                .addOnSuccessListener {
                    Toast.makeText(context, "Book updated successfully", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update book: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Book not found", Toast.LENGTH_SHORT).show()
        }
    }.addOnFailureListener { e ->
        Toast.makeText(context, "Error getting book data: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun addDataToFirebase(
    title: String,
    author: String,
    type: String,
    condition: String,
    description: String,
    edition: String,
    year: String,
    context: Context,
    navController: NavController
) {
    val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    val dbCourses: CollectionReference = db.collection("books")
    val currentUser = FirebaseAuth.getInstance().currentUser
    val titleLower = title.lowercase()

    val docRef = dbCourses.document()
    val documentId = docRef.id

    val books = Book(
        id = documentId,
        author = author,
        condition = condition,
        description = description,
        edition = edition,
        title = title,
        titleLower = titleLower,
        type = type,
        userEmail = currentUser?.email.toString(),
        userId = currentUser?.uid.toString(),
        year = year
    )

    docRef.set(books)
        .addOnSuccessListener {
            Toast.makeText(
                context,
                "Your book has been added to Firebase Firestore",
                Toast.LENGTH_SHORT
            ).show()
            navController.popBackStack()
        }
        .addOnFailureListener { e ->
            Toast.makeText(context, "Fail to add book \n$e", Toast.LENGTH_SHORT).show()
        }
}
