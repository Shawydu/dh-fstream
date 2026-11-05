# Design

## Context

See proposal.md - Why. Findings from probing the provider:

- streamed.pk API: no auth, JSON. `GET /api/matches/{live|all-today|all|<sport>}[/popular]` returns events with `sources: [{source, id}]`. `GET /api/stream/{source}/{id}` returns streams with `{id, streamNo, language, hd, viewers, source, embedUrl}`.
- A sample `embedUrl` (`https://embed.st/embed/admin/<id>/<n>`) is a ~640KB HTML page running its own player (Clappr, P2P HLS engine, minified bundle). It sends no `X-Frame-Options` or CSP header. It has a hidden `/ad.html` iframe and overlay markup. Autoplay behavior is decided at runtime and can't be read from the static HTML.
- Device: Chromecast with Google TV (HD). Low-end SoC, ~1.5GB RAM, 1080p, Android TV OS, Play-updated Android System WebView.

## Goals / Non-Goals

**Goals:**
- Get a clear feasibility verdict for embed playback on the HD before investing in UI.
- Make the spike code the real player, not throwaway code.
- Keep providers swappable behind one normalized model.

**Non-Goals:**
- Native media playback (Media3/ExoPlayer). Deferred until a direct-media source exists.
- Inspecting, deobfuscating or bypassing provider player code to extract media URLs.
- Polished visual design.

## Decisions

### D1. Standalone Android TV app instead of Cast sender/receiver
The HD runs Android TV, so the app can be the client itself: it fetches provider APIs and plays content with the TV remote.
- Alternatives: tab casting (laggy, the laptop must stay on); a Custom Web Receiver hosting an iframe (Cast Console registration, no remote control of the cross-origin player, weaker runtime); the embed's built-in cast button (unknown behavior inside an iframe).
- A native app also removes CORS, so streamed.pk is called directly with no proxy.

### D2. Native Compose for TV for lists, WebView only for playback
Compose for TV (`androidx.tv`) gives correct d-pad focus handling for free.
- Alternative: a WebView-shell app with an HTML UI. Rejected because d-pad focus in HTML is a constant fight, and a second heavy WebView competes with the player for the HD's ~1.5GB RAM.

### D3. Normalized provider model
- `ProviderAdapter`: `listCategories()`, `listEvents(filter)`, `listStreams(event)`.
- `PlayableSource { kind: embed | hls | dash, url, headers?, loadMode? }`.
- The UI depends only on this model. Only `kind = embed` is implemented in the MVP.
- Keyed providers later read their keys from `local.properties` → `BuildConfig` (personal sideload only). A proxy becomes required if the APK is ever distributed.

### D4. Embed player = one reused full-screen WebView
Configuration:
- JavaScript and DOM storage enabled; `mediaPlaybackRequiresUserGesture = false`; hardware acceleration on.
- `WebChromeClient.onCreateWindow` returns false, which blocks popup windows.
- `onShowCustomView` / `onHideCustomView` handle page-requested fullscreen video.
- `shouldOverrideUrlLoading` blocks top-level navigation away from the embed host.
- `shouldInterceptRequest` applies a small, configurable ad-host blocklist.
- One WebView instance is reused across streams. On exit, load `about:blank` and pause/stop it, to keep memory bounded.

### D5. Two load modes, selectable per provider
- `top-level` (default): `loadUrl(embedUrl)`. The page's own fullscreen and player logic keep working.
- `iframe-wrapper`: a local page loaded via `loadDataWithBaseURL` that holds `<iframe src=embedUrl allow="autoplay; fullscreen">`. This copies how the embed runs on a normal website, for providers that only render when framed.
- The default mode is a field on the adapter / `PlayableSource`.

### D6. Remote input strategy, tried in order
1. **OK → JS play:** call `evaluateJavascript` to play the first `<video>` it can reach. This only works if the video is in the top frame.
2. **OK → synthetic tap:** dispatch a touch DOWN/UP at the screen center. This works through iframes, like a real tap.
3. **Fallback → virtual cursor:** a d-pad-driven pointer overlay where the arrows move it and OK taps. This works on any page but is clumsy.

- BACK is always handled by the app (exit custom view, then exit the player) and is never passed to the page.
- The strategy is selectable in Embed Lab, and the winner becomes the per-provider default.

### D7. Embed Lab as the feasibility harness
It's a debug screen inside the real app:
- Inputs: a source picker (live event from the API, or a pasted URL), the load mode, popup/ad toggles, the user-agent toggle and the OK strategy.
- HUD: player state, time to first frame, dropped/total frames (`getVideoPlaybackQuality()` when reachable), app memory (`Debug.getMemoryInfo`).

Feasibility pass criteria:

| # | Check | Pass |
|---|---|---|
| 1 | Start | First frame ≤10 s, starts by itself or with one remote press |
| 2 | Fullscreen | Video fills the 1080p screen |
| 3 | Smooth | <1% dropped frames, no stall over a 10-min live soak |
| 4 | Ads | No popup windows; no overlay that needs a pointer, or the cursor fallback clears it |
| 5 | Stable | 5 consecutive stream switches, no crash, memory flat (±10%) |
| 6 | Back | Back always exits the player |

Verdict: checks 1 to 6 pass with at least one config, and that config becomes the default. If check 3 fails on every config, embed playback is not feasible on the HD, and the project falls back to direct-media sources only.

## Risks / Trade-offs

- [Remote keys can't drive the web player] → D6 fallback chain; the virtual cursor always works.
- [The HD can't decode or run the P2P HLS player smoothly] → measured in check 3. There's no in-scope mitigation; a failure is the "not feasible" verdict.
- [Ad overlays hijack playback or navigation] → popup blocking, navigation lock, host blocklist, and the cursor to dismiss overlays.
- [WebView memory growth over repeated plays] → single reused WebView plus check 5.
- [Provider changes its API or embed page] → adapter isolation; Embed Lab makes breakage quick to diagnose.
- [Unclear content licensing at providers] → the app only renders the providers' public embeds, with no circumvention; the provider layer stays generic so licensed sources can be added.

## Migration Plan

Not applicable: new project, sideloaded. Rollback = `adb uninstall`.

## Open Questions

- Which ad hosts to put in the default blocklist. This is tuned during the spike and doesn't change the design.
