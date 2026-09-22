# Chat Integration

## Paper 1.21.11

Paper chat integration uses the modern `AsyncChatEvent` and `ChatRenderer` API. The renderer is viewer-unaware because NameTag-Core produces the same output for every recipient.

The integration is enabled only when:

1. global `chatEnabled` is true;
2. the sender has `nametag.chat`;
3. the sender has an enabled active tag;
4. the active tag has chat enabled.

If any condition is false, NameTag-Core leaves the existing chat renderer untouched.

## Format

The configured `chatFormat` supports these placeholders:

- `{item}` — the active tag item icon, rendered from Minecraft 1.21.11's items atlas; empty when no item is configured.
- `{tag}` — active tag display name; it may be empty for an icon-only tag.
- `{player}` — the normal Paper player display-name component.
- `{message}` — the original chat message component.
- `{tag_meta:key}` — a tag metadata value; unknown keys remain literal.

Unknown placeholders remain literal text.

Default:

```yaml
chatFormat: "[{item}{tag}] {player}: {message}"
```

Example:

```text
[<item icon>OWNER] UltraOP: Hello everyone!
```

## Styling

The Paper renderer applies the active tag's supported color and text decorations to the `{tag}` component.

- RGB colors are rendered directly.
- Preset colors use the Paper text-color mapping.
- Gradient colors currently use the gradient start color as a static chat representation.
- Random colors fall back to the normal component color.
- Bold, italic, underline, strikethrough and obfuscated styles are preserved for the tag.
- The message itself is not recolored by the tag.

## Effects

Animated nameplate effects are not replayed every chat message. The built-in Glitch effect therefore falls back to the stable tag display name in chat. This avoids per-message animation state, duplicate listeners and unsafe manipulation of signed chat content.

## Reload

The renderer reads the current immutable configuration snapshot when a message is rendered. A successful `/nametag reload` therefore changes chat formatting without restarting the Paper plugin.

## Lifecycle

The Paper adapter registers the chat listener during `start()` and unregisters it during `stop()`. No duplicate listener is intentionally retained across plugin disable/enable cycles.

## Platform boundary

The common API remains platform-neutral. Paper's Adventure `Component`, `AsyncChatEvent` and `ChatRenderer` types are isolated inside the Paper 1.21.11 version adapter.

## Item + rank composition

A tag can independently contain:

- text only: `[OWNER] Player: Message`
- item/icon only: `[<item icon>] Player: Message`
- item/icon + text: `[<item icon>OWNER] Player: Message`

The item is always emitted before the tag text, and the player name is emitted exactly once by the platform chat renderer. Existing configurations that still use `[{tag}] {player}: {message}` are automatically normalized at render time so the item is inserted before the tag when one is configured.

Use `/nametag tag edit <tag> name none` to make a tag icon-only. The item itself is configured with `/nametag display item <tag> set <item>`.
