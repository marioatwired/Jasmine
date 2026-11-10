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
    -> PlayerService          track loading and enqueue
    -> PlaybackControlService pause, stop, volume, repeat, seek, and skip
    -> QueueService           queue inspection, removal, movement, and shuffle
    -> PlaylistService        saved playlist CRUD, queue/play, and orchestration
    -> FavoritesService       add the current track to Favorites
    -> HistoryPlaylistService export playback history to a saved playlist
    -> PlaylistTransferService import/export playlist text files
    -> PlaylistDraftService   interactive playlist draft state and CRUD
    -> PlaylistPreviewService playlist previews and paginated track views
    -> HistoryService         history queries and history-to-queue operations
    -> NowPlayingService      now-playing and queue view data
  SearchService  MusicCommandValidator
                  |
Infrastructure
  audio (PlayerManager, AudioHandler, queues, monitors)
  config (loader, migration, validation, render)
  playlist  queue persistence  settings  listener  utils
```

Commands and GUI call the same services. Services talk to `AudioHandler` and the
per-guild `Settings`; they do not depend on JDA command types directly. The
`OutputAdapter` abstraction keeps a service call independent of whether the
response is a text message or a slash-command reply. `TrackEnqueuer` is the
narrow interface used by audio handlers and history/playlist/search services
that need to add tracks without depending on the whole `MusicService` facade.

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
| `playlist` | Playlist model, parsing, and file-system storage. |
| `queue` | `AbstractQueue`, `LinearQueue`, `FairQueue`, `PlaybackHistory`, and queue persistence. |
| `service` | `MusicService` facade; `PlayerService`, `PlaybackControlService`, `QueueService`, `PlaylistService`, `FavoritesService`, `HistoryPlaylistService`, `PlaylistTransferService`, `PlaylistDraftService`, `PlaylistPreviewService`, `HistoryService`, `NowPlayingService`; `SearchService`; `OutputAdapter`, `TrackEnqueuer`; audio load result handlers. |
| `service.dto` | DTOs shared by the facade, services, commands, listeners, and GUI. |
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
       -> QueuePersistenceService
       -> PlayerManager
       -> NowPlayingHandler
       -> AloneInVoiceHandler
       -> PlayerService
       -> MusicService (facade)
       -> SearchService
       -> YoutubeOauth2TokenHandler
  -> GUI (optional)
  -> CommandFactory / SlashCommandRegistry
  -> PlayerManager.init()      (register enabled audio sources)
  -> DiscordService.createJDA()
  -> StartupLifecycleListener.onReady()
       -> QueuePersistenceService.start()
       -> QueuePersistenceService.restoreAll()
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
  -> TrackEnqueuer (PlayerService)
  -> AudioHandler.addTrack()
  -> AudioPlayer.playTrack()
  -> NativeAudioSendFactory (udpqueue)
  -> Discord voice
```

Queue inspection and mutation follow the same facade, then go to
`QueueService` and the guild's `AudioHandler` queue.

Each guild has its own `AudioHandler`, queue, and `Settings`. `AudioHandler`
extends Lavaplayer's `AudioEventAdapter` to drive track start/end, now-playing
updates, repeat modes, and history.

`QueueType` selects the queue strategy:

- `LINEAR`: append order.
- `FAIR`: interleaves tracks by requester.

`PlaybackHistory` stores previous tracks for the `previous` and `history`
commands.

## Queue persistence

`QueuePersistenceService` stores per-guild snapshots in `queuedata.json`. Each
entry contains the current track, its position, the pending queue, and playback
history. Tracks are stored as URIs plus `RequestMetadata`; Lavaplayer reloads
them when the queue is restored. The currently playing track is stored
separately and omitted from the history snapshot so it is not immediately
replayed by `/previous` after a restart.

- Snapshots are written atomically every 15 seconds and before shutdown.
- On `ReadyEvent`, saved queues and history are restored into their guild
  handlers. The handler stays idle: Jasmine does not reconnect to voice or start
  audio.
- `/play` with no arguments resumes the first restored track when the player is
  idle. `/queue`, `/history`, and `/previous` remain available while nothing is
  playing.
- Tracks whose URI can no longer be loaded are skipped and logged.
- Version 1 state files are still accepted; their history starts empty.

## Service decomposition

`MusicService` is a thin facade kept for command/listener compatibility. It
delegates to focused services and owns no service-layer business logic:

- `PlayerService`: track loading, validation, and enqueueing.
- `PlaybackControlService`: volume, repeat, seek, skip, stop, pause, previous
  navigation, and play-now/skip-to transitions.
- `QueueService`: queue inspection, removal, movement, shuffle, and queue-size
  queries.
- `PlaylistService`: saved playlist CRUD, playlist queue/play operations, and
  orchestration of favorites/history export. Delegates direct file edits to
  `PlaylistMutationService`.
- `PlaylistMutationService`: direct append/remove/move/replace edits of saved
  playlist files, preserving shuffle directives.
- `FavoritesService`: adds the currently playing track to the favorites playlist.
- `HistoryPlaylistService`: exports http(s) playback history to a saved playlist.
- `PlaylistTransferService`: imports playlist text attachments and exports raw
  playlist files without applying shuffle directives.
- `PlaylistDraftService`: interactive playlist draft state, mutations, saves, and
  discards.
- `PlaylistPreviewService`: playlist track previews and paginated track views.
- `HistoryService`: history view data plus queue/play operations from history.
- `NowPlayingService`: now-playing and queue view data formatting.

`PlaylistService`, `HistoryService`, `SearchService`, and
`AudioLoadResultHandlers` receive `TrackEnqueuer` directly. `PlaylistService`
and `HistoryService` also receive focused collaborators instead of the facade.
The facade is therefore an entry point, not a dependency of the service layer.

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
- `queuedata.json`: per-guild queue snapshots (Jackson).
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
`PlayerServiceTest`, `MusicServiceTest`, `QueueServiceTest`,
`PlaylistDraftServiceTest`, and `PlaylistPreviewServiceTest` cover the focused
services directly and through the facade.
`QueuePersistenceServiceTest` covers queue and history snapshots, restoration,
malformed state files, and failed track reloads.
`PlaylistTransferServiceTest` covers import validation, raw export, and playlist
name sanitization.
`AudioLoadResultHandlersTest` covers load results and fallback paths.
`SearchCmdTest` and `ForceRemoveCmdTest` cover the select-menu interaction flows.
`PlaylistParserTest` covers playlist parsing and shuffle directives.
Config tests cover legacy migration, rendering, diagnostics, validation, and
file updates. Listener tests use Mockito and the fixtures under `testutil`.
The `coverage` profile enforces a global line-coverage floor and a higher floor
for the `service` package.

## Known technical debt

- `PlaybackControlService` still coordinates several playback and DJ actions;
  skip/seek handling could be extracted further if those flows grow.
- `PlaylistsInteractionListener` still owns playlist list/detail button routing;
  it can be split further once the interaction flows stabilize.
- The v1 and v2 command layers duplicate some presentation logic.
- Performance monitoring singletons are convenient but harder to test than
  injected instances.
- Remaining JDA-Chewtils deprecations (`guildOnly`, `OrderedMenu`) need an
  upstream-compatible interaction migration.

These are refactors, not behavior changes. Keep tests green while addressing
them.
