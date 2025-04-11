package com.example.framer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun HueSlider(value: Float, onValueChange: (Float) -> Unit, currentColor: Color) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(32.dp)
                                .background(
                                        brush =
                                                Brush.horizontalGradient(
                                                        colors =
                                                                listOf(
                                                                        Color(1f, 0f, 0f), // Red
                                                                        Color(1f, 1f, 0f), // Yellow
                                                                        Color(0f, 1f, 0f), // Green
                                                                        Color(0f, 1f, 1f), // Cyan
                                                                        Color(0f, 0f, 1f), // Blue
                                                                        Color(
                                                                                1f,
                                                                                0f,
                                                                                1f
                                                                        ), // Magenta
                                                                        Color(
                                                                                1f,
                                                                                0f,
                                                                                0f
                                                                        ) // Red again
                                                                )
                                                ),
                                        shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 4.dp)
        ) {
                Slider(
                        value = value,
                        onValueChange = onValueChange,
                        valueRange = 0f..360f,
                        colors =
                                SliderDefaults.colors(
                                        thumbColor = currentColor,
                                        activeTrackColor = Color.Transparent,
                                        inactiveTrackColor = Color.Transparent
                                ),
                        modifier = Modifier.padding(vertical = 0.dp)
                )
        }
}

@Composable
fun SaturationSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        currentColor: Color,
        hue: Float
) {
        val gray = Color(0.5f, 0.5f, 0.5f)
        val fullColor = com.example.framer.data.model.HSLColor(hue, 1f, 0.5f).toColor()

        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(32.dp)
                                .background(
                                        brush =
                                                Brush.horizontalGradient(
                                                        colors = listOf(gray, fullColor)
                                                ),
                                        shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 4.dp)
        ) {
                Slider(
                        value = value,
                        onValueChange = onValueChange,
                        valueRange = 0f..1f,
                        colors =
                                SliderDefaults.colors(
                                        thumbColor = currentColor,
                                        activeTrackColor = Color.Transparent,
                                        inactiveTrackColor = Color.Transparent
                                ),
                        modifier = Modifier.padding(vertical = 0.dp)
                )
        }
}

@Composable
fun LightnessSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        currentColor: Color,
        hue: Float,
        saturation: Float
) {
        val black = Color(0f, 0f, 0f)
        val fullColor = com.example.framer.data.model.HSLColor(hue, saturation, 0.5f).toColor()
        val white = Color(1f, 1f, 1f)

        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(32.dp)
                                .background(
                                        brush =
                                                Brush.horizontalGradient(
                                                        colors = listOf(black, fullColor, white)
                                                ),
                                        shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 4.dp)
        ) {
                Slider(
                        value = value,
                        onValueChange = onValueChange,
                        valueRange = 0f..1f,
                        colors =
                                SliderDefaults.colors(
                                        thumbColor = currentColor,
                                        activeTrackColor = Color.Transparent,
                                        inactiveTrackColor = Color.Transparent
                                ),
                        modifier = Modifier.padding(vertical = 0.dp)
                )
        }
}
