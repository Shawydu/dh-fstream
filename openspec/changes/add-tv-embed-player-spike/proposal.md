# Proposal

## Why

We want to watch streams from embed-style providers (starting with streamed.pk) on a Chromecast with Google TV (HD) with minimal UI. These providers return a web page (`embedUrl`), not a media URL, so Cast's default media receiver cannot play them. The open question is whether a native Android TV app can play these embed pages acceptably on the HD's limited hardware, using only the TV remote. We need to answer that before building anything else.

## What Changes

- New sideloaded Android TV app for Chromecast with Google TV (HD). It runs standalone, with no Cast sender/receiver.
- Native browse flow (Compose for TV): Live / Sports → Events → Streams, fed by a provider adapter. streamed.pk is the first provider.
- Full-screen WebView embed player for `embedUrl` sources, controllable with the TV remote (OK, Back), with popups and ad overlays suppressed.
- "Embed Lab" debug screen: paste or pick an `embedUrl`, toggle the load mode, ad blocking and OK-key strategy, and read a playback HUD (time to first frame, dropped frames, memory). It produces the pass/fail feasibility verdict.
- Provider abstraction that normalizes events and streams into a `PlayableSource` with `kind` (`embed` now; `hls`/`dash` reserved), so keyed providers can be added later without UI changes.

Out of scope for this MVP: native HLS/DASH playback (Media3), a backend proxy, keyed providers, publishing to Play, Cast sender support, and extracting or rewriting media URLs from provider players.

## Capabilities

### New Capabilities

- `stream-browsing`: discover live/upcoming events and their available streams from one or more providers, navigable with a TV remote.
- `embed-playback`: play an embed-page stream full screen on the TV, with remote control of start and exit, popup/ad suppression, and a diagnostics mode for feasibility measurement.

### Modified Capabilities

None. This is a new project.

## Impact

- New Android project (Kotlin, Compose for TV, Ktor or Retrofit with kotlinx.serialization, Android System WebView).
- Runtime dependency on the streamed.pk public API (`/api/sports`, `/api/matches/*`, `/api/stream/{source}/{id}`) and on third-party embed hosts (e.g. `embed.st`).
- Target device: Chromecast with Google TV (HD), sideloaded over ADB. No store distribution.
- Content note: these providers aggregate streams of unclear licensing. The app only renders what the provider's public API and embed pages offer. It does not circumvent provider player protections.
