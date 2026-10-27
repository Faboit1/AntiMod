# Skript to Kite Migration Notes

## Converted Scripts

### teleportation/
- **spawn.kite.kts** — `/setspawn` and `/spawn` commands with PersistentDataContainer storage
- **warp.kite.kts** — `/setwarp`, `/delwarp`, `/warp` (3s countdown with movement check), `/warps`
- **tpa.kite.kts** — TPA request system with accept/deny, auto-accept toggle, cooldowns, expiry

### network/
- **send-afk.kite.kts** — `/afk` command to transfer to AFK server via BungeeCord plugin messaging
- **transfer.kite.kts** — `/send` command to transfer players to other servers

### server/
- **minimessage.kite.kts** — `/minimessage` preview command
- **enderchest-blocker.kite.kts** — Blocks `/enderchest` and `/ec` commands
- **utilities.kite.kts** — `/store`, `/ip`, `/nv`, `/clearmychat` utility commands
- **menu.kite.kts** — `/menu` with clickable links
- **rules.kite.kts** — `/rules` display command
- **rank-system.kite.kts** — LuckPerms rank display switcher with `/rank` command
- **join-order-group.kite.kts** — Auto-assigns "newog" group to early joiners via PlaceholderAPI

### player/
- **spectator.kite.kts** — `/spectator` gamemode toggle
- **music-player.kite.kts** — `/music` toggle with random disc playlist
- **double-jump.kite.kts** — Multi-jump system with permission-based jump counts and `/fly`
- **sit.kite.kts** — Sit on stairs/slabs using invisible armor stands
- **scoreboard-settings.kite.kts** — `/scoreboard` element visibility toggles

### economy/
- **shard-giver.kite.kts** — Gives 5 shards to all players every 5 minutes
- **give-book.kite.kts** — `/givebook` for enchanted books
- **give-vault.kite.kts** — `/givevault` for unlocking vault slots
- **key-all.kite.kts** — Gives crate keys to all players every 30 minutes
- **pay.kite.kts** — `/pay` with k/m/b suffixes, daily limits, Vault integration
- **gg-wave.kite.kts** — GG wave event with shard rewards
- **auction-list.kite.kts** — `/listah` auction house listing helper

### tools/
- **color-wand.kite.kts** — ColorWand tool for cycling block colors
- **drill.kite.kts** — 3x3 mining Quartz Drill with custom durability
- **quartz-axe.kite.kts** — Tree-felling Quartz Axe with custom durability

### anticheat/
- **disable-map-dupe.kite.kts** — Prevents duplication of locked maps
- **anti-dupe.kite.kts** — Item UID tracking for dupe detection

### minigames/
- **totem-practice.kite.kts** — Totem clicking practice with combo/CPS tracking
- **duel-queue.kite.kts** — Random duel queue (`/rtpq`) and `/duel` system
- **cheese-touch.kite.kts** — Cheese Touch infection/tag game
- **cheese-camel/** — Full Cheese Camel boss system (multi-file)

### moderation/
- **captcha.kite.kts** — Text-based CAPTCHA for new players
- **reports.kite.kts** — Player report system with staff notifications

### admin/
- **camel-cleaner.kite.kts** — Cheese camel bossbar cleanup
- **view-enderchest.kite.kts** — View player ender chests
- **delete-old-prefixes.kite.kts** — Batch removal of legacy LuckPerms prefixes

## Skipped Scripts

### Disabled (hyphen prefix)
- `-camelssssss.sk` — Disabled cheese camel script
- `-lobby.sk` — Disabled lobby BungeeCord integration
- `-lp-poll-detector.sk` — Disabled LuckPerms polling detector
- `-lp-eventbus-listener.sk` — Disabled LuckPerms event bus listener
- `-channel-watcher.sk` — Disabled channel watcher
- `-protocollib-packet-logger.sk` — Disabled ProtocolLib packet logger
- `-SHARDINSPAWN.sk.dontuse` — Explicitly disabled
- `-examples/` — Skript example files

### Unconvertible
- `discord/discord-bancheck.sk` — Uses DiSky (Discord bot framework), no Kite equivalent. Contains bot token.
- `discord/discordLink.sk` — Uses DiSky for Discord account linking. Contains bot token.

## Key Differences from Skript

1. **No `wait` keyword** — Kite uses `server.scheduler.runTask(delayTicks)` and `runTaskTimer(periodTicks, delayTicks)` instead
2. **No SkBee dialogs** — Interactive dialogs replaced with clickable chat messages using MiniMessage `<click:...>` and `<hover:...>` tags
3. **No Skript variables** — Uses Kotlin data structures (`ConcurrentHashMap`, `mutableListOf`, etc.) and PersistentDataContainer for persistence
4. **No skript-placeholders** — PlaceholderAPI accessed via Java reflection or direct API
5. **No skript-reflect** — Kite is already Kotlin, so Java APIs are accessed directly
6. **Data persistence** — World PersistentDataContainer used instead of Skript's `{variable}` system. Consider using a config file or database for larger datasets.
7. **Commands** — Use `command("name") { execute { sender, args -> } }` instead of Skript's command syntax
8. **Events** — Use `on<EventType> { event -> }` instead of Skript's `on <event>:` syntax

## Dependencies

Some scripts require external plugin APIs on the classpath:
- **pay.kite.kts** — Vault API (auto-downloaded via `@file:Dependency`)
- **rank-system.kite.kts** — LuckPerms (must be installed on the server)
- **join-order-group.kite.kts** — PlaceholderAPI + FirstLogin (must be installed)

## Placement

Copy the `scripts/` folder contents to your `plugins/Kite/scripts/` directory.
