# Architecture

Jasmine is a single-module Java 25 application. It uses JDA for Discord, Lavaplayer
for audio, HOCON for configuration, and Swing for the desktop GUI.

## Layers

```text
Presentation
  commands.v1 (text)  commands.v2 (slash)  gui
        |                   |                 |
        +---------+---------+-----------------+
                  |
Business logic
  MusicService (facade)
    -> PlayerService          playback control and queue operations
    -> PlaylistService        saved playlists, drafts, favorites, previews
    -> HistoryService         history queries and history-to-queue operations
    -> NowPlayingService      now-playing and queue view data
  SearchService  MusicCommandValidator
                  |
Infrastructure
  audio (PlayerManager, AudioHandler, queues, monitors)
  config (loader, migration, validation, render)
  playlist  settings  listener  utils
```

Commands and GUI call the same services. Services talk to `AudioHandler` and the
per-guild `Settings`; they do not depend on JDA command types directly. The
`OutputAdapter` abstraction keeps a service call independent of whether the
response is a text message or a slash-command reply.

## Packages

| Package | Responsibility |
|---|---|
| `Jasmine`, `Bot`, `BotConfig`, `DiscordService` | Startup, composition root, JDA setup. |
| `audio` | `PlayerManager`, `AudioHandler`, queue glue, now-playing, alone-in-voice, health/metrics monitors. |
| `commands` | Shared validation, slash-command registry, output adapters. |
| `commands.v1` | Classic prefix commands. |
| `commands.v2` | Slash commands and component interactions. |
| `config` | HOCON loading, versioned migrations, diagnostics, rendering, file updates. |
| `entities` | `Prompt`, `UserInteraction`, and small shared types. |
| `gui` | Main window, console, settings, status, performance, timeline, and source-health panels. |
| `listener` | JDA listeners for voice state, now-playing cleanup, playback buttons, queue/history/playlist/settings interactions, and startup lifecycle. |
| `playlist` | Playlist parsing and storage. |
| `queue` | `AbstractQueue`, `LinearQueue`, `FairQueue`, `PlaybackHistory`. |
| `service` | `MusicService` facade; `PlayerService`, `PlaylistService`, `HistoryService`, `NowPlayingService`; `SearchService`; audio load result handlers. |
| `settings` | Per-guild settings and JSON persistence. |
| `utils` | Formatting, proxy, YouTube OAuth, console redirection, instance lock. |

## Startup

```text
main()
  -> Prompt / GUI decision
  -> InstanceLock
  -> version/Java checks
  -> BotConfig.load()          (reference.conf + config.txt + migrations)
  -> SettingsManager
  -> Bot
       -> PlaylistLoader
       -> PlayerManager
       -> NowPlayingHandler
       -> AloneInVoiceHandler
       -> MusicService
       -> SearchService
       -> YoutubeOauth2TokenHandler
  -> GUI (optional)
  -> CommandFactory / SlashCommandRegistry
  -> PlayerManager.init()      (register enabled audio sources)
  -> DiscordService.createJDA()
```

`PlayerManager.init()` is intentionally delayed until logging and the GUI are
ready. Audio sources are registered only when enabled in `playback.audioSources`.

## Audio flow

```text
User command / slash command
  -> MusicService (facade)
  -> PlayerService
  -> PlayerManager.loadItemOrdered()
  -> Lavaplayer source manager
  -> AudioLoadResultHandlers
  -> AudioHandler.addTrack()
  -> AudioPlayer.playTrack()
  -> NativeAudioSendFactory (udpqueue)
  -> Discord voice
```

Each guild has its own `AudioHandler`, queue, and `Settings`. `AudioHandler`
extends Lavaplayer's `AudioEventAdapter` to drive track start/end, now-playing
updates, repeat modes, and history.

`QueueType` selects the queue strategy:

- `LINEAR`: append order.
- `FAIR`: interleaves tracks by requester.

`PlaybackHistory` stores previous tracks for the `previous` and `history`
commands.

## Service decomposition

`MusicService` is a thin facade kept for command/listener compatibility. It
owns the shared DTOs and delegates to four focused services:

- `PlayerService`: playback control, queue mutation, volume, repeat, seek, skip,
  shuffle, stop, pause, and track-add helpers.
- `PlaylistService`: saved playlist reads/writes, playlist drafts, favorites,
  playlist previews, and history-to-playlist export.
- `HistoryService`: history view data plus queue/play operations from history.
- `NowPlayingService`: now-playing and queue view data formatting.

`PlaylistService` and `HistoryService` call back through the `MusicService`
facade for shared queue behavior. This keeps the public command-facing API stable
while the implementation is split.

## Commands

There are two presentation layers:

- `commands.v1`: JDA-Chewtils text commands, using `CommandEvent`.
- `commands.v2`: JDA slash commands and button/select interactions.

Both are registered in `CommandFactory`. Shared validation lives in
`MusicCommandValidator`; shared responses go through `OutputAdapter`
implementations (`TextOutputAdapters`, `SlashOutputAdapters`).

`SlashCommandRegistry` computes a hash of the command definitions and only
upserts them when they change.

## Configuration

`BotConfig` is the facade. Internally:

1. `ConfigLoader` parses `config.txt` and `reference.conf`.
2. `ConfigMigration` applies versioned migrations (`LegacyToV1`, `V1ToV2`).
3. `ConfigValidator` checks the token and owner ID.
4. `ConfigDiagnostics` reports missing, deprecated, or unknown keys.
5. `ConfigUpdater` writes updates with `.bak` backups and preserves comments.

`configVersion` lives under `meta`. Configs without it are treated as legacy
version 0.

## GUI

The GUI is optional and enabled by default. `gui.enabled` and `-Dnogui=true`
are combined in `Bot.isNoGUI()`. The GUI is intentionally kept out of the
service layer:

- `MainFrame` owns the tabs and refresh timers.
- `ConsolePanel` receives redirected `System.out`/`System.err`.
- `StatusPanel`, `SettingsPanel`, `ConfigPanel`, `PerformancePanel`,
  `EnhancedTimelinePanel`, `SystemHealthPanel`, and `SourceHealthPanel` render
  state and call services.

In Docker and headless deployments, the GUI is disabled and monitoring wrappers
fall back to no-op implementations where possible.

## Persistence

- `config.txt`: application configuration, HOCON.
- `serversettings.json`: per-guild settings (Jackson).
- `Playlists/`: playlist files.
- `youtubetoken.txt`: optional YouTube OAuth refresh token.
- `.jasmine.lock`: prevents two instances from using the same directory.

Runtime files are ignored by git.

## Testing

- Unit tests: `src/test/java/.../unit`.
- Integration tests: `src/test/java/.../integration`.
- Shared builders and fixtures: `src/test/java/.../testutil`.

`MusicServiceTest` exercises the facade and the extracted playback, queue,
history, playlist, volume, repeat, shuffle, skip, stop, and pause behavior.
Config tests cover legacy migration, rendering, diagnostics, validation, and
file updates. Listener tests use Mockito and the fixtures under `testutil`.

## Known technical debt

- `PlayerService` and `PlaylistService` are still relatively large; further
  extraction is possible once their use cases stabilize.
- `PlaylistLoader` and `PlaylistsInteractionListener` are large and mix storage,
  parsing, and UI state.
- The v1 and v2 command layers duplicate some presentation logic.
- Performance monitoring singletons are convenient but harder to test than
  injected instances.
- `MusicService` exposes several result DTOs as nested classes; some can become
  records or move to a dedicated package.

These are refactors, not behavior changes. Keep tests green while addressing
them.
