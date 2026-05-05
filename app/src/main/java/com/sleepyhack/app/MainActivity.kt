package com.sleepyhack.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.sleepyhack.app.wallpaper.DailyWallpaperWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnSetAll: Button
    private lateinit var btnSetCover: Button
    private lateinit var btnSetMain: Button
    private lateinit var btnSelectDate: Button
    private lateinit var currentDateText: TextView
    private lateinit var targetDateText: TextView
    private lateinit var ddayPreviewText: TextView

    private var lastPermissionRequest: WallpaperConfig? = null

    private val displayDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd EEE", Locale.KOREAN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        btnSetAll = findViewById(R.id.btn_set_all)
        btnSetCover = findViewById(R.id.btn_set_cover)
        btnSetMain = findViewById(R.id.btn_set_main)
        btnSelectDate = findViewById(R.id.btn_select_date)
        currentDateText = findViewById(R.id.current_date_text)
        targetDateText = findViewById(R.id.target_date_text)
        ddayPreviewText = findViewById(R.id.dday_preview_text)

        btnSetAll.setOnClickListener { applyWallpapers(setCover = true, setMain = true) }
        btnSetCover.setOnClickListener { applyWallpapers(setCover = true, setMain = false) }
        btnSetMain.setOnClickListener { applyWallpapers(setCover = false, setMain = true) }
        btnSelectDate.setOnClickListener { openDatePicker() }

        refreshDatePreview()
        scheduleDailyWallpaperUpdate()
    }

    private fun applyWallpapers(setCover: Boolean, setMain: Boolean) {
        val requestedConfig = WallpaperConfig(setCover = setCover, setMain = setMain)
        // Check SET_WALLPAPER permission (normal permission, but check anyway)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER)
            != PackageManager.PERMISSION_GRANTED
        ) {
            lastPermissionRequest = requestedConfig
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SET_WALLPAPER),
                0
            )
            return
        }

        setButtonsEnabled(false)
        statusText.text = getString(R.string.status_setting)

        lifecycleScope.launch {
            try {
                val results = mutableListOf<Pair<String, Result<Unit>>>()
                val targetDate = TargetDateStore.getTargetDate(this@MainActivity)
                TargetDateStore.saveLastWallpaperConfig(
                    this@MainActivity,
                    requestedConfig
                )

                val today = LocalDate.now()

                withContext(Dispatchers.IO) {
                    val currentSize = resolveCurrentDisplaySize()
                    val coverSize = if (setCover) currentSize else null
                    val mainSize = if (setMain) currentSize else null

                    if (setCover) {
                        val resolvedCoverSize = coverSize ?: return@withContext
                        val coverWidth = resolvedCoverSize.x
                        val coverHeight = resolvedCoverSize.y
                    // Cover Home - Yellow
                    val coverHomeBitmap = DateImageGenerator.generate(
                        DateImageGenerator.WallpaperType.COVER_HOME,
                        coverWidth,
                        coverHeight,
                        targetDate,
                        today
                    )
                    val result = WallpaperHelper.setWallpaper(
                        this@MainActivity, coverHomeBitmap, FLAG_SYSTEM
                    )
                    results.add("접은화면 홈" to result)
                    coverHomeBitmap.recycle()

                    // Cover Lock - Green
                    val coverLockBitmap = DateImageGenerator.generate(
                        DateImageGenerator.WallpaperType.COVER_LOCK,
                        coverWidth,
                        coverHeight,
                        targetDate,
                        today
                    )
                    val lockResult = WallpaperHelper.setWallpaper(
                        this@MainActivity, coverLockBitmap, FLAG_LOCK
                    )
                    results.add("접은화면 잠금" to lockResult)
                    coverLockBitmap.recycle()
                }

                if (setMain) {
                    val resolvedMainSize = mainSize ?: return@withContext
                    val mainWidth = resolvedMainSize.x
                    val mainHeight = resolvedMainSize.y
                    // Main Home - Red
                    val mainHomeBitmap = DateImageGenerator.generate(
                        DateImageGenerator.WallpaperType.MAIN_HOME,
                        mainWidth,
                        mainHeight,
                        targetDate,
                        today
                    )
                    val result = WallpaperHelper.setWallpaper(
                        this@MainActivity, mainHomeBitmap, FLAG_SYSTEM
                    )
                    results.add("편화면 홈" to result)
                    mainHomeBitmap.recycle()

                    // Main Lock - Blue
                    val mainLockBitmap = DateImageGenerator.generate(
                        DateImageGenerator.WallpaperType.MAIN_LOCK,
                        mainWidth,
                        mainHeight,
                        targetDate,
                        today
                    )
                    val lockResult = WallpaperHelper.setWallpaper(
                        this@MainActivity, mainLockBitmap, FLAG_LOCK
                    )
                    results.add("편화면 잠금" to lockResult)
                    mainLockBitmap.recycle()
                }
                }

                // Show results
                val successCount = results.count { it.second.isSuccess }
                val totalCount = results.size

                if (successCount == totalCount) {
                    statusText.text = getString(R.string.status_done)
                    Toast.makeText(this@MainActivity, R.string.toast_wallpaper_success, Toast.LENGTH_SHORT).show()
                } else {
                    val failed = results
                        .filter { it.second.isFailure }
                        .joinToString(", ") { it.first }
                    val errorDetail = results
                        .firstOrNull { it.second.isFailure }
                        ?.second
                        ?.exceptionOrNull()
                        ?.localizedMessage
                        ?.takeIf { it.isNotBlank() }
                    val failedCount = totalCount - successCount
                    statusText.text = getString(R.string.status_error)
                    Toast.makeText(
                        this@MainActivity,
                        getString(
                            R.string.toast_wallpaper_failed,
                            failedCount,
                            failed,
                            errorDetail ?: getString(R.string.toast_wallpaper_failed_unknown)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                setButtonsEnabled(true)
            }
        }
    }

    private fun openDatePicker() {
        val storedTargetDate = TargetDateStore.getTargetDate(this)
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.btn_select_date)
            .setSelection(storedTargetDate.toEpochMillis())
            .build()

        picker.addOnPositiveButtonClickListener { selectionMillis ->
            val selectedDate = selectionMillis.toLocalDate()
            TargetDateStore.setTargetDate(this, selectedDate)
            refreshDatePreview()
        }

        picker.show(supportFragmentManager, "target_date_picker")
    }

    private fun refreshDatePreview() {
        val today = LocalDate.now()
        val targetDate = TargetDateStore.getTargetDate(this)
        currentDateText.text = displayDateFormatter.format(today)
        targetDateText.text = displayDateFormatter.format(targetDate)
        ddayPreviewText.text = DateImageGenerator.formatDdayPreview(today, targetDate)
    }

    private fun scheduleDailyWallpaperUpdate() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<DailyWallpaperWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DAILY_WALLPAPER_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val config = lastPermissionRequest ?: return
                lastPermissionRequest = null
                applyWallpapers(setCover = config.setCover, setMain = config.setMain)
            } else {
                lastPermissionRequest = null
                Toast.makeText(this, R.string.toast_permission_required, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnSetAll.isEnabled = enabled
        btnSetCover.isEnabled = enabled
        btnSetMain.isEnabled = enabled
    }

    private fun resolveCurrentDisplaySize(): Point {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            @Suppress("DEPRECATION")
            val display = wm.defaultDisplay
            Point().also { display.getRealSize(it) }
        }
    }

    private fun LocalDate.toEpochMillis(): Long {
        return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
    }

    companion object {
        private const val DAILY_WALLPAPER_WORK = "daily_wallpaper_work"
    }
}
