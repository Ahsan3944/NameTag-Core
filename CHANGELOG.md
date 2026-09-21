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
- Added a 50,000-entry bounded-cache stress regression test to guard against unbounded active-tag cache growth.
- CI now publishes the verified module JARs as a retained build artifact after a successful build.
- Added the official Gradle 9.2.1 Wrapper and distribution SHA-256 verification for reproducible CI/local builds.
- Added manual CI workflow dispatch and read-only repository permissions.
- Updated installation instructions to use the checked-in Gradle Wrapper on Linux/macOS and Windows.
- Added a repository-level `.editorconfig` formatting policy without introducing formatter-driven source churn.
- Added CI architecture-boundary validation to prevent Fabric, Minecraft, Bukkit, or Paper imports from leaking into core/API/common.
- Added CI artifact-presence validation for every expected module before artifacts are uploaded.
- Added platform-neutral prefix/suffix presentation metadata with bounded validation.
- Added `{tag_id}`, `{tag_priority}`, `{tag_prefix}`, and `{tag_suffix}` chat placeholders across Fabric, Paper, and the common renderer.
- Added core and platform regression tests for the new presentation and placeholder behavior.
- Added per-tag temporary assignments with persistent expiration timestamps and duration-aware `/nametag give` support.
- Added append-only domain audit logging for tag and assignment mutations.
- Added dynamic `{tag_meta:key}` chat placeholder support across the common, Fabric, and Paper renderers.
- Added safe YAML tag-pack import/export with sandboxed filenames.
- Added a generic animated-effect contract with built-in Rainbow, Pulse and Wave nameplate effects.
- Added `/nametag effect <tag> <none|rainbow|pulse|wave>` with validated animation cadence/intensity defaults.
- Added deterministic core regression coverage and wired animated effects into Fabric/Paper 1.21.11 nameplate rendering.
- Kept the GUI milestone explicitly deferred until the client entrypoint, server-authoritative networking contract, and version-specific client implementation can be delivered together.
