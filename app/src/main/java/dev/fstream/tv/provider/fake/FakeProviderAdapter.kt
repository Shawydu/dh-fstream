package dev.fstream.tv.provider.fake

import dev.fstream.tv.model.Category
import dev.fstream.tv.model.Event
import dev.fstream.tv.model.LoadMode
import dev.fstream.tv.model.PlayableSource
import dev.fstream.tv.model.SourceKind
import dev.fstream.tv.model.SourceRef
import dev.fstream.tv.model.Stream
import dev.fstream.tv.model.Team
import dev.fstream.tv.provider.EventFilter
import dev.fstream.tv.provider.ProviderAdapter
import dev.fstream.tv.provider.ProviderException

/** In-memory adapter for UI development and tests. Set [failing] to make every call throw. */
class FakeProviderAdapter(@Volatile var failing: Boolean = false) : ProviderAdapter {
    override val id = "fake"
    override val displayName = "Fake"

    private val cats = listOf(Category("football", "Football"), Category("basketball", "Basketball"))

    private val all = listOf(
        Event(
            id = "e1", title = "Alpha vs Beta", category = "football",
            startEpochMs = 1_700_000_000_000, posterUrl = null, popular = true,
            home = Team("Alpha", null), away = Team("Beta", null),
            sourceRefs = listOf(SourceRef("alpha", "1")),
        ),
        Event(
            id = "e2", title = "Gamma vs Delta", category = "basketball",
            startEpochMs = 1_700_003_600_000, posterUrl = null, popular = false,
            home = Team("Gamma", null), away = Team("Delta", null),
            sourceRefs = listOf(SourceRef("alpha", "2")),
        ),
    )

    private fun check() {
        if (failing) throw ProviderException("Fake provider failure")
    }

    override suspend fun categories(): List<Category> {
        check(); return cats
    }

    override suspend fun events(filter: EventFilter, category: String?): List<Event> {
        check(); return all.filter { category == null || it.category == category }
    }

    override suspend fun streams(event: Event): List<Stream> {
        check()
        return (1..2).map { n ->
            Stream(
                id = "${event.id}-$n", number = n, language = "English", hd = n == 1, viewers = 100 * n,
                sourceLabel = "alpha",
                playable = PlayableSource(SourceKind.EMBED, "https://example.com/embed/${event.id}/$n", loadMode = LoadMode.TOP_LEVEL),
            )
        }
    }
}
