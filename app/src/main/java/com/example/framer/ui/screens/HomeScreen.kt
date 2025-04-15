package com.example.framer.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.framer.R
import com.example.framer.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Data class to represent aspect ratios
data class AspectRatio(val name: String, val ratio: Float)

// Data class to represent quality options
data class QualityOption(val name: String, val quality: Int)

// HSL Color class and conversion functions
data class HSLColor(
        val hue: Float, // 0-360
        val saturation: Float, // 0-1
        val lightness: Float // 0-1
) {
        fun toColor(): Color {
                val c = (1f - abs(2f * lightness - 1f)) * saturation
                val x = c * (1f - abs((hue / 60f) % 2f - 1f))
                val m = lightness - c / 2f

                val (r1, g1, b1) =
                        when {
                                hue < 60f -> Triple(c, x, 0f)
                                hue < 120f -> Triple(x, c, 0f)
                                hue < 180f -> Triple(0f, c, x)
                                hue < 240f -> Triple(0f, x, c)
                                hue < 300f -> Triple(x, 0f, c)
                                else -> Triple(c, 0f, x)
                        }

                return Color(red = r1 + m, green = g1 + m, blue = b1 + m)
        }
}

fun Color.toHSLColor(): HSLColor {
        val r = red
        val g = green
        val b = blue

        val max = max(max(r, g), b)
        val min = min(min(r, g), b)
        val d = max - min

        val h =
                when {
                        d == 0f -> 0f
                        max == r -> 60f * ((g - b) / d + 6f) % 360f
                        max == g -> 60f * ((b - r) / d + 2f)
                        else -> 60f * ((r - g) / d + 4f)
                }

        val l = (max + min) / 2f
        val s =
                when {
                        l == 0f || l == 1f -> 0f
                        else -> d / (1f - abs(2f * l - 1f))
                }

        return HSLColor(h, s, l)
}

@Composable
private fun ColorPickerItem(color: Color, isSelected: Boolean, onClick: () -> Unit) {
        Box(
                modifier =
                        Modifier.size(48.dp)
                                .background(color, CircleShape)
                                .border(
                                        width = 2.dp,
                                        color =
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                )
                                .clickable(onClick = onClick)
        )
}

@Composable
private fun AspectRatioItem(ratio: AspectRatio, color: Color, onClick: () -> Unit) {
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

@Composable
private fun HueSlider(value: Float, onValueChange: (Float) -> Unit, currentColor: Color) {
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
private fun SaturationSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        currentColor: Color,
        hue: Float
) {
        val gray = Color(0.5f, 0.5f, 0.5f)
        val fullColor = HSLColor(hue, 1f, 0.5f).toColor()

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
private fun LightnessSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        currentColor: Color,
        hue: Float,
        saturation: Float
) {
        val black = Color(0f, 0f, 0f)
        val fullColor = HSLColor(hue, saturation, 0.5f).toColor()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var showAboutScreen by remember { mutableStateOf(false) }
        var showDrawer by remember { mutableStateOf(false) }
        val drawerWidth = 280.dp

        // Animation for drawer
        val drawerOffset by
                animateFloatAsState(
                        targetValue = if (showDrawer) 0f else 1f,
                        animationSpec = tween(durationMillis = 300),
                        label = "drawerOffset"
                )

        // Add bitmap cache
        val bitmapCache = remember { mutableMapOf<Uri, Bitmap>() }

        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var frameColor by remember { mutableStateOf(White) }
        var frameThickness by remember { mutableStateOf(20.dp) }
        var ratioExpanded by remember { mutableStateOf(false) }
        var showQualityDialog by remember { mutableStateOf(false) }
        var showColorPicker by remember { mutableStateOf(false) }

        // Debounced frame thickness for smoother updates
        val debouncedFrameThickness by
                remember(frameThickness) { derivedStateOf { frameThickness } }

        // Optimize bitmap loading with caching
        val loadedBitmap =
                remember(selectedImageUri) {
                        if (selectedImageUri != null) {
                                bitmapCache[selectedImageUri]
                                        ?: try {
                                                val bitmap =
                                                        if (Build.VERSION.SDK_INT >=
                                                                        Build.VERSION_CODES.P
                                                        ) {
                                                                val source =
                                                                        ImageDecoder.createSource(
                                                                                context.contentResolver,
                                                                                selectedImageUri!!
                                                                        )
                                                                ImageDecoder.decodeBitmap(source) {
                                                                        decoder,
                                                                        _,
                                                                        _ ->
                                                                        decoder.allocator =
                                                                                ImageDecoder
                                                                                        .ALLOCATOR_SOFTWARE
                                                                        decoder.isMutableRequired =
                                                                                true
                                                                }
                                                        } else {
                                                                @Suppress("DEPRECATION")
                                                                MediaStore.Images.Media.getBitmap(
                                                                        context.contentResolver,
                                                                        selectedImageUri!!
                                                                )
                                                        }
                                                bitmapCache[selectedImageUri!!] = bitmap
                                                bitmap
                                        } catch (e: Exception) {
                                                Toast.makeText(
                                                                context,
                                                                "Error loading image: ${e.message}",
                                                                Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                                null
                                        }
                        } else null
                }

        // Define available aspect ratios
        val aspectRatios =
                listOf(
                        AspectRatio("1:1", 1f),
                        AspectRatio("4:3", 4f / 3f),
                        AspectRatio("3:4", 3f / 4f),
                        AspectRatio("16:9", 16f / 9f),
                        AspectRatio("9:16", 9f / 16f)
                )
        var selectedAspectRatio by remember { mutableStateOf(aspectRatios[0]) }
        var selectedAspectRatioColor by remember { mutableStateOf(Red) }

        // Define quality options
        val qualityOptions =
                listOf(
                        QualityOption("FULL QUALITY", 100),
                        QualityOption("HIGH QUALITY", 80),
                        QualityOption("MEDIUM QUALITY", 60),
                        QualityOption("LOW QUALITY", 30)
                )

        val imagePicker =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? -> selectedImageUri = uri }

        // Request storage permission
        val permissionLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                        if (isGranted) {
                                showQualityDialog = true
                        } else {
                                Toast.makeText(
                                                context,
                                                "Storage permission required to save images",
                                                Toast.LENGTH_LONG
                                        )
                                        .show()
                        }
                }

        // VSCO-style typography
        val vscoTypography =
                MaterialTheme.typography.copy(
                        headlineSmall =
                                MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = FontFamily.SansSerif, // Closest to Helvetica
                                        color = MaterialTheme.colorScheme.onBackground
                                ),
                        bodyMedium =
                                MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.onBackground
                                ),
                        bodySmall =
                                MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.SansSerif,
                                        color = MaterialTheme.colorScheme.onBackground
                                )
                )

        // Add HSL state
        var hslColor by remember { mutableStateOf<HSLColor>(frameColor.toHSLColor()) }

        // Quality selection dialog
        if (showQualityDialog) {
                Dialog(onDismissRequest = { showQualityDialog = false }) {
                        Surface(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                shape = RoundedCornerShape(24.dp),
                                color = Black
                        ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                        Text(
                                                "SELECT QUALITY",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = White
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        qualityOptions.forEach { option ->
                                                Button(
                                                        onClick = {
                                                                showQualityDialog = false
                                                                if (selectedImageUri != null) {
                                                                        scope.launch {
                                                                                saveImage(
                                                                                        context,
                                                                                        selectedImageUri!!,
                                                                                        frameColor,
                                                                                        frameThickness,
                                                                                        selectedAspectRatio,
                                                                                        option.quality
                                                                                )
                                                                        }
                                                                } else {
                                                                        Toast.makeText(
                                                                                        context,
                                                                                        "Please select an image first",
                                                                                        Toast.LENGTH_SHORT
                                                                                )
                                                                                .show()
                                                                }
                                                        },
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(vertical = 4.dp),
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor = Blue,
                                                                        contentColor = White
                                                                ),
                                                        shape = RoundedCornerShape(24.dp)
                                                ) { Text(option.name, color = White) }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(
                                                onClick = { showQualityDialog = false },
                                                modifier = Modifier.align(Alignment.End)
                                        ) { Text("CANCEL", color = White) }
                                }
                        }
                }
        }

        // Color Picker Dialog
        if (showColorPicker) {
                Dialog(onDismissRequest = { showColorPicker = false }) {
                        Surface(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                shape = RoundedCornerShape(24.dp),
                                color = White
                        ) {
                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .border(
                                                                2.dp,
                                                                Yellow,
                                                                RoundedCornerShape(24.dp)
                                                        )
                                                        .padding(24.dp)
                                ) {
                                        Text(
                                                "SELECT COLOR",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Black
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // HSL Sliders with visual feedback
                                        Text("Hue", color = Black)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        HueSlider(
                                                value = hslColor.hue,
                                                onValueChange = { newValue ->
                                                        hslColor = hslColor.copy(hue = newValue)
                                                        frameColor = hslColor.toColor()
                                                },
                                                currentColor = frameColor
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Saturation", color = Black)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        SaturationSlider(
                                                value = hslColor.saturation,
                                                onValueChange = { newValue ->
                                                        hslColor =
                                                                hslColor.copy(saturation = newValue)
                                                        frameColor = hslColor.toColor()
                                                },
                                                currentColor = frameColor,
                                                hue = hslColor.hue
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Lightness", color = Black)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LightnessSlider(
                                                value = hslColor.lightness,
                                                onValueChange = { newValue ->
                                                        hslColor =
                                                                hslColor.copy(lightness = newValue)
                                                        frameColor = hslColor.toColor()
                                                },
                                                currentColor = frameColor,
                                                hue = hslColor.hue,
                                                saturation = hslColor.saturation
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Preview box
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(48.dp)
                                                                .background(frameColor)
                                                                .border(1.dp, Black)
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                        ) {
                                                TextButton(onClick = { showColorPicker = false }) {
                                                        Text("CANCEL", color = Black)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                TextButton(onClick = { showColorPicker = false }) {
                                                        Text("OK", color = Black)
                                                }
                                        }
                                }
                        }
                }
        }

        // Aspect Ratio Dialog
        if (ratioExpanded) {
                Dialog(onDismissRequest = { ratioExpanded = false }) {
                        Surface(
                                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                shape = RoundedCornerShape(24.dp),
                                color = White
                        ) {
                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .border(
                                                                2.dp,
                                                                Yellow,
                                                                RoundedCornerShape(24.dp)
                                                        )
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
                                                                        .background(
                                                                                pillColor,
                                                                                RoundedCornerShape(
                                                                                        16.dp
                                                                                )
                                                                        )
                                                                        .clickable {
                                                                                selectedAspectRatio =
                                                                                        ratio
                                                                                selectedAspectRatioColor =
                                                                                        pillColor
                                                                                ratioExpanded =
                                                                                        false
                                                                        }
                                                                        .padding(
                                                                                vertical = 12.dp,
                                                                                horizontal = 16.dp
                                                                        )
                                                ) {
                                                        Text(
                                                                ratio.name.uppercase(),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelLarge,
                                                                color =
                                                                        if (pillColor == Yellow)
                                                                                Black
                                                                        else White,
                                                                modifier =
                                                                        Modifier.align(
                                                                                Alignment.Center
                                                                        )
                                                        )
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                        ) {
                                                TextButton(onClick = { ratioExpanded = false }) {
                                                        Text("CANCEL", color = Black)
                                                }
                                        }
                                }
                        }
                }
        }

        Box(modifier = Modifier.fillMaxSize()) {
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                                        .systemBarsPadding()
                                        .navigationBarsPadding()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                ) {
                        // Logo and Title with Menu
                        Row(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(top = 24.dp, bottom = 4.dp)
                                                .padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Start
                                ) {
                                        Box(modifier = Modifier.size(56.dp).offset(x = (-24).dp)) {
                                                Image(
                                                        painter =
                                                                painterResource(
                                                                        id = R.drawable.framer_logo
                                                                ),
                                                        contentDescription = "Framer Logo",
                                                        modifier = Modifier.fillMaxSize()
                                                )
                                        }
                                        Text(
                                                text = "Framer",
                                                style =
                                                        MaterialTheme.typography.headlineLarge.copy(
                                                                fontFamily = FontFamily.SansSerif,
                                                                fontWeight = FontWeight.Medium,
                                                                fontSize = 32.sp
                                                        ),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.offset(x = (-24).dp)
                                        )
                                }

                                // Menu Button
                                IconButton(onClick = { showDrawer = true }) {
                                        Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Menu",
                                                tint = MaterialTheme.colorScheme.onBackground
                                        )
                                }
                        }

                        // Image Preview with Frame
                        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                val maxSquareSize = minOf(maxWidth, maxHeight)
                                val width: Float
                                val height: Float

                                if (selectedAspectRatio.ratio >= 1f) {
                                        // Wider than tall
                                        width = maxSquareSize.value
                                        height = width / selectedAspectRatio.ratio
                                } else {
                                        // Taller than wide
                                        height = maxSquareSize.value
                                        width = height * selectedAspectRatio.ratio
                                }

                                Box(
                                        modifier =
                                                Modifier.width(width.dp)
                                                        .height(height.dp)
                                                        .align(Alignment.Center)
                                                        .border(1.dp, Yellow)
                                                        .background(frameColor)
                                                        .padding(frameThickness)
                                ) {
                                        if (loadedBitmap != null) {
                                                Image(
                                                        bitmap = loadedBitmap.asImageBitmap(),
                                                        contentDescription = "Selected image",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                )
                                        } else {
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxSize().clickable {
                                                                        imagePicker.launch(
                                                                                "image/*"
                                                                        )
                                                                },
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(64.dp)
                                                                                .background(
                                                                                        Yellow,
                                                                                        CircleShape
                                                                                )
                                                                                .padding(8.dp),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default.Add,
                                                                        contentDescription =
                                                                                "Select image",
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        32.dp
                                                                                ),
                                                                        tint = Black
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons Row
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                Button(
                                        onClick = { imagePicker.launch("image/*") },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Blue,
                                                        contentColor = White
                                                ),
                                        shape = RoundedCornerShape(24.dp)
                                ) {
                                        Text(
                                                "SELECT IMAGE",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = White
                                        )
                                }

                                Button(
                                        onClick = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                                ) {
                                                        showQualityDialog = true
                                                } else {
                                                        permissionLauncher.launch(
                                                                Manifest.permission
                                                                        .WRITE_EXTERNAL_STORAGE
                                                        )
                                                }
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        enabled = selectedImageUri != null,
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = Red,
                                                        contentColor = White,
                                                        disabledContainerColor = DarkGray
                                                ),
                                        shape = RoundedCornerShape(24.dp)
                                ) {
                                        Row(
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Icon(
                                                        Icons.Default.Save,
                                                        contentDescription = "Save",
                                                        tint = White
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                        "SAVE",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = White
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Aspect Ratio Button
                        Button(
                                onClick = { ratioExpanded = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = selectedAspectRatioColor,
                                                contentColor =
                                                        if (selectedAspectRatioColor == Yellow)
                                                                Black
                                                        else White
                                        ),
                                shape = RoundedCornerShape(24.dp)
                        ) {
                                Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                selectedAspectRatio.name.uppercase(),
                                                style = MaterialTheme.typography.labelLarge
                                        )
                                        Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Select aspect ratio",
                                                tint =
                                                        if (selectedAspectRatioColor == Yellow)
                                                                Black
                                                        else White
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Frame Color Picker
                        Text(
                                "FRAME COLOR",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        // Cache the color list to prevent recreation on scroll
                        val colorList = remember {
                                listOf(
                                        Pair(White, "White"),
                                        Pair(Black, "Black"),
                                        Pair(Blue, "Blue"),
                                        Pair(Yellow, "Yellow"),
                                        Pair(Red, "Red"),
                                        Pair(Green, "Green"),
                                        Pair(Purple, "Purple"),
                                        Pair(Pink, "Pink"),
                                        Pair(Orange, "Orange"),
                                        Pair(Teal, "Teal"),
                                        Pair(Indigo, "Indigo")
                                )
                        }

                        LazyRow(
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                                items(items = colorList, key = { it.first.toArgb() }) {
                                        (color, name) ->
                                        ColorPickerItem(
                                                color = color,
                                                isSelected = frameColor == color,
                                                onClick = { frameColor = color }
                                        )
                                }

                                item(key = "custom") {
                                        Box(
                                                modifier =
                                                        Modifier.size(48.dp)
                                                                .background(White, CircleShape)
                                                                .border(
                                                                        width = 2.dp,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        shape = CircleShape
                                                                )
                                                                .clickable {
                                                                        showColorPicker = true
                                                                },
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Custom color",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Frame Thickness Slider
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        "FRAME THICKNESS",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                        "${(frameThickness.value / 10).toInt()}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                )
                        }

                        Slider(
                                value = frameThickness.value,
                                onValueChange = { frameThickness = it.dp },
                                valueRange = 5f..100f,
                                steps = 8,
                                colors =
                                        SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor =
                                                        MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor =
                                                        MaterialTheme.colorScheme.surface
                                        ),
                                modifier = Modifier.fillMaxWidth()
                        )
                }

                // Custom Drawer
                if (showDrawer) {
                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .clickable { showDrawer = false }
                                                .background(
                                                        MaterialTheme.colorScheme.scrim.copy(
                                                                alpha = 0.32f * (1f - drawerOffset)
                                                        )
                                                )
                        ) {
                                Box(
                                        modifier =
                                                Modifier.align(Alignment.CenterEnd)
                                                        .width(drawerWidth)
                                                        .fillMaxHeight()
                                                        .background(
                                                                MaterialTheme.colorScheme.background
                                                                        .copy(
                                                                                alpha =
                                                                                        1f -
                                                                                                drawerOffset
                                                                        )
                                                        )
                                                        .offset(
                                                                x =
                                                                        (drawerWidth.value *
                                                                                        drawerOffset)
                                                                                .dp
                                                        )
                                                        .clickable(enabled = false) {
                                                        } // Prevent click propagation
                                ) {
                                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                                // Close Button
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(bottom = 16.dp),
                                                        horizontalArrangement = Arrangement.End
                                                ) {
                                                        IconButton(
                                                                onClick = { showDrawer = false }
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default.Close,
                                                                        contentDescription =
                                                                                "Close drawer",
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onBackground
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        1f -
                                                                                                                drawerOffset
                                                                                        )
                                                                )
                                                        }
                                                }

                                                // Navigation Items
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(horizontal = 12.dp)
                                                                        .background(
                                                                                Red.copy(
                                                                                        alpha =
                                                                                                1f -
                                                                                                        drawerOffset
                                                                                ),
                                                                                RoundedCornerShape(
                                                                                        24.dp
                                                                                )
                                                                        )
                                                                        .clickable {
                                                                                showDrawer = false
                                                                                showAboutScreen =
                                                                                        true
                                                                        }
                                                                        .padding(
                                                                                vertical = 12.dp,
                                                                                horizontal = 16.dp
                                                                        )
                                                ) {
                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(12.dp),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Default.Info,
                                                                        contentDescription =
                                                                                "About",
                                                                        tint =
                                                                                White.copy(
                                                                                        alpha =
                                                                                                1f -
                                                                                                        drawerOffset
                                                                                )
                                                                )
                                                                Text(
                                                                        "About",
                                                                        color =
                                                                                White.copy(
                                                                                        alpha =
                                                                                                1f -
                                                                                                        drawerOffset
                                                                                ),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelLarge
                                                                )
                                                        }
                                                }

                                                // Spacer to push logo to bottom
                                                Spacer(modifier = Modifier.weight(1f))

                                                // Logo at bottom
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(bottom = 16.dp),
                                                        contentAlignment = Alignment.Center
                                                ) {
                                                        Box(modifier = Modifier.size(64.dp)) {
                                                                Image(
                                                                        painter =
                                                                                painterResource(
                                                                                        id =
                                                                                                R.drawable
                                                                                                        .framer_logo
                                                                                ),
                                                                        contentDescription =
                                                                                "Framer Logo",
                                                                        modifier =
                                                                                Modifier.fillMaxSize()
                                                                                        .alpha(
                                                                                                1f -
                                                                                                        drawerOffset
                                                                                        )
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }

        // Show About Screen
        if (showAboutScreen) {
                AboutScreen(onNavigateBack = { showAboutScreen = false })
        }
}

// Function to save the framed image
private suspend fun saveImage(
        context: Context,
        imageUri: Uri,
        frameColor: Color,
        frameThickness: androidx.compose.ui.unit.Dp,
        aspectRatio: AspectRatio,
        quality: Int
) {
        withContext(Dispatchers.IO) {
                try {
                        // Create a bitmap from the original image with reduced sample size
                        val originalBitmap =
                                try {
                                        val source =
                                                ImageDecoder.createSource(
                                                        context.contentResolver,
                                                        imageUri
                                                )
                                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                                decoder.isMutableRequired = true
                                        }
                                } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                                context,
                                                                "Error loading image: ${e.message}",
                                                                Toast.LENGTH_LONG
                                                        )
                                                        .show()
                                        }
                                        return@withContext
                                }

                        // Get original dimensions
                        val originalWidth = originalBitmap.width
                        val originalHeight = originalBitmap.height

                        // Determine the target aspect ratio from the selection
                        val targetAspectRatio = aspectRatio.ratio

                        // Calculate content dimensions with aspect ratio
                        val (contentWidth, contentHeight) =
                                if (originalWidth.toFloat() / originalHeight > targetAspectRatio) {
                                        // Original is wider than target, use height as reference
                                        Pair(
                                                (originalHeight * targetAspectRatio).toInt(),
                                                originalHeight
                                        )
                                } else {
                                        // Original is taller than target, use width as reference
                                        Pair(
                                                originalWidth,
                                                (originalWidth / targetAspectRatio).toInt()
                                        )
                                }

                        // Calculate frame thickness in pixels
                        val percentage = frameThickness.value / 400f
                        val frameThicknessPx =
                                (Math.min(contentWidth, contentHeight) * percentage).toInt()

                        // Create final dimensions including the frame
                        val finalWidth = contentWidth + (frameThicknessPx * 2)
                        val finalHeight = contentHeight + (frameThicknessPx * 2)

                        // Create the framed bitmap with the frame color
                        val framedBitmap =
                                Bitmap.createBitmap(
                                        finalWidth,
                                        finalHeight,
                                        Bitmap.Config.ARGB_8888
                                )
                        framedBitmap.eraseColor(frameColor.toArgb())

                        // Scale the original image to fit the inner area while maintaining its
                        // aspect ratio
                        val scaledWidth: Int
                        val scaledHeight: Int

                        val originalAspectRatio = originalWidth.toFloat() / originalHeight
                        if (originalAspectRatio > contentWidth.toFloat() / contentHeight) {
                                // Image is wider than inner area, scale by width
                                scaledWidth = contentWidth
                                scaledHeight = (contentWidth / originalAspectRatio).toInt()
                        } else {
                                // Image is taller than inner area, scale by height
                                scaledHeight = contentHeight
                                scaledWidth = (contentHeight * originalAspectRatio).toInt()
                        }

                        // Create scaled bitmap with bilinear filtering for better quality
                        val scaledBitmap =
                                Bitmap.createScaledBitmap(
                                        originalBitmap,
                                        scaledWidth,
                                        scaledHeight,
                                        true
                                )

                        // Center the scaled bitmap in the inner area
                        val x = frameThicknessPx + (contentWidth - scaledWidth) / 2
                        val y = frameThicknessPx + (contentHeight - scaledHeight) / 2

                        // Draw the scaled bitmap centered within the frame
                        val canvas = android.graphics.Canvas(framedBitmap)
                        canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)

                        // Clean up unused bitmaps
                        originalBitmap.recycle()
                        scaledBitmap.recycle()

                        // Create filename
                        val fileName =
                                "Framer_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // For Android 10 and above
                                val contentValues =
                                        ContentValues().apply {
                                                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                                put(
                                                        MediaStore.MediaColumns.RELATIVE_PATH,
                                                        Environment.DIRECTORY_DCIM
                                                )
                                        }

                                val uri =
                                        context.contentResolver.insert(
                                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                contentValues
                                        )
                                uri?.let {
                                        context.contentResolver.openOutputStream(it)?.use { os ->
                                                framedBitmap.compress(
                                                        Bitmap.CompressFormat.JPEG,
                                                        quality,
                                                        os
                                                )
                                        }
                                }
                        } else {
                                // For older Android versions
                                val imagesDir =
                                        Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_DCIM
                                        )
                                if (!imagesDir.exists()) imagesDir.mkdirs()
                                val imageFile = File(imagesDir, fileName)
                                FileOutputStream(imageFile).use { fos ->
                                        framedBitmap.compress(
                                                Bitmap.CompressFormat.JPEG,
                                                quality,
                                                fos
                                        )
                                }

                                // Add to gallery
                                val values =
                                        ContentValues().apply {
                                                put(
                                                        MediaStore.Images.Media.DATA,
                                                        imageFile.absolutePath
                                                )
                                                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                        }
                                context.contentResolver.insert(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        values
                                )
                        }

                        // Clean up the final bitmap
                        framedBitmap.recycle()

                        // Notify user
                        withContext(Dispatchers.Main) {
                                Toast.makeText(
                                                context,
                                                "Image saved to Camera Roll",
                                                Toast.LENGTH_SHORT
                                        )
                                        .show()
                        }
                } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                                Toast.makeText(
                                                context,
                                                "Error saving image: ${e.message}",
                                                Toast.LENGTH_LONG
                                        )
                                        .show()
                        }
                }
        }
}

// Add this extension function at the end of the file
fun Color.toHex(): String {
        val alpha = (alpha * 255).toInt()
        val red = (red * 255).toInt()
        val green = (green * 255).toInt()
        val blue = (blue * 255).toInt()
        return String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
}

// HSL Color conversion functions
fun Color.toHSL(): Triple<Float, Float, Float> {
        val r = red
        val g = green
        val b = blue

        val max = max(max(r, g), b)
        val min = min(min(r, g), b)
        val d = max - min

        val h =
                when {
                        d == 0f -> 0f
                        max == r -> 60f * ((g - b) / d + 6f) % 360f
                        max == g -> 60f * ((b - r) / d + 2f)
                        else -> 60f * ((r - g) / d + 4f)
                }

        val l = (max + min) / 2f
        val s =
                when {
                        l == 0f || l == 1f -> 0f
                        else -> d / (1f - abs(2f * l - 1f))
                }

        return Triple(h, s, l)
}

fun Color.copy(hue: Float? = null, saturation: Float? = null, lightness: Float? = null): Color {
        val (h, s, l) = toHSL()
        val newH = hue ?: h
        val newS = saturation ?: s
        val newL = lightness ?: l

        val c = (1f - abs(2f * newL - 1f)) * newS
        val x = c * (1f - abs((newH / 60f) % 2f - 1f))
        val m = newL - c / 2f

        val (r1, g1, b1) =
                when {
                        newH < 60f -> Triple(c, x, 0f)
                        newH < 120f -> Triple(x, c, 0f)
                        newH < 180f -> Triple(0f, c, x)
                        newH < 240f -> Triple(0f, x, c)
                        newH < 300f -> Triple(x, 0f, c)
                        else -> Triple(c, 0f, x)
                }

        return Color(red = r1 + m, green = g1 + m, blue = b1 + m, alpha = alpha)
}

val Color.hue: Float
        get() = toHSL().first

val Color.saturation: Float
        get() = toHSL().second

val Color.lightness: Float
        get() = toHSL().third
