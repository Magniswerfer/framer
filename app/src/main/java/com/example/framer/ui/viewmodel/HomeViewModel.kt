package com.example.framer.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framer.data.model.AspectRatio
import com.example.framer.data.model.HSLColor
import com.example.framer.data.model.QualityOption
import com.example.framer.utils.saveImage
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    var selectedImageUri by mutableStateOf<Uri?>(null)
    var frameColor by mutableStateOf(Color.White)
    var frameThickness by mutableStateOf(20.dp)
    var showDrawer by mutableStateOf(false)
    var showAboutScreen by mutableStateOf(false)
    var showQualityDialog by mutableStateOf(false)
    var showColorPicker by mutableStateOf(false)
    var ratioExpanded by mutableStateOf(false)
    var selectedAspectRatio by mutableStateOf(aspectRatios[0])
    var selectedAspectRatioColor by mutableStateOf(Color.Red)
    var hslColor by mutableStateOf(HSLColor(0f, 0f, 1f))

    companion object {
        val aspectRatios =
                listOf(
                        AspectRatio("1:1", 1f),
                        AspectRatio("4:3", 4f / 3f),
                        AspectRatio("3:4", 3f / 4f),
                        AspectRatio("16:9", 16f / 9f),
                        AspectRatio("9:16", 9f / 16f)
                )

        val qualityOptions =
                listOf(
                        QualityOption("FULL QUALITY", 100),
                        QualityOption("HIGH QUALITY", 80),
                        QualityOption("MEDIUM QUALITY", 60),
                        QualityOption("LOW QUALITY", 30)
                )
    }

    fun onHueChange(newValue: Float) {
        hslColor = hslColor.copy(hue = newValue)
        frameColor = hslColor.toColor()
    }

    fun onSaturationChange(newValue: Float) {
        hslColor = hslColor.copy(saturation = newValue)
        frameColor = hslColor.toColor()
    }

    fun onLightnessChange(newValue: Float) {
        hslColor = hslColor.copy(lightness = newValue)
        frameColor = hslColor.toColor()
    }

    fun saveImage(context: Context, quality: Int) {
        selectedImageUri?.let { uri ->
            viewModelScope.launch {
                saveImage(
                        context = context,
                        imageUri = uri,
                        frameColor = frameColor,
                        frameThickness = frameThickness,
                        aspectRatio = selectedAspectRatio,
                        quality = quality
                )
            }
        }
    }
}
