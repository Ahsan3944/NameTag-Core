# NameTag-Core

A cross-platform, version-aware Minecraft NameTag framework designed to provide one stable core for both **Fabric Server Mods** and **Paper Server Plugins**.

## Project Status

**Phase:** Fabric/Paper 1.21.11 — core rendering, lifecycle, chat, persistence, GameTests and stability hardening implemented; release preparation in progress  
**Target baseline:** Minecraft Java Edition **1.21.11**  
**Platforms:** Fabric Server + Paper Server  
**Primary goal:** Build the core once, isolate platform/version-specific code, and make future Minecraft version upgrades predictable and maintainable.

## Vision

NameTag-Core is not intended to be a single-version, single-platform plugin. The project is designed as a layered system:

- **Core:** platform-independent NameTag domain logic.
- **API:** stable public interfaces for tags, players, effects, storage, permissions and integrations.
- **Platform adapters:** Fabric and Paper implementations.
- **Version adapters:** Minecraft-version-specific code isolated behind platform contracts.
- **Effect engine:** extensible visual/text effects, beginning with Glitch.
- **Storage abstraction:** configurable persistence without coupling the core to one database format.

This separation prevents Minecraft API changes from spreading through the entire project.

## Planned Features

### NameTag management
- Create, edit, delete and list tags.
- Assign and remove tags from players.
- One primary active tag in the first release, with the core designed to support multiple/layered tags later.
- UUID-based player identity.
- Tag priority support.
- Temporary tag assignments with persistent per-tag expiration.

### Colors
- Minecraft named/preset colors supported by the target text API.
- Custom RGB/hex colors.
- Random color.
- Animated/randomized color mode as an optional future effect.
- Built-in Rainbow, Pulse and Wave nameplate effects with bounded animation cadence/intensity.
- Gradient colors.

### Text formatting
- Bold.
- Italic.
- Underline.
- Strikethrough.
- Obfuscated.
- Reset/clean formatting behavior.

### Effects
- Effect API from the beginning.
- Glitch NameTag effect in the initial feature design.
- Glitch modes: **white** and **colorful**.
- Source tag length is preserved while characters are selectively corrupted.
- Configurable glitch intensity and animation speed.
- Future effects can be added without changing the tag model.
- Prefix/suffix presentation metadata is supported without coupling the core to platform text APIs.
- Configurable effect speed/intensity where supported.

### Permissions
Permission nodes are platform-neutral in the core and mapped to the native permission system by each adapter. Automatic role tags use the `auto-permission` tag metadata key; when no usable explicit assignment resolves, the highest-priority enabled tag whose permission matches becomes active.

Planned nodes include:
- `nametag.use`
- `nametag.create`
- `nametag.edit`
- `nametag.delete`
- `nametag.give`
- `nametag.remove`
- `nametag.reload`
- `nametag.admin`

OP access will be the default administrative fallback where the platform supports it.

### Commands

The current command namespace is:

`/nametag`

Planned commands:
- `/nametag create`
- `/nametag list`
- `/nametag give <player> <tag> [duration]`
- `/nametag remove <player>`
- `/nametag set <player> <tag>`
- `/nametag clear <player>`
- `/nametag delete <tag>`
- `/nametag reload`
- `/nametag glitch <tag> <white|colorful>`
- `/nametag effect <tag> <none|rainbow|pulse|wave>`
- `/nametag role <tag> <permission|clear>`
- `/nametag export <file>`
- `/nametag import <file>`

Command behavior is implemented through the common command layer and exposed consistently by Fabric and Paper adapters.

### Automatic Role Tags

Automatic role tags allow a tag to activate from a permission without storing a player assignment. Set the tag metadata key `auto-permission` to a permission node and use `/nametag role <tag> <permission|clear>` to manage it. Explicit assigned tags remain authoritative; automatic resolution is used when no usable explicit tag resolves. Matching enabled tags are ordered by priority, with tag ID used as the deterministic tie-breaker. Automatic results are not persisted, so permission changes are reflected on the next active-tag resolution.

## GUI

A GUI/editor is planned, but it will not be allowed to contaminate the core domain layer.

The editor will eventually expose:
- Tag name.
- Preset color.
- RGB/hex color.
- Gradient.
- Random color.
- Bold/italic/underline/strikethrough/obfuscated.
- Effect selection.
- Effect configuration.
- Preview.
- Save/cancel.

The first stable milestone prioritizes the API and command path before the GUI.

### Common Command Layer

Command business rules are implemented once in the platform-neutral common module through a stable command contract. Fabric and Paper provide sender/player-resolution adapters rather than maintaining separate command logic.

The message layer is also platform-neutral and currently provides default English messages with placeholder formatting. Typed YAML configuration is loaded during platform bootstrap, and safe runtime reload is provided through the common configuration reload contract.

## Configuration

The common configuration service provides an immutable typed runtime snapshot backed by `configuration.yml`. See [docs/CONFIGURATION.md](docs/CONFIGURATION.md) for the schema, defaults, validation rules and platform file locations. Installation is documented in [docs/INSTALLATION.md](docs/INSTALLATION.md), supported versions in [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md), and public contracts in [docs/API.md](docs/API.md). Chat placeholder and rendering rules are documented in [docs/CHAT.md](docs/CHAT.md). Supported chat placeholders include `{tag}`, `{tag_id}`, `{tag_priority}`, `{tag_prefix}`, `{tag_suffix}`, `{tag_meta:key}`, `{player}`, and `{message}`. Paper player lifecycle behavior is documented in [docs/PAPER_PLAYER_LIFECYCLE.md](docs/PAPER_PLAYER_LIFECYCLE.md). Paper plugin integration coverage is documented in [docs/PAPER_INTEGRATION_TESTS.md](docs/PAPER_INTEGRATION_TESTS.md). Fabric chat behavior is documented in [docs/FABRIC_CHAT.md](docs/FABRIC_CHAT.md). Fabric player lifecycle behavior is documented in [docs/FABRIC_PLAYER_LIFECYCLE.md](docs/FABRIC_PLAYER_LIFECYCLE.md).

## Caching

Active NameTag resolution uses a bounded LRU cache with mutation-aware invalidation. See [docs/CACHING.md](docs/CACHING.md) for the cache scope and safety rules.

## API

External mods/plugins should be able to interact with NameTag-Core through a stable API.

Planned API capabilities:
- Create/read/update/delete tags.
- Assign/remove tags.
- Read active tag.
- Query tag existence.
- Access color/style/effect definitions.
- Register custom effects through the EffectRegistry.
- Register storage providers where supported.
- Listen to tag/player lifecycle events through the domain event bus.


## Glitch NameTag

The built-in Glitch effect is based on the earlier GlitchIdentity rendering concept, adapted for persistent NameTags. It does not replace the complete tag with a random short string; instead, it preserves the original tag length and generates rapidly changing corrupted-character frames.

Two modes are supported:
- **white** — glitch frames use white glyphs.
- **colorful** — each glyph can receive a rapidly changing RGB color.

Example:
```text
/nametag glitch owner white
/nametag glitch creator colorful
```

The command changes only the effect and keeps the tag's other properties intact. See [docs/GLITCH.md](docs/GLITCH.md) for the effect contract and rendering rules.

## Chat Integration

The active NameTag is also designed to appear in player chat like a server rank/prefix.

Example:

```
[OWNER] UltraOP: Hello everyone!
```

Chat integration includes:
- Global enable/disable.
- Per-tag chat visibility.
- `nametag.chat` permission.
- Configurable tag/name/message placement.
- Reuse of tag color and supported formatting.
- The same active-tag/priority resolution used by the in-world nameplate.
- Platform-specific Fabric/Paper chat rendering adapters.
- Graceful fallback when a specific visual effect cannot be safely represented in chat.

The chat system is part of the v1.0 compatibility target, not a post-release add-on.

## Architecture

Conceptual dependency direction:

```
                    NameTag API
                         |
                    NameTag Core
             /-----------+-----------\\
            /            |            \\
      Effects        Storage       Permissions
            \\            |            /
             \\-----------+-----------/
                         |
                Platform Contracts
                  /             \\
                 /               \\
             Fabric             Paper
                |                 |
        Version Adapter    Version Adapter
                |                 |
          Minecraft API       Minecraft API
```

The core must never directly depend on Fabric-only or Paper-only classes. Domain events and effect registration remain platform-neutral; only rendering providers depend on platform/version APIs.

## Repository Structure

The planned repository layout is:

```
NameTag-Core/
├── README.md
├── LICENSE
├── .gitignore
├── docs/
│   ├── BLUEPRINT.md
│   └── ROADMAP.md
├── gradle/
├── settings.gradle
├── build.gradle
├── gradle.properties
├── core/
├── api/
├── common/
├── platform/
│   ├── fabric/
│   └── paper/
└── versions/
    ├── fabric-1.21.11/
    └── paper-1.21.11/
```

The exact Gradle module names may be refined during implementation, but the architectural boundary is fixed: **core/API code must not be mixed with platform/version code**.

## Versioning Strategy

The project uses semantic project versioning independently from Minecraft versions.

Example:

- NameTag-Core `0.1.0` = architecture/development milestone.
- NameTag-Core `1.0.0` = first stable release.
- Minecraft target remains a separate compatibility dimension.

A future Minecraft upgrade should normally add/update an adapter module rather than fork the entire codebase.

## Data Model

A tag should conceptually contain:

- Unique ID.
- Display name.
- Color definition.
- Optional gradient definition.
- Text style.
- Effect definition.
- Priority.
- Enabled state.
- Optional metadata.

Player data should use UUID as the stable identifier and store:
- Assigned tag(s).
- Active tag.
- Optional expiration metadata.

Player names must not be the primary identity key.

## Storage

NameTag-Core supports a configurable persistence provider selected in `storage.yml`.

Supported providers:
- YAML file storage (default).
- SQLite.
- MySQL.
- MariaDB.
- PostgreSQL.

The JDBC providers use the same repository contracts as YAML storage, create a versioned schema on first startup, use transactional replacement for writes, and can perform a one-time YAML-to-database migration when `migrateYaml: true`. The core does not depend on a specific database driver; platform packaging supplies the runtime JDBC drivers.

The default `storage.yml` is:

```yaml
schemaVersion: 1
type: yaml
jdbcUrl: ""
username: ""
password: ""
migrateYaml: true
```

For SQLite, leaving `jdbcUrl` blank uses `nametag.db` in the platform data directory. Remote providers require a JDBC URL and credentials as appropriate.

## Rendering

The core describes **what** a tag is. Platform/version adapters decide **how** Minecraft renders it.

This is important because text APIs and player-name rendering mechanisms can change between Minecraft versions.

The rendering layer must therefore be replaceable without changing tag management logic.

## Compatibility Policy

Initial target:

| Component | Baseline |
|---|---|
| Minecraft | 1.21.11 |
| Fabric | Server |
| Paper | Server |
| Java | 21 |

Future versions are added only after the baseline passes build, startup, command, persistence, assignment, rendering and regression tests.

## Development Rules

1. Do not duplicate core business logic between Fabric and Paper.
2. Do not place Minecraft-version-specific classes inside the core.
3. Do not use player names as persistent primary identifiers.
4. Do not add a feature directly to one platform without defining its core contract first, unless the feature is inherently platform-specific.
5. Every public API change must be documented.
6. Every Minecraft version adapter must be tested independently.
7. Existing stable behavior must not be silently changed during a version upgrade.
8. New effects must use the effect abstraction.
9. Storage must remain replaceable.
10. Documentation and implementation must stay synchronized.

## Build Philosophy

The project will be built in controlled milestones:

1. Blueprint and architecture.
2. Gradle multi-module foundation.
3. Core domain model.
4. API contracts.
5. Storage and player assignment.
6. Command abstraction.
7. Paper 1.21.11 adapter.
8. Fabric 1.21.11 adapter.
9. Rendering/nameplate integration.
10. Glitch effect, including white/colorful modes and frame generation.
11. Tests and regression checks.
12. GUI/editor.
13. Release packaging.
14. Future version adapters.

No platform implementation should begin before the corresponding core contract is stable.

## Build

The repository uses the checked-in Gradle Wrapper so local development and CI use the same Gradle version.

Linux/macOS:

`./gradlew build`

Windows:

`gradlew.bat build`

CI validates the official Gradle Wrapper JAR before running the build and verifies the Gradle 9.2.1 distribution checksum.

## Roadmap

See [docs/ROADMAP.md](docs/ROADMAP.md).

## Full Technical Blueprint

See [docs/BLUEPRINT.md](docs/BLUEPRINT.md).

## License

License will be finalized before the first public stable release.
