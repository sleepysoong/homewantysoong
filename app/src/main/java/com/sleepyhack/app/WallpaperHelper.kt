package com.sleepyhack.app

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import java.io.IOException

/**
 * Handles setting wallpaper on different display configurations.
 *
 * FLAG_SYSTEM (1) = Home screen
 * FLAG_LOCK   (2) = Lock screen
 *
 * For foldables, the system treats the cover and main displays
 * as having independent wallpaper state, but the public API
 * only exposes FLAG_SYSTEM and FLAG_LOCK. The actual per-display
 * separation is handled at the system level (WallpaperManagerService).
 *
 * Strategy: We set the wallpaper WHILE the device is in the desired
 * folded state. The system will apply it to the currently active
 * display configuration. The user should apply cover wallpapers
 * while folded, and main wallpapers while unfolded.
 */
object WallpaperHelper {

    /**
     * Set a bitmap as wallpaper for the given which flags.
     *
     * @param context Application context
     * @param bitmap  The bitmap to set
     * @param which   FLAG_SYSTEM, FLAG_LOCK, or both
     */
    fun setWallpaper(context: Context, bitmap: Bitmap, which: Int): Result<Unit> {
        return try {
            val wm = WallpaperManager.getInstance(context)
            wm.setBitmap(bitmap, null, true, which)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }
}

/** WallpaperManager.setWallpaperFlags */
const val FLAG_SYSTEM = 1
const val FLAG_LOCK = 2