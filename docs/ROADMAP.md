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

## Phase 9 — GUI
- [ ] Tag browser.
- [ ] Create editor.
- [ ] Edit editor.
- [ ] Color selector.
- [ ] RGB input.
- [ ] Gradient editor.
- [ ] Style controls.
- [ ] Effect controls.
- [ ] Preview.
- [ ] Save/cancel.

> GUI remains intentionally deferred from the server-only 1.21.11 baseline. A correct implementation requires a client entrypoint, client-side screens/widgets, server-authoritative mutations, and a versioned client/server networking contract. Fabric's 1.21.11 API provides client screen and networking APIs, but introducing only part of this stack would create an incomplete and unsafe feature boundary.

## Phase 10 — Stability
- [x] Full unit test suite.
- [x] Full integration suite.
- [x] Restart persistence test.
- [x] Reload test.
- [x] Permission regression test.
- [x] Cross-platform behavior comparison.
- [x] Performance/stress regression coverage for bounded cache behavior.
- [x] Documentation audit.

## Phase 11 — v1.0 Release
- [x] Release artifacts published by CI.
- [x] Compatibility matrix.
- [x] Changelog.
- [x] Installation instructions.
- [x] Configuration reference.
- [x] API reference.
- [ ] Git tag.

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

## Backlog

Potential future additions:
- Multiple active/layered tags.
- [x] Prefix/suffix.
- [x] Chat placeholders (tag ID, priority, prefix, suffix).
- [x] Temporary tag assignments.
- [x] Per-tag expiration with persistent timestamps.
- Automatic role tags.
- Rainbow animation.
- Pulse/wave effects.
- Placeholder support.
- LuckPerms integration.
- SQLite.
- MySQL/MariaDB.
- PostgreSQL.
- Import/export.
- Per-world tags.
- Per-region tags.
- Web management.
- [x] Audit logs.
- Tag packs.
