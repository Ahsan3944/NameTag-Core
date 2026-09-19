undefined

## 39. Chat Integration

The assigned NameTag must also be represented in player chat, similar to a server rank prefix.

### Required behavior

When a player has an active tag and sends a chat message, the chat renderer should compose:

```
[OWNER] UltraOP: Hello everyone!
```

or, depending on the configured chat format:

```
[OWNER] Hello everyone!
```

The exact placement of the player name and tag is configurable, but the active tag's visual properties must be reused consistently:
- Tag display name.
- Color.
- RGB/hex color.
- Gradient.
- Text formatting.
- Supported effects/animation behavior where chat rendering technically permits it.

### Chat Format Model

Chat formatting must be represented separately from the NameTag definition so the same tag can be rendered in different contexts without duplicating the tag itself.

Conceptually:

```
ChatFormat
├── tagPosition
├── playerNamePosition
├── messagePosition
├── separator
└── styling/visibility rules
```

Supported tag positions should include at minimum:
- Before player name.
- After player name.
- Before message.
- Hidden from chat while still visible above the player.

### Chat API

The platform adapter must expose a chat-rendering bridge. The core provides the active-tag information; Fabric/Paper handles the actual server chat event/component API for that Minecraft version.

The system must not assume that Fabric and Paper expose identical chat APIs.

### Permissions and Controls

Planned controls:
- `nametag.chat` — allow a player's active tag to appear in chat.
- Server configuration to enable/disable chat integration globally.
- Per-tag option to allow/disable chat display.
- Optional per-player override in future versions.

### Placeholder/Format Compatibility

Future integrations should be able to consume the active tag through a placeholder such as:

`%nametag%`

or an API equivalent.

This is an integration feature and must not require the core to depend directly on third-party placeholder/chat plugins.

### Priority Rule

If multiple tags are assigned, the chat renderer must use the same active-tag/priority resolution rules as the Nameplate renderer. It must not independently select a different tag.

### Fallback

If the platform/version cannot safely render a particular tag feature in chat, the renderer should gracefully fall back to supported plain text/color formatting rather than breaking the entire message.

### Testing Requirements

Chat integration is part of the v1.0 compatibility contract:
- Active tag appears in chat when enabled.
- Tag does not appear when disabled.
- Color/style is preserved where supported.
- Correct player identity is preserved.
- Normal chat delivery continues when no tag is assigned.
- Permissions are enforced.
- Fabric 1.21.11 and Paper 1.21.11 are tested independently.
- Reload does not duplicate chat formatting.
