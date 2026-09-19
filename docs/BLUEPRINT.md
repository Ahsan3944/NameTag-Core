# NameTag-Core — Master Technical Blueprint

**Document status:** Locked architecture baseline  
**Target:** Minecraft Java Edition 1.21.11  
**Platforms:** Fabric Server + Paper Server

## 1. Purpose

NameTag-Core is a reusable NameTag framework, not merely a one-off plugin.

The system must support:
- Custom player NameTags.
- Preset Minecraft colors.
- RGB/hex colors.
- Random colors.
- Gradients.
- Text formatting.
- Extensible visual/text effects.
- Glitch effect.
- Player assignment.
- Permissions.
- Persistent storage.
- API integrations.
- Fabric and Paper.
- Future Minecraft version adapters.

The central architectural rule is:

> **Business logic is written once in the core. Platform and Minecraft-version details live in adapters.**

## 2. Non-Goals

The initial project will not:
- Copy the same business logic into Fabric and Paper modules.
- Hard-code all behavior around Minecraft 1.21.11.
- Require an external database.
- Make GUI the foundation of the system.
- Treat player usernames as permanent identifiers.
- Promise compatibility with future Minecraft versions before those versions are tested.

## 3. Layer Model

### Layer A — Domain/Core

Contains:
- Tag.
- Tag identifier.
- Tag manager.
- Player assignment model.
- Color model.
- Gradient model.
- Text style model.
- Effect model.
- Priority model.
- Validation rules.
- Domain events.
- Core services.

This layer knows nothing about Fabric, Paper, Bukkit, Fabric API, Minecraft classes or server internals.

### Layer B — API

Provides stable interfaces for:
- Tag CRUD.
- Player assignment.
- Querying.
- Events.
- Effects.
- Storage providers.
- Permission checks.
- Platform integration.

The API is the contract between core logic and external integrations.

### Layer C — Common Services

Contains reusable services that are not platform-specific but may depend on API contracts:
- Command service.
- Serialization helpers.
- Validation helpers.
- Configuration handling.
- Localization/message handling.
- Caching where appropriate.

### Layer D — Platform

Two implementations:

- Fabric server adapter.
- Paper server adapter.

Each adapter translates platform events, commands, permissions, player objects and rendering operations into core interfaces.

### Layer E — Version Adapter

Minecraft-version-sensitive code is isolated here.

Baseline:
- Fabric 1.21.11.
- Paper 1.21.11.

A future version should be able to replace only the affected adapter layer.

## 4. Dependency Rules

Allowed:

```
Version Adapter -> Platform Adapter -> Common/API -> Core
External Integration -> API
GUI -> API/Common
Storage Provider -> Storage API
Effect Provider -> Effect API
```

Forbidden:

```
Core -> Fabric
Core -> Paper
Core -> Minecraft implementation classes
Fabric -> Paper
Paper -> Fabric
Tag model -> platform-specific player object
```

## 5. NameTag Domain Model

Conceptual model:

```
Tag
├── id
├── displayName
├── color
├── gradient
├── style
├── effect
├── priority
├── enabled
└── metadata
```

### Tag ID

A stable internal identifier.

Example:

`owner`

### Display Name

The actual visible text.

Example:

`OWNER`

The ID and display name must be separate so the visible name can change without breaking assignments.

## 6. Color Model

The color system supports:

### Preset

A named color mapped through the platform text adapter.

### RGB

A full 24-bit color represented by RGB/hex.

Example:

`#FF4B4B`

### Random

A random color selected according to a defined randomization strategy.

Random behavior must be deterministic when the configuration explicitly requires persistence; it must not unexpectedly change every server restart.

### Gradient

Two or more color stops.

Example:

`#FF0000 -> #0000FF`

The core stores the gradient definition; rendering decides how characters/components receive the colors.

## 7. Text Style

Style model:

- Bold.
- Italic.
- Underline.
- Strikethrough.
- Obfuscated.
- Reset/normal state.

Styles must be represented as data, not scattered conditional rendering code.

## 8. Effect Engine

Effects are first-class extensions.

```
Effect
├── id
├── name
├── configuration
├── lifecycle
└── renderer/processor contract
```

Initial built-in effects:

- None.
- Glitch.

Future candidates:
- Rainbow.
- Pulse.
- Wave.
- Flicker.
- Color cycle.
- Typewriter.
- Custom animated gradient.

An effect must be able to declare whether it is:
- Static.
- Time-based.
- Player-dependent.
- Server-tick dependent.

The core must not assume that every effect runs every tick. Performance-sensitive scheduling belongs to the platform/runtime layer.

## 9. Glitch Effect

Glitch is a supported feature, but it must remain modular.

Possible modes:

### Character corruption

Temporarily replace or alter characters.

### Flicker

Rapidly alternate visible representations.

### Color glitch

Temporarily change color stops.

### Obfuscation bursts

Temporarily apply obfuscated formatting.

### Intensity

Controls the probability/amount of distortion.

### Speed

Controls update frequency.

The exact rendering capabilities may differ between Minecraft text APIs. If a requested glitch behavior is not technically reliable on a target platform, the adapter must provide the closest supported implementation rather than corrupting the core model.

## 10. Player Assignment

Player identity uses UUID.

Conceptual:

```
PlayerAssignment
├── playerUuid
├── activeTagId
├── assignedTagIds
├── priority/selection metadata
└── expiration metadata
```

Version 1 may expose one active tag to players while the underlying model remains capable of multiple assignments.

## 11. Tag Priority

Priority is required for future automation and multiple-tag support.

Example:

```
OWNER = 100
ADMIN = 90
MOD = 80
VIP = 50
MEMBER = 10
```

The numeric values are examples, not hard-coded defaults.

## 12. Commands

Primary namespace:

`/nametag`

Initial command contract:

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

The command parser belongs to the platform/common layer. The command operation itself delegates to core services.

Tab completion must be implemented for:
- Player names.
- Tag IDs/names.
- Subcommands.
- Valid configuration values where applicable.

## 13. Create/Edit Workflow

Initial command-driven workflow:

1. Admin executes create.
2. System validates permissions.
3. System collects tag ID/display name.
4. System collects color.
5. System collects formatting.
6. System collects effect.
7. System validates the complete definition.
8. System saves it.
9. System returns a confirmation and preview.

Later GUI workflow will call the same core service rather than duplicate creation logic.

## 14. GUI

The GUI is a client-facing management layer.

Planned sections:
- Tag list.
- Create.
- Edit.
- Delete confirmation.
- Color selector.
- RGB input.
- Gradient editor.
- Style toggles.
- Effect selector.
- Effect settings.
- Preview.
- Save/cancel.

GUI must never directly mutate storage. It calls core/API services.

## 15. Permissions

Platform-neutral permission IDs:

```
nametag.use
nametag.create
nametag.edit
nametag.delete
nametag.give
nametag.remove
nametag.reload
nametag.admin
```

The platform adapter maps these to the platform's permission system.

Default policy:
- OP can perform administrative operations where supported.
- Normal players can use assigned tags.
- Explicit permissions override the default where the platform allows.

## 16. Storage Architecture

Storage interface:

```
TagRepository
PlayerAssignmentRepository
```

Initial provider:
- Local human-readable file storage.

Possible implementations:
- JSON.
- YAML.
- SQLite.
- MySQL/MariaDB.
- PostgreSQL.

The storage API must support:
- Load.
- Save.
- Delete.
- Exists.
- List.
- Migration/version metadata.

Storage writes should be atomic where practical to avoid corruption.

## 17. Configuration

Separate:
- Plugin/mod configuration.
- Tag definitions.
- Player assignment data.
- Messages/localization.

Never mix all persistent data into one unversioned configuration file.

Configuration should have a schema/version field so future migrations are possible.

## 18. Rendering Architecture

Rendering is deliberately separated.

Core says:

```
Render this tag:
name + style + color + effect
```

Platform/version layer decides:

```
How does Minecraft 1.21.11 display this?
```

This is essential for future Minecraft API changes.

## 19. Nameplate Strategy

The project must distinguish between:
- Player's vanilla displayed name.
- Custom tag/prefix.
- Full composed nameplate.

The initial stable implementation should provide a clean abstraction such as:

```
Nameplate = Tag + PlayerName
```

Exact visual placement—above name, before name, after name, below name, scoreboard/team-based, or another supported mechanism—must be selected during the rendering implementation for each platform.

The core must not hard-code a specific rendering mechanism.

## 20. API Events

Planned events:

- TagCreated.
- TagUpdated.
- TagDeleted.
- TagAssigned.
- TagRemoved.
- ActiveTagChanged.
- EffectRegistered.

Events should be cancellable only where cancellation is meaningful. Domain operations must not expose platform event classes.

## 21. Caching

Caching may be used for active player tags and tag definitions.

Rules:
- Cache must never become the source of truth.
- Mutations invalidate/update cache.
- Reload clears or refreshes relevant caches.
- Player disconnect/reconnect must not create stale assignments.

## 22. Reload

`/nametag reload` should:
1. Validate configuration.
2. Reload tag definitions.
3. Reload messages/configuration.
4. Refresh caches.
5. Reconcile active assignments.
6. Report validation errors without silently destroying valid data.

## 23. Validation

Tag validation must check:
- ID format.
- Empty names.
- Maximum lengths.
- Duplicate IDs.
- Unsupported colors.
- Invalid RGB values.
- Invalid gradient stops.
- Invalid effect configuration.
- Invalid priority.
- Unsupported platform rendering options.

Validation must happen before persistence.

## 24. Error Handling

User-facing errors must be clear.

Examples:
- Tag not found.
- Player not found.
- No permission.
- Invalid color.
- Invalid tag ID.
- Tag already exists.
- Cannot render effect on this platform/version.
- Storage failure.

Internal errors must be logged with enough context for debugging without exposing unnecessary sensitive information.

## 25. Performance

The project must avoid:
- Unnecessary per-tick work.
- Rebuilding identical text components repeatedly.
- Unbounded caches.
- Synchronous heavy database operations on the main server thread.
- Running animation effects for players who do not have an animated tag.

Effects must use controlled scheduling.

## 26. Security / Reliability

- Validate all command input.
- Never trust client-provided identifiers.
- Sanitize file names/paths if tag IDs are used in file storage.
- Use UUIDs for player records.
- Prevent path traversal in storage providers.
- Do not allow normal players to execute admin operations through client-side GUI assumptions.
- Re-check permissions server-side for every privileged operation.

## 27. Testing Strategy

Required test categories:

### Unit
- Tag creation.
- Validation.
- Color parsing.
- RGB parsing.
- Gradient parsing.
- Style serialization.
- Effect configuration.
- Assignment logic.
- Priority logic.

### Integration
- Storage save/load.
- Reload.
- Commands.
- Permission checks.
- Player assignment.
- Rendering bridge.

### Platform
- Fabric 1.21.11 startup.
- Paper 1.21.11 startup.
- Commands.
- Player join/leave.
- Tag assignment.
- Nameplate rendering.
- Effect behavior.

### Regression
Every future version adapter must pass the baseline behavior suite.

## 28. Build Structure

Gradle multi-module project.

Target logical modules:

```
:core
:api
:common
:platform:fabric
:platform:paper
:versions:fabric-1.21.11
:versions:paper-1.21.11
```

The actual module layout may be simplified if the chosen loader/toolchain makes another structure cleaner, but the dependency boundaries must remain.

## 29. Version Upgrade Procedure

For a new Minecraft version:

1. Create a version adapter/module.
2. Update only platform/version integration points.
3. Compile.
4. Fix API changes.
5. Run unit tests.
6. Run platform integration tests.
7. Test commands.
8. Test storage migration.
9. Test rendering.
10. Test effects.
11. Build release artifacts.
12. Update compatibility matrix.
13. Update README.
14. Tag the release.

Do not copy the entire repository into a new version branch.

## 30. Release Artifacts

Future releases may provide:
- Fabric JAR.
- Paper JAR.
- API JAR where useful.
- Source JAR.
- Changelog.
- Compatibility matrix.

Artifacts must clearly state:
- NameTag-Core version.
- Minecraft version.
- Platform.
- Required dependencies.

## 31. Dependency Policy

Keep dependencies minimal.

Core should have no platform dependency.

Platform dependencies are declared only in platform/version modules.

Optional integrations must remain optional.

## 32. Logging

Log:
- Startup/version/platform.
- Configuration load.
- Storage initialization.
- Migration.
- Effect registration.
- Critical errors.

Do not spam the server console every tick.

## 33. Localization

Messages should be externalized so server owners can customize:
- Prefix.
- Success messages.
- Error messages.
- Permission messages.
- Tag previews.

English will be the baseline language. Additional languages can be added later without changing core logic.

## 34. Migration System

Persistent data must have a schema version.

Example:

```
schemaVersion: 1
```

Future versions can migrate old data instead of deleting or resetting it.

## 35. Future Features

Designed-for extensions:
- Multiple simultaneous tags.
- Prefix/suffix.
- Temporary tags.
- Expiration.
- Automatic permission-based tags.
- Animated tags.
- More effects.
- GUI editor.
- Placeholder integration.
- LuckPerms integration.
- Database providers.
- Web/API management.
- Import/export.
- Tag packs.
- Per-world tags.
- Per-region tags.
- Role synchronization.
- Tag cooldowns.
- Audit logs.

These are extension points, not promises for the first stable release.

## 36. Definition of Done — v1.0

A stable release is not complete until:

- Core builds cleanly.
- API builds cleanly.
- Fabric 1.21.11 starts cleanly.
- Paper 1.21.11 starts cleanly.
- Commands work on both platforms.
- Permissions work.
- Tags persist after restart.
- UUID assignments persist.
- Preset colors work.
- RGB works.
- Random color works.
- Text styles work.
- Glitch effect works within the supported rendering model.
- Reload works.
- Invalid configuration is handled safely.
- No core module imports platform-specific classes.
- Documentation matches the implementation.
- Version compatibility is documented.

## 37. Architectural Decision Record

### Decision: Shared Core

**Decision:** one platform-independent core.

**Reason:** prevents duplication and makes upgrades manageable.

### Decision: Adapter-Based Platform Support

**Decision:** Fabric and Paper adapters.

**Reason:** platform APIs differ and should not leak into core logic.

### Decision: Version Isolation

**Decision:** isolate Minecraft-version-specific code.

**Reason:** future Minecraft updates should not require rewriting the whole project.

### Decision: UUID-Based Player Storage

**Decision:** UUID is the persistent player identifier.

**Reason:** usernames are mutable.

### Decision: Effect Abstraction

**Decision:** effects are modular.

**Reason:** Glitch is only the first effect and future effects should not require redesigning tags.

### Decision: API First

**Decision:** GUI and integrations call API/core services.

**Reason:** prevents duplicated business logic.

## 38. Single Source of Truth

This document is the master technical blueprint.

If another document conflicts with it:
1. Implementation must be checked.
2. The blueprint must be updated if the architecture changes intentionally.
3. README must then be synchronized.
4. No silent architectural divergence is allowed.

Feature implementation should reference this blueprint before adding new modules or duplicated logic.
