package dev.fstream.tv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPolicyTest {
    private val p = NavigationPolicy("embed.st")

    @Test fun allowsMainFrameOnEmbedHostAndSubdomains() {
        assertTrue(p.allowNavigation("embed.st", true))
        assertTrue(p.allowNavigation("cdn.embed.st", true))
    }

    @Test fun blocksOtherHostsInMainFrame() {
        assertFalse(p.allowNavigation("example.com", true))
        assertFalse(p.allowNavigation("evilembed.st", true))
        assertFalse(p.allowNavigation(null, true))
    }

    @Test fun allowsSubframesExceptBlocklistedHosts() {
        assertTrue(p.allowNavigation("embed.st", false))
        assertTrue(p.allowNavigation("player.example.com", false))
        assertFalse(p.allowNavigation("ads.doubleclick.net", false))
    }

    @Test fun blocklistRespectsToggle() {
        assertTrue(p.shouldBlockRequest("ads.doubleclick.net", true))
        assertFalse(p.shouldBlockRequest("ads.doubleclick.net", false))
        assertFalse(p.shouldBlockRequest("embed.st", true))
    }
}
