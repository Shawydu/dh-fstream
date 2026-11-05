package dev.fstream.tv.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.fstream.tv.model.Stream
import dev.fstream.tv.player.PlayerActivity
import dev.fstream.tv.provider.UNAVAILABLE_LABEL
import dev.fstream.tv.provider.isPlayable
import java.text.NumberFormat

fun streamLine(s: Stream): String = buildString {
    append("Stream ${s.number}")
    if (s.language.isNotBlank()) append(" · ${s.language}")
    if (s.hd) append(" · HD")
    if (s.viewers > 0) append(" · ${NumberFormat.getIntegerInstance(java.util.Locale.US).format(s.viewers)} viewers")
    append(" · ${s.sourceLabel}")
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StreamsScreen(vm: StreamsViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(vm) { vm.load() }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(vm.event.title, style = MaterialTheme.typography.headlineMedium)
        when (val s = state) {
            Load.Loading -> LoadingState()
            is Load.Error -> ErrorState(s.message, vm::retry)
            is Load.Content -> if (s.data.isEmpty()) {
                Text("No streams available", style = MaterialTheme.typography.headlineSmall)
            } else {
                val first = remember(s.data) { FocusRequester() }
                val firstPlayable = s.data.indexOfFirst { it.isPlayable() }
                LaunchedEffect(s.data) { runCatching { first.requestFocus() } }
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.data.size) { i ->
                        val st = s.data[i]
                        val playable = st.isPlayable()
                        ListItem(
                            selected = false,
                            enabled = playable,
                            onClick = { PlayerActivity.start(context, st.playable, vm.event.title) },
                            modifier = Modifier.fillMaxWidth().let { if (i == firstPlayable) it.focusRequester(first) else it },
                            headlineContent = { Text(streamLine(st)) },
                            trailingContent = if (playable) null else { { Text(UNAVAILABLE_LABEL) } },
                        )
                    }
                }
            }
        }
    }
}
