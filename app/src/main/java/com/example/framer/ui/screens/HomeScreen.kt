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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import com.example.framer.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.example.framer.R

// Data class to represent aspect ratios
data class AspectRatio(val name: String, val ratio: Float)

// Data class to represent quality options
data class QualityOption(val name: String, val quality: Int)

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var frameColor by remember { mutableStateOf(White) }
    var frameThickness by remember { mutableStateOf(20.dp) }
    var ratioExpanded by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    
    // Debounced frame thickness for smoother updates
    val debouncedFrameThickness by remember(frameThickness) {
        derivedStateOf { frameThickness }
    }
    
    // Cache the loaded bitmap
    val loadedBitmap = remember(selectedImageUri) {
        if (selectedImageUri != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, selectedImageUri!!)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, selectedImageUri!!)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_LONG).show()
                null
            }
        } else null
    }
    
    // Define available aspect ratios
    val aspectRatios = listOf(
        AspectRatio("1:1", 1f),
        AspectRatio("4:3", 4f/3f),
        AspectRatio("3:4", 3f/4f),
        AspectRatio("16:9", 16f/9f),
        AspectRatio("9:16", 9f/16f)
    )
    var selectedAspectRatio by remember { mutableStateOf(aspectRatios[0]) }
    var selectedAspectRatioColor by remember { mutableStateOf(Red) }
    
    // Define quality options
    val qualityOptions = listOf(
        QualityOption("FULL QUALITY", 100),
        QualityOption("HIGH QUALITY", 80),
        QualityOption("MEDIUM QUALITY", 60),
        QualityOption("LOW QUALITY", 30)
    )
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    // Request storage permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showQualityDialog = true
        } else {
            Toast.makeText(context, "Storage permission required to save images", Toast.LENGTH_LONG).show()
        }
    }
    
    // VSCO-style typography
    val vscoTypography = MaterialTheme.typography.copy(
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = FontFamily.SansSerif, // Closest to Helvetica
            color = MaterialTheme.colorScheme.onBackground
        ),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onBackground
        ),
        bodySmall = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onBackground
        )
    )
    
    // Quality selection dialog
    if (showQualityDialog) {
        Dialog(onDismissRequest = { showQualityDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = Black
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
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
                                        saveImage(context, selectedImageUri!!, frameColor, frameThickness, 
                                                selectedAspectRatio, option.quality)
                                    }
                                } else {
                                    Toast.makeText(context, "Please select an image first", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Blue,
                                contentColor = White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                option.name,
                                color = White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showQualityDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            "CANCEL",
                            color = White
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo and Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .offset(x = (-24).dp)  // Negative offset to counteract the SVG padding
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.framer_logo),
                        contentDescription = "Framer Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = "Framer",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 32.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.offset(x = (-24).dp)  // Add negative offset to move text closer to logo
                )
            }

            // Image Preview with Frame
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
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
                    modifier = Modifier
                        .width(width.dp)
                        .height(height.dp)
                        .align(Alignment.Center)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
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
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { imagePicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Select image",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            showQualityDialog = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = selectedImageUri != null,
                    colors = ButtonDefaults.buttonColors(
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
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SAVE",
                            style = MaterialTheme.typography.labelLarge,
                            color = White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Aspect Ratio Button
            Button(
                onClick = { ratioExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedAspectRatioColor,
                    contentColor = if (selectedAspectRatioColor == Yellow) Black else White
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
                        tint = if (selectedAspectRatioColor == Yellow) Black else White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Frame Color Picker
            Text(
                "FRAME COLOR",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(White, Black, Blue, Yellow, Red).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(color, CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (frameColor == color) 
                                    MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surface,
                                shape = CircleShape
                            )
                            .clickable { frameColor = color }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Frame Thickness Slider
            Text(
                "FRAME THICKNESS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            
            Slider(
                value = frameThickness.value,
                onValueChange = { frameThickness = it.dp },
                valueRange = 5f..100f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (ratioExpanded) {
            Popup(
                onDismissRequest = { ratioExpanded = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { ratioExpanded = false }
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {}  // Prevent click propagation
                            .background(White, RoundedCornerShape(24.dp))
                            .border(2.dp, Yellow, RoundedCornerShape(24.dp))
                            .padding(8.dp)
                    ) {
                        aspectRatios.forEachIndexed { index, ratio ->
                            val pillColor = when (index % 3) {
                                0 -> Red
                                1 -> Yellow
                                else -> Blue
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .background(pillColor, RoundedCornerShape(16.dp))
                                    .clickable { 
                                        selectedAspectRatio = ratio
                                        selectedAspectRatioColor = pillColor
                                        ratioExpanded = false
                                    }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    ratio.name.uppercase(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (pillColor == Yellow) Black else White
                                )
                            }
                        }
                    }
                }
            }
        }
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
            // Create a bitmap from the original image
            val originalBitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_LONG).show()
                }
                return@withContext
            }
            
            // Get original dimensions
            val originalWidth = originalBitmap.width
            val originalHeight = originalBitmap.height
            
            // Determine the target aspect ratio from the selection
            val targetAspectRatio = aspectRatio.ratio
            
            // First, calculate the content area dimensions according to the target aspect ratio
            var contentWidth: Int
            var contentHeight: Int
            
            // Resize content area based on target aspect ratio
            if (originalWidth.toFloat() / originalHeight > targetAspectRatio) {
                // Original is wider than target, use height as reference
                contentHeight = originalHeight
                contentWidth = (originalHeight * targetAspectRatio).toInt()
            } else {
                // Original is taller than target, use width as reference
                contentWidth = originalWidth
                contentHeight = (originalWidth / targetAspectRatio).toInt()
            }
            
            // Calculate frame thickness using Option 1: Direct percentage of smaller dimension
            val percentage = frameThickness.value / 400f
            val frameThicknessPx = (Math.min(contentWidth, contentHeight) * percentage).toInt()
            
            // Create final dimensions including the frame
            val finalWidth = contentWidth + (frameThicknessPx * 2)
            val finalHeight = contentHeight + (frameThicknessPx * 2)
            
            // Create the framed bitmap with the frame color
            val framedBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            framedBitmap.eraseColor(frameColor.toArgb())
            
            // Calculate the inner area dimensions
            val innerWidth = contentWidth
            val innerHeight = contentHeight
            
            // Scale the original image to fit the inner area while maintaining its aspect ratio
            val scaledWidth: Int
            val scaledHeight: Int
            
            val originalAspectRatio = originalWidth.toFloat() / originalHeight
            if (originalAspectRatio > innerWidth.toFloat() / innerHeight) {
                // Image is wider than inner area, scale by width
                scaledWidth = innerWidth
                scaledHeight = (innerWidth / originalAspectRatio).toInt()
            } else {
                // Image is taller than inner area, scale by height
                scaledHeight = innerHeight
                scaledWidth = (innerHeight * originalAspectRatio).toInt()
            }
            
            // Create scaled bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
            
            // Center the scaled bitmap in the inner area
            val x = frameThicknessPx + (innerWidth - scaledWidth) / 2
            val y = frameThicknessPx + (innerHeight - scaledHeight) / 2
            
            // Draw the scaled bitmap centered within the frame
            val canvas = android.graphics.Canvas(framedBitmap)
            canvas.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
            
            // Create filename
            val fileName = "Framer_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM) // Save to camera roll
                }
                
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    val outputStream = context.contentResolver.openOutputStream(it)
                    outputStream?.use { os ->
                        framedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, os)
                    }
                }
            } else {
                // For older Android versions
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) // Save to camera roll
                if (!imagesDir.exists()) imagesDir.mkdirs()
                val imageFile = File(imagesDir, fileName)
                val fos = FileOutputStream(imageFile)
                framedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                fos.close()
                
                // Add to gallery
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }
            
            // Notify user
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image saved to Camera Roll", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
} 