package dev.fstream.tv.player

import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView

/** D6 remote-input strategies. [targetView] is the view that receives synthetic taps. */
class OkStrategies(
    private val webView: WebView,
    private val cursor: CursorOverlay,
    private val strategy: OkStrategy,
    private val targetView: () -> View,
    private val onStatus: (String) -> Unit = {},
) {
    private var lastMove = 0L
    private var repeat = 0

    init {
        cursor.setActive(strategy == OkStrategy.CURSOR)
    }

    fun isOk(code: Int) = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER ||
        code == KeyEvent.KEYCODE_NUMPAD_ENTER

    /** Returns true if the arrow key was consumed by the cursor. */
    fun onArrow(code: Int): Boolean {
        if (strategy != OkStrategy.CURSOR) return false
        val (ux, uy) = when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> -1 to 0
            KeyEvent.KEYCODE_DPAD_RIGHT -> 1 to 0
            KeyEvent.KEYCODE_DPAD_UP -> 0 to -1
            KeyEvent.KEYCODE_DPAD_DOWN -> 0 to 1
            else -> return false
        }
        val now = SystemClock.uptimeMillis()
        repeat = if (now - lastMove < 250) repeat + 1 else 0
        lastMove = now
        val step = (12f + repeat * 6f).coerceAtMost(120f)
        cursor.moveBy(ux * step, uy * step)
        return true
    }

    fun onOk() {
        when (strategy) {
            OkStrategy.JS_PLAY -> jsPlay { found ->
                onStatus(if (found) "JS_PLAY: video found" else "JS_PLAY: no video, center tap")
                if (!found) tapCenter()
            }
            OkStrategy.CENTER_TAP -> { tapCenter(); onStatus("CENTER_TAP") }
            OkStrategy.CURSOR -> { tap(cursor.cursorX, cursor.cursorY); onStatus("CURSOR tap") }
        }
    }

    fun jsPlay(cb: (Boolean) -> Unit) {
        webView.evaluateJavascript(JS_PLAY) { cb(it == "true") }
    }

    fun tapCenter() {
        val v = targetView()
        tap(v.width / 2f, v.height / 2f)
    }

    fun tap(x: Float, y: Float) {
        val v = targetView()
        val t = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(t, t + 60, MotionEvent.ACTION_UP, x, y, 0)
        v.dispatchTouchEvent(down)
        v.dispatchTouchEvent(up)
        down.recycle(); up.recycle()
    }

    companion object {
        /** Plays the first <video> in the top document or same-origin iframes; returns true if one was found. */
        const val JS_PLAY = """(function(){
  function f(d){
    var v=d.querySelector('video');
    if(v){var p=v.play(); if(p&&p.catch){p.catch(function(){});} return true;}
    var fr=d.querySelectorAll('iframe');
    for(var i=0;i<fr.length;i++){try{var c=fr[i].contentDocument; if(c&&f(c)){return true;}}catch(e){}}
    return false;
  }
  return f(document);
})()"""
    }
}
