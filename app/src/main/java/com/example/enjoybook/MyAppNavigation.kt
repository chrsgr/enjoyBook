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
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.enjoybook.pages.*


@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel, searchViewModel: SearchViewModel){
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    val scannedTitle = remember { mutableStateOf("") }
    val scannedAuthor = remember { mutableStateOf("") }
    val scannedYear = remember { mutableStateOf("") }
    val scannedDescription = remember { mutableStateOf("") }
    val scannedCategory = remember { mutableStateOf("") }

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
            HomePage(navController, authViewModel)
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
        composable("bookUser"){
            BookPage(navController)
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