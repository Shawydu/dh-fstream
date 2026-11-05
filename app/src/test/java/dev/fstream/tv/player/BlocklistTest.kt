package dev.fstream.tv.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlocklistTest {
    private val b = Blocklist()

    @Test fun exactAndSubdomainMatch() {
        assertTrue(b.matches("doubleclick.net"))
        assertTrue(b.matches("ad.doubleclick.net"))
        assertTrue(b.matches("A.B.Popads.NET"))
        assertTrue(b.matches("adservice.google.com"))
    }

    @Test fun noPartialStringMatch() {
        assertFalse(b.matches("notdoubleclick.net"))
        assertFalse(b.matches("doubleclick.net.evil.org"))
        assertFalse(b.matches("google.com"))
    }

    @Test fun nullAndEmpty() {
        assertFalse(b.matches(null))
        assertFalse(b.matches(""))
    }

    @Test fun customSet() {
        val c = Blocklist(setOf(".foo.com"))
        assertTrue(c.matches("x.foo.com"))
        assertFalse(c.matches("doubleclick.net"))
    }
}
