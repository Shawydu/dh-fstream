package dev.fstream.tv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StallDetectorTest {
    private fun p(t: Double, paused: Boolean = false, found: Boolean = true) = ProbeResult(found, paused, t, null, null, false)

    @Test fun advancingNeverReloads() {
        val d = StallDetector()
        for (i in 0..60) assertFalse(d.onProbe(p(1.0 + i), i * 1000L))
    }

    @Test fun stalledReloadsAfter15sThenCapsAtThree() {
        val d = StallDetector()
        var now = 0L
        repeat(3) {
            assertFalse(d.onProbe(p(5.0), now))
            assertFalse(d.onProbe(p(5.0), now + 14_000))
            assertTrue(d.onProbe(p(5.0), now + 15_000))
            now += 100_000
        }
        assertEquals(3, d.reloads)
        d.onProbe(p(5.0), now); assertFalse(d.onProbe(p(5.0), now + 20_000))
    }

    @Test fun pausedOrNeverPlayedOrNoVideoBeforePlayDoesNotReload() {
        val d = StallDetector()
        assertFalse(d.onProbe(null, 0)); assertFalse(d.onProbe(p(0.0), 60_000))
        assertFalse(d.onProbe(p(5.0, paused = true), 100_000)); assertFalse(d.onProbe(p(5.0, paused = true), 200_000))
    }

    @Test fun videoVanishingAfterPlayCountsAsStall() {
        val d = StallDetector()
        d.onProbe(p(5.0), 0)
        assertFalse(d.onProbe(null, 10_000)); assertTrue(d.onProbe(p(0.0, found = false), 15_000))
    }
}
