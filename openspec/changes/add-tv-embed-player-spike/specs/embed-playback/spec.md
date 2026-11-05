# Spec Delta

## Purpose

Plays embed-page streams full screen on the TV with remote control of start and exit, and provides a diagnostics mode for judging whether embed playback is feasible on the target device.

## ADDED Requirements

### Requirement: Full-screen embed playback
The system SHALL open a selected `embed` source full screen and render the provider's embed page so that its video fills the screen.

#### Scenario: Start playback from stream list
- **WHEN** the viewer selects an embed stream
- **THEN** the player opens full screen and the embed page loads

#### Scenario: Page requests fullscreen video
- **WHEN** the embed page requests fullscreen for its video
- **THEN** the video is shown covering the whole screen

### Requirement: Remote starts playback
The system SHALL let the viewer start or resume playback from the remote when the embed does not autoplay, using the provider's configured input strategy.

#### Scenario: Embed waiting for interaction
- **WHEN** the embed shows a play overlay and the viewer presses OK
- **THEN** playback starts

#### Scenario: Pointer fallback
- **WHEN** the configured strategy is the virtual cursor
- **THEN** a pointer is shown that the arrow keys move and OK activates at its position

### Requirement: Back always exits
The Back key SHALL always be handled by the app and never consumed by the embed page. Back SHALL first leave page fullscreen if active, then close the player and return to the stream list.

#### Scenario: Exit player
- **WHEN** the viewer presses Back during playback
- **THEN** playback stops and the stream list is shown

### Requirement: Popup and navigation containment
The player SHALL NOT open new windows. It SHALL block top-level navigation away from the embed's host and requests to hosts on the configured blocklist.

#### Scenario: Embed attempts a popup
- **WHEN** the embed page tries to open a new window
- **THEN** no window opens and playback continues

#### Scenario: Embed attempts to navigate away
- **WHEN** the embed page navigates the top frame to another host
- **THEN** the navigation is cancelled and the embed stays loaded

### Requirement: Bounded resource use across sessions
Each playback session SHALL fully stop audio and video when closed. Memory use SHALL NOT grow across repeated sessions.

#### Scenario: Repeated stream switching
- **WHEN** the viewer opens and closes 5 streams in a row
- **THEN** no audio continues after closing and app memory stays within 10% of the first session

### Requirement: Embed Lab diagnostics
The system SHALL provide a diagnostics screen for loading any embed URL with a selectable load mode (top-level or iframe-wrapper), popup/ad blocking toggles, and an OK-key strategy. It SHALL display a HUD with player state, time to first frame, dropped/total frames when measurable, and app memory.

#### Scenario: Measure a stream
- **WHEN** the tester loads an embed URL in Embed Lab and playback starts
- **THEN** the HUD shows time to first frame and updates frames and memory at least once per second

#### Scenario: Metrics not measurable
- **WHEN** the video element is not reachable from the app (e.g. inside a cross-origin frame)
- **THEN** the HUD shows frame metrics as "n/a" instead of a value

### Requirement: Feasibility verdict
The Embed Lab session SHALL record pass/fail for: start ≤10 s, fullscreen, <1% dropped frames with no stall over 10 minutes, no popups, 5 stable switches, and Back exits. The passing configuration SHALL become the provider's default.

#### Scenario: All checks pass
- **WHEN** a configuration passes all six checks
- **THEN** that load mode and input strategy are saved as the provider's playback defaults

#### Scenario: Smoothness fails everywhere
- **WHEN** no configuration passes the smoothness check
- **THEN** embed playback is recorded as not feasible on the device
