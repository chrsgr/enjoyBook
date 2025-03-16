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
import com.example.enjoybook.pages.LoginPage
import com.example.enjoybook.pages.ProfilePage
import com.example.enjoybook.pages.SearchPage
import com.example.enjoybook.pages.SignupPage
import com.example.enjoybook.viewModel.AuthViewModel
import com.example.enjoybook.viewModel.SearchViewModel


@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel, searchViewModel: SearchViewModel){
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext

    NavHost(navController = navController, startDestination = "login") {
        composable("login"){
            LoginPage(modifier, navController, authViewModel)
        }
        composable("signup"){
            SignupPage(modifier, navController, authViewModel)
        }
        composable("main"){
            MainPage(modifier, navController, authViewModel, searchViewModel)
        }
        composable("home"){
            HomePage(modifier, navController, authViewModel)
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
            ProfilePage(modifier, navController, authViewModel)
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
        composable("bookUser"){
            BookPage(navController, authViewModel)
        }

        composable("library"){
            LibraryPage(navController, authViewModel)
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
    }

}