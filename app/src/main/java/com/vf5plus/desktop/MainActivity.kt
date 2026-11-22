package com.vf5plus.desktop

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvAutoStatus: TextView
    private lateinit var tvShizukuStatus: TextView

    // Layout buttons
    private lateinit var btnLayout70: Button
    private lateinit var btnLayout50: Button
    private lateinit var btnLayout3Pane: Button
    private lateinit var btnLayoutFull: Button

    // Pane 1 buttons
    private lateinit var btnPane1Maps: Button
    private lateinit var btnPane1Vietmap: Button
    private lateinit var btnPane1OsmDark: Button
    private lateinit var btnPane1YouTube: Button

    // Pane 2 buttons
    private lateinit var btnPane2YouTube: Button
    private lateinit var btnPane2TikTok: Button
    private lateinit var btnPane2Zing: Button
    private lateinit var btnPane2Vietmap: Button

    // Pane 3 buttons
    private lateinit var btnPane3TikTok: Button
    private lateinit var btnPane3YouTube: Button
    private lateinit var btnPane3Vietmap: Button
    private lateinit var btnPane3Zing: Button

    private lateinit var btnToggleCarEditMode: Button

    private val statusChecker = object : Runnable {
        override fun run() {
            updateConnectionStatus()
            handler.postDelayed(this, 2000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefs = getSharedPreferences(DesktopPresentation.PREFS_NAME, Context.MODE_PRIVATE)

        bindViews()
        setupListeners()
        updateButtonStates()
        checkShizukuStatus()
    }

    override fun onResume() {
        super.onResume()
        handler.post(statusChecker)
    }

    override fun onPause() {
        handler.removeCallbacks(statusChecker)
        super.onPause()
    }

    private fun bindViews() {
        tvAutoStatus = findViewById(R.id.tvAutoStatus)
        tvShizukuStatus = findViewById(R.id.tvShizukuStatus)

        btnLayout70 = findViewById(R.id.btnLayout70)
        btnLayout50 = findViewById(R.id.btnLayout50)
        btnLayout3Pane = findViewById(R.id.btnLayout3Pane)
        btnLayoutFull = findViewById(R.id.btnLayoutFull)

        btnPane1Maps = findViewById(R.id.btnPane1Maps)
        btnPane1Vietmap = findViewById(R.id.btnPane1Vietmap)
        btnPane1OsmDark = findViewById(R.id.btnPane1OsmDark)
        btnPane1YouTube = findViewById(R.id.btnPane1YouTube)

        btnPane2YouTube = findViewById(R.id.btnPane2YouTube)
        btnPane2TikTok = findViewById(R.id.btnPane2TikTok)
        btnPane2Zing = findViewById(R.id.btnPane2Zing)
        btnPane2Vietmap = findViewById(R.id.btnPane2Vietmap)

        btnPane3TikTok = findViewById(R.id.btnPane3TikTok)
        btnPane3YouTube = findViewById(R.id.btnPane3YouTube)
        btnPane3Vietmap = findViewById(R.id.btnPane3Vietmap)
        btnPane3Zing = findViewById(R.id.btnPane3Zing)

        btnToggleCarEditMode = findViewById(R.id.btnToggleCarEditMode)
    }

    private fun setupListeners() {
        // Layout Mode
        btnLayout70.setOnClickListener {
            setLayoutMode(DesktopPresentation.LAYOUT_70_30)
        }
        btnLayout50.setOnClickListener {
            setLayoutMode(DesktopPresentation.LAYOUT_50_50)
        }
        btnLayout3Pane.setOnClickListener {
            setLayoutMode(DesktopPresentation.LAYOUT_3_PANE)
        }
        btnLayoutFull.setOnClickListener {
            setLayoutMode(DesktopPresentation.LAYOUT_FULL)
        }

        // Pane 1 Apps
        btnPane1Maps.setOnClickListener { setPaneApp(1, DesktopPresentation.APP_MAPS) }
        btnPane1Vietmap.setOnClickListener { setPaneApp(1, DesktopPresentation.APP_VIETMAP) }
        btnPane1OsmDark.setOnClickListener { setPaneApp(1, DesktopPresentation.APP_OSM_DARK) }
        btnPane1YouTube.setOnClickListener { setPaneApp(1, DesktopPresentation.APP_YOUTUBE) }

        // Pane 2 Apps
        btnPane2YouTube.setOnClickListener { setPaneApp(2, DesktopPresentation.APP_YOUTUBE) }
        btnPane2TikTok.setOnClickListener { setPaneApp(2, DesktopPresentation.APP_TIKTOK) }
        btnPane2Zing.setOnClickListener { setPaneApp(2, DesktopPresentation.APP_ZING) }
        btnPane2Vietmap.setOnClickListener { setPaneApp(2, DesktopPresentation.APP_VIETMAP) }

        // Pane 3 Apps
        btnPane3TikTok.setOnClickListener { setPaneApp(3, DesktopPresentation.APP_TIKTOK) }
        btnPane3YouTube.setOnClickListener { setPaneApp(3, DesktopPresentation.APP_YOUTUBE) }
        btnPane3Vietmap.setOnClickListener { setPaneApp(3, DesktopPresentation.APP_VIETMAP) }
        btnPane3Zing.setOnClickListener { setPaneApp(3, DesktopPresentation.APP_ZING) }

        // Quick Controls
        btnToggleCarEditMode.setOnClickListener {
            DesktopPresentation.activeInstance?.toggleEditMode()
            val isEdit = DesktopPresentation.activeInstance?.isEditMode ?: false
            btnToggleCarEditMode.text = if (isEdit) "🎨 Đang bật viền cam tùy biến trên xe (Chạm để tắt)" else "🎨 Bật viền cam tùy biến trực quan trên màn hình xe"
            Toast.makeText(this, if (isEdit) "Đã bật chế độ viền cam trên xe!" else "Đã tắt chế độ tùy biến", Toast.LENGTH_SHORT).show()
        }

        val btnSwapPanes: Button = findViewById(R.id.btnSwapPanes)
        btnSwapPanes.setOnClickListener {
            DesktopPresentation.activeInstance?.swapPanes()
            Toast.makeText(this, "Đã đảo vị trí các ô trên xe", Toast.LENGTH_SHORT).show()
            updateButtonStates()
        }

        val btnToggleSpeedBubble: Button = findViewById(R.id.btnToggleSpeedBubble)
        btnToggleSpeedBubble.setOnClickListener {
            DesktopPresentation.activeInstance?.toggleBubble()
            val isVisible = DesktopPresentation.activeInstance?.isBubbleVisible ?: true
            Toast.makeText(this, if (isVisible) "Đã bật biển tốc độ" else "Đã tắt biển tốc độ", Toast.LENGTH_SHORT).show()
        }

        val btnCheckAdb: Button = findViewById(R.id.btnCheckAdb)
        btnCheckAdb.setOnClickListener {
            checkShizukuStatus()
            Toast.makeText(this, "Đang kiểm tra kết nối Shizuku / Wireless ADB...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setLayoutMode(mode: Int) {
        DesktopPresentation.activeInstance?.setLayoutMode(mode)
        prefs.edit().putInt(DesktopPresentation.KEY_LAYOUT_MODE, mode).apply()
        updateButtonStates()
        Toast.makeText(this, "Đã đổi bố cục sang: ${getLayoutName(mode)}", Toast.LENGTH_SHORT).show()
    }

    private fun setPaneApp(paneIndex: Int, appKey: String) {
        DesktopPresentation.activeInstance?.setAppForPane(paneIndex, appKey)
        val key = when (paneIndex) {
            1 -> DesktopPresentation.KEY_PANE_A_APP
            2 -> DesktopPresentation.KEY_PANE_B1_APP
            3 -> DesktopPresentation.KEY_PANE_B2_APP
            else -> return
        }
        prefs.edit().putString(key, appKey).apply()
        updateButtonStates()
        Toast.makeText(this, "Ô $paneIndex đã chọn: $appKey", Toast.LENGTH_SHORT).show()
    }

    private fun updateButtonStates() {
        val currentLayout = DesktopPresentation.activeInstance?.currentLayoutMode ?: prefs.getInt(DesktopPresentation.KEY_LAYOUT_MODE, DesktopPresentation.LAYOUT_70_30)
        val currentPane1 = DesktopPresentation.activeInstance?.currentPaneAApp ?: prefs.getString(DesktopPresentation.KEY_PANE_A_APP, DesktopPresentation.APP_MAPS) ?: DesktopPresentation.APP_MAPS
        val currentPane2 = DesktopPresentation.activeInstance?.currentPaneB1App ?: prefs.getString(DesktopPresentation.KEY_PANE_B1_APP, DesktopPresentation.APP_YOUTUBE) ?: DesktopPresentation.APP_YOUTUBE
        val currentPane3 = DesktopPresentation.activeInstance?.currentPaneB2App ?: prefs.getString(DesktopPresentation.KEY_PANE_B2_APP, DesktopPresentation.APP_TIKTOK) ?: DesktopPresentation.APP_TIKTOK

        // Layout buttons
        setButtonActive(btnLayout70, currentLayout == DesktopPresentation.LAYOUT_70_30)
        setButtonActive(btnLayout50, currentLayout == DesktopPresentation.LAYOUT_50_50)
        setButtonActive(btnLayout3Pane, currentLayout == DesktopPresentation.LAYOUT_3_PANE)
        setButtonActive(btnLayoutFull, currentLayout == DesktopPresentation.LAYOUT_FULL)

        // Pane 1 buttons
        setButtonActive(btnPane1Maps, currentPane1 == DesktopPresentation.APP_MAPS)
        setButtonActive(btnPane1Vietmap, currentPane1 == DesktopPresentation.APP_VIETMAP)
        setButtonActive(btnPane1OsmDark, currentPane1 == DesktopPresentation.APP_OSM_DARK)
        setButtonActive(btnPane1YouTube, currentPane1 == DesktopPresentation.APP_YOUTUBE)

        // Pane 2 buttons
        setButtonActive(btnPane2YouTube, currentPane2 == DesktopPresentation.APP_YOUTUBE)
        setButtonActive(btnPane2TikTok, currentPane2 == DesktopPresentation.APP_TIKTOK)
        setButtonActive(btnPane2Zing, currentPane2 == DesktopPresentation.APP_ZING)
        setButtonActive(btnPane2Vietmap, currentPane2 == DesktopPresentation.APP_VIETMAP)

        // Pane 3 buttons
        setButtonActive(btnPane3TikTok, currentPane3 == DesktopPresentation.APP_TIKTOK)
        setButtonActive(btnPane3YouTube, currentPane3 == DesktopPresentation.APP_YOUTUBE)
        setButtonActive(btnPane3Vietmap, currentPane3 == DesktopPresentation.APP_VIETMAP)
        setButtonActive(btnPane3Zing, currentPane3 == DesktopPresentation.APP_ZING)
    }

    private fun setButtonActive(btn: Button, isActive: Boolean) {
        val color = if (isActive) Color.parseColor("#2563EB") else Color.parseColor("#334155")
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun getLayoutName(mode: Int): String {
        return when (mode) {
            DesktopPresentation.LAYOUT_70_30 -> "70 / 30"
            DesktopPresentation.LAYOUT_50_50 -> "50 / 50"
            DesktopPresentation.LAYOUT_3_PANE -> "3 Ô"
            DesktopPresentation.LAYOUT_FULL -> "Toàn màn hình"
            else -> "Tùy biến"
        }
    }

    private fun updateConnectionStatus() {
        if (DesktopPresentation.activeInstance != null) {
            tvAutoStatus.text = "🟢 ĐÃ KẾT NỐI XE VF5 QUA ANDROID AUTO (Màn hình xe đang hoạt động)"
            tvAutoStatus.setTextColor(Color.parseColor("#34D399"))
        } else {
            tvAutoStatus.text = "⚪ Đang chờ cắm USB kết nối Android Auto trên xe VF5"
            tvAutoStatus.setTextColor(Color.parseColor("#94A3B8"))
        }
    }

    private fun checkShizukuStatus() {
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
            tvShizukuStatus.setTextColor(Color.parseColor("#38BDF8"))
        }
    }
}
