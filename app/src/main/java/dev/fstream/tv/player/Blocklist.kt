package dev.fstream.tv.player

/** Pure host-suffix ad/popup blocklist. */
class Blocklist(hosts: Set<String> = DEFAULT_HOSTS) {
    private val suffixes: Set<String> = hosts.map { it.trim().lowercase().trimStart('.') }.filter { it.isNotEmpty() }.toSet()

    /** True when [host] equals a listed suffix or is a subdomain of it (never a mere string suffix). */
    fun matches(host: String?): Boolean {
        if (host.isNullOrEmpty()) return false
        val h = host.lowercase().trimEnd('.')
        return suffixes.any { h == it || h.endsWith(".$it") }
    }

    companion object {
        val DEFAULT_HOSTS: Set<String> = setOf(
            "doubleclick.net", "googlesyndication.com", "adservice.google.com",
            "popads.net", "propellerads.com", "exoclick.com", "juicyads.com",
            "adsterra.com", "onclickads.net", "hilltopads.net",
        )
    }
}
