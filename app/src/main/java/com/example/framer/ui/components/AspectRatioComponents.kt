package com.example.framer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.framer.data.model.AspectRatio
import com.example.framer.ui.theme.*

@Composable
fun AspectRatioItem(ratio: AspectRatio, color: Color, onClick: () -> Unit) {
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
                ratio.name.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (color == Yellow) Black else White
        )
    }
}
