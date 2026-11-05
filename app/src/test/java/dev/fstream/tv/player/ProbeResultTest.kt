package dev.fstream.tv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbeResultTest {
    private fun p(found: Boolean, t: Double, xo: Boolean) = ProbeResult(found, false, t, null, null, xo)

    @Test
    fun hudHiddenOnlyWhilePlaying() {
        val playing = ProbeResult(true, false, 5.0, null, null, false)
        assertFalse(ProbeResult.hudVisible(playing, error = false, always = false))
        assertTrue(ProbeResult.hudVisible(playing, error = false, always = true))
        assertTrue(ProbeResult.hudVisible(playing, error = true, always = false))
        assertTrue(ProbeResult.hudVisible(playing.copy(paused = true), false, false))
        assertTrue(ProbeResult.hudVisible(null, false, false))
        assertTrue(ProbeResult.hudVisible(p(true, 0.0, true), false, false))
    }

    @Test
    fun crossOriginWithoutAdvancingVideoIsNotVisible() {
        assertFalse(p(true, 0.0, true).videoVisible)
        assertFalse(p(false, 0.0, true).videoVisible)
        assertTrue(p(true, 5.0, true).videoVisible)
        assertTrue(p(true, 0.0, false).videoVisible)
    }
}
