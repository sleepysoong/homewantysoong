package com.sleepyhack.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates simple solid-color wallpaper images with today's date text.
 *
 * Colors per spec:
 *   Cover Home:  Yellow
 *   Cover Lock:  Green
 *   Main  Home:  Red
 *   Main  Lock:  Blue
 */
object DateImageGenerator {

    private val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREAN)
    private val dayFormat = SimpleDateFormat("EEEE", Locale.KOREAN)

    enum class WallpaperType(val bgColor: Int, val textColor: Int) {
        COVER_HOME(
            bgColor = Color.rgb(255, 235, 59),   // Yellow
            textColor = Color.rgb(33, 33, 33)    // Dark gray
        ),
        COVER_LOCK(
            bgColor = Color.rgb(76, 175, 80),    // Green
            textColor = Color.rgb(255, 255, 255) // White
        ),
        MAIN_HOME(
            bgColor = Color.rgb(244, 67, 54),    // Red
            textColor = Color.rgb(255, 255, 255) // White
        ),
        MAIN_LOCK(
            bgColor = Color.rgb(33, 150, 243),   // Blue
            textColor = Color.rgb(255, 255, 255) // White
        )
    }

    /**
     * Generate a wallpaper bitmap for the given [type] and [size].
     * The bitmap fills with the background color and renders today's date centered.
     */
    fun generate(type: WallpaperType, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill background
        canvas.drawColor(type.bgColor)

        // Draw date text
        val now = Date()
        val dateText = dateFormat.format(now)
        val dayText = dayFormat.format(now)

        // Configure paints
        val datePaint = Paint().apply {
            color = type.textColor
            textSize = width * 0.09f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }

        val dayPaint = Paint().apply {
            color = type.textColor
            textSize = width * 0.12f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            alpha = 200
        }

        // Draw centered
        val cx = width / 2f
        val cy = height / 2f
        val lineSpacing = datePaint.textSize * 1.4f
        val totalHeight = datePaint.textSize + lineSpacing + dayPaint.textSize
        val startY = cy - totalHeight / 2f

        canvas.drawText(dateText, cx, startY + datePaint.textSize, datePaint)
        canvas.drawText(dayText, cx, startY + datePaint.textSize + lineSpacing + dayPaint.textSize, dayPaint)

        return bitmap
    }
}