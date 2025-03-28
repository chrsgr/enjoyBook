package com.example.enjoybook.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BookInfoItem(label: String, value: String, textColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Display label on its own line
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        // Small spacer between label and value
        Spacer(modifier = Modifier.height(2.dp))

        // Display value on its own line
        Text(
            text = value,
            color = textColor
        )
    }
}