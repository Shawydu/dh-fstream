package dev.fstream.tv.provider

import dev.fstream.tv.model.PlayableSource
import dev.fstream.tv.model.SourceKind
import dev.fstream.tv.model.Stream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayabilityTest {
    private fun s(k: SourceKind) = Stream("i", 1, "en", true, 1, "x", PlayableSource(k, "u"))

    @Test
    fun onlyEmbedIsPlayable() {
        assertTrue(s(SourceKind.EMBED).isPlayable())
        assertFalse(s(SourceKind.HLS).isPlayable())
        assertFalse(s(SourceKind.DASH).isPlayable())
        assertEquals("unavailable", UNAVAILABLE_LABEL)
    }
}
