package com.example.framer.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

fun Color.toHex(): String {
    val alpha = (alpha * 255).toInt()
    val red = (red * 255).toInt()
    val green = (green * 255).toInt()
    val blue = (blue * 255).toInt()
    return String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
}

fun Color.toHSL(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
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
