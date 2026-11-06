# Changelog

All notable changes to Jasmine are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Split `MusicService` into focused `PlayerService`, `PlaylistService`,
  `HistoryService`, and `NowPlayingService` implementations. `MusicService` is
  now a facade that preserves the existing command and listener API.

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
