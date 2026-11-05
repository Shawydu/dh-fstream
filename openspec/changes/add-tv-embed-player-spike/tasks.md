# Tasks

## 1. Project setup and device loop

- [x] 1.1 Create the Android TV project (Kotlin, Compose for TV, minSdk matching the HD's OS) with a `LEANBACK_LAUNCHER` activity, 320×180 banner, and `touchscreen required=false`; verify `./gradlew assembleDebug` succeeds
- [x] 1.2 Enable developer mode and network debugging on the HD, `adb connect <tv-ip>:5555`, and install; verify the app tile appears on the Google TV home screen and opens a placeholder screen
- [ ] 1.3 Document the device setup and install loop in `README.md`; verify a fresh install follows it exactly

## 2. Embed player core (spike first)

- [x] 2.1 Build the player screen with one reused full-screen WebView configured per design D4; verify a hardcoded live `embedUrl` loads on the HD
- [x] 2.2 Handle `onShowCustomView`/`onHideCustomView` so page-requested fullscreen video covers the screen; verify on the HD (check 2)
- [x] 2.3 Block popups (`onCreateWindow`), lock top-level navigation to the embed host, and add the configurable host blocklist via `shouldInterceptRequest`; verify with instrumented tests using a local test page that calls `window.open` and navigates away
- [x] 2.4 Implement Back handling (exit custom view, then exit player, with `about:blank` + pause on exit); verify no audio continues after Back on the HD (check 6)
- [x] 2.5 Implement the D6 OK strategies (JS play, synthetic center tap, virtual cursor overlay); verify each against a local test page with a click-to-play overlay

## 3. Embed Lab and feasibility run

- [ ] 3.1 Build the Embed Lab screen (URL input or live-event picker, toggles, OK strategy); verify it loads the sample embed top-level
- [ ] 3.2 Add the HUD (state, time to first frame, dropped/total frames or "n/a", app memory, 1 Hz refresh); verify values update against a local `<video>` test page
- [x] 3.3 Run checks 1–6 from design D7 on the HD against at least 3 live streams from different sources; record results in `openspec/changes/add-tv-embed-player-spike/feasibility.md`
- [x] 3.4 Record the verdict and the winning OK strategy in `feasibility.md`; if smoothness fails on every config, stop here and report "not feasible"

## 4. Provider layer

- [x] 4.1 Define the normalized model (`Category`, `Event`, `Stream`, `PlayableSource{kind,url,headers?}`) and the `ProviderAdapter` interface; verify it compiles with a fake adapter
- [x] 4.2 Implement the strmfree.pk adapter (streamed.pk shut down 2026-10; categories, streams live/today/by category, sources per stream → `kind=embed`, applying the provider defaults from 3.4); verify unit tests against recorded JSON fixtures, including empty and error responses
- [x] 4.3 Map unsupported kinds (`hls`, `dash`) to "unavailable"; verify with a unit test

## 5. Browse UI

- [x] 5.1 Build the Home screen with Live / Today / sport filters and the event list (title, time, teams); verify on the TV emulator that d-pad focus is always visible
- [x] 5.2 Build the Streams screen merging all sources (language, HD, viewers, source label) with a "no streams" state; verify Back returns focus to the selected event
- [x] 5.3 Add error + Retry states for failed or timed-out provider calls; verify with an adapter fake that fails, then succeeds
- [x] 5.4 Wire Streams → Player so selecting an embed stream opens the player; verify end to end on the HD: launch → live event → stream → playback → Back

## 6. Integration check

- [x] 6.1 On the HD, run 5 consecutive stream switches through the browse UI; verify no crash, no lingering audio, and memory within 10% (check 5 via the real flow)
