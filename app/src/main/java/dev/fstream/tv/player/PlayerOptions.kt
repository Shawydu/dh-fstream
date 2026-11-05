package dev.fstream.tv.player

import kotlinx.serialization.Serializable

@Serializable
enum class OkStrategy { JS_PLAY, CENTER_TAP, CURSOR }

@Serializable
data class PlayerOptions(
    val okStrategy: OkStrategy = OkStrategy.CENTER_TAP,
    val showHud: Boolean = true,
    val blocklistEnabled: Boolean = true,
    val autoStart: Boolean = true,
)
