package com.example.enjoybook.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

@Composable
fun ScrollableTextWithScrollbar(
    text: String,
    textColor: Color,
    scrollbarColor: Color,
    maxHeight: Dp = 120.dp
) {
    val scrollState = rememberScrollState()
    val shouldShowScrollbar = scrollState.maxValue > 0

    Box(
        modifier = Modifier
            .heightIn(max = maxHeight)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(end = if (shouldShowScrollbar) 16.dp else 0.dp)
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
        }

        if (shouldShowScrollbar) {
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd),
                scrollState = scrollState,
                color = scrollbarColor
            )
        }
    }
}

@Composable
fun VerticalScrollbar(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    color: Color,
    scrollbarWidth: Dp = 6.dp
) {
    val scrollbarHeight = scrollState.maxValue.toFloat().let { maxValue ->
        if (maxValue > 0) 120.dp else 0.dp
    }
    val thumbHeight = scrollbarHeight * 0.2f

    Box(
        modifier = modifier
            .width(scrollbarWidth)
            .height(scrollbarHeight)
            .background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbHeight)
                .offset(y = (scrollState.value / scrollState.maxValue.toFloat()) * (scrollbarHeight - thumbHeight))
                .background(color, shape = RoundedCornerShape(3.dp))
        )
    }
}
