package com.example.enjoybook


import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.enjoybook.pages.AddPage
import com.example.enjoybook.pages.BookDetails
import com.example.enjoybook.pages.BookPage
import com.example.enjoybook.pages.FavouritePage
import com.example.enjoybook.pages.FilteredBooksPage
import com.example.enjoybook.pages.HomePage
import com.example.enjoybook.pages.LibraryPage
import com.example.enjoybook.pages.ListBookAddPage
import com.example.enjoybook.auth.LoginPage
import com.example.enjoybook.pages.ProfilePage
import com.example.enjoybook.pages.SearchPage
import com.example.enjoybook.auth.SignupPage
import com.example.enjoybook.viewModel.AuthViewModel
import com.example.enjoybook.viewModel.SearchViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.enjoybook.admin.AdminListReports
import com.example.enjoybook.admin.AdminPanel
import com.example.enjoybook.auth.ForgotPasswordPage
import com.example.enjoybook.data.Notification
import com.example.enjoybook.data.User
import com.example.enjoybook.pages.*
import com.example.enjoybook.theme.errorColor
import com.example.enjoybook.utils.deleteNotification
import com.example.enjoybook.utils.handleAcceptLoanRequest
import com.example.enjoybook.utils.handleRejectLoanRequest
import com.example.enjoybook.utils.markNotificationsAsRead
import com.example.enjoybook.viewModel.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel, searchViewModel: SearchViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val authState = authViewModel.authState.observeAsState()

    // Ottieni la rotta corrente
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Definisci le rotte che non dovrebbero mostrare le barre
    val routesWithoutBars = listOf("login", "signup", "forgotpass")

    // Determina se mostrare le barre
    val shouldShowBars = remember(currentRoute) {
        currentRoute != null && !routesWithoutBars.any { route ->
            currentRoute.startsWith(route)
        }
    }

    // Osserva lo stato di autenticazione per reindirizzare se necessario
    LaunchedEffect(authState.value) {
        when(authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            is AuthState.Authenticated -> {
                if (currentRoute == "login") {
                    navController.navigate("main") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (shouldShowBars) {
                MainTopBar(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        },
        bottomBar = {
            if (shouldShowBars) {
                MainBottomBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Le tue destinazioni di navigazione esistenti
            composable("login") {
                LoginPage(modifier, navController, authViewModel)
            }

            composable("forgotpass") {
                ForgotPasswordPage(modifier, navController, authViewModel)
            }

            composable("signup") {
                SignupPage(modifier, navController, authViewModel)
            }

            composable("main") {
                HomePage(navController, authViewModel)
            }

            composable("home"){
                HomePage(navController, authViewModel)
            }

            composable("admin"){
                AdminPanel(navController, authViewModel)
            }

            composable("adminReports"){
                AdminListReports(navController, authViewModel)
            }

            composable(
                route = "addPage?bookId={bookId}&isEditing={isEditing}",
                arguments = listOf(
                    navArgument("bookId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("isEditing") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId")
                val isEditing = backStackEntry.arguments?.getBoolean("isEditing") ?: false

                AddPage(
                    navController = navController,
                    context = LocalContext.current,
                    isEditing = isEditing,
                    bookId = bookId
                )
            }

            composable("search"){
                SearchPage(searchViewModel, navController)
            }

            composable("profile"){
                ProfilePage(navController)
            }

            composable("favourite"){
                FavouritePage(navController)
            }

            composable(
                route = "bookDetails/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) {
                val bookId = it.arguments?.getString("bookId") ?: ""
                BookDetails(navController, authViewModel, bookId)
            }

            composable(
                route = "userDetails/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) {
                val userId = it.arguments?.getString("userId") ?: ""
                UserDetails(navController, authViewModel, userId)
            }


            composable("bookUser") {
                BookPage(navController, authViewModel)
            }

            composable("library"){
                LibraryPage(navController)
            }

            composable("listBookAdd"){
                ListBookAddPage(navController)
            }



            composable("filteredbooks/{category}", arguments = listOf(navArgument("category") { type = NavType.StringType })){
                    backStackEntry ->
                val category = backStackEntry.arguments?.getString("category") ?: ""
                Log.d("NavHost", "Navigato a filteredbooks con categoria: $category")
                // Passa category al ViewModel
                FilteredBooksPage(category, navController, searchViewModel)
            }

            composable(
                "edit_screen/{bookId}",
                arguments = listOf(
                    navArgument("bookId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId")
                if (bookId != null) {
                    AddPage(
                        navController = navController,
                        context = context,
                        isEditing = true,
                        bookId = bookId
                    )
                }
            }

            composable("book_scan_screen") {
                BookScanScreen(
                    navController = navController,
                    onBookInfoRetrieved = { title, author, year, description, type ->
                        // Navigate back to AddPage with the retrieved information
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_book_title", title)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_book_author", author)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_book_year", year)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_book_description", description)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("scanned_book_type", type)
                    }
                )
            }

            composable("add_book_screen") {
                // Get the data from the scan if available
                val bookTitle = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("scanned_book_title") ?: ""
                val bookAuthor = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("scanned_book_author") ?: ""
                val bookYear = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("scanned_book_year") ?: ""
                val bookDescription = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("scanned_book_description") ?: ""
                val bookType = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("scanned_book_type") ?: ""

                // Clear the saved state handle after reading
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_book_title")
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_book_author")
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_book_year")
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_book_description")
                navController.currentBackStackEntry?.savedStateHandle?.remove<String>("scanned_book_type")

                AddPage(
                    navController = navController,
                    context = LocalContext.current,
                    initialTitle = bookTitle,
                    initialAuthor = bookAuthor,
                    initialYear = bookYear,
                    initialDescription = bookDescription,
                    initialCategory = bookType
                )
            }
        }
    }

}

@Composable
fun MainBottomBar(navController: NavHostController, currentRoute: String?) {
    val navItemList = listOf(
        NavItem("Home", Icons.Default.Home, "main"),
        NavItem("Search", Icons.Default.Search, "search"),
        NavItem("Add", Icons.Default.Add, "add_book_screen"),
        NavItem("Favourite", Icons.Default.Favorite, "favourite"),
        NavItem("Book", Icons.Default.Book, "bookUser")
    )

    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)

    // Determina l'indice selezionato basandosi sulla rotta corrente
    val selectedIndex = navItemList.indexOfFirst { it.route == currentRoute }
        .takeIf { it >= 0 } ?: 0

    NavigationBar(
        containerColor = backgroundColor
    ) {
        navItemList.forEachIndexed { index, navItem ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = {
                    // Naviga alla destinazione corrispondente
                    navController.navigate(navItem.route) {
                        // Evita di aggiungere la stessa destinazione ripetutamente allo stack
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Evita più copie della stessa destinazione
                        launchSingleTop = true
                        // Ripristina lo stato se precedentemente salvato
                        restoreState = true
                    }
                },
                icon = {
                    Icon(imageVector = navItem.icon, contentDescription = navItem.label)
                },
                label = {
                    Text(text = navItem.label, color = textColor)
                }
            )
        }
    }
}

// Componente separato per la TopBar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(navController: NavHostController, authViewModel: AuthViewModel) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var unreadNotifications by remember { mutableIntStateOf(0) }
    var showNotificationPopup by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFFB4E4E8)
    val textColor = Color(0xFF333333)
    val notificationcolor = Color(0xFFF5F5F5)
    var userId = "";
    var user by remember { mutableStateOf<User?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()
            db.collection("notifications")
                .whereEqualTo("recipientId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val notificationsList = snapshot.documents.mapNotNull { doc ->
                            val notification = doc.toObject(Notification::class.java)
                            notification?.copy(id = doc.id)
                        }
                        notifications = notificationsList
                        unreadNotifications = notificationsList.count { !it.isRead }
                    }
                }
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        user = document.toObject(User::class.java)
                    }
                }
                .addOnFailureListener { e ->
                    errorMessage = "Error: ${e.message}"
                    showErrorDialog = true
                }
        }
    }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = primaryColor,
            titleContentColor = Color.Black,
        ),
        title = {
            Text("EnjoyBooks", color = textColor)
        },
        actions = {
            IconButton(
                onClick = {
                    showNotificationPopup = true
                    if (unreadNotifications > 0) {
                        markNotificationsAsRead()
                        unreadNotifications = 0
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.Black
                    )
                    if (unreadNotifications > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x = 5.dp, y = (-5).dp)
                                .clip(CircleShape)
                                .background(notificationcolor)
                        ) {
                            Text(
                                text = unreadNotifications.toString(),
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            //se sono admin vedo questo
            if (user?.role == "admin"){
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { navController.navigate("admin") },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Admin",
                        tint = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            // Settings icon
            IconButton(
                onClick = { navController.navigate("profile") },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Profile icon
            IconButton(
                onClick = { navController.navigate("userDetails/${userId}") },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Logout button
            IconButton(
                onClick = { authViewModel.signout() },
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Output,
                    contentDescription = "Logout",
                    tint = Color.Black
                )
            }

        }
    )

    // Dialog per le notifiche
    if (showNotificationPopup) {
        Dialog(onDismissRequest = { showNotificationPopup = false }) {
            Surface(
                modifier = Modifier.width(320.dp),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textColor
                            )
                        }

                        IconButton(
                            onClick = { showNotificationPopup = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No notifications",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        LazyColumn {
                            items(notifications.size) { index ->
                                val notification = notifications[index]
                                com.example.enjoybook.utils.NotificationItem(
                                    notification = notification,
                                    primaryColor = primaryColor,
                                    textColor = textColor,
                                    errorColor = errorColor,
                                    onAccept = { handleAcceptLoanRequest(notification) },
                                    onReject = { handleRejectLoanRequest(notification) },
                                    onDelete = { deleteNotification(notification.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Estendi la classe NavItem per includere la rotta
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)