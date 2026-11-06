# Changelog

All notable changes to Jasmine are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
