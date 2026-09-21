# API Reference

## TagService

`com.ultraop.nametag.api.TagService`

Core operations:

- `create(Tag)`
- `update(Tag)`
- `delete(TagId)`
- `find(TagId)`
- `list()`
- `assign(UUID, TagId)`
- `assignUntil(UUID, TagId, Instant)`
- `setActive(UUID, TagId)`
- Tag-pack import/export through the common codec
- `remove(UUID, TagId)`
- Generic `setEffect(TagId, TagEffect)` helper preserving all non-effect properties.
- `clear(UUID)`
- `activeTag(UUID)`
- `activeTags(UUID, TagResolutionContext)` for contextual multi-tag resolution.

`activeTag(UUID)` remains the backward-compatible single-tag API. `activeTags(UUID, context)` resolves all matching assigned or automatic-role layers for the supplied world/position context. The explicit active assignment is placed first when it matches the context; remaining layers are ordered deterministically by priority and tag ID.

Built-in default operations also expose glitch configuration through `setGlitch(...)` and `clearGlitch(...)`.

## NameTagCommandHandler

`com.ultraop.nametag.api.NameTagCommandHandler`

The common command contract executes platform-neutral command arguments and exposes platform-neutral suggestions.

Implemented command families:

- `create`
- `list`
- `give`
- `set`
- `remove`
- `clear`
- `delete`
- `glitch`
- `effect`
- `reload`
- `role`
- `scope`
- `export`
- `import`

The `scope` command manages persistent world and cuboid-region metadata:
- `/nametag scope <tag> clear`
- `/nametag scope <tag> world <world>`
- `/nametag scope <tag> region <name> <world> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>`

## Automatic role tags

`/nametag role <tag> <permission|clear>` manages the `auto-permission` metadata used by automatic role resolution. The common service keeps explicit assignments authoritative and falls back to the automatic role resolver only when no usable explicit tag is available.

## NameplateRenderer

`com.ultraop.nametag.api.NameplateRenderer`

The platform adapter owns Minecraft-specific nameplate lifecycle and rendering. Paper and Fabric 1.21.11 adapters resolve contextual layers and compose multiple tags while preserving the legacy single-tag rendering contract.

## ChatTagRenderer

`com.ultraop.nametag.api.ChatTagRenderer`

The platform adapter translates resolved tags into the native chat component type. The `{tag}` placeholder remains first-layer/backward-compatible; `{tags}` renders all resolved chat-enabled layers separated by a single space.

## PermissionService

`com.ultraop.nametag.api.PermissionService`

The stable contract is:

`boolean has(UUID playerUuid, String permission)`

Automatic role resolution uses tag metadata key `auto-permission`. When a player has no usable explicit assigned tag, enabled tags with a matching permission are considered and resolved together in deterministic order. Automatic results are not persisted.

The command layer uses permission nodes such as:

- `nametag.use`
- `nametag.create`
- `nametag.edit`
- `nametag.delete`
- `nametag.give`
- `nametag.remove`
- `nametag.reload`
- `nametag.admin`

## Domain events

`com.ultraop.nametag.api.TagEventBus` exposes thread-safe domain events from `TagService.events()`.

The event model currently includes:

- `TagEvent.Created`
- `TagEvent.Updated`
- `TagEvent.Deleted`
- `TagEvent.AssignmentChanged`

Events are emitted only after the corresponding persistence mutation succeeds. No-op assignment/clear operations do not emit redundant events.

`com.ultraop.nametag.common.FileTagAuditLogger` can subscribe to the event bus and persist append-only audit lines for tag creation/update/deletion and assignment changes.

## EffectProvider

`com.ultraop.nametag.api.EffectProvider`

An effect provider supplies platform/version-specific rendering behavior for an effect identifier.

## EffectRegistry

`com.ultraop.nametag.api.EffectRegistry`

The common `DefaultEffectRegistry` provides:

- unique effect IDs
- registration/unregistration
- lookup
- immutable list snapshots
- concurrent access

The registry is deliberately separate from `TagEffect`: the tag model remains a small immutable data definition while providers own runtime rendering behavior.

## Contextual resolution

`com.ultraop.nametag.api.TagResolutionContext` contains:
- world identifier
- block X/Y/Z

World scopes use exact world identifiers. Region scopes use inclusive normalized cuboid bounds stored in tag metadata under `region.minX`, `region.minY`, `region.minZ`, `region.maxX`, `region.maxY`, and `region.maxZ`. Malformed region metadata is ignored during resolution.
