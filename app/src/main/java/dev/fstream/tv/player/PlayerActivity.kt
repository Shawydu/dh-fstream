package dev.fstream.tv.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.webkit.WebViewAssetLoader
import dev.fstream.tv.model.PlayableSource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI

class PlayerActivity : ComponentActivity() {
    val stats = PlayerStats()
    lateinit var okStrategies: OkStrategies
        private set

    private lateinit var root: FrameLayout
    private lateinit var webContainer: FrameLayout
    private lateinit var customContainer: FrameLayout
    private lateinit var cursor: CursorOverlay
    private lateinit var hud: TextView
    private lateinit var options: PlayerOptions
    private var webView: WebView? = null
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private val handler = Handler(Looper.getMainLooper())

    // HUD state
    private var loadStartMs = 0L
    private var pageFinished = false
    private var errorText: String? = null
    private var ttffMs: Long? = null
    private var lastProbe: ProbeResult? = null
    private var lastStatus = ""
    private var tick = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = Json.decodeFromString<PlayableSource>(intent.getStringExtra(EXTRA_SOURCE)!!)
        options = intent.getStringExtra(EXTRA_OPTIONS)?.let { Json.decodeFromString<PlayerOptions>(it) } ?: PlayerOptions()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setDecorFitsSystemWindows(false)

        root = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        webContainer = FrameLayout(this)
        customContainer = FrameLayout(this).apply { visibility = View.GONE; setBackgroundColor(0xFF000000.toInt()) }
        cursor = CursorOverlay(this)
        hud = TextView(this).apply {
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x99000000.toInt())
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(16, 8, 16, 8)
            visibility = if (options.showHud) View.VISIBLE else View.GONE
        }
        val match = FrameLayout.LayoutParams.MATCH_PARENT
        root.addView(webContainer, FrameLayout.LayoutParams(match, match))
        root.addView(customContainer, FrameLayout.LayoutParams(match, match))
        root.addView(cursor, FrameLayout.LayoutParams(match, match))
        root.addView(hud, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START))
        setContentView(root)

        val embedHost = hostOf(source.url)
        val policy = NavigationPolicy(embedHost, Blocklist())
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(applicationContext))
            .build()

        val wv = WebViewHolder.attach(this, webContainer)
        webView = wv
        wv.webViewClient = PlayerWebViewClient(
            policy, options.blocklistEnabled, assetLoader, stats,
            onStarted = { runOnUiThread { handler.removeCallbacks(autoStart); pageFinished = false; errorText = null } },
            onFinished = { runOnUiThread { pageFinished = true; scheduleAutoStart() } },
            onError = { e -> runOnUiThread { errorText = e } },
        )
        wv.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                stats.blockedPopups.incrementAndGet()
                return false
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) { callback.onCustomViewHidden(); return }
                customView = view
                customCallback = callback
                customContainer.addView(view, FrameLayout.LayoutParams(match, match))
                customContainer.visibility = View.VISIBLE
                webContainer.visibility = View.GONE
            }

            override fun onHideCustomView() { exitCustomView() }
        }
        okStrategies = OkStrategies(wv, cursor, options.okStrategy, { customView ?: wv }, { lastStatus = it })

        onBackPressedDispatcher.addCallback(this) { handleBack() }

        loadStartMs = SystemClock.elapsedRealtime()
        wv.loadUrl(source.url, source.headers)
        handler.post(ticker)
    }


    // Once-per-load center tap so the viewer doesn't have to press OK; skipped if the probe already sees playing video.
    private val autoStart = Runnable {
        webView?.evaluateJavascript(ProbeResult.JS) { r ->
            val p = ProbeResult.parse(r)
            if (p != null && p.videoVisible && !p.paused) return@evaluateJavascript
            okStrategies.tapCenter()
            lastStatus = "auto-start tap"
        }
    }

    private fun scheduleAutoStart() {
        if (!options.autoStart) return
        handler.removeCallbacks(autoStart)
        handler.postDelayed(autoStart, AUTO_START_DELAY_MS)
    }

    private fun exitCustomView() {
        val v = customView ?: return
        customContainer.removeView(v)
        customContainer.visibility = View.GONE
        webContainer.visibility = View.VISIBLE
        customView = null
        customCallback?.onCustomViewHidden()
        customCallback = null
    }

    private fun handleBack() {
        handler.removeCallbacks(autoStart)
        if (customView != null) {
            exitCustomView()
        } else {
            WebViewHolder.detach()
            finish()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (code == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) handleBack()
            return true // page never sees BACK
        }
        if (okStrategies.isOk(code)) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) okStrategies.onOk()
            return true
        }
        if (okStrategies.onArrow(code)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        if (WebViewHolder.isAttached) webView?.onResume()
    }

    override fun onStop() {
        handler.removeCallbacks(autoStart)
        webView?.onPause()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) window.insetsController?.hide(WindowInsets.Type.systemBars())
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (customView != null) exitCustomView()
        if (WebViewHolder.isAttached && webContainer.childCount > 0) WebViewHolder.detach()
        super.onDestroy()
    }

    // ---- HUD ----
    private val ticker = object : Runnable {
        override fun run() {
            tick++
            val wv = webView
            if (wv != null && pageFinished && (ttffMs == null || tick % 4 == 0)) {
                wv.evaluateJavascript(ProbeResult.JS) { r ->
                    val p = ProbeResult.parse(r)
                    lastProbe = p
                    if (p != null && p.found && p.currentTime > 0 && ttffMs == null) {
                        ttffMs = SystemClock.elapsedRealtime() - loadStartMs
                    }
                }
            }
            if (tick % 4 == 0 && options.showHud) hud.text = hudText()
            handler.postDelayed(this, 250)
        }
    }

    private fun hudText(): String {
        val p = lastProbe
        val state = when {
            errorText != null -> "error"
            !pageFinished -> "loading"
            p != null && p.videoVisible -> if (p.paused) "paused" else "playing"
            p?.crossOrigin == true -> "page loaded (video cross-origin, state unknown)"
            else -> "loaded"
        }
        val frames = if (p?.dropped != null && p.total != null) "${p.dropped}/${p.total}" else "n/a"
        val ttff = ttffMs?.let { "${it}ms" } ?: if (p?.crossOrigin == true) "n/a" else "..."
        val mb = Debug.getPss() / 1024
        return buildString {
            append("state: $state").append(errorText?.let { " ($it)" } ?: "").append('\n')
            append("ttff: $ttff  dropped/total: $frames\n")
            append("mem: ${mb}MB  blocked req: ${stats.blockedRequests.get()}  popups: ${stats.blockedPopups.get()}  nav: ${stats.blockedNavigations.get()}\n")
            append("ok: ${options.okStrategy}")
            if (lastStatus.isNotEmpty()) append("\n").append(lastStatus)
        }
    }

    companion object {
        const val AUTO_START_DELAY_MS = 800L
        const val EXTRA_SOURCE = "source"
        const val EXTRA_TITLE = "title"
        const val EXTRA_OPTIONS = "options"

        fun hostOf(url: String): String = runCatching { URI(url).host }.getOrNull() ?: ""

        fun intent(context: Context, source: PlayableSource, title: String, options: PlayerOptions = PlayerOptions()): Intent {
            val intent = Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_SOURCE, Json.encodeToString(source))
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_OPTIONS, Json.encodeToString(options))
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        }

        fun start(context: Context, source: PlayableSource, title: String, options: PlayerOptions = PlayerOptions()) {
            context.startActivity(intent(context, source, title, options))
        }
    }
}
