package com.example.enjoybook

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.enjoybook.data.NavItem
import com.example.enjoybook.pages.AddPage
import com.example.enjoybook.pages.BookPage
import com.example.enjoybook.pages.FavouritePage
import com.example.enjoybook.pages.HomePage
import com.example.enjoybook.pages.ProfilePage
import com.example.enjoybook.pages.SearchPage
import com.example.enjoybook.viewModel.AuthState
import com.example.enjoybook.viewModel.AuthViewModel
import com.example.enjoybook.viewModel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel, searchViewModel: SearchViewModel){

    val authState = authViewModel.authState.observeAsState()

    val context = LocalContext.current

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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("EnjoyBooks")
                },
                actions = {
                    IconButton(onClick = { authViewModel.signout() }) {
                        Icon(imageVector = Icons.Default.Output, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar{
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
                            Text(text = navItem.label)
                        }
                    )
                }
            }
        }
    ){ innerPadding ->
        ContentScreen(modifier = Modifier.padding(innerPadding), selectedIndex, navController, authViewModel, context, searchViewModel)
    }
}

@Composable
fun ContentScreen(modifier: Modifier = Modifier, selectedIndex : Int, navController: NavController, authViewModel: AuthViewModel, context : Context, searchViewModel: SearchViewModel){
    when(selectedIndex){
        0 -> HomePage(modifier, navController, authViewModel)
        1 -> SearchPage(searchViewModel, navController)
        2 -> AddPage(modifier, navController, context)
        3 -> FavouritePage(navController)
        4 -> BookPage( navController, authViewModel)
    }
}

