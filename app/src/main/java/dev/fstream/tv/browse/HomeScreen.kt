package dev.fstream.tv.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.fstream.tv.model.Event
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

fun formatStartTime(epochMs: Long): String =
    timeFormat.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

fun teamsLine(e: Event): String? {
    val h = e.home?.name
    val a = e.away?.name
    return if (h != null && a != null) "$h vs $a" else null
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel, onOpenEvent: (Event) -> Unit, onOpenLab: () -> Unit) {
    val state by vm.state.collectAsState()
    // Fires on entering Home (initial, back from Streams) and on app resume; the list stays visible while refetching.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.load() }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Button(onClick = onOpenLab) { Text("Embed Lab") } }
            val filters = listOf(HomeFilter.Live, HomeFilter.Today) + state.categories.map { HomeFilter.Sport(it) }
            items(filters) { f ->
                val selected = f == state.filter
                Button(
                    onClick = { vm.selectFilter(f) },
                    colors = if (selected) ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) else ButtonDefaults.colors(),
                ) {
                    Text(
                        when (f) {
                            HomeFilter.Live -> "Live"
                            HomeFilter.Today -> "Today"
                            is HomeFilter.Sport -> f.category.name
                        },
                    )
                }
            }
        }

        when (val ev = state.events) {
            Load.Loading -> LoadingState()
            is Load.Error -> ErrorState(ev.message, vm::retry)
            is Load.Content -> EventList(
                events = ev.data,
                live = state.filter == HomeFilter.Live,
                selectedId = state.selectedEventId,
                onOpen = { vm.selectEvent(it.id); onOpenEvent(it) },
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EventList(events: List<Event>, live: Boolean, selectedId: String?, onOpen: (Event) -> Unit) {
    if (events.isEmpty()) {
        Text("No events", style = MaterialTheme.typography.headlineSmall)
        return
    }
    val listState = rememberLazyListState()
    val requesters = remember(events) { events.associate { it.id to FocusRequester() } }
    LaunchedEffect(events) {
        val idx = events.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        listState.scrollToItem(idx)
        // The row is composed after the scroll; retry once on the next frame if not yet attached.
        repeat(3) {
            if (runCatching { requesters.getValue(events[idx].id).requestFocus() }.isSuccess) return@LaunchedEffect
            androidx.compose.runtime.withFrameNanos { }
        }
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(events, key = { it.id }) { e ->
            ListItem(
                selected = false,
                onClick = { onOpen(e) },
                modifier = Modifier.fillMaxWidth().focusRequester(requesters.getValue(e.id)),
                headlineContent = { Text(e.title) },
                supportingContent = teamsLine(e)?.let { t -> { Text(t) } },
                overlineContent = { Text(if (live) "LIVE · ${formatStartTime(e.startEpochMs)}" else formatStartTime(e.startEpochMs)) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${e.sourceRefs.size} source${if (e.sourceRefs.size == 1) "" else "s"}")
                    }
                },
            )
        }
    }
}
