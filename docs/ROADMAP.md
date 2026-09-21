# NameTag-Core Roadmap

## Phase 0 — Blueprint
- [x] Repository created.
- [x] Architecture defined.
- [x] Feature scope defined.
- [x] Version strategy defined.
- [x] Fabric/Paper separation defined.
- [x] Effect engine defined.
- [x] Storage abstraction defined.
- [x] API direction defined.
- [x] Testing strategy defined.

## Phase 1 — Build Foundation
- [x] Gradle wrapper.
- [x] Root build.
- [x] Multi-module configuration.
- [x] Java/toolchain configuration.
- [x] Formatting policy via repository-level `.editorconfig`.
- [x] Test framework.
- [x] Base package structure.

## Phase 2 — Core Domain
- [x] Tag model.
- [x] Tag ID/display name.
- [x] Color model.
- [x] RGB parser.
- [x] Random color strategy.
- [x] Gradient model.
- [x] Text style model.
- [x] Effect model.
- [x] Priority.
- [x] Validation.

## Phase 3 — API
- [x] Tag service.
- [x] Assignment service.
- [x] Query interfaces.
- [x] Events.
- [x] Effect registration.
- [x] Storage contracts.
- [x] Permission contracts.

## Phase 4 — Persistence
- [x] Tag repository.
- [x] Player assignment repository.
- [x] File provider.
- [x] Schema version.
- [x] Migration foundation.
- [x] Atomic save/recovery strategy.

## Phase 5 — Common Services
- [x] Command contracts.
- [x] Message/localization service.
- [x] Paper command source/player adapters.
- [x] Fabric command source/player adapters.
- [x] Common command handler wired to Paper/Fabric.
- [x] Configuration service.
- [x] Reload service.
- [x] Caching.

## Phase 6 — Paper 1.21.11
- [x] Chat integration.
- [x] Paper bootstrap.
- [x] Commands.
- [x] Permissions.
- [x] Player lifecycle.
- [x] Storage integration.
- [x] Nameplate rendering.
- [x] Text styles.
- [x] Effects.
- [x] Integration tests.

## Phase 7 — Fabric 1.21.11
- [x] Chat integration.
- [x] Fabric bootstrap.
- [x] Commands.
- [x] Permissions.
- [x] Player lifecycle.
- [x] Storage integration.
- [x] Nameplate rendering.
- [x] Text styles.
- [x] Effects.
- [x] Integration tests.

## Phase 8 — Glitch
- [x] Glitch configuration model.
- [x] White mode.
- [x] Colorful RGB mode.
- [x] Glitch processor/frame engine.
- [x] `/nametag glitch <tag> <white|colorful>` platform command wiring.
- [x] Nameplate frame scheduling.
- [x] Intensity validation.
- [x] Speed validation.
- [x] Core unit tests.
- [x] Platform-specific rendering tests.
- [x] Chat fallback integration.
- [x] Performance safeguards.

## Phase 9 — Command-First Management
- [x] Command-only administration.
- [x] Tag create/list/delete.
- [x] Tag edit for display name.
- [x] Preset/RGB/random/gradient color commands.
- [x] Bold/italic/plain style commands.
- [x] Enable/disable and chat-visibility commands.
- [x] Fabric and Paper command integration.
- [x] Command tab completion.
## Phase 10 — Stability
- [x] Full unit test suite.
- [x] Full integration suite.
- [x] Restart persistence test.
- [x] Reload test.
- [x] Permission regression test.
- [x] Cross-platform behavior comparison.
- [x] Performance/stress regression coverage for bounded cache behavior.
- [x] Documentation audit.

## Phase 11 — v1.0 Release Preparation
- [x] Release artifacts uploaded by CI.
- [x] Compatibility matrix.
- [x] Changelog.
- [x] Installation instructions.
- [x] Configuration reference.
- [x] API reference.
- [x] Core feature scope finalized.
- [ ] Final release-version promotion.
- [ ] Git tag.

> Future Minecraft-version adapters are separate implementation tracks and are not v1.0 release blockers.

## Phase 12 — Future Minecraft Versions
For each new version:
- [ ] Create/update version adapter.
- [ ] Compile.
- [ ] Fix API changes.
- [ ] Run regression suite.
- [ ] Test rendering.
- [ ] Test effects.
- [ ] Test persistence.
- [ ] Update compatibility documentation.
- [ ] Release platform artifacts.

## Deferred feature gates

These are intentionally separate from the 1.21.11 server-core baseline:

- **Multiple active/layered tags:** [x] Implemented with contextual multi-tag resolution and deterministic renderer/chat composition on Paper and Fabric.
- **Per-world tags:** [x] Implemented with world-aware resolution context and persistent tag metadata scopes.
- **Per-region tags:** [x] Implemented as persistent metadata-defined cuboid regions, without a hard dependency on a region plugin.
- **Future Minecraft versions:** each version requires a dedicated version adapter/mapping pass and regression run; it is not safe to bulk-change the existing 1.21.11 adapter.

## Backlog

Potential future additions:
- [x] Multiple active/layered tags.
- [x] Prefix/suffix.
- [x] Chat placeholders (tag ID, priority, prefix, suffix).
- [x] Temporary tag assignments.
- [x] Per-tag expiration with persistent timestamps.
- [x] Automatic role tags.
- [x] Rainbow animation.
- [x] Pulse/wave effects.
- [x] Dynamic tag metadata placeholders.
- [x] LuckPerms integration.
- [x] SQLite.
- [x] MySQL/MariaDB.
- [x] PostgreSQL.
- [x] Import/export.
- [x] Per-world tags.
- [x] Per-region tags.
- [x] Audit logs.
- [x] Tag packs.
- [x] Generic animated-effect contract and built-in rainbow/pulse/wave effects.
