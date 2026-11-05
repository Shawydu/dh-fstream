package dev.fstream.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import dev.fstream.tv.browse.HomeScreen
import dev.fstream.tv.browse.HomeViewModel
import dev.fstream.tv.browse.StreamsScreen
import dev.fstream.tv.browse.StreamsViewModel
import dev.fstream.tv.nav.Route

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FstreamApp).container
        setContent { FstreamRoot(container) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FstreamRoot(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val provider = container.providers.first()
    val homeVm = remember { HomeViewModel(provider, scope) }
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    BackHandler(enabled = backStack.size > 1) { backStack.removeAt(backStack.lastIndex) }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val route = backStack.last()) {
                Route.Home -> HomeScreen(
                    vm = homeVm,
                    onOpenEvent = { backStack.add(Route.Streams(it)) },
                    onOpenLab = { backStack.add(Route.Lab) },
                )
                is Route.Streams -> {
                    val vm = remember(route.event.id) { StreamsViewModel(provider, route.event, scope) }
                    StreamsScreen(vm)
                }
                Route.Lab -> LabEntry(container, onBack = { backStack.removeAt(backStack.lastIndex) })
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)

@Composable
fun LabEntry(container: AppContainer, onBack: () -> Unit) {
    dev.fstream.tv.lab.LabScreen(container, onBack)
}
