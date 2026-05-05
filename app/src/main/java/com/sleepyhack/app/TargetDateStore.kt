package com.sleepyhack.app

import android.content.Context
import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeParseException

object TargetDateStore {
    private const val PREFS_NAME = "sleepyhack_prefs"
    private const val KEY_TARGET_DATE = "target_date"
    private const val KEY_SET_COVER = "last_set_cover"
    private const val KEY_SET_MAIN = "last_set_main"

    fun getTargetDate(context: Context): LocalDate {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawDate = prefs.getString(KEY_TARGET_DATE, null) ?: return LocalDate.now()
        val parsedDate = parseIsoDate(rawDate)
        if (parsedDate != null) {
            return parsedDate
        }
        Log.w("TargetDateStore", "Invalid stored target date: $rawDate")
        prefs.edit().remove(KEY_TARGET_DATE).apply()
        return LocalDate.now()
    }

    fun setTargetDate(context: Context, targetDate: LocalDate) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TARGET_DATE, targetDate.toString()).apply()
    }

    fun getLastWallpaperConfig(context: Context): WallpaperConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return WallpaperConfig(
            setCover = prefs.getBoolean(KEY_SET_COVER, false),
            setMain = prefs.getBoolean(KEY_SET_MAIN, false)
        )
    }

    fun saveLastWallpaperConfig(context: Context, config: WallpaperConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SET_COVER, config.setCover)
            .putBoolean(KEY_SET_MAIN, config.setMain)
            .apply()
    }

    private fun parseIsoDate(raw: String): LocalDate? = try {
        LocalDate.parse(raw)
    } catch (_: DateTimeParseException) {
        null
    }
}

data class WallpaperConfig(
    val setCover: Boolean,
    val setMain: Boolean
)
