# Changelog

All notable changes to Jasmine are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Added `/clear` and `!clear` to clear the song queue without interrupting the
  current track. Channel cleanup remains available through `clearchannel`.

## [0.11.1] - 2026-11-11

### Changed

- Playlist append input parsing and result messages are now shared between
  text and slash commands, with correct singular/plural wording.
- Interaction ownership checks now use a shared guard with consistent wording.
- Slash playlist editing commands now distinguish server-only usage from
  missing DJ permissions.
- Updated the Jackson BOM to 2.23.0.

### Added

- Regression tests for playlist input parsing, append messages, and interaction
  ownership guards.
- Deterministic YouTube source tests that no longer use reflection or trigger
  real OAuth/network flows.

### Fixed

- Invalid `playback.youtube.remoteCipherUrl` values are now ignored and logged
  instead of being passed to the YouTube source.

## [0.11.0] - 2026-11-11

### Added

- CI now scans the Docker image with Trivy and publishes HIGH/CRITICAL SARIF
  results to the repository security tab.
- Release images now include an SPDX SBOM and SLSA provenance attestation, and
  the release workflow verifies both before publishing.
- Added `scripts/verify-image-attestations.sh` for local verification.

### Changed

- Docker base images and GitHub Actions are pinned to immutable digests/commits.
- Dependabot now updates Docker base images in addition to Maven and GitHub
  Actions dependencies.
- The Maven wrapper distribution is verified with a SHA-256 checksum.

### Security

- Updated commons-io to 2.22.0 to resolve CVE-2024-47554, reported by the new
  Trivy image scan.

## [0.10.0] - 2026-11-11

### Added

- YouTube OAuth playback now adds secondary playback clients after the TV
  clients, so Web, MWeb, and AndroidVR can recover playback when TV clients
  cannot load stream formats.
- Added `playback.youtube.fallbackClients` to disable the secondary clients.
- Made the remote cipher URL, password, and user agent configurable through
  `playback.youtube.remoteCipherUrl`, `remoteCipherPassword`, and
  `remoteCipherUserAgent`. A blank URL uses the local cipher.

## [0.9.0] - 2026-11-10

### Added

- `/playlists` now shows the number of songs in each playlist.
- Added `/playlist addcurrent` and `playlist addcurrent` to save the currently
  playing track to a playlist.
- Added `/playlist rename` and `playlist rename` to rename saved playlists.

### Changed

- Playlist append commands now skip duplicate entries and report how many were
  skipped.

## [0.8.0] - 2026-11-10

### Added

- Added the `/playlist` slash command group with `view`, `append`, `remove`,
  `move`, and `replace` subcommands for parity with the text commands.

### Changed

- CI and release workflows now build the Docker image and run a container smoke
  test for first-run config generation and environment-based credentials before
  publishing.
- The release workflow now rejects tags that do not point to the current
  `master` head.

## [0.7.0] - 2026-11-10

### Added

- Docker credentials can be supplied with `JASMINE_TOKEN` / `JASMINE_OWNER`, or
  with `JASMINE_TOKEN_FILE` / `JASMINE_OWNER_FILE` for Docker secrets. The
  `DISCORD_TOKEN` and `DISCORD_OWNER` aliases are also supported.
- Added `playlist view` / `playlist edit` with paginated playlist tracks and
  interactive editing.
- Added `playlist remove`, `playlist move`, and `playlist replace` for editing
  saved playlist entries.

### Changed

- Playlist append and editing commands are available to DJs, members with
  Manage Server, and the bot owner instead of only the bot owner.
- The `/playlists` details button is now labeled **View / Edit** and the editor
  hints that selecting a track exposes edit actions.

### Fixed

- First startup now generates `config.txt` when it is missing instead of
  immediately prompting for a bot token. This makes the Docker image usable on
  bind mounts where stdin is not interactive.
- `serversettings.json` writes now use an atomic file replacement fallback when
  the existing file is read-only.
- Playlist shuffle directives are preserved when editing a shuffled playlist.
- Playlist previews and editors now show the stored file order instead of a
  shuffled snapshot.

## [0.6.3] - 2026-11-10

### Changed

- Updated `youtube-source` to 1.18.2.

### Fixed

- YouTube OAuth playback now uses a compatible TVHTML5 user-agent, avoiding
  the "The page needs to be reloaded" failure seen on some videos.

## [0.6.2] - 2026-11-10

### Changed

- Split playback control, DJ actions, and history navigation out of
  `PlayerService` into `PlaybackControlService`. `PlayerService` now focuses on
  track loading, validation, and enqueueing.

### Fixed

- Multi-architecture Docker builds no longer try to resolve unused JavaFX
  artifacts pulled in by FlatLaf Extras. The Docker dependency prefetch now
  uses `dependency:resolve` instead of `dependency:go-offline`.

## [0.6.1] - 2026-11-10

### Fixed

- The `about` command no longer displays the unresolved JDA-Chewtils version
  placeholder.
- Config migrations can now replace read-only `config.txt` files, including
  files created by Docker bind mounts.
- Custom command aliases are no longer reported as deprecated/unknown keys.

## [0.6.0] - 2026-11-09

### Added

- Import playlists from attached `.txt` files and export saved playlists as
  attachments with `/playlist import` and `/playlist export`.

### Changed

- Playlist names are sanitized consistently, including path separators and
  leading/trailing dots.

## [0.5.0] - 2026-11-09

### Added

- Playback history is now persisted alongside the queue, so `/history` and
  `/previous` survive bot restarts.

### Changed

- `queuedata.json` schema version 2. Version 1 files are read with an empty
  history.

## [0.4.0] - 2026-11-09

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
