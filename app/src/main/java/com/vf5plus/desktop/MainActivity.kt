package com.vf5plus.desktop

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var controllerLayout: LinearLayout
    private lateinit var hudLayout: LinearLayout
    private lateinit var hudContentContainer: LinearLayout
    private lateinit var tvHudClock: TextView
    private lateinit var tvHudSpeed: TextView
    private lateinit var tvShizukuStatus: TextView

    private var isHudMirrored = false
    private var locationManager: LocationManager? = null
    private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())

    private val hudClockRunnable = object : Runnable {
        override fun run() {
            tvHudClock.text = clockFormat.format(Date())
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Giữ màn hình luôn sáng khi mở app
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        controllerLayout = findViewById(R.id.controllerLayout)
        hudLayout = findViewById(R.id.hudLayout)
        hudContentContainer = findViewById(R.id.hudContentContainer)
        tvHudClock = findViewById(R.id.tvHudClock)
        tvHudSpeed = findViewById(R.id.tvHudSpeed)
        tvShizukuStatus = findViewById(R.id.tvShizukuStatus)

        setupControllerButtons()
        setupHudButtons()
        checkLocationPermission()
        checkShizukuStatus()
    }

    private fun setupControllerButtons() {
        val btnEnterHud: Button = findViewById(R.id.btnEnterHud)
        val btnCheckAdb: Button = findViewById(R.id.btnCheckAdb)
        val btnLayout50: Button = findViewById(R.id.btnLayout50)
        val btnLayout70: Button = findViewById(R.id.btnLayout70)
        val btnLayout3Pane: Button = findViewById(R.id.btnLayout3Pane)
        val btnLayoutFull: Button = findViewById(R.id.btnLayoutFull)
        val btnToggleBubble: Button = findViewById(R.id.btnToggleBubble)

        btnEnterHud.setOnClickListener {
            showHudMode()
        }

        btnCheckAdb.setOnClickListener {
            checkShizukuStatus()
            Toast.makeText(this, "Đang kiểm tra kết nối Shizuku / Wireless ADB...", Toast.LENGTH_SHORT).show()
        }

        btnLayout50.setOnClickListener {
            DesktopPresentation.activeInstance?.setLayoutMode(DesktopPresentation.LAYOUT_50_50)
            Toast.makeText(this, "Đã đổi bố cục xe: 50 / 50", Toast.LENGTH_SHORT).show()
        }

        btnLayout70.setOnClickListener {
            DesktopPresentation.activeInstance?.setLayoutMode(DesktopPresentation.LAYOUT_70_30)
            Toast.makeText(this, "Đã đổi bố cục xe: 70 / 30", Toast.LENGTH_SHORT).show()
        }

        btnLayout3Pane.setOnClickListener {
            DesktopPresentation.activeInstance?.setLayoutMode(DesktopPresentation.LAYOUT_3_PANE)
            Toast.makeText(this, "Đã đổi bố cục xe: 3 Ô", Toast.LENGTH_SHORT).show()
        }

        btnLayoutFull.setOnClickListener {
            DesktopPresentation.activeInstance?.setLayoutMode(DesktopPresentation.LAYOUT_FULL)
            Toast.makeText(this, "Đã đổi bố cục xe: Toàn màn hình", Toast.LENGTH_SHORT).show()
        }

        btnToggleBubble.setOnClickListener {
            DesktopPresentation.activeInstance?.toggleBubble()
            Toast.makeText(this, "Đã bật/tắt bong bóng tốc độ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHudButtons() {
        val btnMirrorHud: Button = findViewById(R.id.btnMirrorHud)
        val btnExitHud: Button = findViewById(R.id.btnExitHud)

        btnMirrorHud.setOnClickListener {
            isHudMirrored = !isHudMirrored
            // Lật ngược chữ theo trục ngang (ScaleX = -1) để khi phản chiếu lên kính chắn gió sẽ thành chữ xuôi
            hudContentContainer.scaleX = if (isHudMirrored) -1f else 1f
            btnMirrorHud.text = if (isHudMirrored) "🔄 Đã lật (Chiếu kính)" else "🔄 Lật ngược chữ (Chiếu kính)"
        }

        btnExitHud.setOnClickListener {
            hideHudMode()
        }
    }

    private fun showHudMode() {
        controllerLayout.visibility = View.GONE
        hudLayout.visibility = View.VISIBLE
        handler.post(hudClockRunnable)
    }

    private fun hideHudMode() {
        handler.removeCallbacks(hudClockRunnable)
        hudLayout.visibility = View.GONE
        controllerLayout.visibility = View.VISIBLE
    }

    private fun checkShizukuStatus() {
        // Kiểm tra xem Shizuku Manager có được cài đặt trên máy không
        val isInstalled = try {
            packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            false
        }

        if (isInstalled) {
            tvShizukuStatus.text = "✅ Đã tìm thấy Shizuku: Có thể mở mọi App gốc (Vietmap, Maps, YouTube) lên xe"
            tvShizukuStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        } else {
            tvShizukuStatus.text = "ℹ️ Chế độ nhúng Web/Desktop sẵn sàng. Cài thêm Shizuku nếu muốn mở trực tiếp App gốc."
            tvShizukuStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startGps()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 102)
        }
    }

    private fun startGps() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1.0f, this)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun onLocationChanged(location: Location) {
        val speedKmh = if (location.hasSpeed() && location.speed > 0.8f) {
            (location.speed * 3.6f).toInt()
        } else {
            0
        }
        tvHudSpeed.text = "$speedKmh"
    }

    override fun onDestroy() {
        handler.removeCallbacks(hudClockRunnable)
        locationManager?.removeUpdates(this)
        super.onDestroy()
    }
}
