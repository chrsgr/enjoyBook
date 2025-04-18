package com.example.enjoybook.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.focus.onFocusChanged

@Composable
fun SignupPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validazioni
    var nameError by remember { mutableStateOf<String?>(null) }
    var surnameError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var isCheckingUsername by remember { mutableStateOf(false) }
    var isUsernameTaken by remember { mutableStateOf(false) }

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    val primaryColor = Color(0xFF2CBABE)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)
    val errorColor = Color.Red

    // Validazione email
    val emailPattern = remember { Regex("[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}") }

    // Funzione per validare tutti i campi
    fun validateFields(): Boolean {
        var isValid = true

        nameError = if (name.isEmpty()) {
            isValid = false
            "Name is required"
        } else null

        surnameError = if (surname.isEmpty()) {
            isValid = false
            "Surname is required"
        } else null


        usernameError = if (username.isEmpty()) {
            isValid = false
            "Username is required"
        } else if (isUsernameTaken) {
            isValid = false
            "Username already taken"
        } else null

        emailError = if (email.isEmpty()) {
            isValid = false
            "Email is required"
        } else if (!emailPattern.matches(email)) {
            isValid = false
            "Invalid email format"
        } else null

        val minLength = 8
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        passwordError = if (password.isEmpty()) {
            isValid = false
            "Password is required"
        } else if (password.length < minLength) {
            isValid = false
            "Password must be at least ${minLength} characters"
        } else if(!hasUpperCase){
            isValid = false
            "The password must contain at least one uppercase letter"
        } else if(!hasDigit){
            isValid = false
            "The password must contain at least one digit"
        }
        else if(!hasSpecialChar){
            isValid = false
            "The password must contain at least one special character (@, #, $, etc...)"
        }
        else null

        confirmPasswordError = if (confirmPassword.isEmpty()) {
            isValid = false
            "Please confirm your password"
        } else if (password != confirmPassword) {
            isValid = false
            "Passwords do not match"
        } else null

        return isValid
    }

    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Authenticated -> {
                isLoading = false
                navController.navigate("main")
            }

            is AuthState.Error -> {
                isLoading = false
                Toast.makeText(
                    context,
                    (authState.value as AuthState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            is AuthState.Loading -> {
                isLoading = true
            }

            is AuthState.WaitingForVerification -> {
                Toast.makeText(
                    context,
                    "Please check your mailbox for the verification!",
                    Toast.LENGTH_LONG
                ).show()
                navController.navigate("login") {
                    popUpTo("signup") {
                        inclusive = true
                    }  // Rimuove la schermata attuale dallo stack
                }
            }

            else -> {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Contenuto principale
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "App Logo",
                modifier = Modifier.size(60.dp),
                tint = primaryColor
            )

            Text(
                "Create Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Nome
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError != null) nameError = null
                },
                label = { Text("Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    errorBorderColor = errorColor,
                    errorLabelColor = errorColor
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Name"
                    )
                },
                isError = nameError != null,
                supportingText = {
                    nameError?.let {
                        Text(
                            text = it,
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cognome
            OutlinedTextField(
                value = surname,
                onValueChange = {
                    surname = it
                    if (surnameError != null) surnameError = null
                },
                label = { Text("Surname") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    errorBorderColor = errorColor,
                    errorLabelColor = errorColor
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Surname"
                    )
                },
                isError = surnameError != null,
                supportingText = {
                    surnameError?.let {
                        Text(
                            text = it,
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Modifica il campo username come segue
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    // Resetta solo l'errore manuale, non l'errore di username già preso
                    if (usernameError != null && usernameError != "Username already taken") {
                        usernameError = null
                    }
                },
                label = { Text("Username") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        // Verifica l'username solo quando il campo perde il focus
                        if (!focusState.isFocused && username.isNotEmpty()) {
                            isCheckingUsername = true
                            authViewModel.isUsernameTaken(username) { taken ->
                                isUsernameTaken = taken
                                if (taken) {
                                    usernameError = "Username already taken"
                                }
                                isCheckingUsername = false
                            }
                        }
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    errorBorderColor = errorColor,
                    errorLabelColor = errorColor
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AccountBox,
                        contentDescription = "Username"
                    )
                },
                trailingIcon = {
                    if (isCheckingUsername) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                isError = usernameError != null,
                supportingText = {
                    usernameError?.let {
                        Text(
                            text = it,
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (emailError != null) emailError = null
                },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    errorBorderColor = errorColor,
                    errorLabelColor = errorColor
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email"
                    )
                },
                isError = emailError != null,
                supportingText = {
                    emailError?.let {
                        Text(
                            text = it,
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Password
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                    if (confirmPassword.isNotEmpty() && confirmPasswordError != null) {
                        confirmPasswordError = if (it != confirmPassword) "Passwords do not match" else null
                    }
                },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    errorBorderColor = errorColor,
                    errorLabelColor = errorColor
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                isError = passwordError != null,
                supportingText = {
                    passwordError?.let {
                        Text(
                            text = it,
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
            PasswordRequirementsIndicator(password)

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Conferma Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (confirmPasswordError != null) {
                        confirmPasswordError = if (it != password) "Passwords do not match" else null
                    }
                },
                label = { Text("Confirm Password") },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor,
                    errorBorderColor = errorColor,
                    errorLabelColor = errorColor
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Confirm Password"
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                isError = confirmPasswordError != null,
                supportingText = {
                    confirmPasswordError?.let {
                        Text(
                            text = it,
                            color = errorColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo Telefono (opzionale)
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    focusedLabelColor = primaryColor,
                    cursorColor = primaryColor
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone"
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pulsante di registrazione con validazione
            Button(
                onClick = {
                    if (validateFields()) {
                        authViewModel.signup(name, surname, username, email, password, phone)
                    }
                },
                colors = ButtonColors(
                    containerColor = primaryColor,
                    contentColor = textColor,
                    disabledContainerColor = Color(0xFFE0E0E0).copy(alpha = 0.5f),
                    disabledContentColor = textColor.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading &&
                        name.isNotEmpty() &&
                        surname.isNotEmpty() &&
                        username.isNotEmpty() &&
                        email.isNotEmpty() &&
                        password.isNotEmpty() &&
                        confirmPassword.isNotEmpty()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = textColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "CREATE ACCOUNT",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Link per login
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Already have an account? ",
                    color = textColor
                )
                TextButton(
                    onClick = { navController.navigate("login") },
                    enabled = !isLoading
                ) {
                    Text(
                        "Login",
                        color = Color(0xFF046C70),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                LoadingSpinner(message = "Creating account...")
            }
        }
    }
}

@Composable
fun LoadingSpinner(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(60.dp),
                color = Color(0xFFA7E8EB),
                strokeWidth = 5.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                color = Color(0xFF333333),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            val rotation = rememberInfiniteTransition(label = "loading").animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
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
}

@Composable
fun PasswordRequirementsIndicator(password: String) {
    val minLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecialChar = password.any { !it.isLetterOrDigit() }

    AnimatedVisibility(
        visible = password.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "The password must contains:",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                PasswordRequirement("at least 8 characters", minLength)
                PasswordRequirement("at least one uppercase letter", hasUpperCase)
                PasswordRequirement("at least one digit", hasDigit)
                PasswordRequirement("at least one special character", hasSpecialChar)
            }
        }
    }
}

@Composable
fun PasswordRequirement(text: String, isSatisfied: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isSatisfied) Icons.Default.Check else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isSatisfied) Color(0xFF4CAF50) else Color.Gray,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSatisfied) Color.DarkGray else Color.Gray,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}