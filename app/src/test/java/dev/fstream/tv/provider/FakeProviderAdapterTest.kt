package dev.fstream.tv.provider

import dev.fstream.tv.provider.fake.FakeProviderAdapter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeProviderAdapterTest {
    @Test
    fun returnsData() = runTest {
        val a = FakeProviderAdapter()
        val events = a.events(EventFilter.ALL)
        assertTrue(events.isNotEmpty())
        assertEquals(2, a.streams(events.first()).size)
        assertTrue(a.categories().isNotEmpty())
    }

    @Test(expected = ProviderException::class)
    fun failureToggleThrows() = runTest {
        FakeProviderAdapter(failing = true).categories()
    }
}
