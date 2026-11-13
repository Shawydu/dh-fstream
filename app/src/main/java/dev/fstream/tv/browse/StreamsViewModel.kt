package dev.fstream.tv.browse

import dev.fstream.tv.model.Event
import dev.fstream.tv.model.Stream
import dev.fstream.tv.provider.ProviderAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StreamsViewModel(
    private val provider: ProviderAdapter,
    val event: Event,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<Load<List<Stream>>>(Load.Loading)
    val state: StateFlow<Load<List<Stream>>> = _state.asStateFlow()
    private var job: Job? = null

    fun load() {
        job?.cancel()
        job = scope.launch {
            if (_state.value is Load.Error) _state.value = Load.Loading
            _state.value = try {
                Load.Content(provider.streams(event))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Load.Error(e.message ?: "Failed to load streams")
            }
        }
    }

    fun retry() = load()
}
