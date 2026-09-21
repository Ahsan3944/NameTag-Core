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
- `remove(UUID, TagId)`
- `clear(UUID)`
- `activeTag(UUID)`

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
- `reload`

## NameplateRenderer

`com.ultraop.nametag.api.NameplateRenderer`

The platform adapter owns Minecraft-specific nameplate lifecycle and rendering.

## ChatTagRenderer

`com.ultraop.nametag.api.ChatTagRenderer`

The platform adapter translates the active tag into the native chat component type.

## PermissionService

`com.ultraop.nametag.api.PermissionService`

The stable contract is:

`boolean has(UUID playerUuid, String permission)`

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
