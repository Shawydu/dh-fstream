package dev.fstream.tv.player

/** Pure decisions for the WebView client: embed host lock and ad-host blocking. */
class NavigationPolicy(private val allowedHost: String, private val blocklist: Blocklist = Blocklist()) {
    fun isAllowedHost(host: String?): Boolean {
        if (host.isNullOrEmpty()) return false
        val h = host.lowercase()
        val a = allowedHost.lowercase()
        return h == a || h.endsWith(".$a")
    }

    /**
     * Top-level navigation is locked to the embed host (or subdomain). Subframe navigation is allowed so
     * players inside iframes keep working; ad hosts in subframes are cut by [shouldBlockRequest] instead.
     */
    fun allowNavigation(host: String?, isMainFrame: Boolean): Boolean =
        if (isMainFrame) isAllowedHost(host) else !blocklist.matches(host)

    fun shouldBlockRequest(host: String?, blocklistEnabled: Boolean): Boolean =
        blocklistEnabled && blocklist.matches(host)
}
