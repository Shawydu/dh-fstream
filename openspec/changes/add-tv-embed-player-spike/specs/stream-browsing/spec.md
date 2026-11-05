# Spec Delta

## Purpose

Lets a TV viewer discover live and upcoming events and their available streams from one or more stream providers, using only a TV remote.

## ADDED Requirements

### Requirement: Browse events by category
The system SHALL list events from the active provider under the filters Live, Today and each provider sport category. Each event SHALL show its title, start time, and team names when available.

#### Scenario: Live events shown on launch
- **WHEN** the app opens
- **THEN** the Live filter is selected and the provider's live events are listed

#### Scenario: Filter by sport
- **WHEN** the viewer selects a sport category
- **THEN** only events in that category are listed

### Requirement: List streams for an event
The system SHALL list every stream available for a selected event across all of its provider sources. Each stream SHALL show its language, HD flag, and viewer count when provided.

#### Scenario: Event with multiple sources
- **WHEN** the viewer selects an event that has three sources
- **THEN** streams from all three sources are listed together, each labeled with its source

#### Scenario: Event with no streams
- **WHEN** no source returns any stream for the selected event
- **THEN** the system shows a "no streams available" message and the viewer can go back

### Requirement: Remote-only navigation
Every browse screen SHALL be fully operable with the d-pad, OK and Back. Focus SHALL always be visible on exactly one item.

#### Scenario: Back navigation
- **WHEN** the viewer presses Back on the streams list
- **THEN** the event list is shown with focus on the previously selected event

### Requirement: Provider-independent stream model
The system SHALL represent every selectable stream as a playable source of kind `embed`, `hls` or `dash` with a URL. Browse screens SHALL behave the same regardless of which provider supplied the data.

#### Scenario: Unsupported source kind
- **WHEN** a stream's playable source kind is not supported by this build
- **THEN** the stream is shown as unavailable and selecting it does not start playback

### Requirement: Provider failure handling
The system SHALL show an error with a retry action when a provider request fails or times out. It SHALL NOT crash or show an endless loading state.

#### Scenario: Network failure
- **WHEN** the event list request fails
- **THEN** an error message with a focusable Retry action is shown, and pressing it re-requests the list
