package dev.fstream.tv.provider

import dev.fstream.tv.model.Category
import dev.fstream.tv.model.Event
import dev.fstream.tv.model.Stream

class ProviderException(message: String, cause: Throwable? = null) : Exception(message, cause)

enum class EventFilter { LIVE, TODAY, ALL }

interface ProviderAdapter {
    val id: String
    val displayName: String
    suspend fun categories(): List<Category>
    suspend fun events(filter: EventFilter, category: String? = null): List<Event>
    suspend fun streams(event: Event): List<Stream>
}
