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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
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
        repeat(40) { index ->
            FallingBook(
                delayMillis = index * 50L,
                horizontalOffset = Random.nextInt(-150, 150).toFloat(),
                size = Random.nextInt(40, 80).toFloat()
            )
        }
    }
}
@Composable
fun FallingBook(delayMillis: Long, horizontalOffset: Float, size: Float) {
    var startAnimation by remember { mutableStateOf(false) }
    var animationCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        delay(delayMillis)
        startAnimation = true
    }

    val animationDuration = 2000
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
            tint = Color(0xFF95D1D3)
        )
    }
}


@Composable
fun LoginPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel){

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(authState.value) {
        when(authState.value){
            is AuthState.Authenticated -> navController.navigate("main")
            is AuthState.Error -> Toast.makeText(context, (authState.value as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            else -> Unit
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(modifier = modifier.fillMaxSize()) {
        FallingBooksAnimation()




        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Text("Login", fontSize = 32.sp)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = {
                    Text(text = "Email")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text(text = "Password")
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    authViewModel.login(email, password)
                },
                enabled = authState.value != AuthState.Loading,
                colors = ButtonDefaults.buttonColors(Color(0xFFA7E8EB))
            ) {
                Text(text = "LOGIN", color = Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = {
                navController.navigate("signup")
            }) {
                Text("You don't have an account? Sign up", color = Color.Blue)
            }
        }
    }
    if (authState.value == AuthState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000)),
            contentAlignment = Alignment.Center
        ) {
            LoadingSpinner()
        }
    }
}

@Composable
fun LoadingSpinner() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(60.dp),
            color = Color(0xFFA7E8EB),
            strokeWidth = 5.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "login..",
            color = Color.White,
            fontSize = 18.sp
        )

        // Animazione libro rotante
        val rotation = rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = Icons.Filled.MenuBook,
            contentDescription = "Loading Book",
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    rotationZ = rotation.value
                },
            tint = Color(0xFFA7E8EB)
        )
    }
}