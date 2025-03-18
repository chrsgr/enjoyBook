package com.example.enjoybook

import android.content.Context
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.enjoybook.data.Notification
import com.example.enjoybook.pages.AddPage
import com.example.enjoybook.pages.BookPage
import com.example.enjoybook.pages.FavouritePage
import com.example.enjoybook.pages.HomePage
import com.example.enjoybook.pages.SearchPage
import com.example.enjoybook.theme.errorColor
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.example.enjoybook.viewModel.SearchViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel, searchViewModel: SearchViewModel){

    val authState = authViewModel.authState.observeAsState()
    val notificationcolor = Color(0xFFF5F5F5)

    val context = LocalContext.current

    val currentUser = FirebaseAuth.getInstance().currentUser
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var unreadNotifications by remember { mutableIntStateOf(0) }
    var showNotificationPopup by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
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
        }
    }


    val navItemList = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Search", Icons.Default.Search),
        NavItem("Add", Icons.Default.Add),
        NavItem("Favourite", Icons.Default.Favorite),
        NavItem("Book", Icons.Default.Book)
    )



    var selectedIndex by remember {
        mutableIntStateOf(0)
    }

    val primaryColor = Color(0xFFB4E4E8)
    val backgroundColor = Color(0xFFF5F5F5)
    val textColor = Color(0xFF333333)



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
                containerColor = backgroundColor
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
                                        NotificationItem(
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
@Composable
fun NotificationItem(
    notification: Notification,
    primaryColor: Color,
    textColor: Color,
    errorColor: Color,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = textColor,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        notification.timestamp,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString(),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Delete button
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete notification",
                    tint = Color.Red
                )
            }
        }

        if (notification.type == "LOAN_REQUEST") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onReject() },
                    colors = ButtonDefaults.textButtonColors(contentColor = errorColor)
                ) {
                    Text("Reject")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onAccept() },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Accept", color = Color.White)
                }
            }
        }

        Divider(
            color = Color.LightGray.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
private fun markNotificationsAsRead() {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return
    val db = FirebaseFirestore.getInstance()

    db.collection("notifications")
        .whereEqualTo("recipientId", currentUser.uid)
        .whereEqualTo("isRead", false)
        .get()
        .addOnSuccessListener { documents ->
            val batch = db.batch()
            for (document in documents) {
                batch.update(document.reference, "isRead", true)
            }
            batch.commit().addOnSuccessListener {
            }
        }
}
private fun handleAcceptLoanRequest(notification: Notification) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    db.collection("books").document(notification.bookId)
        .update("isAvailable", false)
        .addOnSuccessListener {
            Log.d("Notifications", "Book marked as unavailable")

            val requesterId = notification.senderId

            // Create a new borrow record
            val borrowData = hashMapOf(
                "bookId" to notification.bookId,
                "ownerId" to currentUser?.uid,
                "borrowerId" to requesterId,
                "borrowDate" to System.currentTimeMillis(),
                "status" to "accepted",
                "title" to notification.title
            )

            db.collection("borrows").add(borrowData)
                .addOnSuccessListener { documentReference ->
                    Log.d("Notifications", "Borrow record created with ID: ${documentReference.id}")

                    val confirmationNotification = Notification(
                        recipientId = requesterId,
                        senderId = currentUser?.uid ?: "",
                        message = "La tua richiesta per il libro '${notification.title}' è stata accettata",
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        type = "LOAN_ACCEPTED",
                        bookId = notification.bookId,
                        title = notification.title
                    )

                    db.collection("notifications").add(confirmationNotification)
                        .addOnSuccessListener {
                            Log.d("Notifications", "Acceptance notification sent")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Notifications", "Error sending acceptance notification", e)
                        }

                    db.collection("notifications").document(notification.id)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("Notifications", "Original notification deleted")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Notifications", "Error deleting notification", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Notifications", "Error creating borrow record", e)
                }
        }
        .addOnFailureListener { e ->
            Log.e("Notifications", "Error updating book status", e)
        }
}

private fun handleRejectLoanRequest(notification: Notification) {
    val db = FirebaseFirestore.getInstance()

    val requesterId = notification.senderId

    val rejectionNotification = Notification(
        recipientId = requesterId,
        senderId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
        message = "La tua richiesta per il libro '${notification.title}' è stata rifiutata",
        timestamp = System.currentTimeMillis(),
        isRead = false,
        type = "LOAN_REJECTED",
        bookId = notification.bookId,
        title = notification.title
    )

    db.collection("notifications").add(rejectionNotification)
        .addOnSuccessListener {
            Log.d("Notifications", "Rejection notification sent")

            db.collection("notifications").document(notification.id)
                .delete()
                .addOnSuccessListener {
                    Log.d("Notifications", "Original notification deleted")
                }
                .addOnFailureListener { e ->
                    Log.e("Notifications", "Error deleting notification", e)
                }
        }
        .addOnFailureListener { e ->
            Log.e("Notifications", "Error sending rejection notification", e)
        }
}

private fun deleteNotification(notificationId: String) {
    val db = FirebaseFirestore.getInstance()
    db.collection("notifications").document(notificationId)
        .delete()
        .addOnSuccessListener {
            Log.d("Notifications", "Notification deleted successfully")
        }
        .addOnFailureListener { e ->
            Log.e("Notifications", "Error deleting notification", e)
        }
}