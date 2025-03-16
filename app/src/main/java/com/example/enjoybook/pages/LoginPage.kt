package com.example.enjoybook.pages


import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
@Composable
fun FallingBooksAnimation(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        repeat(30) { index ->
            FallingBook(
                delayMillis = index * 75L,
                horizontalOffset = Random.nextInt(-150, 150).toFloat(),
                size = Random.nextInt(40, 80).toFloat(),
                color = when (index % 4) {
                    0 -> Color(0xFF2CBABE) // Primary color
                    1 -> Color(0xFF1D8A8E) // Darker shade of primary
                    2 -> Color(0xFF3ECECC) // Lighter shade of primary
                    else -> Color(0xFF25A3A8) // Medium shade of primary
                }
            )
        }
    }
}

@Composable
fun FallingBook(delayMillis: Long, horizontalOffset: Float, size: Float, color: Color) {
    var startAnimation by remember { mutableStateOf(false) }
    var animationCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        delay(delayMillis)
        startAnimation = true
    }

    val animationDuration = 2500
    val animationSpec = tween<Float>(
        durationMillis = animationDuration,
        easing = FastOutSlowInEasing
    )

    val yOffsetAnimation = remember { Animatable(-100f) }
    val rotationAnimation = remember { Animatable(0f) }
    val alphaAnimation = remember { Animatable(1f) }
    val scaleAnimation = remember { Animatable(1f) }

    LaunchedEffect(startAnimation) {
        if (startAnimation && !animationCompleted) {
            launch {
                yOffsetAnimation.animateTo(
                    targetValue = 800f,
                    animationSpec = animationSpec
                )
            }

            launch {
                rotationAnimation.animateTo(
                    targetValue = Random.nextInt(-360, 360).toFloat(),
                    animationSpec = animationSpec
                )
            }

            launch {
                delay(animationDuration / 2L)
                alphaAnimation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = animationDuration / 2,
                        easing = LinearEasing
                    )
                )
            }

            launch {
                scaleAnimation.animateTo(
                    targetValue = 0.6f,
                    animationSpec = animationSpec
                )
                animationCompleted = true
            }
        }
    }

    if (startAnimation && !animationCompleted) {
        Icon(
            imageVector = Icons.Filled.MenuBook,
            contentDescription = "Falling Book",
            modifier = Modifier
                .offset(x = horizontalOffset.dp, y = yOffsetAnimation.value.dp)
                .size(size.dp)
                .alpha(alphaAnimation.value)
                .graphicsLayer {
                    rotationZ = rotationAnimation.value
                    scaleX = scaleAnimation.value
                    scaleY = scaleAnimation.value
                },
            tint = color
        )
    }
}

@Composable
fun LoginPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isEmailValid by remember { mutableStateOf(true) }
    var isPasswordValid by remember { mutableStateOf(true) }

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)
    val errorColor = Color(0xFFD32F2F)

    LaunchedEffect(authState.value) {
        when(authState.value) {
            is AuthState.Authenticated -> navController.navigate("main")
            is AuthState.Error -> Toast.makeText(
                context,
                (authState.value as AuthState.Error).message,
                Toast.LENGTH_SHORT
            ).show()
            else -> Unit
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Background animation
        FallingBooksAnimation()

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App logo with animation
            val rotation = rememberInfiniteTransition().animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        rotationZ = rotation.value
                    },
                tint = primaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "EnjoyBook",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field with validation
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    isEmailValid = it.isEmpty() || it.contains('@')
                },
                label = { Text("Email", color = textColor.copy(alpha = 0.8f)) },
                singleLine = true,
                isError = !isEmailValid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                    errorBorderColor = errorColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isEmailValid) {
                Text(
                    text = "Please enter a valid email address",
                    color = errorColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password field with validation
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    isPasswordValid = it.isEmpty() || it.length >= 6
                },
                label = { Text("Password", color = textColor.copy(alpha = 0.8f)) },
                singleLine = true,
                isError = !isPasswordValid,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        if (isEmailValid && isPasswordValid && email.isNotEmpty() && password.isNotEmpty()) {
                            authViewModel.login(email, password)
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = primaryColor.copy(alpha = 0.5f),
                    errorBorderColor = errorColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isPasswordValid) {
                Text(
                    text = "Password must be at least 6 characters",
                    color = errorColor,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login button with improved styling
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (isEmailValid && isPasswordValid && email.isNotEmpty() && password.isNotEmpty()) {
                        authViewModel.login(email, password)
                    }
                },
                enabled = authState.value != AuthState.Loading && email.isNotEmpty() && password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "LOGIN",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign up link with improved styling
            TextButton(
                onClick = { navController.navigate("signup") }
            ) {
                Text(
                    text = "Don't have an account? Sign up",
                    color = primaryColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Loading overlay
        if (authState.value == AuthState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner(primaryColor)
            }
        }
    }
}

@Composable
fun LoadingSpinner(primaryColor: Color) {
    // Add a card background for better visibility
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                color = primaryColor,
                strokeWidth = 5.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Logging in...",
                color = Color(0xFF333333),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            // Animated book icon
            val rotation = rememberInfiniteTransition().animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = "Loading Book",
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        rotationZ = rotation.value
                    },
                tint = primaryColor
            )
        }
    }
}