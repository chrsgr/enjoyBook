package com.example.enjoybook.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon

import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.hoverable
import com.example.enjoybook.viewModel.AuthViewModel


@Composable
fun BookPage(navController: NavController, authViewModel: AuthViewModel) {
    var expanded by remember { mutableStateOf(true) } // Start expanded by default

    // Hover state for menu items
    var isLibraryHovered by remember { mutableStateOf(false) }
    var isAddBookHovered by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.5f,
        animationSpec = tween(durationMillis = 200),
        label = "alphaAnimation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable {
                        expanded = false
                        navController.navigate("home")
                    }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    navController.navigate("home")
                },
                modifier = Modifier.width(200.dp)
            ) {
                DropdownMenuItem(
                    onClick = {
                        navController.navigate("library")
                        expanded = false
                    },
                    modifier = Modifier
                        .hoverable(
                            interactionSource = remember { MutableInteractionSource() },
                        )
                        .background(if (isLibraryHovered)  Color(0xFFA7E8EB).copy(alpha = animatedAlpha)  else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Library",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Your Library")
                }

                Divider()

                DropdownMenuItem(
                    onClick = {
                        navController.navigate("listBookAdd")
                        expanded = false
                    },
                    modifier = Modifier
                        .hoverable(
                            interactionSource = remember { MutableInteractionSource() },
                        )
                        .background(if (isAddBookHovered) Color(0xFFA7E8EB).copy(alpha = animatedAlpha)else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Book",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Your Add Book")
                }
            }
        }
    }
}
