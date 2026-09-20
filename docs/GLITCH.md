# NameTag-Core — Glitch NameTag Specification

## Purpose

The built-in **Glitch NameTag** effect adapts the earlier GlitchIdentity visual concept for persistent NameTags.

The important difference is that a NameTag is an identity label, so the glitch engine **does not replace the complete tag with a random 5–7 character string**. It preserves the original tag length and repeatedly corrupts individual characters.

Example source:

`[OWNER]`

Possible frames:

`[0WNER]` → `[0W#ER]` → `[O#N3R]` → `[OWNER]`

The exact frame is generated from the configured intensity and animation frame.

## Modes

A glitch tag has exactly one visual mode:

### White

`/nametag glitch <tag> white`

- Glitched and normal glyphs are rendered white by the glitch frame.
- The tag remains animated.
- Character corruption is driven by the configured intensity.

### Colorful

`/nametag glitch <tag> colorful`

- Each glyph receives a fast-changing neon-style RGB color.
- Character corruption continues independently from color changes.
- The output is designed to resemble the earlier colorful hacker/glitch identity effect.

The mode is optional at creation time but, when the glitch effect is enabled, it must be either `white` or `colorful`.

## Configuration

Default values:

| Setting | Default | Allowed |
|---|---:|---:|
| mode | white | white / colorful |
| intensity | 45 | 0–100 |
| speed-ms | 80 | 30–2000 |

Speed is an animation cadence. The platform adapter owns scheduling; the core engine only generates frames.

## Command

Canonical command:

`/nametag glitch <tag> <white|colorful>`

The command changes only the tag's effect. Existing display name, color, gradient, formatting, priority, enabled state, chat visibility and metadata remain unchanged.

Future administrative options may expose intensity and speed, but the two public modes remain the required v1 contract.

## Rendering contract

Core produces a platform-independent `GlitchFrame`:

- final text for the frame;
- one glyph entry per source character;
- an RGB value per glyph.

Fabric and Paper adapters translate the frame into their native text/nameplate representation.

Core must never import Minecraft, Fabric, Bukkit or Paper classes.

## Animation behavior

- Frames are deterministic for a `(seed, frameIndex)` pair.
- Consecutive frames are allowed to change rapidly.
- Whitespace is never replaced.
- Source length is preserved.
- The engine does not allocate an unbounded history.
- Only players/tags with an active animated effect should be scheduled.
- Platform adapters must avoid blindly running expensive work every server tick.

## Chat behavior

Chat should reuse the same active tag and effect definition.

If a platform chat API cannot safely reproduce animated per-glyph glitch frames, it should use a graceful static representation rather than corrupting signed chat or registering duplicate listeners. The Paper 1.21.11 adapter currently uses the stable tag display name in chat.

## Relationship to GlitchIdentity

The earlier GlitchIdentity work established the visual language:

- rapidly changing corruption;
- character substitution;
- colorful per-character output;
- configurable speed/intensity;
- server/platform separation.

NameTag-Core reuses those principles while changing the output contract for NameTags: the source tag remains recognizable and its length remains stable.