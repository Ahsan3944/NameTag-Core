# NameTag-Core — Master Blueprint

**Status:** Architecture baseline / implementation source of truth  
**Minecraft baseline:** Java Edition 1.21.11  
**Platforms:** Fabric Server + Paper Server  
**Goal:** One platform-independent NameTag core with isolated Fabric/Paper and Minecraft-version adapters.

## 1. Product Definition

NameTag-Core provides server-side custom rank/name-tag management. One or more matching active tag layers can be rendered:
1. Above/around the player's in-world nameplate.
2. In player chat as a rank/prefix.
3. Through future integrations/API consumers.

Example chat:
`[OWNER] UltraOP: Hello everyone!`

The same contextual multi-tag resolution rules are used by nameplate and chat rendering.

## 2. Core Features

### Tag lifecycle
- Create, edit, delete, list.
- Assign, set, remove, clear.
- Multiple tag definitions.
- UUID-based player assignment.
- Active-tag and priority resolution.
- Temporary tag assignments with per-tag expiration timestamps.

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
- Optional item-based icon metadata for the tag.

### Effects
- Effect engine from day one.
- None.
- Glitch as the first built-in effect.
- Glitch has two required modes: `white` and `colorful`.
- Glitch preserves the source tag length and selectively corrupts individual characters.
- Glitch supports configurable intensity and animation speed.
- Future Rainbow, Pulse, Wave, Flicker, Color Cycle, animated Gradient, etc.
- Effects must be scheduled efficiently; never blindly run expensive work every tick.

### Chat
- Active tag shown in chat when enabled.
- Optional native item-atlas icon shown before the tag/rank text.
- Icon and tag text are independently optional, so text-only, icon-only, and icon+text tags are supported.
- Player name is rendered exactly once.
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

### Command-first management
All server administration is performed through `/nametag` commands. There is no GUI or web-management layer in the core scope. Commands call the common service layer, which validates and persists changes through the configured storage provider.

## 3. Commands

Primary namespace:

`/nametag`

Canonical grouped command surface:
```
/nametag tag create <tag> name <displayName> style <style> color <color> effect <normal|glitch> ...
/nametag tag create <tag> item <item> spin <true|false> [speed <1-10>]
/nametag tag create <tag> name+item <item> name <displayName> style <style> color <color> effect <normal|glitch> ...

The command tree is grouped for TAB completion: group -> sub-group/option -> valid values.
/nametag tag edit <tag> name <displayName|none>
/nametag tag edit <tag> color <preset|random|#RRGGBB>
/nametag tag edit <tag> gradient <startHex> <endHex>
/nametag tag edit <tag> style <plain|bold|italic|bold_italic>
/nametag tag edit <tag> priority <integer>
/nametag tag edit <tag> enabled <true|false>
/nametag tag edit <tag> chat <true|false>
/nametag tag list
/nametag tag delete <tag>
/nametag player set <player> <tag> [duration]

`give` grants/adds a tag; `set` activates a tag and also assigns it when necessary.
/nametag player remove <player>
/nametag player clear <player>
/nametag display glitch <tag> <white|colorful>
/nametag display effect <tag> <none|rainbow|pulse|wave>
/nametag display item <tag> set <item>
/nametag display item <tag> mode <static|rotate>
/nametag display item <tag> speed <1-10>
/nametag display item <tag> clear
/nametag advanced role <tag> <permission|clear>
/nametag advanced scope <tag> clear
/nametag advanced scope <tag> world <world>
/nametag advanced scope <tag> region <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>
/nametag admin reload
/nametag admin export <file>
/nametag admin import <file>
```

Tab completion is required for subcommands, players, tags and valid options.

## 4. Architecture

```
Command layer
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
per-tag expiration timestamps
```

Tag ID and display name are separate. Player UUID is the persistent identity; username is never the primary key.

Item presentation metadata uses `item`, `item-mode`, and `item-speed`. An empty display name is valid when an item icon is configured for an icon-only tag.

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
The built-in Glitch NameTag is adapted from the earlier GlitchIdentity concept but uses a stable NameTag contract.

Required modes:
- `white` — all glitch-frame glyphs render white.
- `colorful` — each glyph receives a rapidly changing RGB color.

Frame rules:
- Preserve the source tag length.
- Preserve whitespace.
- Selectively replace non-whitespace characters from a controlled glitch-character pool.
- Allow the original character to survive in a frame.
- Generate fast-changing frames from a stable seed and frame index.
- Do not replace the complete NameTag with a random 5–7 character identity; that behavior belongs to the earlier death-message GlitchIdentity design and is unsuitable for a persistent nameplate.

Configuration:
- `mode`: `white` or `colorful`.
- `intensity`: 0–100, default 45.
- `speed-ms`: 30–2000, default 80.

The core exposes a platform-independent `GlitchFrame` containing final text and per-glyph RGB values. Scheduling and native rendering remain platform/version responsibilities.

Canonical command:
`/nametag display glitch <tag> <white|colorful>`

The command changes only the tag effect; display name, base color, style, priority, enabled state, chat visibility and metadata remain unchanged.

The adapter must degrade gracefully if a target rendering API cannot support a specific effect.

## 9.1 Glitch Nameplate Animation

When the active tag has `effect.id = glitch`, the platform adapter schedules frame updates using `speed-ms`. Only players currently using an animated glitch tag are scheduled. The renderer consumes `GlitchFrame` output from core and converts glyph RGB values to the platform's native text component format.

A platform adapter must not assume that a scoreboard/team implementation is always safe to own globally; it must account for existing server-side team/nameplate integrations and isolate its own rendering state.

## 9. Nameplate Rendering

Core describes what should be rendered:
`tag + player name + style + effect`

The platform/version layer decides how it is rendered. Possible mechanisms include supported player-name/team/nameplate APIs. No mechanism is hard-coded into core.

## 10. Chat Rendering

Chat is a first-class integration, not a separate duplicate tag system.

Conceptual:
```
ChatFormat
├── itemPosition
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

The `{item}` placeholder renders the tag's native Minecraft 1.21.11 item-atlas sprite. It is independent from `{tag}`: text-only, icon-only, and icon+text tags are supported. Fabric cannot move the sender name with a content-phase decorator, so tagged chat is rendered as a complete system-chat component in the allow phase. Paper owns the complete rendered component through Adventure. In both platforms the player name is emitted exactly once. Legacy formats that omit `{item}` are normalized at render time by inserting it before `{tag}`/`{tags}`, or before `{player}` when no tag placeholder exists.

The Fabric/Paper adapter owns native chat event/component handling. Fabric's exact pre-name layout intentionally uses a system-chat packet for tagged lines; native signed player-chat formatting remains available when NameTag chat composition is inactive.

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

Automatic role tags use the reserved tag metadata key `auto-permission`. Platform permission bridges resolve that node for online players; explicit assigned tags remain authoritative.

OP is the default admin fallback where supported.

## 12. Storage

Repositories:
```
TagRepository
PlayerAssignmentRepository
```

Current providers:
- YAML file storage.
- SQLite.
- MySQL.
- MariaDB.
- PostgreSQL.

All providers implement the same repository contracts. JDBC providers initialize a versioned schema, use transactional repository writes, and support one-time YAML-to-database migration when configured.

Storage is replaceable, versioned and preferably atomic. Persistent data has a schema version and migration path.

Separate:
- General config.
- Tag definitions.
- Player assignments.
- Messages/localization.

## 13. API

Stable API exposes:
- Tag CRUD.
- Assignment.
- Legacy single active-tag lookup.
- Contextual multi-tag lookup.
- Querying.
- Events.
- Effect registration.
- Storage contracts.
- Permission contracts.

Current domain events:
- `TagEvent.Created`.
- `TagEvent.Updated`.
- `TagEvent.Deleted`.
- `TagEvent.AssignmentChanged`.

No API event exposes platform-specific classes.

## 14. Validation & Reliability

Validate before persistence:
- ID syntax/length.
- Display name; an empty display name is valid for an icon-only tag when an item is configured.
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
- Glitch modes and frame generation.
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

## 19. Definition of Done — v1.0 baseline

- Core/API build cleanly.
- Fabric 1.21.11 build/start/test.
- Paper 1.21.11 build/start/test.
- Create/list/give/remove/delete/reload work.
- Persistence survives restart.
- YAML and configured JDBC providers use the same repository contracts.
- Preset/RGB/random colors work.
- Formatting works.
- Glitch white mode works within supported rendering limits.
- Glitch colorful mode works within supported rendering limits.
- `/nametag display glitch <tag> white|colorful` changes only the effect configuration.
- Active contextual/layered tags render in nameplate.
- Contextual/layered tags render in chat when enabled.
- Native item-icon chat composition supports text-only, icon-only and icon+text tags.
- Legacy chat formats remain compatible with automatic `{item}` insertion.
- Automatic role resolution works without persisting derived assignments.
- Chat permission and per-tag visibility work.
- Reload does not duplicate formatting/listeners.
- No platform dependency leaks into core.
- Documentation matches implementation.

## 20. Documentation Rule

`docs/BLUEPRINT.md` is the master technical source of truth. Any intentional architectural change must update this document first, then README/roadmap. No undocumented duplicate implementation is allowed.


## 21. Common Command and Message Contracts

The command path is the primary management surface and is platform-neutral. Platform adapters provide native implementations of:

- `CommandSource`: sender identity, permissions and message delivery.
- `PlayerResolver`: online-player lookup without exposing platform classes to common code.
- `CommandContext`: immutable command arguments plus the platform-neutral source.
- `NameTagCommandHandler`: command execution and tab-suggestion contract.
- `MessageService`: message lookup and placeholder formatting.

The initial common implementation is `DefaultNameTagCommandHandler` plus `DefaultMessageService`. These own NameTag command business rules and default English messages so Fabric and Paper adapters do not independently reimplement command behavior.

Platform command registration is now an adapter concern: Paper and Fabric convert their native command sources/arguments into this common handler, while preserving their native command parsing where appropriate. Reload/configuration remains a separate milestone and must not be coupled to this contract.
