package dev.fstream.tv.player

import android.os.SystemClock
import android.view.KeyEvent
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.fstream.tv.model.PlayableSource
import dev.fstream.tv.model.SourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PlayerPageTest {
    private val base = "https://appassets.androidplatform.net/assets/testpages/"
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun launch(page: String, ok: OkStrategy = OkStrategy.JS_PLAY, alwaysHud: Boolean = false): ActivityScenario<PlayerActivity> {
        val i = PlayerActivity.intent(ctx, PlayableSource(SourceKind.EMBED, base + page), "t", PlayerOptions(okStrategy = ok, alwaysShowHud = alwaysHud, autoStart = false))
        return ActivityScenario.launch(i)
    }

    private fun js(script: String): String {
        val latch = CountDownLatch(1)
        var out = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            WebViewHolder.webView!!.evaluateJavascript(script) { out = it ?: ""; latch.countDown() }
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS))
        return out
    }

    private fun waitFor(timeoutMs: Long = 10000, cond: () -> Boolean): Boolean {
        val end = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < end) {
            if (cond()) return true
            Thread.sleep(200)
        }
        return cond()
    }

    private fun url(): String {
        var u = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync { u = WebViewHolder.webView!!.url ?: "" }
        return u
    }

    private fun pressOk(s: ActivityScenario<PlayerActivity>) {
        s.onActivity { a ->
            a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
            a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        }
    }

    @Test fun popupIsBlockedAndPageStays() {
        launch("popup.html").use { s ->
            var popups = 0
            assertTrue(waitFor { s.onActivity { popups = it.stats.blockedPopups.get() }; popups >= 1 })
            assertTrue(url().startsWith(base + "popup.html"))
        }
    }

    @Test fun navigationAwayIsBlocked() {
        launch("navaway.html").use { s ->
            var navs = 0
            assertTrue(waitFor { s.onActivity { navs = it.stats.blockedNavigations.get() }; navs >= 1 })
            assertTrue(url().startsWith(base + "navaway.html"))
        }
    }

    @Test fun jsPlayStartsVideo() {
        launch("clicktoplay.html", OkStrategy.JS_PLAY).use { s ->
            assertTrue(waitFor { js("document.getElementById('v')!==null") == "true" })
            assertEquals("true", js("document.getElementById('v').paused"))
            pressOk(s)
            assertTrue(waitFor { js("document.getElementById('v').paused") == "false" })
        }
    }

    @Test fun centerTapStartsVideo() {
        launch("clicktoplay.html", OkStrategy.CENTER_TAP).use { s ->
            assertTrue(waitFor { js("document.getElementById('v')!==null") == "true" })
            assertEquals("true", js("document.getElementById('v').paused"))
            pressOk(s)
            assertTrue(waitFor { js("document.getElementById('v').paused") == "false" })
        }
    }

    @Test fun hudPageAutoplays() {
        launch("hud.html").use {
            assertTrue(waitFor { js("document.getElementById('v').currentTime>0") == "true" })
        }
    }

    private fun hudText(s: ActivityScenario<PlayerActivity>): String {
        var t = ""
        s.onActivity { t = it.findViewById<android.view.View>(android.R.id.content).let { c -> findHud(c)?.text?.toString() ?: "" } }
        return t
    }

    private fun findHud(v: android.view.View): android.widget.TextView? {
        if (v is android.widget.TextView) return v
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) findHud(v.getChildAt(i))?.let { return it }
        return null
    }

    @Test fun hudShowsLiveValuesWhenAlwaysOn() {
        launch("hud.html", alwaysHud = true).use { s ->
            val ok = Regex("state: playing\\nttff: \\d+ms  dropped/total: \\d+/\\d+\\nmem: \\d+MB")
            assertTrue(waitFor(15000) { ok.containsMatchIn(hudText(s)) })
            val first = hudText(s)
            Thread.sleep(1500)
            assertTrue(hudText(s) != first)
        }
    }

    @Test fun hudHiddenWhilePlayingWhenAlwaysOff() {
        launch("hud.html").use { s ->
            assertTrue(waitFor { js("document.getElementById('v').currentTime>0") == "true" })
            var vis = -1
            assertTrue(waitFor(8000) { s.onActivity { vis = findHud(it.findViewById(android.R.id.content))!!.visibility }; vis == android.view.View.GONE })
        }
    }

    @Test fun backFinishesAndBlanks() {
        launch("hud.html").use { s ->
            s.onActivity { a ->
                a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
                a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
            }
            assertTrue(waitFor { s.state == androidx.lifecycle.Lifecycle.State.DESTROYED })
        }
    }
}
