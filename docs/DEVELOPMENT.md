# Development

## Requirements

- JDK 25
- Maven 3.8+ or the Maven Wrapper
- Docker (optional, for the image)

Check the toolchain:

```bash
java -version
./mvnw -v        # Windows: mvnw.cmd -v
```

## Build and test

```bash
./mvnw clean verify
```

This runs:

- Unit tests (`src/test/**/unit`).
- Integration tests (`src/test/**/integration`).
- The shaded JAR build.

Useful commands:

```bash
./mvnw test                       # unit tests only
./mvnw verify                     # unit + integration tests
./mvnw package                    # build target/jasmine-<version>-All.jar
./mvnw package -Pdocker           # shaded JAR without non-Linux natives
./mvnw -Pcoverage verify          # JaCoCo reports in target/site/jacoco
```

## Run locally

Headless:

```bash
java -Dnogui=true --enable-native-access=ALL-UNNAMED \
  -jar target/jasmine-0.3.2-All.jar
```

GUI:

```bash
java --enable-native-access=ALL-UNNAMED -jar target/jasmine-0.3.2-All.jar
```

Generate a default config:

```bash
java -jar target/jasmine-0.3.2-All.jar generate-config
```

## Project layout

```text
src/main/java/com/github/marioatwired/jasmine/
  audio/        Lavaplayer, queues, now-playing, monitoring
  commands/     Text commands (v1) and slash commands (v2)
  config/       Loading, validation, migration, rendering
  entities/     Prompts and user interaction abstractions
  gui/          Swing interface
  listener/     JDA listeners and interaction handlers
  playlist/     Playlist persistence
  service/      Music, search, and playback services
  settings/     Per-guild settings
  utils/        Formatting, proxy, OAuth, console, instance lock
```

Tests mirror the main packages under `src/test/java/.../unit` and
`src/test/java/.../integration`. Shared fixtures live in `testutil`.

## Branch strategy

`master` is the trunk and should always be releasable. Work in short-lived
branches:

- `feature/<slug>`
- `fix/<slug>`
- `chore/<slug>`
- `deps/<slug>`
- `release/<version>`

Use lowercase letters, numbers, and hyphens. Open a pull request against
`master`; keep it focused.

## Commit messages

Use conventional commits when possible:

```text
feat: add playlist import
fix: handle missing voice channel
chore: update dependencies
docs: document proxy settings
refactor: split queue helpers
test: cover repeat modes
```

## Code style

- Match the surrounding style: 4 spaces, Allman braces, existing naming.
- Keep classes focused; extract helpers instead of growing `MusicService`.
- Prefer `record` for immutable data carriers.
- Use SLF4J loggers instead of `System.out` in production code.
- Do not rewrite or remove existing copyright headers.
- New files may use:

```text
Copyright 2026 Mario (marioatwired)
Licensed under the Apache License, Version 2.0.
```

## Configuration changes

When adding or changing a config key:

1. Update `src/main/resources/reference.conf`.
2. Update `ConfigOption` (`config/model`).
3. Add a migration under `config/migration/versions` if existing configs need it.
4. Update `docs/CONFIG.md`.
5. Add or update tests for loading, migration, validation, and rendering.

Config migrations must be forward-only and must never delete user data without a
backup.

## Working with upstream

The repository keeps upstream as a read-only reference:

```bash
git remote -v
# upstream  https://github.com/jagrosh/MusicBot.git
```

Do not merge upstream directly. Review its history and cherry-pick relevant
fixes:

```bash
git fetch upstream
git log --oneline upstream/master
git cherry-pick <commit>
```

The package rename means most upstream patches will need manual adaptation.

## Release process

1. Ensure `master` is green: `./mvnw clean verify`.
2. Update `CHANGELOG.md`.
3. Set the release version in `pom.xml`.
4. Commit: `chore: release 0.3.2`.
5. Create and push the tag:

   ```bash
   git tag v0.3.2
   git push origin master
   git push origin v0.3.2
   ```

6. The `Release` workflow builds `Jasmine-0.3.2.jar`, runs the packaged-JAR
   smoke test, generates the SHA-256 checksum, creates the GitHub release,
   verifies the published Docker manifest, and publishes
   `ghcr.io/marioatwired/jasmine:0.3.2` and `:latest`.
7. Download the release JAR and checksum, then verify them before announcing
   the release.

Docker images are also published for every push to `master` as `:latest` and
`:sha-<short>`.

## Docker

```bash
docker build -t jasmine:local .
docker run --rm -it -v "$(pwd)/data:/musicbot" jasmine:local
```

The Docker build uses the `docker` Maven profile, which excludes the Windows and
macOS JDave/udpqueue native libraries. The runtime image is multi-arch.
