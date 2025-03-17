package com.example.enjoybook

import android.R
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.enjoybook.data.NavItem
import com.example.enjoybook.pages.AddPage
import com.example.enjoybook.pages.BookPage
import com.example.enjoybook.pages.FavouritePage
import com.example.enjoybook.pages.HomePage
import com.example.enjoybook.pages.NotificationItem
import com.example.enjoybook.pages.SearchPage
import com.example.enjoybook.theme.errorColor
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.example.enjoybook.viewModel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel, searchViewModel: SearchViewModel){

    val authState = authViewModel.authState.observeAsState()
    val notificationcolor = Color(0xFFF5F5F5)

    val context = LocalContext.current

    val navItemList = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Search", Icons.Default.Search),
        NavItem("Add", Icons.Default.Add),
        NavItem("Favourite", Icons.Default.Favorite),
        NavItem("Book", Icons.Default.Book)
    )
    val notifications = remember {
        listOf(
            "Richiesta di affitto per libro 'Il nome della rosa'",
            "Richiesta di affitto per libro 'La Divina Commedia'",
            "Richiesta di affitto per libro 'I Promessi Sposi'"
        )
    }


    var selectedIndex by remember {
        mutableIntStateOf(0)
    }

    // Define your colors
    val primaryColor = Color(0xFFB4E4E8)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)

    var unreadNotifications by remember { mutableStateOf(3) }
    var showNotificationPopup by remember { mutableStateOf(false) }

    LaunchedEffect(authState.value){
        when(authState.value){
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.Black,
                ),
                title = {
                    Text("EnjoyBooks", color = textColor)
                },
                actions = {
                    // Notifications with badge
                    IconButton(
                        onClick = {
                            showNotificationPopup = true
                            if (unreadNotifications > 0) {
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

                    Spacer(modifier = Modifier.width(8.dp))

                    // Profile icon
                    IconButton(
                        onClick = { navController.navigate("profile") },
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
        },
        bottomBar = {
            NavigationBar(
                containerColor = backgroundColor // Background color for bottom bar
            ){
                navItemList.forEachIndexed { index, navItem ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = {
                            selectedIndex = index
                        },
                        icon = {
                            Icon(imageVector = navItem.icon, contentDescription = "Icon")

                        },


                        label = {
                            Text(text = navItem.label, color = textColor)
                        }
                    )
                }
            }

            // Notification popup
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

                            // List of notifications
                            notifications.forEach { notification ->
                                NotificationItem(
                                    message = notification,
                                    primaryColor = primaryColor,
                                    textColor = textColor,
                                    errorColor = errorColor
                                )
                            }
                        }
                    }
                }
            }
        }
    ){ innerPadding ->
        ContentScreen(
            modifier = Modifier.padding(innerPadding),
            selectedIndex,
            navController,
            authViewModel,
            context,
            searchViewModel
        )
    }
}

@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex : Int, navController: NavController, authViewModel: AuthViewModel, context : Context, searchViewModel: SearchViewModel){
    when(selectedIndex){
        0 -> HomePage(navController, authViewModel)
        1 -> SearchPage(searchViewModel, navController)
        2 -> AddPage(navController, context)
        3 -> FavouritePage(navController)
        4 -> BookPage(navController)
    }
}

