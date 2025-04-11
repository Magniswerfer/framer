package com.example.framer.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.example.framer.data.model.AspectRatio
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun saveImage(
        context: Context,
        imageUri: Uri,
        frameColor: Color,
        frameThickness: Dp,
        aspectRatio: AspectRatio,
        quality: Int
) {
    withContext(Dispatchers.IO) {
        try {
            // Create a bitmap from the original image with reduced sample size
            val originalBitmap =
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val source =
                                    ImageDecoder.createSource(context.contentResolver, imageUri)
                            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                decoder.isMutableRequired = true
                                // Add sampling to reduce memory usage
                                decoder.setTargetSampleSize(2)
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
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
                        Pair((originalHeight * targetAspectRatio).toInt(), originalHeight)
                    } else {
                        // Original is taller than target, use width as reference
                        Pair(originalWidth, (originalWidth / targetAspectRatio).toInt())
                    }

            // Calculate frame thickness in pixels
            val percentage = frameThickness.value / 400f
            val frameThicknessPx = (Math.min(contentWidth, contentHeight) * percentage).toInt()

            // Create final dimensions including the frame
            val finalWidth = contentWidth + (frameThicknessPx * 2)
            val finalHeight = contentHeight + (frameThicknessPx * 2)

            // Create the framed bitmap with the frame color
            val framedBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            framedBitmap.eraseColor(frameColor.toArgb())

            // Scale the original image to fit the inner area while maintaining its aspect ratio
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
                    Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

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
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
                        }

                val uri =
                        context.contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                        )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        framedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, os)
                    }
                }
            } else {
                // For older Android versions
                val imagesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                if (!imagesDir.exists()) imagesDir.mkdirs()
                val imageFile = File(imagesDir, fileName)
                FileOutputStream(imageFile).use { fos ->
                    framedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                }

                // Add to gallery
                val values =
                        ContentValues().apply {
                            put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            }

            // Clean up the final bitmap
            framedBitmap.recycle()

            // Notify user
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image saved to Camera Roll", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_LONG)
                        .show()
            }
        }
    }
}
