<h1 align="center">Jasmine</h1>

<p align="center">
  A cross-platform Discord music bot with a clean interface, slash commands, and a desktop GUI.
</p>

<p align="center">
  <a href="https://github.com/marioatwired/Jasmine/actions/workflows/ci.yml"><img src="https://github.com/marioatwired/Jasmine/actions/workflows/ci.yml/badge.svg" alt="CI" /></a>
  <a href="https://github.com/marioatwired/Jasmine/releases/latest"><img src="https://img.shields.io/github/v/release/marioatwired/Jasmine" alt="Release" /></a>
  <a href="https://github.com/marioatwired/Jasmine/blob/master/LICENSE"><img src="https://img.shields.io/github/license/marioatwired/Jasmine" alt="License" /></a>
  <a href="https://github.com/marioatwired/Jasmine/pkgs/container/jasmine"><img src="https://img.shields.io/badge/docker-ghcr.io%2Fmarioatwired%2Fjasmine-blue" alt="Docker" /></a>
</p>

> **Jasmine is a fork of [JMusicBot](https://github.com/jagrosh/MusicBot), originally created by [jagrosh](https://github.com/jagrosh), with maintenance and changes from [arif-banai](https://github.com/arif-banai).**
> Jasmine keeps the same configuration format and is licensed under the Apache License 2.0. See [NOTICE](NOTICE) for attribution.
>
> **AI-assisted development:** Parts of Jasmine are developed with the help of AI coding assistants. AI-assisted changes are reviewed and tested by the maintainer before release. This is disclosed openly so users can evaluate the project with full context.

## Features

- Easy to run: install Java and start the JAR.
- Fast track loading and smooth playback.
- Slash commands and classic text commands.
- A desktop GUI with a console, queue timeline, source health, and performance panels.
- Per-guild settings, DJ role, custom prefix, and queue types (linear or fair).
- Playlists, playback history, repeat modes, seeking, and vote-skip.
- YouTube, SoundCloud, Bandcamp, Vimeo, Twitch, HTTP streams, and local files.
- YouTube OAuth device flow for sources that require sign-in.
- Granular HTTP proxy configuration for Lavaplayer, JDA, and GitHub update checks.
- Docker image with a minimal JRE and multi-architecture support (`amd64` and `arm64`).
- No external API keys required besides a Discord bot token.

## Requirements

- **Java 25 or newer.**
- **Message Content Intent** enabled in the [Discord Developer Portal](https://discord.com/developers/applications) under `Bot > Privileged Gateway Intents`. Without it, text commands will not work.
- On Linux, **glibc 2.38 or newer** for the JDave and udpqueue native libraries. Docker already provides a compatible runtime.
- For Docker: Docker Engine 20.10+ (BuildKit enabled) or Docker Desktop.

## Quick start (JAR)

1. Create a Discord application and bot, enable **Message Content Intent**, and copy the bot token.
2. Download the latest `Jasmine-<version>.jar` from [Releases](https://github.com/marioatwired/Jasmine/releases/latest).
3. Generate a default configuration:
   ```bash
   java -jar Jasmine-0.3.0.jar generate-config
   ```
4. Edit `config.txt` and set at least `discord.token` and `discord.owner`.
5. Start the bot:
   ```bash
   java -Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED -jar Jasmine-0.3.0.jar
   ```

On headless servers, add `-Dnogui=true`:

```bash
java -Dfile.encoding=UTF-8 -Dnogui=true --enable-native-access=ALL-UNNAMED -jar Jasmine-0.3.0.jar
```

On Windows PowerShell, quote the JVM options:

```powershell
java "-Dfile.encoding=UTF-8" "-Dnogui=true" "--enable-native-access=ALL-UNNAMED" "-jar" ".\Jasmine-0.3.0.jar"
```

The first run uses the default prefix `@mention`. You can change it in `config.txt` under `commands.prefix` or with the `/prefix` command.

## Quick start (Docker)

```bash
mkdir -p /path/to/jasmine
docker run --rm -it \
  --name jasmine \
  -v "/path/to/jasmine:/musicbot" \
  ghcr.io/marioatwired/jasmine:latest
```

On first run the container generates `/path/to/jasmine/config.txt`. Edit it, add your token and owner ID, and run the container again.

Or use Docker Compose:

```bash
cp docker-compose.example.yml docker-compose.yml
# Edit the volume path and image tag, then:
docker compose up -d
```

The container runs as UID/GID `10001` (`jasmine`). Make sure the mounted directory is writable by that user, or set `user:` in the Compose file.

## Configuration

The default configuration is generated from [`src/main/resources/reference.conf`](src/main/resources/reference.conf) and written to `config.txt`. It uses HOCON syntax and supports versioned migrations, so existing JMusicBot-style configs continue to work.

See [docs/CONFIG.md](docs/CONFIG.md) for the main sections:

- `discord`: token and owner ID.
- `playback`: length limits, history size, skip ratio, audio sources, YouTube OAuth, transforms.
- `commands`: prefix, help word, aliases, clear-channel limits.
- `presence`: activity and status.
- `ui` and `gui`: emojis, theme, font size, GUI enable/disable.
- `nowPlaying`: message layout, buttons, progress bar.
- `voice`: auto-disconnect and stay-in-channel behavior.
- `paths`: playlists folder.
- `updates`: update notifications.
- `logging`: log level.
- `performance`: NAS and frame buffers.
- `proxy`: proxy host/port and per-component enablement.
- `dangerous`: owner-only eval command (disabled by default).

## Development

Requirements: JDK 25 and Maven 3.8+.

```bash
./mvnw clean verify
./mvnw package
java -Dnogui=true --enable-native-access=ALL-UNNAMED -jar target/jasmine-0.3.0-All.jar
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

Useful commands:

```bash
./mvnw test                 # unit tests
./mvnw verify               # unit + integration tests
./mvnw package -Pdocker     # shaded JAR without non-Linux natives
./mvnw -Pcoverage verify    # tests + JaCoCo reports
```

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for the project layout, coding conventions, and release process. The architecture is described in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

### Docker build

```bash
docker build -t jasmine:local .
docker run --rm -it -v "$(pwd)/data:/musicbot" jasmine:local
```

## Project structure

```
src/main/java/com/github/marioatwired/jasmine/
  audio/        Lavaplayer integration, queues, monitoring
  commands/     Text commands (v1) and slash commands (v2)
  config/       HOCON loading, validation, migration, rendering
  gui/          Swing desktop interface
  listener/     JDA event listeners and interaction handlers
  playlist/     Playlist loading and persistence
  service/      Music, search, and playback services
  settings/     Per-guild settings
  utils/        Formatting, proxy, OAuth, and console helpers
```

## Contributing

Bug reports, feature requests, and pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md) before opening an issue.

- Use a short-lived branch: `feature/<slug>`, `fix/<slug>`, or `chore/<slug>`.
- Keep `master` releasable.
- Run `./mvnw verify` before opening a pull request.
- Use conventional commit messages when possible (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`).

## License and attribution

Jasmine is licensed under the [Apache License 2.0](LICENSE).

It is a fork of [JMusicBot](https://github.com/jagrosh/MusicBot) by John Grosh, with changes from [arif-banai/MusicBot](https://github.com/arif-banai/MusicBot). Original copyright notices are preserved in the source files. See [NOTICE](NOTICE) for details.

Thanks to the libraries that make this possible:

- [JDA](https://github.com/discord-jda/JDA)
- [lavaplayer](https://github.com/lavalink-devs/lavaplayer)
- [youtube-source](https://github.com/lavalink-devs/youtube-source)
- [JDA-Chewtils](https://github.com/chew/JDA-Chewtils)
