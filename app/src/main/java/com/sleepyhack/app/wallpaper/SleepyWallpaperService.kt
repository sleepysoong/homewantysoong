package com.sleepyhack.app.wallpaper

import android.graphics.Point
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.sleepyhack.app.DateImageGenerator

/**
 * Live wallpaper service that renders the date-based wallpaper in real-time.
 * Registered in AndroidManifest.xml for the system wallpaper picker.
 */
class SleepyWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = SleepyWallpaperEngine()

    inner class SleepyWallpaperEngine : Engine() {

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            drawWallpaper(holder, width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float,
            xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            val holder = surfaceHolder ?: return
            try {
                val size = Point()
                // Get current surface size
                val canvas = holder.lockCanvas() ?: return
                try {
                    size.set(canvas.width, canvas.height)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
                drawWallpaper(holder, size.x, size.y)
            } catch (_: Exception) {
                // Surface not ready
            }
        }

        private fun drawWallpaper(holder: SurfaceHolder, width: Int, height: Int) {
            if (width <= 0 || height <= 0) return
            val bitmap = DateImageGenerator.generate(
                DateImageGenerator.WallpaperType.COVER_HOME,
                width, height
            )
            try {
                val canvas = holder.lockCanvas() ?: return
                try {
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
                bitmap.recycle()
            } catch (_: Exception) {
                // Surface gone
            }
        }
    }
}