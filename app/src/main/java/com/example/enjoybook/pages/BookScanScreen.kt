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
import com.example.enjoybook.data.Book
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import org.json.JSONObject
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.common.util.concurrent.ListenableFuture

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun BookScanScreen(
    navController: NavController,
    onBookInfoRetrieved: (String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)

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
    val scope = rememberCoroutineScope()

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
                            // Process the image and get book details
                            LaunchedEffect(capturedImageUri) {
                                isProcessing = true
                                try {
                                    processCapturedImage(
                                        context = context,
                                        imageUri = capturedImageUri!!,
                                        onSuccess = { title, author, year, description, type ->
                                            onBookInfoRetrieved(title, author, year, description, type)
                                            navController.popBackStack()
                                        },
                                        onError = { errorMessage ->
                                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                            capturedImageUri = null
                                            isProcessing = false
                                        }
                                    )
                                } catch (e: Exception) {
                                    Log.e("BookScan", "Error processing image", e)
                                    Toast.makeText(context, "Error processing image: ${e.message}", Toast.LENGTH_LONG).show()
                                    capturedImageUri = null
                                    isProcessing = false
                                }
                            }
                        }

                        if (!isProcessing) {
                            // Cancel button
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

            // Utilizziamo un effetto collaterale per attendere che il provider della fotocamera sia pronto
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

private suspend fun processCapturedImage(
    context: Context,
    imageUri: Uri,
    onSuccess: (String, String, String, String, String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        // Step 1: Use ML Kit for OCR to extract text from the image
        val extractedText = extractTextFromImage(context, imageUri)
        if (extractedText.isNullOrBlank()) {
            onError("Couldn't extract any text from the image. Please try again with a clearer image.")
            return
        }

        // Step 2: Use extracted text to search for book information
        val bookInfo = searchBookInfo(extractedText)
        if (bookInfo == null) {
            onError("Couldn't find book information. Please try entering details manually.")
            return
        }

        // Step 3: Return the book information to populate the form
        onSuccess(
            bookInfo.title,
            bookInfo.author,
            bookInfo.year,
            bookInfo.description,
            bookInfo.year
        )
    } catch (e: Exception) {
        Log.e("BookScan", "Error processing image", e)
        onError("Error processing image: ${e.message}")
    }
}

private suspend fun extractTextFromImage(context: Context, imageUri: Uri): String {
    return withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // Utilizziamo l'estensione await di kotlinx.coroutines.tasks
            val result = recognizer.process(image).await()
            val extractedText = result.text

            Log.d("OCR", "Extracted text: $extractedText")
            extractedText
        } catch (e: Exception) {
            Log.e("OCR", "Text recognition failed", e)
            throw e
        }
    }
}

private suspend fun searchBookInfo(query: String): Book? {
    return withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val apiKey = "AIzaSyDA9btjrzoV5g-YqjcmzgLhrqzaWfXjjPw"
            val url = URL("https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&key=$apiKey")

            val connection = url.openConnection()
            val response = connection.getInputStream().bufferedReader().use { it.readText() }

            val jsonObject = JSONObject(response)

            if (jsonObject.has("items") && jsonObject.getJSONArray("items").length() > 0) {
                val bookJson = jsonObject.getJSONArray("items").getJSONObject(0)
                val volumeInfo = bookJson.getJSONObject("volumeInfo")

                val title = if (volumeInfo.has("title")) volumeInfo.getString("title") else ""

                val authors = if (volumeInfo.has("author")) {
                    val authorsArray = volumeInfo.getJSONArray("author")
                    if (authorsArray.length() > 0) authorsArray.getString(0) else ""
                } else ""

                val publishedDate = if (volumeInfo.has("publishedDate")) {
                    val date = volumeInfo.getString("publishedDate")
                    if (date.length >= 4) date.substring(0, 4) else ""
                } else ""

                val description = if (volumeInfo.has("description")) {
                    volumeInfo.getString("description")
                } else ""

                val categories = if (volumeInfo.has("type") && volumeInfo.getJSONArray("categories").length() > 0) {
                    volumeInfo.getJSONArray("type").getString(0)
                } else "Fiction"

                Book(
                    title = title,
                    author = authors,
                    year = publishedDate,
                    description = description,
                    type = mapBookCategory(categories)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("GoogleBooksAPI", "Error fetching book info", e)
            null
        }
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