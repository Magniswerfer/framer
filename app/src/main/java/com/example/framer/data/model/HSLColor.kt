package com.example.framer.data.model

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
