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
- [ ] Gradle wrapper.
- [x] Root build.
- [x] Multi-module configuration.
- [x] Java/toolchain configuration.
- [ ] Checkstyle/formatting policy if required.
- [x] Test framework.
- [x] Base package structure.

## Phase 2 — Core Domain
- [x] Tag model.
- [x] Tag ID/display name.
- [x] Color model.
- [x] RGB parser.
- [ ] Random color strategy.
- [x] Gradient model.
- [x] Text style model.
- [x] Effect model.
- [x] Priority.
- [x] Validation.

## Phase 3 — API
- [x] Tag service.
- [x] Assignment service.
- [x] Query interfaces.
- [ ] Events.
- [ ] Effect registration.
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
- [ ] Integration tests.

## Phase 7 — Fabric 1.21.11
- [ ] Chat integration.
- [x] Fabric bootstrap.
- [x] Commands.
- [x] Permissions.
- [ ] Player lifecycle.
- [x] Storage integration.
- [x] Nameplate rendering.
- [x] Text styles.
- [x] Effects.
- [ ] Integration tests.

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
- [ ] Platform-specific rendering tests.
- [ ] Chat fallback integration.
- [ ] Performance safeguards.

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

## Phase 10 — Stability
- [ ] Full unit test suite.
- [ ] Full integration suite.
- [x] Restart persistence test.
- [x] Reload test.
- [ ] Permission regression test.
- [ ] Cross-platform behavior comparison.
- [ ] Performance test.
- [ ] Documentation audit.

## Phase 11 — v1.0 Release
- [ ] Release artifacts.
- [ ] Compatibility matrix.
- [ ] Changelog.
- [ ] Installation instructions.
- [ ] Configuration reference.
- [ ] API reference.
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
- Prefix/suffix.
- Temporary tags.
- Expiration.
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
- Audit logs.
- Tag packs.
