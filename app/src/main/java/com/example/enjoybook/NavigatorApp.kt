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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.enjoybook.admin.AdminListReports
import com.example.enjoybook.admin.AdminPanel
import com.example.enjoybook.admin.AdminUsersBanned
import com.example.enjoybook.auth.ForgotPasswordPage
import com.example.enjoybook.auth.InsertUsernamePage
import com.example.enjoybook.data.Message
import com.example.enjoybook.data.NavItem
import com.example.enjoybook.data.Notification
import com.example.enjoybook.data.User
import com.example.enjoybook.pages.*
import com.example.enjoybook.theme.errorColor
import com.example.enjoybook.utils.NotificationsDialog
import com.example.enjoybook.utils.markNotificationsAsRead
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.BooksViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel, searchViewModel: SearchViewModel, booksViewModel: BooksViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val authState = authViewModel.authState.observeAsState()

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    val routesWithoutBars = listOf("login", "signup", "forgotpass", "usernameSetup")

    val shouldShowBars = remember(currentRoute) {
        currentRoute != null && !routesWithoutBars.any { route ->
            currentRoute.startsWith(route)
        }
    }

    LaunchedEffect(authState.value) {
        when(authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            is AuthState.WaitingForUsername -> {
                navController.navigate("usernameSetup") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            is AuthState.Authenticated -> {
                if (currentRoute == "login" || currentRoute == "usernameSetup") {
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
            composable("login") {
                LoginPage(modifier, navController, authViewModel)
            }

            composable("forgotpass") {
                ForgotPasswordPage(modifier, navController, authViewModel)
            }

            composable("usernameSetup") {
                InsertUsernamePage(modifier, navController, authViewModel)
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

            composable("adminBanned"){
                AdminUsersBanned(navController, authViewModel)
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
                ProfilePage(navController, authViewModel)
            }

            composable("favourite"){
                FavouritePage(navController)
            }

            composable("allBooks"){
                AllFeaturedBooksPage(navController, authViewModel, booksViewModel)
            }

            composable(
                route = "bookDetails/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType })
            ) {
                val bookId = it.arguments?.getString("bookId") ?: ""
                BookDetails(navController, authViewModel, bookId)
            }


            composable("userDetails/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                    ?: return@composable

                Log.d("Navigation", "UserId: ${userId}")

                UserDetails(
                    navController = navController,
                    authViewModel = authViewModel,
                    userId = userId
                )
            }

            composable("followers/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                FollowersScreen(navController, userId)
            }
            composable("following/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                FollowingScreen(navController, userId)
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

            composable("chatList") {
                ChatListScreen(navController, authViewModel)
            }

            composable("messaging/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")
                    ?: return@composable

                UserMessagingScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    targetUserId = userId
                )
            }



            composable("filteredbooks/{category}", arguments = listOf(navArgument("category") { type = NavType.StringType })){
                    backStackEntry ->
                val category = backStackEntry.arguments?.getString("category") ?: ""
                Log.d("NavHost", "Navigato a filteredbooks con categoria: $category")
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

    val selectedIndex = navItemList.indexOfFirst { it.route == currentRoute }
        .takeIf { it >= 0 } ?: 0

    NavigationBar(
        containerColor = backgroundColor
    ) {
        navItemList.forEachIndexed { index, navItem ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = {
                    navController.navigate(navItem.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(navController: NavHostController, authViewModel: AuthViewModel) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }

    var unreadNotifications by remember { mutableIntStateOf(0) }
    var showNotificationPopup by remember { mutableStateOf(false) }
    val primaryColor = Color(0xFFB4E4E8)
    val textColor = Color(0xFF333333)
    val notificationcolor = Color(0xFFF5F5F5)
    var userId by remember { mutableStateOf("") }
    var user by remember { mutableStateOf<User?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var unreadMessages by remember { mutableIntStateOf(0) }


    fun removeNotificationLocally(notificationId: String) {
        notifications = notifications.filter { it.id != notificationId }
        unreadNotifications = notifications.count { !it.isRead }
    }


    fun markMessagesAsRead() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("messages")
            .whereEqualTo("receiverId", currentUser.uid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (document in documents) {
                    batch.update(document.reference, "isRead", true)
                }
                batch.commit()
            }
    }


    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            userId = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            db.collection("messages")
                .whereEqualTo("receiverId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Messages", "Error fetching messages", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val messagesList = snapshot.documents.mapNotNull { doc ->
                            val message = doc.toObject(Message::class.java)
                            message?.copy(id = doc.id)
                        }

                        messages = messagesList
                        unreadMessages = messagesList.count { !it.read }
                    }
                }


            Log.d("Navigation", "Current user: ${currentUser.uid}, ${userId}")
            db.collection("notifications")
                .whereEqualTo("recipientId", currentUser.uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Notifications", "Error fetching notifications", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        Log.d("Navigation", "Current user in the if: ${currentUser.uid}, ${userId}")
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
                        /*user = User(
                            userId = document.id,
                            name = document.getString("name") ?: "",
                            surname = document.getString("surname") ?: "",
                            username = document.getString("username") ?: "",
                            email = document.getString("email") ?: "",
                            phone = document.getString("phone") ?: "",
                            role = document.getString("role") ?: "",
                            isBanned = document.getBoolean("isBanned") ?: null,
                            isPrivate = document.getBoolean("isPrivate") ?: null,
                            profilePictureUrl = document.getString("profilePictureUrl") ?: ""
                        )*/
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
                    navController.navigate("chatList")
                    if (unreadMessages > 0) {
                        markMessagesAsRead()
                        unreadMessages = 0
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Chat list",
                        tint = Color.Black
                    )
                    if (unreadMessages > 0) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(18.dp)
                                .offset(x =5.dp, y = (-5).dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Text(
                                text = unreadMessages.toString(),
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // NOTIFICATIONS BUTTON
            IconButton(
                onClick = {
                    showNotificationPopup = true
                    if (unreadNotifications > 0) {
                        markNotificationsAsRead()
                        unreadNotifications = 0
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 4.dp)
            ){
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
                    onClick = {
                        navController.navigate("admin") {
                            launchSingleTop = true
                        }},
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

            // Profile icon
            IconButton(
                onClick = {
                    navController.navigate("userDetails/${userId}") {
                        launchSingleTop = true
                    }},
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
        NotificationsDialog(
            notifications = notifications,
            showNotificationPopup = showNotificationPopup,
            onDismiss = { showNotificationPopup = false },
            navController = navController,
            primaryColor = primaryColor,
            textColor = textColor,
            errorColor = errorColor,
            removeNotificationLocally = { notificationId ->
                removeNotificationLocally(notificationId)
            }
        )
    }
}


