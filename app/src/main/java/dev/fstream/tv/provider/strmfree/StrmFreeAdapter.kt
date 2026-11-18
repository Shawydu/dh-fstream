package dev.fstream.tv.provider.strmfree

import dev.fstream.tv.model.Category
import dev.fstream.tv.model.Event
import dev.fstream.tv.model.PlayableSource
import dev.fstream.tv.model.SourceKind
import dev.fstream.tv.model.SourceRef
import dev.fstream.tv.model.Stream
import dev.fstream.tv.model.Team
import dev.fstream.tv.provider.EventFilter
import dev.fstream.tv.provider.ProviderAdapter
import dev.fstream.tv.provider.ProviderException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class StrmFreeAdapter(
    private val client: OkHttpClient,
    baseUrl: String = "https://strmfree.pk",
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) : ProviderAdapter {
    override val id = "strmfree"
    override val displayName = "StreamFree"

    private val base = baseUrl.trimEnd('/')

    @Serializable
    private data class CategoriesDto(val categories: List<String>? = null)

    @Serializable
    private data class TeamDto(val name: String? = null, val logo: String? = null)

    @Serializable
    private data class SourceDto(val quality: String? = null, val url: String? = null, val live: Boolean? = null)

    @Serializable
    private data class StreamDto(
        val name: String? = null,
        val category: String? = null,
        @SerialName("stream_key") val streamKey: String? = null,
        @SerialName("match_timestamp") val matchTimestamp: Long? = null,
        @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
        val iframe: String? = null,
        @SerialName("additional_sources") val additionalSources: List<JsonElement>? = null,
        val viewers: Int? = null,
        val team1: TeamDto? = null,
        val team2: TeamDto? = null,
    )

    @Serializable
    private data class StreamsDto(val streams: List<StreamDto>? = null)

    @Serializable
    private data class SourcesDto(val sources: List<SourceDto>? = null)

    override suspend fun categories(): List<Category> =
        get("/api/v1/categories", CategoriesDto.serializer()).categories.orEmpty()
            .filter { it.isNotBlank() }
            .map { Category(it, it.replaceFirstChar(Char::uppercase)) }

    override suspend fun events(filter: EventFilter, category: String?): List<Event> {
        val path = "/api/v1/streams" + if (category != null) "?category=$category" else ""
        val raw = get(path, StreamsDto.serializer()).streams.orEmpty()
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.Instant.ofEpochMilli(clock()).atZone(zone).toLocalDate()
        return raw.filter {
            when (filter) {
                // The listing no longer carries per-source live flags: live = already started and has viewers.
                EventFilter.LIVE -> (it.matchTimestamp ?: 0L) * 1000 <= clock() && (it.viewers ?: 0) > 0
                EventFilter.TODAY -> java.time.Instant.ofEpochMilli((it.matchTimestamp ?: 0L) * 1000)
                    .atZone(zone).toLocalDate() == today
                EventFilter.ALL -> true
            }
        }.mapNotNull { it.toEvent() }.sortedBy { it.startEpochMs }
    }

    override suspend fun streams(event: Event): List<Stream> {
        val api = try {
            get("/api/v1/sources/${event.id}", SourcesDto.serializer()).sources.orEmpty()
        } catch (e: NotFound) {
            emptyList()
        } catch (e: ProviderException) {
            if (event.sourceRefs.isEmpty()) throw e else emptyList()
        }
        // Nothing is dropped except blank URLs; API entries come first so their live flag wins on duplicate URLs.
        val all = (api + event.sourceRefs.map { SourceDto(it.source, it.id, null) })
            .filter { !it.url.isNullOrBlank() }
            .distinctBy { it.url }
        val order = listOf("SD", "HD", "FHD", "4K")
        return all
            .sortedWith(compareBy({ it.live != true }, { order.indexOf(it.quality).let { i -> if (i < 0) order.size else i } }))
            .mapIndexed { i, s ->
                val q = s.quality?.takeIf { it.isNotBlank() } ?: "Source ${i + 1}"
                Stream(
                    id = q,
                    number = i + 1,
                    language = "",
                    hd = q != "SD",
                    viewers = 0,
                    sourceLabel = q + if (s.live == false) " · not live yet" else "",
                    playable = PlayableSource(SourceKind.EMBED, s.url!!),
                )
            }
    }

    // Lenient: a string is a URL; an object may use url/iframe/embed plus quality/name/label.
    private fun JsonElement.toRef(): SourceRef? {
        if (this is JsonPrimitive) return contentOrNull?.takeIf { it.isNotBlank() }?.let { SourceRef("", it) }
        val o = this as? JsonObject ?: return null
        fun f(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        val url = f("url") ?: f("iframe") ?: f("embed") ?: return null
        return SourceRef(f("quality") ?: f("name") ?: f("label") ?: "", url)
    }

    private fun StreamDto.toEvent(): Event? {
        val key = streamKey?.takeIf { it.isNotBlank() } ?: return null
        return Event(
            id = key,
            title = name ?: key,
            category = category ?: "",
            startEpochMs = (matchTimestamp ?: 0L) * 1000,
            posterUrl = thumbnailUrl?.takeIf { it.isNotBlank() },
            popular = false,
            home = team1?.toTeam(),
            away = team2?.toTeam(),
            sourceRefs = listOfNotNull(iframe?.takeIf { it.isNotBlank() }?.let { SourceRef("", it) }) +
                additionalSources.orEmpty().mapNotNull { it.toRef() },
        )
    }

    private fun TeamDto.toTeam(): Team? = name?.let { Team(it, logo?.takeIf { l -> l.isNotBlank() }) }

    private class NotFound(path: String) : Exception(path)

    private suspend fun <T> get(path: String, serializer: KSerializer<T>): T = withContext(dispatcher) {
        val request = Request.Builder().url(base + path).build()
        val body = try {
            client.newCall(request).execute().use { resp ->
                if (resp.code == 404 && path.startsWith("/api/v1/sources/")) throw NotFound(path)
                if (!resp.isSuccessful) throw ProviderException("HTTP ${resp.code} for $path")
                resp.body?.string() ?: throw ProviderException("Empty body for $path")
            }
        } catch (e: ProviderException) {
            throw e
        } catch (e: NotFound) {
            throw e
        } catch (e: IOException) {
            throw ProviderException("Network error for $path: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw ProviderException("Bad request for $path", e)
        }
        try {
            json.decodeFromString(serializer, body)
        } catch (e: Exception) {
            throw ProviderException("Malformed response for $path", e)
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    }
}
