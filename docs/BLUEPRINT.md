# NameTag-Core — Master Blueprint

**Status:** Architecture baseline / implementation source of truth  
**Minecraft baseline:** Java Edition 1.21.11  
**Platforms:** Fabric Server + Paper Server  
**Goal:** One platform-independent NameTag core with isolated Fabric/Paper and Minecraft-version adapters.

## 1. Product Definition

NameTag-Core provides server-side custom rank/name-tag management. An assigned active tag can be rendered:
1. Above/around the player's in-world nameplate.
2. In player chat as a rank/prefix.
3. Through future integrations/API consumers.

Example chat:
`[OWNER] UltraOP: Hello everyone!`

The same active-tag resolution must be used by nameplate and chat rendering.

## 2. Core Features

### Tag lifecycle
- Create, edit, delete, list.
- Assign, set, remove, clear.
- Multiple tag definitions.
- UUID-based player assignment.
- Active-tag and priority resolution.
- Optional future temporary/expiring tags.

### Appearance
- Minecraft preset colors.
- RGB/hex colors.
- Random color.
- Gradient.
- Bold.
- Italic.
- Underline.
- Strikethrough.
- Obfuscated.
- Clean/reset formatting.

### Effects
- Effect engine from day one.
- None.
- Glitch as the first built-in effect.
- Future Rainbow, Pulse, Wave, Flicker, Color Cycle, animated Gradient, etc.
- Speed/intensity configuration.
- Effects must be scheduled efficiently; never blindly run expensive work every tick.

### Chat
- Active tag shown in chat when enabled.
- Configurable tag/name/message placement.
- Reuse tag color/style where the target platform supports it.
- Global chat toggle.
- Per-tag chat visibility.
- `nametag.chat` permission.
- Future placeholder/API integration.
- If an advanced effect cannot be represented safely in chat, fall back to supported text/color formatting.

### Administration
- OP fallback for administrative actions.
- Platform-neutral permissions.
- Clear validation and error messages.
- Reload without duplicating listeners or corrupting data.

### GUI
Future editor using the same API/core services:
- Tag browser.
- Create/edit.
- Color picker.
- RGB.
- Gradient.
- Formatting.
- Effect editor.
- Preview.
- Save/cancel.

GUI must never contain independent business logic or write storage directly.

## 3. Commands

Primary namespace:

`/nametag`

Initial contract:
```
/nametag create
/nametag list
/nametag give <player> <tag>
/nametag set <player> <tag>
/nametag remove <player>
/nametag clear <player>
/nametag delete <tag>
/nametag reload
```

Tab completion is required for subcommands, players, tags and valid options.

## 4. Architecture

```
External integrations / GUI
          |
         API
          |
        Core
   /      |       \
Effects Storage Permissions
          |
   Platform Contracts
      /          \
   Fabric        Paper
      |            |
Version Adapter  Version Adapter
      |            |
 Minecraft API / server API
```

### Rules
- Core never imports Fabric, Paper, Bukkit, Fabric API or Minecraft implementation classes.
- Fabric never depends on Paper.
- Paper never depends on Fabric.
- Version-specific code never enters core.
- Platform adapters translate native events/objects into core contracts.
- Business logic is implemented once.

## 5. Modules

Target Gradle structure:
```
:core
:api
:common
:platform:fabric
:platform:paper
:versions:fabric-1.21.11
:versions:paper-1.21.11
```

The exact Gradle arrangement may be adjusted when required by Loom/Paper tooling, but dependency boundaries are mandatory.

## 6. Domain Model

Tag:
```
id
displayName
color
gradient
style
effect
priority
enabled
chatEnabled
metadata
```

Player assignment:
```
playerUuid
assignedTagIds
activeTagId
expirationMetadata
```

Tag ID and display name are separate. Player UUID is the persistent identity; username is never the primary key.

## 7. Colors

Color abstraction supports:
- Preset.
- RGB/hex.
- Random.
- Gradient.

Random must have explicit semantics: static/persistent random or dynamic animation. It must not unexpectedly change after restart.

## 8. Effects

Effect contract:
```
id
name
configuration
lifecycle
processor/renderer contract
```

Effect lifecycle may be static, time-based, player-dependent or scheduled. Runtime scheduling belongs outside core.

### Glitch
Supported modes may include:
- Character corruption.
- Flicker.
- Color glitch.
- Obfuscation bursts.
- Configurable intensity.
- Configurable speed.

The adapter must degrade gracefully if a target rendering API cannot support a specific effect.

## 9. Nameplate Rendering

Core describes what should be rendered:
`tag + player name + style + effect`

The platform/version layer decides how it is rendered. Possible mechanisms include supported player-name/team/nameplate APIs. No mechanism is hard-coded into core.

## 10. Chat Rendering

Chat is a first-class integration, not a separate duplicate tag system.

Conceptual:
```
ChatFormat
├── tagPosition
├── playerNamePosition
├── messagePosition
├── separator
└── visibility/styling rules
```

If multiple tags are assigned, chat uses the same active-tag/priority resolution as the nameplate.

Permissions:
```
nametag.chat
```

The Fabric/Paper adapter owns native chat event/component handling.

## 11. Permissions

```
nametag.use
nametag.create
nametag.edit
nametag.delete
nametag.give
nametag.remove
nametag.reload
nametag.chat
nametag.admin
```

OP is the default admin fallback where supported.

## 12. Storage

Repositories:
```
TagRepository
PlayerAssignmentRepository
```

Initial provider: local human-readable files.

Future providers:
- JSON/YAML.
- SQLite.
- MySQL/MariaDB.
- PostgreSQL.

Storage is replaceable, versioned and preferably atomic. Persistent data has a schema version and migration path.

Separate:
- General config.
- Tag definitions.
- Player assignments.
- Messages/localization.

## 13. API

Stable API must expose:
- Tag CRUD.
- Assignment.
- Active-tag lookup.
- Querying.
- Events.
- Effect registration.
- Storage contracts.
- Permission contracts.

Planned events:
- TagCreated.
- TagUpdated.
- TagDeleted.
- TagAssigned.
- TagRemoved.
- ActiveTagChanged.
- EffectRegistered.

No API event exposes platform-specific classes.

## 14. Validation & Reliability

Validate before persistence:
- ID syntax/length.
- Display name.
- Duplicate IDs.
- RGB values.
- Gradient stops.
- Effect configuration.
- Priority.
- Unsupported platform options.

Security:
- Validate command input.
- Prevent path traversal.
- Re-check server-side permissions.
- Never trust client GUI state.
- UUID-based records.

## 15. Performance

Avoid:
- Unnecessary per-tick processing.
- Rebuilding identical components.
- Unbounded caches.
- Heavy synchronous database work.
- Running animations for players without animated tags.

Caches are optimization only, never the source of truth.

## 16. Reload

`/nametag reload`:
1. Validate config.
2. Reload definitions/messages.
3. Refresh caches.
4. Reconcile active assignments.
5. Keep valid data if some input is invalid.
6. Never register duplicate chat/render listeners.

## 17. Version Strategy

Minecraft 1.21.11 is the baseline.

Future versions such as 26.1/26.2 require their own tested adapter/build because Minecraft's tooling/mapping model changed after 1.21.11. The core must not be copied wholesale.

Upgrade:
1. Add/update adapter.
2. Compile.
3. Fix platform API changes.
4. Run unit tests.
5. Run platform tests.
6. Test commands.
7. Test storage/migration.
8. Test nameplate.
9. Test chat.
10. Test effects.
11. Update compatibility matrix.
12. Release.

## 18. Testing

Unit:
- Tag validation.
- Colors/RGB/gradient.
- Styles.
- Effects.
- Assignment.
- Priority.

Integration:
- Storage.
- Reload.
- Commands.
- Permissions.
- Rendering bridge.
- Chat bridge.

Platform:
- Fabric 1.21.11 startup and behavior.
- Paper 1.21.11 startup and behavior.

Regression:
Every new Minecraft adapter must pass the same core behavior suite.

## 19. Definition of Done — v1.0

- Core/API build cleanly.
- Fabric 1.21.11 build/start/test.
- Paper 1.21.11 build/start/test.
- Create/list/give/remove/delete/reload work.
- Persistence survives restart.
- Preset/RGB/random colors work.
- Formatting works.
- Glitch works within supported rendering limits.
- Active tag renders in nameplate.
- Active tag renders in chat when enabled.
- Chat permission and per-tag visibility work.
- Reload does not duplicate formatting/listeners.
- No platform dependency leaks into core.
- Documentation matches implementation.

## 20. Documentation Rule

`docs/BLUEPRINT.md` is the master technical source of truth. Any intentional architectural change must update this document first, then README/roadmap. No undocumented duplicate implementation is allowed.
