package dev.fstream.tv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbeResultTest {
    private fun p(found: Boolean, t: Double, xo: Boolean) = ProbeResult(found, false, t, null, null, xo)

    @Test
    fun crossOriginWithoutAdvancingVideoIsNotVisible() {
        assertFalse(p(true, 0.0, true).videoVisible)
        assertFalse(p(false, 0.0, true).videoVisible)
        assertTrue(p(true, 5.0, true).videoVisible)
        assertTrue(p(true, 0.0, false).videoVisible)
    }
}
