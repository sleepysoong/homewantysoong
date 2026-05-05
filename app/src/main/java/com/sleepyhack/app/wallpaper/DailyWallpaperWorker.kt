package com.sleepyhack.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sleepyhack.app.DateImageGenerator
import com.sleepyhack.app.FLAG_LOCK
import com.sleepyhack.app.FLAG_SYSTEM
import com.sleepyhack.app.TargetDateStore
import com.sleepyhack.app.WallpaperConfig
import com.sleepyhack.app.WallpaperHelper
import java.io.IOException

class DailyWallpaperWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val config = TargetDateStore.getLastWallpaperConfig(applicationContext)
        if (!config.shouldUpdate) return Result.success()

        val coverSize = if (config.setCover) resolveCoverSize() else null
        val mainSize = if (config.setMain) resolveMainSize() else null
        val targetDate = TargetDateStore.getTargetDate(applicationContext)
        val today = java.time.LocalDate.now()

        if (config.setCover) {
            val resolvedCoverSize = coverSize ?: return Result.failure()
            val homeResult = updateWallpaper(
                DateImageGenerator.WallpaperType.COVER_HOME,
                resolvedCoverSize,
                targetDate,
                today,
                FLAG_SYSTEM
            )
            if (homeResult.isFailure) {
                return logAndMapFailure(DateImageGenerator.WallpaperType.COVER_HOME, FLAG_SYSTEM, homeResult)
            }
            val lockResult = updateWallpaper(
                DateImageGenerator.WallpaperType.COVER_LOCK,
                resolvedCoverSize,
                targetDate,
                today,
                FLAG_LOCK
            )
            if (lockResult.isFailure) {
                return logAndMapFailure(DateImageGenerator.WallpaperType.COVER_LOCK, FLAG_LOCK, lockResult)
            }
        }

        if (config.setMain) {
            val resolvedMainSize = mainSize ?: return Result.failure()
            val homeResult = updateWallpaper(
                DateImageGenerator.WallpaperType.MAIN_HOME,
                resolvedMainSize,
                targetDate,
                today,
                FLAG_SYSTEM
            )
            if (homeResult.isFailure) {
                return logAndMapFailure(DateImageGenerator.WallpaperType.MAIN_HOME, FLAG_SYSTEM, homeResult)
            }
            val lockResult = updateWallpaper(
                DateImageGenerator.WallpaperType.MAIN_LOCK,
                resolvedMainSize,
                targetDate,
                today,
                FLAG_LOCK
            )
            if (lockResult.isFailure) {
                return logAndMapFailure(DateImageGenerator.WallpaperType.MAIN_LOCK, FLAG_LOCK, lockResult)
            }
        }

        return Result.success()
    }

    private fun resolveCoverSize(): Point {
        val wm = WallpaperManager.getInstance(applicationContext)
        val desiredWidth = wm.desiredMinimumWidth
        val desiredHeight = wm.desiredMinimumHeight
        if (desiredWidth > 0 && desiredHeight > 0) {
            return Point(desiredWidth, desiredHeight)
        }
        return resolveCurrentDisplaySize()
    }

    private fun resolveMainSize(): Point = resolveCurrentDisplaySize()

    private fun resolveCurrentDisplaySize(): Point {
        val wm = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay
            Point().also { display.getRealSize(it) }
        }
    }

    private fun updateWallpaper(
        type: DateImageGenerator.WallpaperType,
        screenSize: Point,
        targetDate: java.time.LocalDate,
        today: java.time.LocalDate,
        flag: Int
    ): Result<Unit> {
        if (screenSize.x <= 0 || screenSize.y <= 0) {
            return Result.failure(IllegalArgumentException("Invalid wallpaper size: ${screenSize.x}x${screenSize.y}"))
        }
        val bitmap = DateImageGenerator.generate(type, screenSize.x, screenSize.y, targetDate, today)
        return try {
            WallpaperHelper.setWallpaper(applicationContext, bitmap, flag)
        } finally {
            bitmap.recycle()
        }
    }

    private fun logAndMapFailure(
        type: DateImageGenerator.WallpaperType,
        flag: Int,
        result: Result<Unit>
    ): Result {
        val error = result.exceptionOrNull() ?: return Result.failure()
        Log.e(TAG, "Failed to set wallpaper: type=$type flag=$flag", error)
        return if (error is IOException) Result.retry() else Result.failure()
    }

    companion object {
        private const val TAG = "DailyWallpaperWorker"
    }
}

private val WallpaperConfig.shouldUpdate: Boolean
    get() = setCover || setMain
