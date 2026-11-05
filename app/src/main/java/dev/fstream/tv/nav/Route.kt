package dev.fstream.tv.nav

import dev.fstream.tv.model.Event

sealed interface Route {
    data object Home : Route
    data class Streams(val event: Event) : Route
    data object Lab : Route
}
