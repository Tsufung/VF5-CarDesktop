package com.vf5plus.desktop

import android.app.Presentation
import android.content.Context
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

        const val LAYOUT_70_30 = 0
        const val LAYOUT_50_50 = 1
        const val LAYOUT_3_PANE = 2
        const val LAYOUT_FULL = 3
    }

    private var currentLayoutMode = LAYOUT_70_30
    private var isBubbleVisible = true

    // UI Elements
    private lateinit var rootLayout: FrameLayout
    private lateinit var workspaceLayout: LinearLayout
    private lateinit var paneAContainer: FrameLayout
    private lateinit var paneBContainer: LinearLayout
    private lateinit var paneB1: FrameLayout
    private lateinit var paneB2: FrameLayout
    private lateinit var dividerView: View
    private lateinit var rightDock: LinearLayout
    private lateinit var speedBubble: FrameLayout
    private lateinit var appDrawerOverlay: FrameLayout
    private lateinit var tvClock: TextView

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
        buildDesktopUi()
        handler.post(clockRunnable)
    }

    private fun buildDesktopUi() {
        rootLayout = FrameLayout(themedContext).apply {
            setBackgroundColor(Color.parseColor("#090D16"))
        }

        // 1. Workspace (Split Windows)
        workspaceLayout = LinearLayout(themedContext).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                rightMargin = dp(64) // Chừa chỗ cho Right Dock
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
        rootLayout.addView(workspaceLayout)

        // 2. Right Floating Dock (giống trong video)
        setupRightDock()
        rootLayout.addView(rightDock)

        // 3. Floating Speed Limit Bubble (Vietmap / Waze Badge)
        setupSpeedBubble()
        rootLayout.addView(speedBubble)

        // 4. App Drawer Overlay
        setupAppDrawerOverlay()
        rootLayout.addView(appDrawerOverlay)

        setContentView(rootLayout)
    }

    // ─── Pane A (Bản đồ / Trình duyệt chính) ──────────────────────────────────

    private fun setupPaneA() {
        webViewPaneA = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            configureWebView(this)
            // Load bản đồ nền tối OpenStreetMap Leaflet
            loadUrl("https://www.google.com/maps")
        }
        paneAContainer.addView(webViewPaneA)
    }

    // ─── Pane B (YouTube / TikTok / App phụ) ──────────────────────────────────

    private fun setupPaneB() {
        paneB1 = FrameLayout(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)
        }
        webViewPaneB1 = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            configureWebView(this)
            loadUrl("https://m.youtube.com")
        }
        paneB1.addView(webViewPaneB1)

        paneB2 = FrameLayout(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)
            visibility = View.GONE // Mặc định ẩn trong chế độ 2 ô
        }
        webViewPaneB2 = WebView(themedContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            configureWebView(this)
            loadUrl("https://www.tiktok.com/explore")
        }
        paneB2.addView(webViewPaneB2)

        paneBContainer.addView(paneB1)
        paneBContainer.addView(paneB2)
    }

    // ─── Floating Right Dock (Điều khiển đa phương tiện & menu) ───────────────

    private fun setupRightDock() {
        rightDock = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#1B2028"))
            setPadding(dp(4), dp(8), dp(4), dp(8))
            layoutParams = FrameLayout.LayoutParams(
                dp(64),
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.END
            }
        }

        // Digital Clock
        tvClock = TextView(themedContext).apply {
            text = timeFormat.format(Date())
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#E5E7EB"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }
        rightDock.addView(tvClock)

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

        rightDock.addView(btnPrev)
        rightDock.addView(btnPlayPause)
        rightDock.addView(btnNext)

        // Divider
        val line = View(themedContext).apply {
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(1)).apply {
                setMargins(0, dp(8), 0, dp(8))
            }
            setBackgroundColor(Color.parseColor("#374151"))
        }
        rightDock.addView(line)

        // Layout Switcher Button
        val btnLayout = createDockIconButton("🔲") {
            cycleLayout()
        }
        rightDock.addView(btnLayout)

        // Swap Panes Button
        val btnSwap = createDockIconButton("⇄") {
            swapPanes()
        }
        rightDock.addView(btnSwap)

        // App Drawer Button (Menu tất cả ứng dụng)
        val btnApps = createDockIconButton("⋮⋮⋮") {
            toggleAppDrawer()
        }
        rightDock.addView(btnApps)
    }

    private fun createDockIconButton(symbol: String, onClick: () -> Unit): TextView {
        return TextView(themedContext).apply {
            text = symbol
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(44)
            ).apply {
                bottomMargin = dp(6)
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#262C36"))
                cornerRadius = dp(8).toFloat()
            }
            setOnClickListener { onClick() }
        }
    }

    // ─── Floating Speed Limit Bubble (Vietmap / Waze) ─────────────────────────

    private fun setupSpeedBubble() {
        val bubbleDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE)
            setStroke(dp(5), Color.parseColor("#DC2626")) // Viền đỏ biển báo
        }

        speedBubble = FrameLayout(themedContext).apply {
            background = bubbleDrawable
            layoutParams = FrameLayout.LayoutParams(dp(54), dp(54)).apply {
                leftMargin = dp(24)
                topMargin = dp(24)
            }
            elevation = dp(12).toFloat()
        }

        val tvSpeedLimit = TextView(themedContext).apply {
            text = "60"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        speedBubble.addView(tvSpeedLimit)

        // Kéo thả bong bóng nổi tự do trên màn xe
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

    // ─── App Drawer Overlay (Lưới tất cả ứng dụng điện thoại) ─────────────────

    private fun setupAppDrawerOverlay() {
        appDrawerOverlay = FrameLayout(themedContext).apply {
            setBackgroundColor(Color.parseColor("#E6090D16"))
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                rightMargin = dp(64)
            }
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
            text = "ỨNG DỤNG ĐIỆN THOẠI (CHẠN ĐỂ MỞ)"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, dp(12))
        }
        container.addView(title)

        // Lấy danh sách app
        val apps = NativeAppLauncher.getInstalledApps(themedContext)
        val grid = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Tạo danh sách app item
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
                    // Mở app lên màn hình phụ qua Shizuku/ADB
                    val success = NativeAppLauncher.launchOnDisplay(themedContext, app, display.displayId)
                    if (!success) {
                        // Nếu chưa cấp ADB, mở phiên bản Web tương ứng vào Pane A
                        when {
                            app.packageName.contains("youtube") -> webViewPaneA.loadUrl("https://m.youtube.com")
                            app.packageName.contains("tiktok") -> webViewPaneA.loadUrl("https://www.tiktok.com/explore")
                            app.packageName.contains("maps") -> webViewPaneA.loadUrl("https://www.google.com/maps")
                            app.packageName.contains("facebook") -> webViewPaneA.loadUrl("https://m.facebook.com")
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

    // ─── Chuyển Bố Cục (Layout Switcher) ──────────────────────────────────────

    fun setLayoutMode(mode: Int) {
        currentLayoutMode = mode
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
            val urlA = webViewPaneA.url ?: ""
            val urlB = webViewPaneB1.url ?: ""
            if (urlA.isNotEmpty() && urlB.isNotEmpty()) {
                webViewPaneA.loadUrl(urlB)
                webViewPaneB1.loadUrl(urlA)
            }
        }
    }

    fun toggleAppDrawer() {
        handler.post {
            appDrawerOverlay.visibility = if (appDrawerOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    fun toggleBubble() {
        handler.post {
            isBubbleVisible = !isBubbleVisible
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
