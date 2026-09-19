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
- [ ] Gradle wrapper and root build.
- [ ] Multi-module configuration.
- [ ] Java/toolchain configuration.
- [ ] Checkstyle/formatting policy if required.
- [ ] Test framework.
- [ ] Base package structure.

## Phase 2 — Core Domain
- [ ] Tag model.
- [ ] Tag ID/display name.
- [ ] Color model.
- [ ] RGB parser.
- [ ] Random color strategy.
- [ ] Gradient model.
- [ ] Text style model.
- [ ] Effect model.
- [ ] Priority.
- [ ] Validation.

## Phase 3 — API
- [ ] Tag service.
- [ ] Assignment service.
- [ ] Query interfaces.
- [ ] Events.
- [ ] Effect registration.
- [ ] Storage contracts.
- [ ] Permission contracts.

## Phase 4 — Persistence
- [ ] Tag repository.
- [ ] Player assignment repository.
- [ ] File provider.
- [ ] Schema version.
- [ ] Migration foundation.
- [ ] Atomic save/recovery strategy.

## Phase 5 — Common Services
- [ ] Command contracts.
- [ ] Message/localization service.
- [ ] Configuration service.
- [ ] Reload service.
- [ ] Caching.

## Phase 6 — Paper 1.21.11
- [ ] Paper bootstrap.
- [ ] Commands.
- [ ] Permissions.
- [ ] Player lifecycle.
- [ ] Storage integration.
- [ ] Nameplate rendering.
- [ ] Text styles.
- [ ] Effects.
- [ ] Integration tests.

## Phase 7 — Fabric 1.21.11
- [ ] Fabric bootstrap.
- [ ] Commands.
- [ ] Permissions.
- [ ] Player lifecycle.
- [ ] Storage integration.
- [ ] Nameplate rendering.
- [ ] Text styles.
- [ ] Effects.
- [ ] Integration tests.

## Phase 8 — Glitch
- [ ] Glitch configuration.
- [ ] Glitch processor.
- [ ] Scheduling.
- [ ] Intensity.
- [ ] Speed.
- [ ] Performance safeguards.
- [ ] Platform-specific rendering tests.

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
- [ ] Restart persistence test.
- [ ] Reload test.
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
