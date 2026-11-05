package dev.fstream.tv.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.content.pm.ApplicationInfo
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView

/** Single process-wide WebView (D4). Never destroyed. */
object WebViewHolder {
    private var wv: WebView? = null
    private var ctx: MutableContextWrapper? = null
    private var appContext: Context? = null

    val webView: WebView? get() = wv
    val isAttached: Boolean get() = wv?.parent != null

    @SuppressLint("SetJavaScriptEnabled")
    private fun obtain(app: Context): WebView {
        wv?.let { return it }
        appContext = app
        val wrapper = MutableContextWrapper(app)
        ctx = wrapper
        if (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        val w = WebView(wrapper)
        w.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true // route every window.open to onCreateWindow, which rejects + counts it
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        w.setBackgroundColor(0xFF000000.toInt())
        wv = w
        return w
    }

    fun attach(activity: Activity, parent: ViewGroup): WebView {
        val w = obtain(activity.applicationContext)
        (w.parent as? ViewGroup)?.removeView(w)
        ctx?.baseContext = activity
        w.onResume()
        parent.addView(w, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return w
    }

    fun detach() {
        val w = wv ?: return
        w.loadUrl("about:blank")
        w.onPause()
        (w.parent as? ViewGroup)?.removeView(w)
        appContext?.let { ctx?.baseContext = it }
    }
}
