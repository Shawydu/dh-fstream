package dev.fstream.tv.provider

import dev.fstream.tv.model.SourceKind
import dev.fstream.tv.model.Stream

/** Only EMBED sources can be played in this spike; HLS/DASH are shown as "unavailable". */
fun Stream.isPlayable(): Boolean = playable.kind == SourceKind.EMBED

const val UNAVAILABLE_LABEL = "unavailable"
