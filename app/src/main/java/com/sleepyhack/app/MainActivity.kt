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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var btnSetAll: Button
    private lateinit var btnSetCover: Button
    private lateinit var btnSetMain: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        btnSetAll = findViewById(R.id.btn_set_all)
        btnSetCover = findViewById(R.id.btn_set_cover)
        btnSetMain = findViewById(R.id.btn_set_main)

        btnSetAll.setOnClickListener { applyWallpapers(setCover = true, setMain = true) }
        btnSetCover.setOnClickListener { applyWallpapers(setCover = true, setMain = false) }
        btnSetMain.setOnClickListener { applyWallpapers(setCover = false, setMain = true) }
    }

    private fun applyWallpapers(setCover: Boolean, setMain: Boolean) {
        // Check SET_WALLPAPER permission (normal permission, but check anyway)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SET_WALLPAPER)
            != PackageManager.PERMISSION_GRANTED
        ) {
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
            val results = mutableListOf<Pair<String, Boolean>>()

            withContext(Dispatchers.IO) {
                // Use full screen size for wallpaper generation
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                val screenSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val bounds = wm.currentWindowMetrics.bounds
                    Point(bounds.width(), bounds.height())
                } else {
                    @Suppress("DEPRECATION")
                    val display = wm.defaultDisplay
                    Point().also { display.getRealSize(it) }
                }

                val width = screenSize.x
                val height = screenSize.y

                if (setCover) {
                    // Cover Home - Yellow
                    val coverHomeBitmap = DateImageGenerator.generate(
                        DateImageGenerator.WallpaperType.COVER_HOME, width, height
                    )
                    val result = WallpaperHelper.setWallpaper(
                        this@MainActivity, coverHomeBitmap, FLAG_SYSTEM
                    )
                    results.add("접은화면 홈" to result.isSuccess)
                    coverHomeBitmap.recycle()

                    // Cover Lock - Green
                    val coverLockBitmap = DateImageGenerator.generate(
                        DateImageGenerator.WallpaperType.COVER_LOCK, width, height
                    )
                    val lockResult = WallpaperHelper.setWallpaper(
                        this@MainActivity, coverLockBitmap, FLAG_LOCK
                    )
                    results.add("접은화면 잠금" to lockResult.isSuccess)
                    coverLockBitmap.recycle()
                }

                if (setMain) {
                    // Main Home - Red
                    val mainHomeBitmap = DateImageGenerator.generate(
                        DateImageGenerator.WallpaperType.MAIN_HOME, width, height
                    )
                    val result = WallpaperHelper.setWallpaper(
                        this@MainActivity, mainHomeBitmap, FLAG_SYSTEM
                    )
                    results.add("편화면 홈" to result.isSuccess)
                    mainHomeBitmap.recycle()

                    // Main Lock - Blue
                    val mainLockBitmap = DateImageGenerator.generate(
                        DateImageGenerator.WallpaperType.MAIN_LOCK, width, height
                    )
                    val lockResult = WallpaperHelper.setWallpaper(
                        this@MainActivity, mainLockBitmap, FLAG_LOCK
                    )
                    results.add("편화면 잠금" to lockResult.isSuccess)
                    mainLockBitmap.recycle()
                }
            }

            // Show results
            val successCount = results.count { it.second }
            val totalCount = results.size

            if (successCount == totalCount) {
                statusText.text = getString(R.string.status_done)
                Toast.makeText(this@MainActivity, "모든 배경화면 설정 완료!", Toast.LENGTH_SHORT).show()
            } else {
                val failed = results.filter { !it.second }.joinToString(", ") { it.first }
                statusText.text = getString(R.string.status_error)
                Toast.makeText(
                    this@MainActivity,
                    "실패: $failed",
                    Toast.LENGTH_LONG
                ).show()
            }

            setButtonsEnabled(true)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Retry
                applyWallpapers(setCover = true, setMain = true)
            } else {
                Toast.makeText(this, "배경화면 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btnSetAll.isEnabled = enabled
        btnSetCover.isEnabled = enabled
        btnSetMain.isEnabled = enabled
    }
}