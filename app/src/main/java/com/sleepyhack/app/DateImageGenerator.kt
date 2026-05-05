package com.sleepyhack.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd EEE", Locale.KOREAN)

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
    fun generate(
        type: WallpaperType,
        width: Int,
        height: Int,
        targetDate: LocalDate,
        today: LocalDate
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fill background
        canvas.drawColor(type.bgColor)

        // Draw date text
        val dateText = dateFormat.format(today)
        val ddayText = formatDdayText(today, targetDate)

        // Configure paints
        val minDimension = minOf(width, height).toFloat()
        val ddayPaint = Paint().apply {
            color = type.textColor
            textSize = minDimension * 0.28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }

        val datePaint = Paint().apply {
            color = type.textColor
            textSize = minDimension * 0.08f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            alpha = 200
        }

        // Draw centered
        val cx = width / 2f
        val cy = height / 2f
        val lineSpacing = ddayPaint.textSize * 0.22f
        val totalHeight = ddayPaint.textSize + lineSpacing + datePaint.textSize
        val startY = cy - totalHeight / 2f

        canvas.drawText(ddayText, cx, startY + ddayPaint.textSize, ddayPaint)
        canvas.drawText(dateText, cx, startY + ddayPaint.textSize + lineSpacing + datePaint.textSize, datePaint)

        return bitmap
    }

    fun formatDdayPreview(today: LocalDate, targetDate: LocalDate): String {
        val ddayText = formatDdayText(today, targetDate)
        return "$ddayText • ${dateFormat.format(targetDate)}"
    }

    private fun formatDdayText(today: LocalDate, targetDate: LocalDate): String {
        val daysUntil = ChronoUnit.DAYS.between(today, targetDate)
        return when {
            daysUntil > 0 -> "D-$daysUntil"
            daysUntil < 0 -> "D+${-daysUntil}"
            else -> "D-Day"
        }
    }
}
