package dev.fstream.tv.player

/** Decides when to reload the embed: video was playing, then currentTime stopped advancing (or the video vanished, ended, errored or sat paused) for [stallMs]. */
class StallDetector(private val stallMs: Long = 15_000, private val maxReloads: Int = 3) {
    private var everPlayed = false
    private var lastTime = 0.0
    private var lastAdvanceMs = 0L
    var reloads = 0
        private set

    /** Any key press counts as user action (a deliberate pause must not trigger a reload). */
    fun onUserAction(nowMs: Long) { lastAdvanceMs = nowMs }

    /** Feed one probe per tick; true means "reload now" (counts against the cap and resets the timer). */
    fun onProbe(p: ProbeResult?, nowMs: Long): Boolean {
        val playing = p != null && p.found && p.currentTime > 0
        val dead = p != null && p.found && (p.ended || p.mediaError || p.noSource)
        if (playing && !dead && (!everPlayed || p!!.currentTime != lastTime)) {
            // Advancing or first sighting reset the stall clock; paused-without-input, ended and errored do not.
            everPlayed = true; lastTime = p!!.currentTime; lastAdvanceMs = nowMs
            return false
        }
        if (!everPlayed || nowMs - lastAdvanceMs < stallMs || reloads >= maxReloads) return false
        reloads++
        everPlayed = false
        return true
    }
}
