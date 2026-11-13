# fstream-tv — Architecture Contract

Binding for all implementation work on `openspec/changes/add-tv-embed-player-spike`.
Design rationale lives in that change's `design.md` (D1–D7); this file pins the concrete code shape.

## Toolchain

- JDK 17: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`
- `ANDROID_HOME=$HOME/Library/Android/sdk` (platform 35, build-tools 35.0.0, platform-tools); `local.properties` holds `sdk.dir`
- Gradle wrapper 8.11.1, AGP 8.7.3, Kotlin 2.0.21 (compose compiler plugin), version catalog `gradle/libs.versions.toml`
- compileSdk 35, targetSdk 35, minSdk 31 (Chromecast with Google TV HD ships Android TV 12+)
- Single module `:app`, package `dev.fstream.tv`

## Libraries (no others without architect approval)

- Compose BOM + `androidx.tv:tv-material` (lists/browse UI), `androidx.activity:activity-compose`
- `androidx.webkit:webkit` (WebView feature checks)
- OkHttp 4 + `kotlinx-serialization-json`, kotlinx-coroutines
- Tests: JUnit4, `okhttp3:mockwebserver`, `kotlinx-coroutines-test`; androidTest: `androidx.test.ext:junit`, `androidx.test:runner`, espresso-web optional
- No DI framework, no navigation library, no Media3 (deferred per design)

## Package layout and ownership

```
dev.fstream.tv
├── FstreamApp.kt            Application; owns AppContainer
├── AppContainer.kt          manual DI: OkHttpClient, ProviderAdapter(s), PlayerConfig
├── MainActivity.kt          Compose host for browse screens + simple Route back stack
├── nav/Route.kt             sealed Route { Home; Streams(event); Lab }
├── model/                   Category, Event, Stream, PlayableSource, SourceKind  (shared, frozen after wave 1)
├── provider/                ProviderAdapter interface + strmfree/ adapter
├── browse/                  Home + Streams Compose screens, ViewModels
├── player/                  PlayerActivity (View-based), WebViewHolder, clients, OK strategies, CursorOverlay, Hud
└── lab/                     Embed Lab Compose screen (launches PlayerActivity)
```

## Key contracts

- Player entry point (the only coupling between browse/lab and player):
  `PlayerActivity.start(context: Context, source: PlayableSource, title: String, options: PlayerOptions = PlayerOptions())`
  Extras are `Json.encodeToString` strings; `PlayableSource` and `PlayerOptions` are `@Serializable`.
- `PlayerOptions(okStrategy: OkStrategy, showHud: Boolean, blocklistEnabled: Boolean, autoStart: Boolean)`. The embed is always loaded top-level (`loadUrl(embedUrl)`); there is no load-mode setting.
- Player is a plain `Activity`/`ComponentActivity` with Views, not Compose: WebView, fullscreen custom view, cursor overlay and HUD are FrameLayout children. BACK handled in `dispatchKeyEvent`/OnBackPressedCallback: custom view → exit custom view; else load `about:blank`, pause, finish.
- One WebView per process (D4): `WebViewHolder` creates it with `MutableContextWrapper(appContext)`, swaps base context to the activity on attach, and detaches + `about:blank` on exit. `destroy()` + recreate only on stall recovery (a long-lived WebView can go blank on the HD; see feasibility.md 2026-11-12).
- `ProviderAdapter` is a suspend interface returning `Result`-free plain values and throwing `ProviderException` on failure; ViewModels map exceptions to UI error state with Retry.
- Unsupported `SourceKind` (`HLS`, `DASH`) is shown as "unavailable" and never launches the player.

## Rules

- `./gradlew assembleDebug testDebugUnitTest` must pass before any task is reported done.
- Instrumented tests (`androidTest`) must compile (`assembleDebugAndroidTest`); they run later on the HD.
- Do not extract, deobfuscate or rewrite stream URLs from embed pages; the player only loads `embedUrl` as given.
- Never touch files outside your assigned packages, except as listed in your brief.
