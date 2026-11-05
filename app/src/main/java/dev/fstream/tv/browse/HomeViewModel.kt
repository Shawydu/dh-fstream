package dev.fstream.tv.browse

import dev.fstream.tv.model.Category
import dev.fstream.tv.model.Event
import dev.fstream.tv.provider.EventFilter
import dev.fstream.tv.provider.ProviderAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HomeFilter {
    data object Live : HomeFilter
    data object Today : HomeFilter
    data class Sport(val category: Category) : HomeFilter
}

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Error(val message: String) : Load<Nothing>
    data class Content<T>(val data: T) : Load<T>
}

data class HomeState(
    val categories: List<Category> = emptyList(),
    val filter: HomeFilter = HomeFilter.Live,
    val events: Load<List<Event>> = Load.Loading,
    /** Event last opened; Home restores focus to it on return from Streams. */
    val selectedEventId: String? = null,
)

/** Plain state holder (no androidx ViewModel); lives in the root composition so state survives navigation. */
class HomeViewModel(private val provider: ProviderAdapter, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    private var job: Job? = null

    fun load() {
        job?.cancel()
        job = scope.launch {
            _state.update { it.copy(events = Load.Loading) }
            if (_state.value.categories.isEmpty()) {
                // Category failure is non-fatal; Live/Today remain usable.
                runCatching { provider.categories() }.onSuccess { c -> _state.update { it.copy(categories = c) } }
            }
            val f = _state.value.filter
            val (ef, cat) = when (f) {
                HomeFilter.Live -> EventFilter.LIVE to null
                HomeFilter.Today -> EventFilter.TODAY to null
                is HomeFilter.Sport -> EventFilter.ALL to f.category.id
            }
            try {
                val events = provider.events(ef, cat)
                _state.update { it.copy(events = Load.Content(events)) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(events = Load.Error(e.message ?: "Failed to load events")) }
            }
        }
    }

    fun selectFilter(filter: HomeFilter) {
        _state.update { it.copy(filter = filter, selectedEventId = null) }
        load()
    }

    fun retry() = load()

    fun selectEvent(id: String) = _state.update { it.copy(selectedEventId = id) }
}
