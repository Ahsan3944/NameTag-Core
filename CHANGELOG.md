# Changelog

## Unreleased

### Stability hardening

- Added deterministic RGB resolution for random and gradient tag colors.
- Added per-glyph gradient/random rendering coverage for Fabric and Paper chat.
- Added per-glyph gradient/random rendering for Fabric and Paper nameplates.
- Added Fabric server GameTests for command bootstrap and player lifecycle.
- Added a GameTest proving a pre-existing scoreboard team is not hijacked by the nameplate renderer.
- Changed runtime nameplate team identity from hash-only names to tag-ID-derived names to avoid Java String hash collisions.
- Added collision-safe team creation so pre-existing scoreboard teams are never reused or unregistered by NameTag-Core.
- Added static nameplate visual caching so unchanged static tags are not rewritten to the scoreboard every server tick.
- Kept animated glitch frames cadence-limited by the configured effect speed.
- Added compatibility, installation and API reference documentation.
- Added thread-safe domain events for tag and player-assignment mutations.
- Added a concurrent EffectRegistry with duplicate-ID validation and immutable snapshots.
