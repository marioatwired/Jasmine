# Changelog

All notable changes to Jasmine are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Per-guild queue persistence in `queuedata.json`, saved periodically and on
  graceful shutdown.
- Saved queues and the current track are restored automatically when Jasmine
  starts. No audio plays until a user invokes a playback command.
- `/play` with no arguments resumes the first restored track when the player is
  idle.
- Queue views are available even when no track is currently playing.

### Fixed

- `playnow` and `skipto` now start the selected track when the player is idle.

## [0.3.3] - 2026-11-08

### Changed

- Split `PlaylistLoader` into a public playlist model/facade, a pure
  `PlaylistParser`, and a file-system `PlaylistStorage` component.
- Extracted `PlaylistDetailsRenderer` from `PlaylistsInteractionListener`.

## [0.3.2] - 2026-11-08

### Added

- Automated shaded-JAR smoke tests in CI and the release workflow.
- SHA-256 checksums published alongside release JARs.
- Dependency Review workflow for pull requests.

### Changed

- The release workflow now verifies the published Docker image manifest.

## [0.3.1] - 2026-11-08

### Changed

- Extracted `FavoritesService` and `HistoryPlaylistService` from
  `PlaylistService`, keeping the public facade and command behavior unchanged.
- Added a JaCoCo coverage gate for the full project and the `service` package.

### Added

- Direct tests for audio load handlers, text search selection menus, and
  force-remove selection menus.

### Removed

- Deprecated `MESSAGE_ID` re-exports from the test fixtures.

## [0.3.0] - 2026-11-08

### Changed

- Moved service DTOs to `service.dto` and promoted `OutputAdapter` to the
  `service` package, leaving `MusicService` as a thin facade.
- Introduced `TrackEnqueuer` and removed service-layer dependencies on the
  `MusicService` facade.
- Split queue operations into `QueueService`, separate from playback control in
  `PlayerService`.
- Replaced deprecated `Member#getColor()`, `URL(String)`, `OrderedMenu`, and
  redundant `guildOnly` assignments with supported APIs. Text search and
  force-remove now use Discord select menus for selection.

### Added

- Direct unit tests for `QueueService`.

## [0.2.0] - 2026-11-07

### Changed

- Split `MusicService` into focused `PlayerService`, `PlaylistService`,
  `PlaylistDraftService`, `PlaylistPreviewService`, `HistoryService`, and
  `NowPlayingService` implementations. `MusicService` is now a facade that
  preserves the existing command and listener API.

### Added

- AI-assisted development disclosure in the README.

## [0.1.1] - 2026-11-06

### Changed

- Updated JDA to 6.7.0, Logback to 1.6.6, Jackson to 2.22.4, jsoup to 1.24.1,
  Mockito to 5.24.0, MockWebServer to 5.5.1, and FlatLaf to 3.7.2.
- Updated JUnit to 6.1.3, JetBrains annotations to 26.1.0, and org.json to
  20260814.
- Updated Maven plugins and the Maven Wrapper distribution to 3.10.0.
- Updated GitHub Actions to Node 24-compatible major versions.

### Fixed

- Removed the stale `commands.aliases.lyrics` entry from the default config.

## [0.1.0] - 2026-11-06

First Jasmine release.

### Changed

- Rebranded the project from the inherited JMusicBot fork to Jasmine.
- New Java package: `com.github.marioatwired.jasmine`.
- New main class: `com.github.marioatwired.jasmine.Jasmine`.
- Updated Maven coordinates, Docker image, documentation, and CI to
  `marioatwired/Jasmine`.
- Update checks now point to the Jasmine GitHub repository.

### Removed

- Unreferenced JDA-NAS native connector binaries (about 16 MB).
- Unreferenced legacy config resource.
- Unused `LyricsCmd`, `IconFactory`, `StyledComponents`, and `Pair` classes.
- CircleCI configuration and redundant branch-validation/preview workflows.
- Unused Maven profiles and duplicated shade configuration.

### Added

- Maven Wrapper.
- Dependabot configuration.
- `NOTICE`, `CONTRIBUTING.md`, `SECURITY.md`, and focused documentation.
- CI workflow with unit/integration tests and GHCR image publishing.
- Tag-driven release workflow that builds the release JAR and Docker image.
