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

## EffectProvider

`com.ultraop.nametag.api.EffectProvider`

This is the extension contract reserved for future effect registration. A general-purpose effect registry is intentionally not declared complete while `TagEffect` remains a sealed core model containing the built-in effect variants.
