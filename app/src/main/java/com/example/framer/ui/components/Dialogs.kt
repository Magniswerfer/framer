package com.example.framer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.framer.data.model.AspectRatio
import com.example.framer.data.model.HSLColor
import com.example.framer.data.model.QualityOption
import com.example.framer.ui.theme.*

@Composable
fun QualityDialog(
        onDismiss: () -> Unit,
        onQualitySelected: (Int) -> Unit,
        qualityOptions: List<QualityOption>
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = Black
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("SELECT QUALITY", style = MaterialTheme.typography.titleMedium, color = White)
                Spacer(modifier = Modifier.height(16.dp))

                qualityOptions.forEach { option ->
                    Button(
                            onClick = {
                                onDismiss()
                                onQualitySelected(option.quality)
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = Blue,
                                            contentColor = White
                                    ),
                            shape = RoundedCornerShape(24.dp)
                    ) { Text(option.name, color = White) }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("CANCEL", color = White)
                }
            }
        }
    }
}

@Composable
fun AspectRatioDialog(
        onDismiss: () -> Unit,
        onRatioSelected: (AspectRatio) -> Unit,
        aspectRatios: List<AspectRatio>,
        selectedRatio: AspectRatio
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = White
        ) {
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .border(2.dp, Yellow, RoundedCornerShape(24.dp))
                                    .padding(24.dp)
            ) {
                Text(
                        "SELECT ASPECT RATIO",
                        style = MaterialTheme.typography.titleMedium,
                        color = Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                aspectRatios.forEachIndexed { index, ratio ->
                    val pillColor =
                            when (index % 3) {
                                0 -> Red
                                1 -> Yellow
                                else -> Blue
                            }

                    Box(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(pillColor, RoundedCornerShape(16.dp))
                                            .clickable {
                                                onRatioSelected(ratio)
                                                onDismiss()
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Text(
                                ratio.name.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (pillColor == Yellow) Black else White,
                                modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = Black) }
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
        onDismiss: () -> Unit,
        currentColor: Color,
        onColorChange: (Color) -> Unit,
        hslColor: HSLColor,
        onHSLChange: (HSLColor) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = White
        ) {
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .border(2.dp, Yellow, RoundedCornerShape(24.dp))
                                    .padding(24.dp)
            ) {
                Text("SELECT COLOR", style = MaterialTheme.typography.titleMedium, color = Black)
                Spacer(modifier = Modifier.height(16.dp))

                // HSL Sliders with visual feedback
                Text("Hue", color = Black)
                Spacer(modifier = Modifier.height(4.dp))
                HueSlider(
                        value = hslColor.hue,
                        onValueChange = { newValue ->
                            val newHSL = hslColor.copy(hue = newValue)
                            onHSLChange(newHSL)
                            onColorChange(newHSL.toColor())
                        },
                        currentColor = currentColor
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Saturation", color = Black)
                Spacer(modifier = Modifier.height(4.dp))
                SaturationSlider(
                        value = hslColor.saturation,
                        onValueChange = { newValue ->
                            val newHSL = hslColor.copy(saturation = newValue)
                            onHSLChange(newHSL)
                            onColorChange(newHSL.toColor())
                        },
                        currentColor = currentColor,
                        hue = hslColor.hue
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Lightness", color = Black)
                Spacer(modifier = Modifier.height(4.dp))
                LightnessSlider(
                        value = hslColor.lightness,
                        onValueChange = { newValue ->
                            val newHSL = hslColor.copy(lightness = newValue)
                            onHSLChange(newHSL)
                            onColorChange(newHSL.toColor())
                        },
                        currentColor = currentColor,
                        hue = hslColor.hue,
                        saturation = hslColor.saturation
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Preview box
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(48.dp)
                                        .background(currentColor)
                                        .border(1.dp, Black)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("CANCEL", color = Black) }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("OK", color = Black) }
                }
            }
        }
    }
}
