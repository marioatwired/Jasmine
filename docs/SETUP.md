# Setup

This guide covers a fresh Jasmine installation.

## Requirements

- **Java 25+** for the JAR.
- **Docker Engine 20.10+** or Docker Desktop for the container image.
- On Linux, **glibc 2.38+** for the native audio libraries.
- A Discord application with a bot token.

## Getting a bot token

1. Open the [Discord Developer Portal](https://discord.com/developers/applications).
2. Click **New Application** and choose a name.
3. Open **Bot** and click **Reset Token**. Copy the token; you will not see it again.
4. Under **Privileged Gateway Intents**, enable **Message Content Intent**.
5. Copy the **Application ID** from the **General Information** page.

Without the Message Content Intent, classic text commands will not work.

## Invite the bot

Open this URL and replace `YOUR_CLIENT_ID` with the Application ID:

```text
https://discord.com/api/oauth2/authorize?client_id=YOUR_CLIENT_ID&permissions=70642752&scope=bot%20applications.commands
```

The permissions include viewing channels, sending messages, embedding links,
attaching files, managing messages, adding reactions, using external emojis,
connecting and speaking in voice, and changing its nickname.

## Finding your user ID

1. In Discord, open **Settings > Advanced** and enable **Developer Mode**.
2. Right-click your username and choose **Copy User ID**.
3. Paste the number into `discord.owner` in `config.txt`.

## Create the configuration

With the release JAR:

```bash
java -jar Jasmine-0.3.2.jar generate-config
```

Or start the bot once; it creates `config.txt` if it is missing.

Edit at least:

```hocon
discord {
  token = "YOUR_BOT_TOKEN"
  owner = 123456789012345678
}
```

See [CONFIG.md](CONFIG.md) for every section.

## Run Jasmine

```bash
java -Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED -jar Jasmine-0.3.2.jar
```

Headless server:

```bash
java -Dfile.encoding=UTF-8 -Dnogui=true --enable-native-access=ALL-UNNAMED -jar Jasmine-0.3.2.jar
```

Windows PowerShell:

```powershell
java "-Dfile.encoding=UTF-8" "-Dnogui=true" "--enable-native-access=ALL-UNNAMED" "-jar" ".\Jasmine-0.3.2.jar"
```

The default prefix is `@mention`. Mention the bot followed by `help`, or use the
slash commands.

## Docker

```bash
mkdir -p /path/to/jasmine
docker run --rm -it \
  --name jasmine \
  -v "/path/to/jasmine:/musicbot" \
  ghcr.io/marioatwired/jasmine:latest
```

The first run creates `config.txt` in the mounted directory. Edit it, then start
the container again.

The image runs as UID/GID `10001`. If your host directory is owned by another
user, either change its ownership or run the container with `--user`:

```bash
docker run --rm -it \
  --name jasmine \
  --user "$(id -u):$(id -g)" \
  -v "/path/to/jasmine:/musicbot" \
  ghcr.io/marioatwired/jasmine:latest
```

Docker Compose:

```bash
cp docker-compose.example.yml docker-compose.yml
docker compose up -d
```

## Troubleshooting

| Symptom | Check |
|---|---|
| Bot does not respond to text commands | Enable **Message Content Intent** and verify the prefix. |
| `Invalid token` | Regenerate the token and paste it without quotes or spaces. |
| Native library error on Linux | Check `ldd --version`; glibc must be 2.38+. |
| GUI does not open on a server | Use `-Dnogui=true`. |
| Slash commands are missing | Reinvite the bot with the `applications.commands` scope and restart it. |
| YouTube asks for sign-in | Enable `playback.youtube.useOAuth` and follow the device flow. |
| Audio stutters | Increase `performance.nasBufferMs` or `performance.frameBufferMs`. |

## Updating

- JAR: stop the bot, replace the JAR, keep `config.txt` and `Playlists/`, start again.
- Docker: `docker pull ghcr.io/marioatwired/jasmine:latest` and recreate the container.

Configuration migrations run automatically. A `.bak` backup is created before
the config file is rewritten.
