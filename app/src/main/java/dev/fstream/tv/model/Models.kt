package dev.fstream.tv.model

import kotlinx.serialization.Serializable

@Serializable
enum class SourceKind { EMBED, HLS, DASH }

@Serializable
data class PlayableSource(
    val kind: SourceKind,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

@Serializable
data class Category(val id: String, val name: String)

@Serializable
data class Team(val name: String, val badgeUrl: String?)

@Serializable
data class SourceRef(val source: String, val id: String)

@Serializable
data class Event(
    val id: String,
    val title: String,
    val category: String,
    val startEpochMs: Long,
    val posterUrl: String?,
    val popular: Boolean,
    val home: Team?,
    val away: Team?,
    val sourceRefs: List<SourceRef>,
)

@Serializable
data class Stream(
    val id: String,
    val number: Int,
    val language: String,
    val hd: Boolean,
    val viewers: Int,
    val sourceLabel: String,
    val playable: PlayableSource,
)
