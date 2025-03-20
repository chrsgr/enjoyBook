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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.google.common.util.concurrent.ListenableFuture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScanScreen(
    navController: NavController,
    onBookInfoRetrieved: (String, String, String, String, String) -> Unit // (title, author, year, description, type)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)

    // Create a coroutine scope that is tied to this composable's lifecycle
    val coroutineScope = rememberCoroutineScope()

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    // Request camera permission
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera state
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Used to handle processing errors
    var processingError by remember { mutableStateOf<String?>(null) }

    // Create a key for the coroutine scope that won't change during composition
    val coroutineScopeKey = remember { Object() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SCAN BOOK",
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
            if (hasCameraPermission) {
                if (capturedImageUri == null) {
                    // Camera preview
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        cameraProviderFuture = cameraProviderFuture,
                        imageCapture = imageCapture,
                        lifecycleOwner = lifecycleOwner
                    )

                    // Camera controls
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        IconButton(
                            onClick = {
                                takePhoto(
                                    context = context,
                                    imageCapture = imageCapture,
                                    executor = executor,
                                    onPhotoTaken = { uri ->
                                        capturedImageUri = uri
                                        isProcessing = true
                                        processingError = null

                                        coroutineScope.launch {
                                            try {
                                                val result =
                                                    processImageAndGetBookInfo(context, uri)
                                                if (result != null) {
                                                    // Use type instead of category to match Book data class
                                                    onBookInfoRetrieved(
                                                        result.title,
                                                        result.author,
                                                        result.year,
                                                        result.description,
                                                        result.type // Changed from category to type
                                                    )
                                                    // Call popBackStack on the main thread
                                                    withContext(Dispatchers.Main) {
                                                        navController.popBackStack()
                                                    }
                                                } else {
                                                    processingError =
                                                        "Couldn't find book information. Please try entering details manually."
                                                    isProcessing = false
                                                }
                                            } catch (e: Exception) {
                                                Log.e("BookScan", "Error processing image", e)
                                                processingError =
                                                    "Error processing image: ${e.message}"
                                                isProcessing = false
                                            }
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.7f))
                                .padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Take Photo",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                } else {
                    // Image captured, show processing UI
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = primaryColor)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Analyzing book...",
                                    color = textColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                processingError?.let {
                                    Text(
                                        it,
                                        color = Color.Red,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { capturedImageUri = null },
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                    ) {
                                        Text("Try Again")
                                    }
                                }
                            }
                        }

                        // Cancel button (only show when not processing)
                        if (!isProcessing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                IconButton(
                                    onClick = { capturedImageUri = null },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // No camera permission
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Camera permission is required for this feature",
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("Request Permission")
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    imageCapture: ImageCapture,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current

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
        modifier = modifier
    )
}

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

// Combined function that processes the image and returns book info in one step
// This prevents coroutine cancellation issues
private suspend fun processImageAndGetBookInfo(
    context: Context,
    imageUri: Uri
): BookInfo? = withContext(Dispatchers.IO) {
    try {
        // Step 1: Extract text from image using ML Kit
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val result = suspendCancellableCoroutine<String> { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text) {}
                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "Text recognition failed", e)
                    continuation.resume("") {}
                }
        }

        if (result.isBlank()) {
            Log.d("BookScan", "Couldn't extract text from image")
            return@withContext null
        }

        Log.d("OCR", "Extracted text: $result")

        // Step 2: Search for book info using extracted text
        return@withContext searchBookInfo(result)
    } catch (e: Exception) {
        Log.e("BookScan", "Error in processImageAndGetBookInfo", e)
        throw e
    }
}

private suspend fun searchBookInfo(query: String): BookInfo? = withContext(Dispatchers.IO) {
    if (query.isBlank() || query.length < 3) {
        Log.d("GoogleBooksAPI", "Query too short or blank, skipping API call")
        return@withContext null
    }

    try {
        Log.d("GoogleBooksAPI", "Searching for book with query: $query")
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val apiKey = "AIzaSyDA9btjrzoV5g-YqjcmzgLhrqzaWfXjjPw"
        val url = URL("https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&key=$apiKey&maxResults=1")

        Log.d("GoogleBooksAPI", "API URL: https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&key=<API_KEY>&maxResults=1")

        val connection = url.openConnection()
        connection.connectTimeout = 10000 // 10 seconds timeout
        connection.readTimeout = 10000 // 10 seconds timeout

        // Set request properties to ensure proper response
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Android Book Scanner App")

        Log.d("GoogleBooksAPI", "Starting API request...")
        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        Log.d("GoogleBooksAPI", "Received API response, length: ${response.length}")

        // Log first part of response to see what's being returned
        Log.d("GoogleBooksAPI", "Response first 200 chars: ${response.take(200)}")

        val jsonObject = JSONObject(response)

        if (jsonObject.has("totalItems") && jsonObject.getInt("totalItems") == 0) {
            Log.d("GoogleBooksAPI", "No books found for query")
            return@withContext null
        }

        if (jsonObject.has("items") && jsonObject.getJSONArray("items").length() > 0) {
            val bookJson = jsonObject.getJSONArray("items").getJSONObject(0)
            Log.d("GoogleBooksAPI", "Found book with ID: ${bookJson.optString("id", "unknown")}")

            val volumeInfo = bookJson.getJSONObject("volumeInfo")

            val title = if (volumeInfo.has("title")) {
                val bookTitle = volumeInfo.getString("title")
                Log.d("GoogleBooksAPI", "Book title: $bookTitle")
                bookTitle
            } else {
                Log.d("GoogleBooksAPI", "No title found in book data")
                ""
            }

            val authors = if (volumeInfo.has("authors")) {
                val authorsArray = volumeInfo.getJSONArray("authors")
                if (authorsArray.length() > 0) {
                    val firstAuthor = authorsArray.getString(0)
                    Log.d("GoogleBooksAPI", "Book author: $firstAuthor")
                    firstAuthor
                } else {
                    Log.d("GoogleBooksAPI", "Authors array is empty")
                    ""
                }
            } else {
                Log.d("GoogleBooksAPI", "No authors found in book data")
                ""
            }

            val publishedDate = if (volumeInfo.has("publishedDate")) {
                val date = volumeInfo.getString("publishedDate")
                val year = if (date.length >= 4) date.substring(0, 4) else ""
                Log.d("GoogleBooksAPI", "Book year: $year")
                year
            } else {
                Log.d("GoogleBooksAPI", "No published date found in book data")
                ""
            }

            val description = if (volumeInfo.has("description")) {
                val desc = volumeInfo.getString("description")
                Log.d("GoogleBooksAPI", "Book description: ${desc.take(50)}...")
                desc
            } else {
                Log.d("GoogleBooksAPI", "No description found in book data")
                ""
            }

            val categories = if (volumeInfo.has("categories") && volumeInfo.getJSONArray("categories").length() > 0) {
                val category = volumeInfo.getJSONArray("categories").getString(0)
                Log.d("GoogleBooksAPI", "Book category: $category")
                category
            } else {
                Log.d("GoogleBooksAPI", "No categories found, using default 'Fiction'")
                "Fiction"
            }

            val bookType = mapBookCategory(categories)
            Log.d("GoogleBooksAPI", "Mapped book type: $bookType")

            return@withContext BookInfo(
                title = title,
                author = authors,
                year = publishedDate,
                description = description,
                type = bookType
            )
        } else {
            Log.d("GoogleBooksAPI", "No 'items' found in API response")
            return@withContext null
        }
    } catch (e: Exception) {
        Log.e("GoogleBooksAPI", "Error fetching book info", e)
        // Print more detailed error information
        Log.e("GoogleBooksAPI", "Error message: ${e.message}")
        Log.e("GoogleBooksAPI", "Error cause: ${e.cause}")
        e.printStackTrace()
        return@withContext null
    }
}

private fun mapBookCategory(googleCategory: String): String {
    val bookTypes = listOf(
        "Adventure", "Classics", "Crime", "Folk", "Fantasy", "Historical",
        "Horror", "Literary fiction", "Mystery", "Poetry", "Plays",
        "Romance", "Science fiction", "Short stories", "Thrillers",
        "War", "Women's fiction", "Young adult"
    )

    // Map Google Books categories to app categories
    return when {
        googleCategory.contains("adventure", ignoreCase = true) -> "Adventure"
        googleCategory.contains("classic", ignoreCase = true) -> "Classics"
        googleCategory.contains("crime", ignoreCase = true) -> "Crime"
        googleCategory.contains("folk", ignoreCase = true) -> "Folk"
        googleCategory.contains("fantasy", ignoreCase = true) -> "Fantasy"
        googleCategory.contains("histor", ignoreCase = true) -> "Historical"
        googleCategory.contains("horror", ignoreCase = true) -> "Horror"
        googleCategory.contains("literary", ignoreCase = true) -> "Literary fiction"
        googleCategory.contains("mystery", ignoreCase = true) -> "Mystery"
        googleCategory.contains("poetry", ignoreCase = true) -> "Poetry"
        googleCategory.contains("play", ignoreCase = true) -> "Plays"
        googleCategory.contains("drama", ignoreCase = true) -> "Plays"
        googleCategory.contains("romance", ignoreCase = true) -> "Romance"
        googleCategory.contains("science fiction", ignoreCase = true) -> "Science fiction"
        googleCategory.contains("sci-fi", ignoreCase = true) -> "Science fiction"
        googleCategory.contains("short", ignoreCase = true) -> "Short stories"
        googleCategory.contains("thriller", ignoreCase = true) -> "Thrillers"
        googleCategory.contains("war", ignoreCase = true) -> "War"
        googleCategory.contains("women", ignoreCase = true) -> "Women's fiction"
        googleCategory.contains("young adult", ignoreCase = true) -> "Young adult"
        googleCategory.contains("ya", ignoreCase = true) -> "Young adult"
        else -> bookTypes.firstOrNull { it.equals(googleCategory, ignoreCase = true) } ?: "Literary fiction"
    }
}

data class BookInfo(
    val title: String,
    val author: String,
    val year: String,
    val description: String,
    val type: String // Changed from category to type to match Book data class
)