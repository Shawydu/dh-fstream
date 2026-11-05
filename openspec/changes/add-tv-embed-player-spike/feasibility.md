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
