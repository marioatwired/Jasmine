# Configuration

Jasmine reads `config.txt` from its working directory. The file uses
[HOCON](https://github.com/lightbend/config/blob/main/HOCON.md), so it supports
nested blocks and comments with `#` or `//`.

Generate a fresh file with:

```bash
java -jar Jasmine-0.10.0.jar generate-config
```

The source of truth for defaults is
[`src/main/resources/reference.conf`](../src/main/resources/reference.conf).
The `meta.configVersion` field is managed by the bot; do not edit it manually.

## Minimal configuration

```hocon
discord {
  token = "YOUR_BOT_TOKEN"
  owner = 123456789012345678
}
```

Everything else has a default.

## Environment overrides

Docker deployments can provide credentials without editing `config.txt`:

| Variable | Description |
|---|---|
| `JASMINE_TOKEN` | Discord bot token. |
| `JASMINE_OWNER` | Discord user ID of the bot owner. |
| `JASMINE_TOKEN_FILE` | Path to a file containing the token (for Docker secrets). |
| `JASMINE_OWNER_FILE` | Path to a file containing the owner ID. |

`DISCORD_TOKEN` and `DISCORD_OWNER` are accepted as aliases. Direct variables
take precedence over file variables, and environment values take precedence
over values from `config.txt`. You can set only one of them; the other is read
from the config file or prompted for when an interactive terminal is available.

## Sections

### `meta`

| Key | Description |
|---|---|
| `configVersion` | Internal config schema version. Managed automatically. |

### `discord`

| Key | Description |
|---|---|
| `token` | Bot token from the Discord Developer Portal. |
| `owner` | Your Discord user ID. The owner can run owner-only commands. |

### `playback`

| Key | Default | Description |
|---|---|---|
| `maxTrackSeconds` | `0` | Maximum track length in seconds. `0` disables the limit. |
| `maxYouTubePlaylistPages` | `10` | Maximum YouTube playlist pages (100 tracks per page). |
| `maxHistorySize` | `40` | Playback history entries. `0` disables history. |
| `skipRatio` | `0.55` | Fraction of listeners required to vote-skip. |
| `youtube.useOAuth` | `false` | Use the YouTube OAuth device flow. |
| `youtube.fallbackClients` | `true` | Add secondary playback clients after the OAuth TV clients. |
| `youtube.remoteCipherUrl` | `https://cipher.kikkia.dev/` | Remote cipher URL for OAuth playback. Blank uses the local cipher. |
| `youtube.remoteCipherPassword` | empty | Optional remote cipher password. |
| `youtube.remoteCipherUserAgent` | `jasmine` | User agent sent to the remote cipher service. |
| `audioSources` | all `true` | Enable or disable YouTube, SoundCloud, Bandcamp, Vimeo, Twitch, Beam, GetYarn, Nico, HTTP, and local sources. |
| `transforms` | `{}` | Advanced input rewriting rules. |

### `commands`

| Key | Default | Description |
|---|---|---|
| `prefix` | `"@mention"` | Text command prefix. |
| `altPrefix` | `"NONE"` | Optional second prefix. |
| `help` | `"help"` | Help word. |
| `clearChannel.deleteLimit` | `50` | Maximum messages deleted per clear operation. |
| `clearChannel.ageDays` | `14` | Maximum age of messages eligible for clearing. |
| `aliases` | built-in | Per-command aliases. |

### `presence`

| Key | Default | Description |
|---|---|---|
| `game` | `"DEFAULT"` | Activity text, `"NONE"`, or `"DEFAULT"`. |
| `status` | `"ONLINE"` | `ONLINE`, `IDLE`, `DND`, or `INVISIBLE`. |
| `songInStatus` | `false` | Show the current song in the bot presence. |

### `ui` and `gui`

| Key | Default | Description |
|---|---|---|
| `ui.emojis.success` | built-in | Success emoji. |
| `ui.emojis.warning` | built-in | Warning emoji. |
| `ui.emojis.error` | built-in | Error emoji. |
| `ui.emojis.loading` | built-in | Loading emoji. |
| `ui.emojis.searching` | built-in | Searching emoji. |
| `gui.enabled` | `true` | Enable the Swing GUI. |
| `gui.theme` | `"dark"` | `light`, `dark`, `darcula`, or `intellij`. |
| `gui.fontSize` | `12` | Base GUI font size, 8–24. |

On servers, use `-Dnogui=true` or set `gui.enabled = false`.

### `nowPlaying`

| Key | Default | Description |
|---|---|---|
| `images` | `false` | Show YouTube thumbnails. Requires `minimalMessage = false`. |
| `minimalMessage` | `false` | Compact now-playing layout. |
| `showButtons` | `true` | Add playback buttons. |
| `showProgressBar` | `false` | Show a progress bar and enable the refresh thread. |

### `voice`

| Key | Default | Description |
|---|---|---|
| `stayInChannel` | `false` | Stay connected after the queue ends. |
| `aloneTimeUntilStopSeconds` | `10` | Leave after being alone for this long. `0` disables. |

### `paths`

| Key | Default | Description |
|---|---|---|
| `playlistsFolder` | `"Playlists"` | Playlist storage directory, relative or absolute. |

### `updates` and `logging`

| Key | Default | Description |
|---|---|---|
| `updates.alerts` | `true` | DM the owner when a new release is available. |
| `logging.level` | `"info"` | `off`, `error`, `warn`, `info`, `debug`, `trace`, or `all`. |

### `performance`

| Key | Default | Description |
|---|---|---|
| `nasBufferMs` | `800` | Native audio send buffer. Higher values tolerate longer JVM pauses. |
| `frameBufferMs` | `2000` | Decoded audio buffered ahead of playback. |

### `proxy`

| Key | Default | Description |
|---|---|---|
| `host` | `""` | HTTP proxy host. Empty disables the proxy. |
| `port` | `0` | HTTP proxy port. |
| `lavaplayer` | `false` | Route audio source requests through the proxy. |
| `jda` | `false` | Route Discord API traffic through the proxy. |
| `github` | `false` | Route update checks through the proxy. |

Only HTTP proxies are supported.

## YouTube OAuth

If YouTube asks the bot to sign in, enable:

```hocon
playback {
  youtube {
    useOAuth = true
  }
}
```

On the next startup, Jasmine prints a device code (and DMs the owner when
possible). Open <https://www.google.com/device>, enter the code, and authorize
the bot. The refresh token is stored locally and reused.

When OAuth is enabled, Jasmine uses the TV clients first for playback and then
falls back to `AndroidVrWithThumbnail`, `MWebWithThumbnail`, and
`WebWithThumbnail` if the TV clients cannot load stream formats. Set
`fallbackClients = false` to use only the original TV clients.

OAuth playback also uses the remote cipher service configured in
`remoteCipherUrl`. Leave it empty to use the local cipher implementation. Set
`remoteCipherPassword` and `remoteCipherUserAgent` when the service requires
them.

`poToken` is not available in `youtube-source` 1.18.2, so Jasmine does not expose
that option yet.

## Configuration updates

Jasmine migrates old configs automatically:

- Legacy flat configs (no `meta.configVersion`) are migrated to version 1.
- Version 1 configs are migrated to version 2.
- A numbered backup (`.bak`, `.bak1`, …) is created before writing.
- Comments and formatting are preserved where possible.

If a config cannot be migrated, Jasmine reports the error and keeps the original
file untouched.
