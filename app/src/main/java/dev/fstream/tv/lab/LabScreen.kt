package dev.fstream.tv.lab

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import dev.fstream.tv.AppContainer
import dev.fstream.tv.model.LoadMode
import dev.fstream.tv.model.PlayableSource
import dev.fstream.tv.model.SourceKind
import dev.fstream.tv.model.Stream
import dev.fstream.tv.player.OkStrategy
import dev.fstream.tv.player.PlayerActivity
import dev.fstream.tv.player.PlayerOptions
import dev.fstream.tv.provider.EventFilter
import kotlinx.coroutines.launch

private const val DEFAULT_URL = "https://strmfree.st/embed/basketball/philadelphia-76ers-vs-detroit-pistons1080p"
private const val ASSETS = "https://appassets.androidplatform.net/assets/testpages/"
private val LOAD_MODES: List<LoadMode?> = listOf(null, LoadMode.TOP_LEVEL, LoadMode.IFRAME_WRAPPER)

private fun prefs(c: Context) = c.getSharedPreferences("embed_lab", Context.MODE_PRIVATE)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LabScreen(container: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val p = remember { prefs(context) }
    var url by remember { mutableStateOf(p.getString("url", DEFAULT_URL) ?: DEFAULT_URL) }
    var loadMode by remember { mutableStateOf(p.getString("loadMode", null)?.let { runCatching { LoadMode.valueOf(it) }.getOrNull() }) }
    var ok by remember { mutableStateOf(runCatching { OkStrategy.valueOf(p.getString("ok", "") ?: "") }.getOrDefault(OkStrategy.CENTER_TAP)) }
    var hud by remember { mutableStateOf(p.getBoolean("hud", true)) }
    var blocklist by remember { mutableStateOf(p.getBoolean("blocklist", true)) }
    var autoStart by remember { mutableStateOf(p.getBoolean("autoStart", true)) }
    var pickStatus by remember { mutableStateOf("") }
    val streams = remember { mutableStateListOf<Stream>() }

    BackHandler(onBack = onBack)

    fun save() = p.edit().putString("url", url).putString("loadMode", loadMode?.name)
        .putString("ok", ok.name).putBoolean("hud", hud).putBoolean("blocklist", blocklist).putBoolean("autoStart", autoStart).apply()

    fun play(u: String = url) {
        save()
        PlayerActivity.start(
            context, PlayableSource(SourceKind.EMBED, u), "Lab",
            PlayerOptions(loadMode, ok, hud, blocklist, autoStart),
        )
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF101418)).verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Embed Lab")
        BasicTextField(
            value = url,
            onValueChange = { url = it },
            singleLine = true,
            textStyle = TextStyle(color = Color.White),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier.fillMaxWidth().background(Color(0xFF263040)).padding(12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { loadMode = LOAD_MODES[(LOAD_MODES.indexOf(loadMode) + 1) % LOAD_MODES.size]; save() }) {
                Text("Load mode: ${loadMode ?: "default"}")
            }
            Button(onClick = { ok = OkStrategy.entries[(ok.ordinal + 1) % OkStrategy.entries.size]; save() }) { Text("OK: $ok") }
            Button(onClick = { hud = !hud; save() }) { Text("HUD: ${if (hud) "on" else "off"}") }
            Button(onClick = { blocklist = !blocklist; save() }) { Text("Blocklist: ${if (blocklist) "on" else "off"}") }
            Button(onClick = { autoStart = !autoStart; save() }) { Text("Auto-start: ${if (autoStart) "on" else "off"}") }
        }
        Button(onClick = { play() }) { Text("Play") }

        Text("Pick live event")
        Button(onClick = {
            pickStatus = "Loading..."
            streams.clear()
            scope.launch {
                try {
                    val provider = container.providers.first()
                    val event = provider.events(EventFilter.LIVE).firstOrNull()
                    if (event == null) {
                        pickStatus = "No live events"
                    } else {
                        streams.addAll(provider.streams(event))
                        pickStatus = "${event.title}: ${streams.size} streams"
                    }
                } catch (e: Exception) {
                    pickStatus = "Error: ${e.message}"
                }
            }
        }) { Text("Load first live event streams") }
        if (pickStatus.isNotEmpty()) Text(pickStatus)
        streams.forEach { s ->
            Button(onClick = { url = s.playable.url; save() }) {
                Text("#${s.number} ${s.language} ${if (s.hd) "HD" else "SD"} (${s.sourceLabel}) ${s.playable.url}")
            }
        }

        Text("Local test pages")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("clicktoplay", "popup", "navaway", "hud").forEach { name ->
                Button(onClick = { play("$ASSETS$name.html") }) { Text(name) }
            }
        }
    }
}
