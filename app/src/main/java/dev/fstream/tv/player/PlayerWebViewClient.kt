package dev.fstream.tv.player

import android.graphics.Bitmap
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

class PlayerStats {
    val blockedRequests = AtomicInteger()
    val blockedPopups = AtomicInteger()
    val blockedNavigations = AtomicInteger()
}

class PlayerWebViewClient(
    private val policy: NavigationPolicy,
    private val blocklistEnabled: Boolean,
    private val assetLoader: WebViewAssetLoader?,
    private val stats: PlayerStats,
    private val onStarted: () -> Unit,
    private val onFinished: () -> Unit,
    private val onError: (String) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val host = request.url.host
        if (policy.allowNavigation(host, request.isForMainFrame)) return false
        stats.blockedNavigations.incrementAndGet()
        Log.i(TAG, "blocked navigation ${request.url} mainFrame=${request.isForMainFrame}")
        return true
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        assetLoader?.shouldInterceptRequest(request.url)?.let { return it }
        if (policy.shouldBlockRequest(request.url.host, blocklistEnabled)) {
            stats.blockedRequests.incrementAndGet()
            return WebResourceResponse("text/plain", "utf-8", 204, "No Content", emptyMap(), ByteArrayInputStream(ByteArray(0)))
        }
        return null
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        if (url != "about:blank") onStarted()
    }

    override fun onPageFinished(view: WebView, url: String?) {
        if (url != "about:blank") onFinished()
    }

    override fun onRenderProcessGone(view: WebView, detail: android.webkit.RenderProcessGoneDetail): Boolean {
        Log.e(TAG, "renderer gone crashed=${detail.didCrash()}")
        onError("renderer gone")
        return true
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
        if (request.isForMainFrame) onError("${error.errorCode} ${error.description}")
    }

    companion object { private const val TAG = "PlayerWebViewClient" }
}
