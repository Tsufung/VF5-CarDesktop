package com.vf5plus.desktop

import android.app.Presentation
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DesktopPresentation(
    private val themedContext: Context,
    display: Display
) : Presentation(themedContext, display, android.R.style.Theme_DeviceDefault_NoActionBar) {

    companion object {
        var activeInstance: DesktopPresentation? = null
            private set

        const val PREFS_NAME = "vf5_desktop_prefs"
        const val KEY_LAYOUT_MODE = "layout_mode"
        const val KEY_PANE_A_APP = "pane_a_app"
        const val KEY_PANE_B1_APP = "pane_b1_app"
        const val KEY_PANE_B2_APP = "pane_b2_app"
        const val KEY_DOCK_POSITION = "dock_position"
        const val KEY_BUBBLE_VISIBLE = "bubble_visible"

        const val LAYOUT_70_30 = 0
        const val LAYOUT_50_50 = 1
        const val LAYOUT_3_PANE = 2
        const val LAYOUT_FULL = 3

        const val APP_MAPS = "maps"
        const val APP_VIETMAP = "vietmap"
        const val APP_OSM_DARK = "osm_dark"
        const val APP_YOUTUBE = "youtube"
        const val APP_TIKTOK = "tiktok"
        const val APP_ZING = "zing"
    }

    private val prefs: SharedPreferences = themedContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var currentLayoutMode = LAYOUT_70_30
        private set

    var currentPaneAApp = APP_MAPS
        private set
    var currentPaneB1App = APP_YOUTUBE
        private set
    var currentPaneB2App = APP_TIKTOK
        private set
    var currentDockPosition = "right" // "right" or "bottom"
        private set
    var isBubbleVisible = true
        private set
    var isEditMode = false
        private set

    // UI Elements
    private lateinit var rootLayout: FrameLayout
    private lateinit var mainContentLayout: LinearLayout
    private lateinit var workspaceLayout: LinearLayout
    private lateinit var paneAContainer: FrameLayout
    private lateinit var paneBContainer: LinearLayout
    private lateinit var paneB1: FrameLayout
    private lateinit var paneB2: FrameLayout
    private lateinit var dividerView: View
    private lateinit var dockView: LinearLayout
    private lateinit var speedBubble: FrameLayout
    private lateinit var appDrawerOverlay: FrameLayout
    private lateinit var tvClock: TextView

    // Edit Mode Headers (Thanh đổi app nổi trên đầu từng ô khi tùy biến)
    private lateinit var headerPaneA: LinearLayout
    private lateinit var headerPaneB1: LinearLayout
    private lateinit var headerPaneB2: LinearLayout

    // WebViews in Panes
    private lateinit var webViewPaneA: WebView
    private lateinit var webViewPaneB1: WebView
    private lateinit var webViewPaneB2: WebView

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            if (::tvClock.isInitialized) {
                tvClock.text = timeFormat.format(Date())
            }
            handler.postDelayed(this, 10000L)
        }
    }

    init {
        activeInstance = this
        loadSavedPreferences()
        buildDesktopUi()
        handler.post(clockRunnable)
    }

    private fun loadSavedPreferences() {
        currentLayoutMode = prefs.getInt(KEY_LAYOUT_MODE, LAYOUT_70_30)
        currentPaneAApp = prefs.getString(KEY_PANE_A_APP, APP_MAPS) ?: APP_MAPS
        currentPaneB1App = prefs.getString(KEY_PANE_B1_APP, APP_YOUTUBE) ?: APP_YOUTUBE
        currentPaneB2App = prefs.getString(KEY_PANE_B2_APP, APP_TIKTOK) ?: APP_TIKTOK
        currentDockPosition = prefs.getString(KEY_DOCK_POSITION, "right") ?: "right"
        isBubbleVisible = prefs.getBoolean(KEY_BUBBLE_VISIBLE, true)
    }

    private fun savePreferences() {
        prefs.edit().apply {
            putInt(KEY_LAYOUT_MODE, currentLayoutMode)
            putString(KEY_PANE_A_APP, currentPaneAApp)
            putString(KEY_PANE_B1_APP, currentPaneB1App)
            putString(KEY_PANE_B2_APP, currentPaneB2App)
            putString(KEY_DOCK_POSITION, currentDockPosition)
            putBoolean(KEY_BUBBLE_VISIBLE, isBubbleVisible)
            apply()
        }
    }

    private fun buildDesktopUi() {
        rootLayout = FrameLayout(themedContext).apply {
            setBackgroundColor(Color.parseColor("#090D16"))
        }

        mainContentLayout = LinearLayout(themedContext).apply {
            orientation = if (currentDockPosition == "bottom") LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 1. Workspace (Multi-pane Split Windows)
        workspaceLayout = LinearLayout(themedContext).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f).apply {
                if (currentDockPosition == "bottom") {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = 0
                }
            }
        }

        // Pane A (Left / Primary)
        paneAContainer = FrameLayout(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 70f)
            setBackgroundColor(Color.parseColor("#0D1117"))
        }
        setupPaneA()

        // Divider
        dividerView = View(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(dp(4), ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#21262D"))
        }

        // Pane B (Right / Secondary)
        paneBContainer = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 30f)
            setBackgroundColor(Color.parseColor("#161B22"))
        }
        setupPaneB()

        workspaceLayout.addView(paneAContainer)
        workspaceLayout.addView(dividerView)
        workspaceLayout.addView(paneBContainer)

        // 2. Control Dock (Thanh dock đa năng cạnh phải hoặc đáy)
        setupDock()

        if (currentDockPosition == "bottom") {
            mainContentLayout.addView(workspaceLayout)
            mainContentLayout.addView(dockView)
        } else {
            mainContentLayout.addView(workspaceLayout)
            mainContentLayout.addView(dockView)
        }

        rootLayout.addView(mainContentLayout)

        // 3. Floating Speed Limit Bubble (Biển báo cảnh báo tốc độ nổi)
        setupSpeedBubble()
        rootLayout.addView(speedBubble)

        // 4. App Drawer Overlay (Lưới tất cả ứng dụng điện thoại)
        setupAppDrawerOverlay()
        rootLayout.addView(appDrawerOverlay)

        setContentView(rootLayout)

        // Áp dụng bố cục và app đã lưu
        setLayoutMode(currentLayoutMode)
        loadAppIntoPane(1, currentPaneAApp)
        loadAppIntoPane(2, currentPaneB1App)
        loadAppIntoPane(3, currentPaneB2App)
        updateEditModeUi()
    }

    // ─── Pane A (Ô Chính) ────────────────────────────────────────────────────

    private fun setupPaneA() {
        webViewPaneA = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            configureWebView(this)
        }
        paneAContainer.addView(webViewPaneA)

        // Header đổi app ô 1 (Hiện khi bật Tùy biến)
        headerPaneA = createPaneEditHeader("Ô 1 (Chính)") { selectedApp ->
            setAppForPane(1, selectedApp)
        }
        paneAContainer.addView(headerPaneA)
    }

    // ─── Pane B (Ô Phụ 1 và Ô Phụ 2) ─────────────────────────────────────────

    private fun setupPaneB() {
        // Pane B1
        paneB1 = FrameLayout(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)
        }
        webViewPaneB1 = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            configureWebView(this)
        }
        paneB1.addView(webViewPaneB1)

        headerPaneB1 = createPaneEditHeader("Ô 2 (Phụ 1)") { selectedApp ->
            setAppForPane(2, selectedApp)
        }
        paneB1.addView(headerPaneB1)

        // Pane B2
        paneB2 = FrameLayout(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)
            visibility = View.GONE
        }
        webViewPaneB2 = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            configureWebView(this)
        }
        paneB2.addView(webViewPaneB2)

        headerPaneB2 = createPaneEditHeader("Ô 3 (Phụ 2)") { selectedApp ->
            setAppForPane(3, selectedApp)
        }
        paneB2.addView(headerPaneB2)

        paneBContainer.addView(paneB1)
        paneBContainer.addView(paneB2)
    }

    // ─── Thanh Header Đổi App Nổi Trên Mỗi Ô (Chế độ Custom) ──────────────────

    private fun createPaneEditHeader(paneTitle: String, onAppSelected: (String) -> Unit): LinearLayout {
        return LinearLayout(themedContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#E61E293B"))
            setPadding(dp(8), dp(4), dp(8), dp(4))
            visibility = if (isEditMode) View.VISIBLE else View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(40)
            ).apply {
                gravity = Gravity.TOP
            }

            val tvTitle = TextView(themedContext).apply {
                text = "⚡ $paneTitle: "
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#F59E0B"))
            }
            addView(tvTitle)

            val scroll = HorizontalScrollView(themedContext).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isHorizontalScrollBarEnabled = false
            }

            val btnRow = LinearLayout(themedContext).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val apps = listOf(
                "🗺️ Maps" to APP_MAPS,
                "📍 Vietmap" to APP_VIETMAP,
                "🌙 Bản đồ Tối" to APP_OSM_DARK,
                "🎬 YouTube" to APP_YOUTUBE,
                "🎵 TikTok" to APP_TIKTOK,
                "📻 Zing MP3" to APP_ZING
            )

            for ((label, appKey) in apps) {
                val btn = TextView(themedContext).apply {
                    text = label
                    textSize = 11f
                    setTextColor(Color.WHITE)
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#334155"))
                        cornerRadius = dp(4).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = dp(6)
                    }
                    setOnClickListener {
                        onAppSelected(appKey)
                    }
                }
                btnRow.addView(btn)
            }

            scroll.addView(btnRow)
            addView(scroll)
        }
    }

    // ─── Load App Vào Từng Ô ─────────────────────────────────────────────────

    fun setAppForPane(paneIndex: Int, appKey: String) {
        when (paneIndex) {
            1 -> {
                currentPaneAApp = appKey
                loadAppIntoPane(1, appKey)
            }
            2 -> {
                currentPaneB1App = appKey
                loadAppIntoPane(2, appKey)
            }
            3 -> {
                currentPaneB2App = appKey
                loadAppIntoPane(3, appKey)
            }
        }
        savePreferences()
    }

    private fun loadAppIntoPane(paneIndex: Int, appKey: String) {
        val targetWebView = when (paneIndex) {
            1 -> webViewPaneA
            2 -> webViewPaneB1
            3 -> webViewPaneB2
            else -> return
        }

        handler.post {
            when (appKey) {
                APP_MAPS -> targetWebView.loadUrl("https://www.google.com/maps")
                APP_VIETMAP -> targetWebView.loadUrl("https://maps.vietmap.vn")
                APP_OSM_DARK -> {
                    // Bản đồ OpenStreetMap nền tối mượt mà không đòi API key
                    val darkOsmHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                            <style>
                                html, body, #map { height: 100%; margin: 0; padding: 0; background: #121212; }
                                .leaflet-tile { filter: invert(100%) hue-rotate(180deg) brightness(95%) contrast(90%); }
                            </style>
                        </head>
                        <body>
                            <div id="map"></div>
                            <script>
                                var map = L.map('map', {zoomControl: false}).setView([10.7769, 106.7009], 15);
                                L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                    maxZoom: 19,
                                    attribution: '© OpenStreetMap'
                                }).addTo(map);
                                if (navigator.geolocation) {
                                    navigator.geolocation.watchPosition(function(pos) {
                                        var lat = pos.coords.latitude;
                                        var lng = pos.coords.longitude;
                                        map.setView([lat, lng], 16);
                                        if (!window.carMarker) {
                                            window.carMarker = L.circleMarker([lat, lng], {radius: 9, color: '#38BDF8', fillColor: '#0284C7', fillOpacity: 0.9}).addTo(map);
                                        } else {
                                            window.carMarker.setLatLng([lat, lng]);
                                        }
                                    });
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    targetWebView.loadDataWithBaseURL("https://openstreetmap.org", darkOsmHtml, "text/html", "UTF-8", null)
                }
                APP_YOUTUBE -> targetWebView.loadUrl("https://m.youtube.com")
                APP_TIKTOK -> targetWebView.loadUrl("https://www.tiktok.com/explore")
                APP_ZING -> targetWebView.loadUrl("https://zingmp3.vn")
                else -> {
                    if (appKey.startsWith("http://") || appKey.startsWith("https://")) {
                        targetWebView.loadUrl(appKey)
                    } else {
                        targetWebView.loadUrl("https://www.google.com/search?q=$appKey")
                    }
                }
            }
        }
    }

    // ─── Control Dock (Thanh dock đa phương tiện & menu) ──────────────────────

    private fun setupDock() {
        val isBottom = currentDockPosition == "bottom"
        dockView = LinearLayout(themedContext).apply {
            orientation = if (isBottom) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#131720"))
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = if (isBottom) {
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(52)
                )
            } else {
                LinearLayout.LayoutParams(
                    dp(58),
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }

        // Digital Clock
        tvClock = TextView(themedContext).apply {
            text = timeFormat.format(Date())
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#E5E7EB"))
            gravity = Gravity.CENTER
            layoutParams = if (isBottom) {
                LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(6)
                }
            } else {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(8)
                }
            }
        }
        dockView.addView(tvClock)

        // Media Controls Widget (Play/Pause, Next, Prev)
        val btnPrev = createDockIconButton("⏮") {
            handleMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
        val btnPlayPause = createDockIconButton("⏯") {
            handleMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
        val btnNext = createDockIconButton("⏭") {
            handleMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
        }

        dockView.addView(btnPrev)
        dockView.addView(btnPlayPause)
        dockView.addView(btnNext)

        // Layout Switcher Button (🔲)
        val btnLayout = createDockIconButton("🔲") {
            cycleLayout()
        }
        dockView.addView(btnLayout)

        // Swap Panes Button (⇄)
        val btnSwap = createDockIconButton("⇄") {
            swapPanes()
        }
        dockView.addView(btnSwap)

        // Toggle Edit Mode Button (🎨 Tùy biến trực quan)
        val btnEdit = createDockIconButton("🎨") {
            toggleEditMode()
        }
        dockView.addView(btnEdit)

        // App Drawer Button (⋮⋮⋮)
        val btnApps = createDockIconButton("⋮⋮⋮") {
            toggleAppDrawer()
        }
        dockView.addView(btnApps)
    }

    private fun createDockIconButton(symbol: String, onClick: () -> Unit): TextView {
        val isBottom = currentDockPosition == "bottom"
        return TextView(themedContext).apply {
            text = symbol
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = if (isBottom) {
                LinearLayout.LayoutParams(dp(44), dp(40)).apply {
                    marginEnd = dp(4)
                }
            } else {
                LinearLayout.LayoutParams(dp(48), dp(40)).apply {
                    bottomMargin = dp(5)
                }
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E2430"))
                cornerRadius = dp(6).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    // ─── Floating Speed Limit Bubble ─────────────────────────────────────────

    private fun setupSpeedBubble() {
        val bubbleDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
            setStroke(dp(5), Color.parseColor("#DC2626")) // Viền đỏ biển báo
        }

        speedBubble = FrameLayout(themedContext).apply {
            background = bubbleDrawable
            visibility = if (isBubbleVisible) View.VISIBLE else View.GONE
            layoutParams = FrameLayout.LayoutParams(dp(50), dp(50)).apply {
                leftMargin = dp(20)
                topMargin = dp(20)
            }
            elevation = dp(12).toFloat()
        }

        val tvSpeedLimit = TextView(themedContext).apply {
            text = "60"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        speedBubble.addView(tvSpeedLimit)

        speedBubble.setOnTouchListener(object : View.OnTouchListener {
            var dX = 0f
            var dY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dX = v.x - event.rawX
                        dY = v.y - event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        v.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }
                }
                return true
            }
        })
    }

    // ─── App Drawer Overlay ──────────────────────────────────────────────────

    private fun setupAppDrawerOverlay() {
        appDrawerOverlay = FrameLayout(themedContext).apply {
            setBackgroundColor(Color.parseColor("#E6090D16"))
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { visibility = View.GONE }
        }

        val scroll = ScrollView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val container = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            padding(dp(16))
        }

        val title = TextView(themedContext).apply {
            text = "ỨNG DỤNG ĐIỆN THOẠI (CHẠM ĐỂ MỞ)"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, dp(12))
        }
        container.addView(title)

        val apps = NativeAppLauncher.getInstalledApps(themedContext)
        val grid = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
        }

        for (app in apps) {
            val item = LinearLayout(themedContext).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                padding(dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#1B2028"))
                    cornerRadius = dp(6).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(6)
                }
                setOnClickListener {
                    val success = NativeAppLauncher.launchOnDisplay(themedContext, app, display.displayId)
                    if (!success) {
                        when {
                            app.packageName.contains("youtube") -> webViewPaneA.loadUrl("https://m.youtube.com")
                            app.packageName.contains("tiktok") -> webViewPaneA.loadUrl("https://www.tiktok.com/explore")
                            app.packageName.contains("maps") -> webViewPaneA.loadUrl("https://www.google.com/maps")
                            app.packageName.contains("vietmap") -> webViewPaneA.loadUrl("https://maps.vietmap.vn")
                            else -> webViewPaneA.loadUrl("https://www.google.com/search?q=${app.label}")
                        }
                    }
                    appDrawerOverlay.visibility = View.GONE
                }
            }

            val icon = ImageView(themedContext).apply {
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                    rightMargin = dp(12)
                }
                setImageDrawable(app.icon)
            }
            val name = TextView(themedContext).apply {
                text = app.label
                textSize = 13f
                setTextColor(Color.WHITE)
            }

            item.addView(icon)
            item.addView(name)
            grid.addView(item)
        }

        container.addView(grid)
        scroll.addView(container)
        appDrawerOverlay.addView(scroll)
    }

    // ─── Tùy Biến Bố Cục (Layout Switcher) ────────────────────────────────────

    fun setLayoutMode(mode: Int) {
        currentLayoutMode = mode
        savePreferences()
        handler.post {
            when (mode) {
                LAYOUT_50_50 -> {
                    paneAContainer.visibility = View.VISIBLE
                    dividerView.visibility = View.VISIBLE
                    paneBContainer.visibility = View.VISIBLE
                    paneB2.visibility = View.GONE

                    (paneAContainer.layoutParams as LinearLayout.LayoutParams).weight = 50f
                    (paneBContainer.layoutParams as LinearLayout.LayoutParams).weight = 50f
                    (paneB1.layoutParams as LinearLayout.LayoutParams).weight = 1f
                }
                LAYOUT_70_30 -> {
                    paneAContainer.visibility = View.VISIBLE
                    dividerView.visibility = View.VISIBLE
                    paneBContainer.visibility = View.VISIBLE
                    paneB2.visibility = View.GONE

                    (paneAContainer.layoutParams as LinearLayout.LayoutParams).weight = 70f
                    (paneBContainer.layoutParams as LinearLayout.LayoutParams).weight = 30f
                    (paneB1.layoutParams as LinearLayout.LayoutParams).weight = 1f
                }
                LAYOUT_3_PANE -> {
                    paneAContainer.visibility = View.VISIBLE
                    dividerView.visibility = View.VISIBLE
                    paneBContainer.visibility = View.VISIBLE
                    paneB2.visibility = View.VISIBLE

                    (paneAContainer.layoutParams as LinearLayout.LayoutParams).weight = 60f
                    (paneBContainer.layoutParams as LinearLayout.LayoutParams).weight = 40f
                    (paneB1.layoutParams as LinearLayout.LayoutParams).weight = 1f
                    (paneB2.layoutParams as LinearLayout.LayoutParams).weight = 1f
                }
                LAYOUT_FULL -> {
                    paneAContainer.visibility = View.VISIBLE
                    dividerView.visibility = View.GONE
                    paneBContainer.visibility = View.GONE

                    (paneAContainer.layoutParams as LinearLayout.LayoutParams).weight = 100f
                }
            }
            workspaceLayout.requestLayout()
        }
    }

    fun cycleLayout() {
        val nextMode = (currentLayoutMode + 1) % 4
        setLayoutMode(nextMode)
    }

    fun swapPanes() {
        handler.post {
            val tempApp = currentPaneAApp
            currentPaneAApp = currentPaneB1App
            currentPaneB1App = tempApp

            loadAppIntoPane(1, currentPaneAApp)
            loadAppIntoPane(2, currentPaneB1App)
            savePreferences()
        }
    }

    fun toggleEditMode() {
        isEditMode = !isEditMode
        updateEditModeUi()
    }

    private fun updateEditModeUi() {
        handler.post {
            headerPaneA.visibility = if (isEditMode) View.VISIBLE else View.GONE
            headerPaneB1.visibility = if (isEditMode) View.VISIBLE else View.GONE
            headerPaneB2.visibility = if (isEditMode && currentLayoutMode == LAYOUT_3_PANE) View.VISIBLE else View.GONE

            val strokeWidth = if (isEditMode) dp(3) else 0
            val strokeColor = Color.parseColor("#F59E0B") // Màu viền cam giống trong video

            paneAContainer.background = GradientDrawable().apply {
                setColor(Color.parseColor("#0D1117"))
                if (isEditMode) setStroke(strokeWidth, strokeColor)
            }
            paneB1.background = GradientDrawable().apply {
                setColor(Color.parseColor("#161B22"))
                if (isEditMode) setStroke(strokeWidth, strokeColor)
            }
            paneB2.background = GradientDrawable().apply {
                setColor(Color.parseColor("#161B22"))
                if (isEditMode) setStroke(strokeWidth, strokeColor)
            }
        }
    }

    fun toggleAppDrawer() {
        handler.post {
            appDrawerOverlay.visibility = if (appDrawerOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    fun toggleBubble() {
        isBubbleVisible = !isBubbleVisible
        savePreferences()
        handler.post {
            speedBubble.visibility = if (isBubbleVisible) View.VISIBLE else View.GONE
        }
    }

    private fun handleMediaKey(keyCode: Int) {
        val audioManager = themedContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val eventTime = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(
            android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_DOWN, keyCode, 0)
        )
        audioManager.dispatchMediaKeyEvent(
            android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_UP, keyCode, 0)
        )
    }

    private fun configureWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
        wv.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                return !(url.startsWith("http://") || url.startsWith("https://"))
            }
        }
    }

    private fun View.padding(px: Int) {
        setPadding(px, px, px, px)
    }

    private fun dp(value: Int): Int {
        val density = themedContext.resources.displayMetrics.density
        return (value * density + 0.5f).toInt()
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(clockRunnable)
        super.onDetachedFromWindow()
    }
}
