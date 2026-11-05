package dev.fstream.tv.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/** Virtual pointer drawn over the WebView; moved with d-pad in CURSOR mode. */
class CursorOverlay(context: Context) : View(context) {
    var cursorX = 0f
        private set
    var cursorY = 0f
        private set
    private var placed = false
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF000000.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val path = Path()

    init {
        visibility = GONE
        isFocusable = false
    }

    fun setActive(active: Boolean) {
        visibility = if (active) VISIBLE else GONE
    }

    fun moveBy(dx: Float, dy: Float) {
        ensurePlaced()
        cursorX = (cursorX + dx).coerceIn(0f, (width - 1).coerceAtLeast(0).toFloat())
        cursorY = (cursorY + dy).coerceIn(0f, (height - 1).coerceAtLeast(0).toFloat())
        invalidate()
    }

    private fun ensurePlaced() {
        if (!placed && width > 0) {
            cursorX = width / 2f; cursorY = height / 2f; placed = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        ensurePlaced()
        path.reset()
        path.moveTo(cursorX, cursorY)
        path.lineTo(cursorX, cursorY + 40f)
        path.lineTo(cursorX + 10f, cursorY + 31f)
        path.lineTo(cursorX + 20f, cursorY + 38f)
        path.lineTo(cursorX + 28f, cursorY + 28f)
        path.lineTo(cursorX + 18f, cursorY + 22f)
        path.lineTo(cursorX + 30f, cursorY + 18f)
        path.close()
        canvas.drawPath(path, fill)
        canvas.drawPath(path, stroke)
    }
}
