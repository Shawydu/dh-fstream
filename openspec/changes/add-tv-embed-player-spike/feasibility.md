# Feasibility run: TV embed player spike

- Date: 2026-11-04
- Device: Chromecast with Google TV (HD), Android 14, 1920x1080, WebView 155
- Provider: strmfree.pk (API) / embeds on strmfree.st
- Method: app driven by adb d-pad keys through Embed Lab and Home; video state read read-only over WebView DevTools (`video.currentTime`, `getVideoPlaybackQuality()`) in the cross-origin iframe context. Nothing else extracted from the embed.
- Note: `adb screencap` does not capture the video layer (video area is black in every screenshot, HUD/cursor/overlay do show). Fullscreen is therefore judged from the video element's box (960x540 CSS px at DPR 2 = the 1920x1080 viewport) rather than from a screenshot.

## Check 1: Start (Embed Lab, TOP_LEVEL unless noted)

| Event | Quality | Load mode | OK strategy | Result | Numbers |
|---|---|---|---|---|---|
| Willow (Home flow) | FHD | TOP_LEVEL | JS_PLAY | FAIL: big play overlay stays, video stays at readyState 0 | after OK and 13 s: paused=false, t=0, no frames |
| 76ers v Pistons | FHD | TOP_LEVEL | JS_PLAY | FAIL: paused, readyState 1 | 40 s, no playback |
| Hawks v Wizards | FHD (1280x720 actual) | TOP_LEVEL | CENTER_TAP | PASS | playing ~3 s after the tap; HUD ttff 21.9 s because the tap came late |
| 76ers v Pistons | FHD | TOP_LEVEL | CENTER_TAP | PASS | tap at 7 s, playing at 12.8 s from Play |
| Bucks v Celtics | FHD | TOP_LEVEL | CENTER_TAP | PASS, but first tap (3.5 s, page not ready) was ignored; second tap started it | 1920x1080 |
| Bucks v Celtics | SD | TOP_LEVEL | CENTER_TAP | PASS (extra taps toggle pause/play) | 15 s with 4 taps |
| Bulls v Knicks | SD | TOP_LEVEL | CENTER_TAP | PASS | tap at 10 s, playing at 12.3 s (about 2 s after tap) |
| Bulls v Knicks | FHD | TOP_LEVEL | CENTER_TAP | PASS but slow | tap at 9 s, playing at 21 s (about 12 s after tap) |
| Bulls v Knicks | SD | TOP_LEVEL | CURSOR | PASS | OK at 9 s, HUD ttff 11.0 s, "CURSOR tap" |
| Hawks v Wizards | FHD | IFRAME_WRAPPER | CENTER_TAP | FAIL: embed returns a JSON "violation" error page when framed by the wrapper, no player | screenshot e6_wrap.png |

No strategy starts playback without a press. Page needs about 6 to 8 s before the overlay accepts a tap. Time from Play to moving video is about 10 to 13 s for SD, 13 to 21 s for FHD, so the 10 s target is only met marginally (SD, tap sent as soon as the page is ready). Probe polling added about 1 to 3 s of measurement error.

## Check 2: Fullscreen

PASS. Video element box = viewport (960x540 CSS px = 1920x1080) for FHD and SD; `document.fullscreenElement` was null, so the page never requested custom-view fullscreen and `onShowCustomView` was not exercised. Video content verified non-black via canvas sample (average 96/255). Visual confirmation not possible (see Method).

## Check 3: Smoothness (10 min soak, Bulls v Knicks SD 540p, TOP_LEVEL + CENTER_TAP)

Run 2 (complete, 11 samples, about 30 fps source):

| Sample | currentTime s | dropped | total | interval dropped % |
|---|---|---|---|---|
| 0 | 54 | 4 | 184 | - |
| 1 | 115 | 81 | 2005 | 4.2 |
| 2 | 177 | 119 | 3786 | 2.1 |
| 3 | 238 | 158 | 5491 | 2.3 |
| 4 | 299 | 180 | 7310 | 1.2 |
| 5 | 360 | 191 | 9077 | 0.6 |
| 6 | 421 | 205 | 10908 | 0.8 |
| 7 | 483 | 213 | 12740 | 0.4 |
| 8 | 544 | 237 | 14571 | 1.3 |
| 9 | 605 | 247 | 16384 | 0.6 |

Overall 247/16384 = 1.51% (HUD at the end: 259/18046 = 1.44%). After minute 5: 67/9074 = 0.74%. No stall, no pause. Run 1 gave the same shape (261/14538 = 1.8% at 9 min; 0.55% over minutes 6 to 9) but the player closed on its own at about 8m50s (see Issues).

FHD (1080p60) is much worse: Bucks FHD 87/936 = 9.3%, Bulls FHD 31/311 = 10% within the first 15 s. The 720p "FHD" Hawks stream: 20/1370 = 1.5% in the first 30 s.

gfxinfo (app UI, not video): run 1 before reset 0 frames; after run 1 521 frames, 45% janky; after run 2 598 frames, 42% janky, 50th percentile 30 ms. Not meaningful for video.

Result: borderline FAIL on the strict <1% over the whole soak (1.5%) because of a start-up burst in minutes 1 to 3 (4.2%, 2.1%, 2.3%); steady state passes (0.4 to 1.3%, 0.74% from minute 5). SD only.

## Check 4: Popups / redirects

PASS. HUD showed `blocked req: 0` (1 on two FHD runs), `popups: 0`, `nav: 0` in every run and at the 5 and 10 min soak points. Screenshots only ever showed the page's own play/pause/loading overlay (sw4.png). The embed loads several third-party iframes (about 5 origins) which the HUD blocklist did not need to block.

## Check 5: Memory over 5 stream switches (Home flow, first stream FHD of each event, JS_PLAY default, one OK press)

| Switch | Event | App PSS (KB) |
|---|---|---|
| 1 | Willow | 187041 |
| 2 | Sky Sports Cricket | 194675 |
| 3 | Sky Sports Tennis | 200701 |
| 4 | 76ers v Pistons | 211744 |
| 5 | Hawks v Wizards | 211786 |

Last vs first = +13.2%: FAIL against the +/-10% criterion, but it plateaued between switches 4 and 5, and PSS after leaving the player was 197211 KB. In this flow JS_PLAY never started video, so growth came from page loads, not decoding. Soak process PSS after 11 min of SD playback was 120733 KB. No crash, same pid throughout, no lingering audio. Needs a repeat with CENTER_TAP and the same event to separate leak from event-to-event variance.

## Check 6: Back

PASS. BACK while playing returns to the previous screen; the app's AAudio player went `started` to `stopped` within 1 s and was gone from `dumpsys audio` at 6 s; media_session showed no active player for the app. Streams screen returns to its focused first stream, Home returns focus to the selected event (swh5.png).

## Task 1.2

`cmd package query-activities ... LEANBACK_LAUNCHER` lists `dev.fstream.tv.MainActivity` with banner resource. A screenshot of the tile in the Google TV Apps row was not captured (home screen is a long scrolling page).

## Verdict

Feasible with caveats. Winning config: TOP_LEVEL load mode + CENTER_TAP (CURSOR also works), SD (540p) stream. IFRAME_WRAPPER does not work with this provider. JS_PLAY does not work (video in a cross-origin iframe, play() is a no-op). FHD 1080p60 is not smooth on the HD (about 10% dropped); SD is about 1.5% over 10 min and 0.7% in steady state.

## Issues found

1. Default OK strategy JS_PLAY never starts playback on strmfree embeds; Home -> Streams -> first stream shows only the play overlay. Repro: Home, DPAD_CENTER on an event, DPAD_CENTER on Stream 1, press OK (sw4.png). Default should be CENTER_TAP.
2. CENTER_TAP toggles: a second OK press pauses. A tap before the page is ready is lost and gives no feedback (Bucks FHD run).
3. FHD streams drop about 10% of frames on the HD; the Streams list marks "HD/FHD" first, so the default pick is the worst one. SD streams marked "not live yet" in the list are often actually live (SD of Bulls/Bucks), so they are not offered as playable per the list label (Streams screen, swb3.png).
4. IFRAME_WRAPPER: embed returns a JSON violation error when framed from the local wrapper page (e6_wrap.png).
5. Player closed by itself once during soak run 1 (about 8m50s, 20:02:38): `PlayerActivity` finished with the process alive, no crash and no FATAL in logcat. `finish()` is only reachable from the BACK handler, a display mode change event was logged 0.5 s before; cause not identified (possible stray BACK key). Not reproduced in run 2.
6. Memory +13.2% over 5 switches (see check 5).
7. HUD ttff is measured from player launch, so it includes the time until the user presses OK (11 to 49 s in the runs); dropped/total works because the HUD probe does reach the iframe video once playing.

Screenshots and scripts: /private/tmp/claude-501/-Users-shawn-du-dev-dh-fstream/2227f0c4-f100-42d9-9914-358d4327b66a/scratchpad/feas/

## Re-run after fixes (2026-11-04)

Fixes applied: default OK strategy CENTER_TAP (also Lab default); StrmFree default load mode TOP_LEVEL; stream order live first, then SD, HD, FHD, 4K; HUD shows "page loaded (video cross-origin, state unknown)" and ttff "n/a" unless the probed video is visible (found and not cross-origin-with-currentTime 0). Same method as above (CDP, read-only). Screenshots in scratchpad/feas2/.

1. End-to-end via d-pad (task 5.4): PASS. Home (Live) -> Chicago Bulls v New York Knicks -> Streams shows "Stream 1 SD, Stream 2 HD/FHD" (SD first; for events where SD is not live yet, live FHD is first by design) -> OK -> player, OK pressed at 8 s (page ready) -> video playing at 10.6 s from the stream OK (CDP: paused=false, t advancing); HUD ttff 9.6 s, state "playing", dropped/total 0/5 at first sight.
2. Back navigation (task 5.2): PASS. Player Back -> Streams (focus on Stream 1); Back -> Home with focus on the previously selected event (Bulls v Knicks), list scrolled to keep it visible (back_streams.png, back_home.png).
3. Memory over 10 switches (task 6.1), SD streams (Bulls / Hornets / Bucks rotation), CENTER_TAP, 20 s playing each, TOTAL PSS KB: 198310, 198049, 187289, 204804, 202160, 206406, 204270, 206516, 204642, 203094. Switch 5 vs 1 = +1.9% (spec: within +/-10%, PASS). Switch 10 vs 1 = +2.4%. Plateaus around 203-206 MB from switch 4. Same pid (11430) throughout, no crash; every switch had a probed moving video (HUD at switch 10: 12/592 dropped, ttff 9.8 s). After the final Back no AudioPlaybackConfiguration for the app's uid in `dumpsys audio`. 
4. 10-minute SD soak (Bucks v Celtics, SD, CENTER_TAP): 11 samples, paused=false and currentTime advancing at every sample (50 s to 658 s). dropped/total: 6/64, 21/1874, 36/3683, 104/5500, 136/7309, 149/9105, 161/10919, 177/12708, 188/14533, 197/16320, 214/18143. Overall 1.18%; after minute 5: 78/10834 = 0.72%. Logcat (events+main filtered for am_proc_died, Force removing, FATAL, dev.fstream) captured to scratchpad/feas2/soak_logcat.txt: no process death, FATAL or finish during the soak window. The player did not die on its own (issue 5 from the first run not reproduced, 2 of 2 clean runs now).
5. Home tile (task 1.2): PASS. Google TV Apps tab -> "Your apps" row shows the "fstream" banner (apps2.png); OK launches MainActivity (launched.png).

Task status: 1.2 pass, 5.2 pass, 5.4 pass, 6.1 pass (+1.9% at switch 5; plateau).

Remaining: SD startup burst is still there (first 3 minutes 1.7 to 3.7% per interval), overall soak 1.18% is above the strict 1% but steady state is 0.72%. The cross-origin HUD fix is covered by a unit test only; in these runs the probe always reached the iframe video once it existed, so the "state unknown" text was not seen on screen.

## Auto-start (2026-11-04)

Implemented: `PlayerOptions.autoStart` (default true, Embed Lab toggle); after main-frame onPageFinished the player waits `AUTO_START_DELAY_MS = 1500` and does one CENTER_TAP, skipped if the HudProbe already sees a visible, playing video. Cancelled on Back, onStop and new load. OK stays manual.

Device results are INCOMPLETE: adb wireless debugging dropped mid-run (device no longer listed, `adb mdns services` empty, TV pingable) so the 5-trial run was not finished.
- Trial a1 (Longwood Lancers v Saint Joseph's Hawks, Stream 1 HD/FHD, no OK pressed): HUD showed "auto-start tap", state playing, CDP paused=false, currentTime advancing; stream OK to playing 19.9 s (includes about 1 s per CDP poll; HUD ttff 12.7 s from player launch). 1280x720, 11/550 dropped.
- Trials b1, b2 (SD "not live yet" streams): no playing video within 45 s (b1); b2 inconclusive (adb lost). Earlier audio-based timing (t2, t3) was 6.3 s and 6.5 s but is not reliable (the HUD still showed the play overlay in t2).
- Not verified: double-tap side effects, popups across 5 trials, Back + audio stop with auto-start, settle-delay tuning (still 1500 ms).

Check 1 (start <= 10 s): NOT confirmed. Only one valid trial, at 12.7 s ttff / 19.9 s measured. Rerun needed.

## Quality policy (2026-11-04)

All qualities (SD, HD, FHD, ...) stay available, SD listed first by default; no SD-only restriction. Check 3 per quality: SD passes steady state (about 0.7% dropped); FHD (1080p60) drops about 10% on this device. Known caveat, not a blocker.

### Auto-start rerun (adb restored, 2026-11-04)
Four valid trials across 3 events, HD streams, no OK pressed, stream-OK to CDP `currentTime` advancing: 34.0 s (1080p), 23.2 s (1080p), 26.3 s (720p), 32.1 s (1080p); mean about 29 s. A fifth trial (2.3 s) is discarded: its frame counters showed over 48 s of prior playback, so the probe hit a stale video. Auto-start tap fired in every trial (no OK needed); the 1500 ms settle delay is not the bottleneck (embed page load and stream start dominate), so it stays at 1500 ms. Screenshots failed to save in this run, so popups, double-tap side effects and Back/audio-stop were not re-verified; the HUD in a1 showed popups 0 and no pause toggle.

Check 1 (start <= 10 s): FAIL. Auto-start removes the need to press OK but start time is 23-34 s on this network/device.

## Auto-start re-run (2026-11-04)

Setup: build with iframe-wrapper removed, real UI path (Home Live list -> event -> Stream 1 -> OK), no OK inside the player, CDP read-only probe (about 1 to 3 s polling error; time = stream OK to paused=false with readyState 4). SD streams were "not live yet" for every live event, so all trials used Stream 1 (HD/FHD, 1080p except Ball State at 720p). Events: Sky Sports F1, Willow, Sky Sports Cricket, Sky Sports Tennis, Ball State v Massachusetts. Two earlier attempts were discarded because two drivers ran at once and adb dropped.

| Trial | Event | Delay | Stream OK to playing (s) |
|---|---|---|---|
| r1 | Sky Sports F1 | 1500 ms | 15.5 |
| r2 | Sky Sports Cricket | 1500 ms | 17.2 |
| r3 | Sky Sports Tennis | 1500 ms | 16.4 |
| r4 | Ball State v Massachusetts (720p) | 1500 ms | 23.8 |
| r5 | Willow | 1500 ms | 17.5 |
| f1 | Sky Sports F1 | 800 ms | 13.1 |
| f2 | Sky Sports Cricket | 800 ms | 18.4 |
| f3 | Sky Sports Tennis | 800 ms | 15.0 |

Median 1500 ms: 17.2 s. Median 800 ms: 15.0 s (3 trials, no missed taps, every trial started without OK). Final `AUTO_START_DELAY_MS = 800`. HUD on f3: "auto-start tap", state playing, ttff 10.8 s from player launch.

Check 1 (start <= 10 s): FAIL on these HD/FHD streams (median 15 to 17 s). The 800 ms delay saves under 1 s; the time is dominated by page load plus the embed's own player start-up after the tap (video is already moving at 4 to 12 s of currentTime when detected), not by the settle delay. Page-load time (onPageFinished) was not logged separately, so "page load dominates" is inferred, not measured. SD was not live, so the earlier SD result (about 10 to 13 s) was not re-measured.

Back and audio: Back from f3 returned to Streams (focus on Stream 1), and no `state:started` playback configuration remained in `dumpsys audio`. HUD showed popups 0, nav 0, blocked req 0. Not verified: skip of the auto tap when video already autoplays (never occurred), and the effect of a second manual OK (CDP probe returned no video during that test). adb flapped during the run; the TV's mDNS serial was used after the IP serial dropped.

Decision (2026-11-04, user-accepted): ~15 s start on this device is acceptable. Check 1 threshold is now "start <= 20 s on Chromecast HD (measured median ~15 s with auto-start)"; the HD/FHD results above therefore PASS check 1.

## Fixes 2026-11-12: error 230001 and stale data

Device: not reachable (adb devices empty, no mDNS services): adb lost, user must re-pair. Everything below is code + unit tests only; no on-device verification was done.

### Bug 2: stale browse data (root cause found in code)
`HomeScreen` had a load-once guard (`if (events !is Load.Content) vm.load()`), and the single `HomeViewModel` lives in the root composition for the process, so returning from Streams, or resuming the app, never refetched. No OkHttp cache or Cache-Control handling exists (grep), so the guard was the only cause. Fix: `LifecycleEventEffect(ON_START) { vm.load() }` in `HomeScreen` (fires on entering Home, back from Streams, and app resume); `selectFilter` always refetches and blanks the old list; `HomeViewModel.load()` and `StreamsViewModel.load()` no longer set Loading when content is showing (only after an error), so the list stays visible while refreshing. Streams already created a fresh VM and fetched on every open. Focus restore (task 5.2) is unchanged: it is keyed on the events list, which only re-fires when the list actually changes. Tests: `BrowseViewModelTest` (reload keeps content, tab switch refetches, streams reload keeps content).

### Bug 1: error 230001 (root cause NOT confirmed)
No reproduction was possible without the device, so there is no log evidence. Code review found nothing in our code that blocks stream traffic: Blocklist only blocks listed ad hosts (prior soaks showed blocked req 0-1), NavigationPolicy only affects navigations, the probe is read-only JS. Gaps that could plausibly matter and were fixed: third-party cookies were not accepted for the WebView (the player runs in third-party iframes), console messages were not logged, and `onRenderProcessGone` was unhandled. Changes: `CookieManager.setAcceptThirdPartyCookies(webView, true)`; `onConsoleMessage` logs under tag `fstream-console`; `onRenderProcessGone` logs and shows an error in the HUD. Recovery (independent of cause): `StallDetector` (pure, unit-tested) sees a video that played and then has not advanced for 15 s (or vanished) and `PlayerActivity` reloads the embed (auto-start re-applies), max 3 per session, HUD shows "reconnecting... (n/3)". Page-text detection of the error message is not done (the player is cross-origin); the stall condition covers it.

### To verify on device
15-minute playback (SD and HD): capture `adb logcat -s fstream-console fstream-player chromium`, check for 230001 and whether the reload resumed playback; tab switch / Back / app re-enter should show fresh requests. If 230001 persists after the cookie fix, retest with blocklist off.
