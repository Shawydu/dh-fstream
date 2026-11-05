package dev.fstream.tv.provider.strmfree

import dev.fstream.tv.model.SourceKind
import dev.fstream.tv.provider.EventFilter
import dev.fstream.tv.provider.ProviderException
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class StrmFreeAdapterTest {
    private lateinit var server: MockWebServer
    private val routes = mutableMapOf<String, MockResponse>()
    private val requested = java.util.Collections.synchronizedList(mutableListOf<String>())
    // 15 min after the first fixture event (00:00 UTC); the 00:30 UTC event is still in the future.
    private val now = 1_793_836_800_000L + 15 * 60_000

    private fun fixture(name: String) = javaClass.getResource("/fixtures/$name")!!.readText()
    private fun ok(body: String) = MockResponse().setBody(body)

    private fun adapter() = StrmFreeAdapter(
        OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build(),
        server.url("/").toString().trimEnd('/'),
        clock = { now },
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requested += request.path!!
                return routes[request.path] ?: MockResponse().setResponseCode(404)
            }
        }
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private suspend fun events(f: EventFilter, category: String? = null): List<String> {
        routes["/api/v1/streams"] = ok(fixture("strmfree_streams.json"))
        return adapter().events(f, category).map { it.id }
    }

    @Test
    fun categories() = runTest {
        routes["/api/v1/categories"] = ok("""{"categories":["soccer","basketball"]}""")
        val c = adapter().categories()
        assertEquals(listOf("soccer", "basketball"), c.map { it.id })
        assertEquals("Soccer", c[0].name)
    }

    @Test
    fun liveFilter() = runTest {
        assertEquals(listOf("skyf1", "philadelphia-76ers-vs-detroit-pistons"), events(EventFilter.LIVE))
    }

    @Test
    fun todayFilter() = runTest {
        assertEquals(
            listOf("philadelphia-76ers-vs-detroit-pistons", "milwaukee-bucks-vs-boston-celtics"),
            events(EventFilter.TODAY),
        )
    }

    @Test
    fun allFilterSortedAndMapped() = runTest {
        routes["/api/v1/streams"] = ok(fixture("strmfree_streams.json"))
        val all = adapter().events(EventFilter.ALL)
        assertEquals(4, all.size)
        assertEquals(all.sortedBy { it.startEpochMs }, all)
        val phi = all.first { it.id == "philadelphia-76ers-vs-detroit-pistons" }
        assertEquals(1_793_836_800_000L, phi.startEpochMs)
        assertEquals("Philadelphia 76ers", phi.home?.name)
        assertEquals("Detroit Pistons", phi.away?.name)
        assertEquals(2, phi.sourceRefs.size)
        assertEquals(null, all.first { it.id == "redzone" }.home)
    }

    @Test
    fun categoryQueryParam() = runTest {
        routes["/api/v1/streams?category=basketball"] = ok(fixture("strmfree_streams.json"))
        adapter().events(EventFilter.ALL, "basketball")
        assertTrue(requested.contains("/api/v1/streams?category=basketball"))
    }

    @Test
    fun streamsMappedAndOrderedLiveFirst() = runTest {
        routes["/api/v1/streams"] = ok(fixture("strmfree_streams.json"))
        val ev = adapter().events(EventFilter.ALL).first()
        routes["/api/v1/sources/${ev.id}"] = ok(
            """{"stream_key":"k","sources":[
              {"quality":"SD","url":"https://x/sd","live":true},
              {"quality":"FHD","url":"https://x/fhd","live":false},
              {"quality":"HD","url":"https://x/hd","live":true},
              {"quality":"HD","url":"","live":true}]}""",
        )
        val s = adapter().streams(ev)
        assertEquals(listOf("SD", "HD", "FHD"), s.map { it.id })
        assertEquals(listOf(1, 2, 3), s.map { it.number })
        assertEquals(listOf(false, true, true), s.map { it.hd })
        assertEquals("FHD · not live yet", s[2].sourceLabel)
        assertEquals(SourceKind.EMBED, s[0].playable.kind)
        assertEquals("https://x/sd", s[0].playable.url)
    }

    @Test
    fun sources404IsEmpty() = runTest {
        routes["/api/v1/streams"] = ok(fixture("strmfree_streams.json"))
        val ev = adapter().events(EventFilter.ALL).first()
        assertTrue(adapter().streams(ev).isEmpty())
    }

    @Test(expected = ProviderException::class)
    fun http500() = runTest {
        routes["/api/v1/streams"] = MockResponse().setResponseCode(500)
        adapter().events(EventFilter.ALL)
    }

    @Test(expected = ProviderException::class)
    fun malformedJson() = runTest {
        routes["/api/v1/categories"] = ok("not json")
        adapter().categories()
    }
}
