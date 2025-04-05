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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

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
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingError by remember { mutableStateOf<String?>(null) }
    var scanInstructions by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SCAN ISBN",
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
            if (hasCameraPermission) {
                if (capturedImageUri == null) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        cameraProviderFuture = cameraProviderFuture,
                        imageCapture = imageCapture,
                        lifecycleOwner = lifecycleOwner
                    )

                    if (scanInstructions) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "Position the ISBN barcode within the frame",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "The ISBN is typically located on the back cover near the barcode",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { scanInstructions = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text("Got it")
                                }
                            }
                        }
                    }

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
                                                val result = processImageAndGetISBN(context, uri)
                                                if (result != null) {
                                                    val bookInfo = searchBookInfoByISBN(result)
                                                    if (bookInfo != null) {
                                                        onBookInfoRetrieved(
                                                            bookInfo.title,
                                                            bookInfo.author,
                                                            bookInfo.year,
                                                            bookInfo.description,
                                                            bookInfo.type
                                                        )
                                                        withContext(Dispatchers.Main) {
                                                            navController.popBackStack()
                                                        }
                                                    } else {
                                                        processingError = "Couldn't find information for ISBN: $result. Please try entering details manually."
                                                        isProcessing = false
                                                    }
                                                } else {
                                                    processingError = "Couldn't detect an ISBN. Please try again or enter details manually."
                                                    isProcessing = false
                                                }
                                            } catch (e: Exception) {
                                                Log.e("BookScan", "Error processing image", e)
                                                processingError = "Error: ${e.message}"
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
                                    "Scanning ISBN...",
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
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            capturedImageUri = null
                                            scanInstructions = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                    ) {
                                        Text("Try Again")
                                    }
                                }
                            }
                        }

                        if (!isProcessing) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        capturedImageUri = null
                                        scanInstructions = false
                                    },
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
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
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

private suspend fun processImageAndGetISBN(
    context: Context,
    imageUri: Uri
): String? = withContext(Dispatchers.IO) {
    try {
        val image = InputImage.fromFilePath(context, imageUri)

        val barcodeResult = suspendCancellableCoroutine<String?> { continuation ->
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    var isbn: String? = null
                    for (barcode in barcodes) {
                        if (barcode.valueType == Barcode.TYPE_ISBN ||
                            barcode.valueType == Barcode.TYPE_PRODUCT) {
                            val rawValue = barcode.rawValue
                            if (rawValue != null &&
                                (rawValue.length == 10 || rawValue.length == 13) &&
                                rawValue.all { it.isDigit() }) {
                                isbn = rawValue
                                break
                            }
                        }
                    }

                    if (isbn != null) {
                        Log.d("ISBN", "Found ISBN via barcode: $isbn")
                        continuation.resume(isbn) {}
                    } else {
                        Log.d("ISBN", "No ISBN barcode found, falling back to text recognition")
                        continuation.resume(null) {}
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ISBN", "Barcode scanning failed", e)
                    continuation.resume(null) {}
                }
        }

        if (barcodeResult != null) {
            return@withContext barcodeResult
        }

        val textResult = suspendCancellableCoroutine<String> { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text) {}
                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "Text recognition failed", e)
                    continuation.resume("") {}
                }
        }

        if (textResult.isBlank()) {
            Log.d("ISBN", "Couldn't extract text from image")
            return@withContext null
        }

        val isbnRegex = """ISBN(?:-1[03])?:?\s*((?:97[89][-\s]?)?(?:\d[-\s]?){9}[\dXx])""".toRegex()
        val isbnNoLabelRegex = """(?:97[89][-\s]?)?(?:\d[-\s]?){9}[\dXx]""".toRegex()

        val match = isbnRegex.find(textResult) ?: isbnNoLabelRegex.find(textResult)
        val isbn = match?.groupValues?.get(1)?.replace(Regex("[-\\s]"), "") ?:
        match?.value?.replace(Regex("[-\\s]"), "")

        if (isbn != null) {
            Log.d("ISBN", "Found ISBN via text recognition: $isbn")
            return@withContext isbn
        }

        Log.d("ISBN", "No ISBN found in text: ${textResult.take(100)}...")
        return@withContext null
    } catch (e: Exception) {
        Log.e("ISBN", "Error in processImageAndGetISBN", e)
        throw e
    }
}

private suspend fun searchBookInfoByISBN(isbn: String): BookInfo? = withContext(Dispatchers.IO) {
    if (isbn.isBlank()) {
        Log.d("GoogleBooksAPI", "ISBN is blank, skipping API call")
        return@withContext null
    }

    try {
        Log.d("GoogleBooksAPI", "Searching for book with ISBN: $isbn")
        val apiKey = "AIzaSyDA9btjrzoV5g-YqjcmzgLhrqzaWfXjjPw"
        val url = URL("https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn&key=$apiKey")
        Log.d("GoogleBooksAPI", "API URL: https://www.googleapis.com/books/v1/volumes?q=isbn:$isbn&key=<API_KEY>")

        val connection = url.openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Android Book Scanner App")

        Log.d("GoogleBooksAPI", "Starting API request...")
        val response = connection.getInputStream().bufferedReader().use { it.readText() }
        Log.d("GoogleBooksAPI", "Received API response, length: ${response.length}")

        val jsonObject = JSONObject(response)
        if (jsonObject.has("totalItems") && jsonObject.getInt("totalItems") == 0) {
            Log.d("GoogleBooksAPI", "No books found for ISBN: $isbn")
            return@withContext null
        }

        if (jsonObject.has("items") && jsonObject.getJSONArray("items").length() > 0) {
            val bookJson = jsonObject.getJSONArray("items").getJSONObject(0)
            val volumeInfo = bookJson.getJSONObject("volumeInfo")

            val title = if (volumeInfo.has("title")) {
                volumeInfo.getString("title")
            } else {
                ""
            }

            val authors = if (volumeInfo.has("authors")) {
                val authorsArray = volumeInfo.getJSONArray("authors")
                if (authorsArray.length() > 0) {
                    authorsArray.getString(0)
                } else {
                    ""
                }
            } else {
                ""
            }

            val publishedDate = if (volumeInfo.has("publishedDate")) {
                val date = volumeInfo.getString("publishedDate")
                if (date.length >= 4) date.substring(0, 4) else ""
            } else {
                ""
            }

            val description = if (volumeInfo.has("description")) {
                volumeInfo.getString("description")
            } else {
                ""
            }

            val categories = if (volumeInfo.has("categories") && volumeInfo.getJSONArray("categories").length() > 0) {
                volumeInfo.getJSONArray("categories").getString(0)
            } else {
                "No Selection"
            }

            val bookType = mapBookCategory(categories)

            Log.d("GoogleBooksAPI", "Found book: $title by $authors")
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
        Log.e("GoogleBooksAPI", "Error fetching book info by ISBN", e)
        e.printStackTrace()
        return@withContext null
    }
}

private fun mapBookCategory(googleCategory: String): String {
    val bookTypes = listOf(
        "No Selection", "Adventure", "Classics", "Crime", "Folk", "Fantasy", "Historical", "Horror",
        "Literary fiction", "Mystery", "Poetry", "Plays", "Romance", "Science fiction",
        "Short stories", "Thrillers", "War", "Women's fiction", "Young adult"
    )

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
        else -> bookTypes.firstOrNull { it.equals(googleCategory, ignoreCase = true) } ?: "No Selection"
    }
}

data class BookInfo(
    val title: String,
    val author: String,
    val year: String,
    val description: String,
    val type: String
)