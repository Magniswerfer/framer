package com.example.framer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AspectRatioItem(ratio: String, color: Color, onClick: () -> Unit) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(4.dp)
                                .background(color, RoundedCornerShape(16.dp))
                                .clickable(onClick = onClick)
                                .padding(12.dp),
                contentAlignment = Alignment.Center
        ) {
                Text(
                        ratio.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (color == Color.Yellow) Color.Black else Color.White
                )
        }
}
