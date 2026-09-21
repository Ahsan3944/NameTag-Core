# Paper Integration Tests

Paper 1.21.11 platform integration tests use MockBukkit 4.110.0 from Maven Central.

## Coverage

- Plugin bootstrap and enable state.
- `/nametag create` and `/nametag give` through the real Paper command entry point.
- Scoreboard team creation and assigned NameTag rendering.
- Player quit cleanup of runtime scoreboard membership.
- Player reconnect restoration of the persisted UUID assignment.
- Immediate join lifecycle rendering without waiting for the periodic renderer tick.

## Boundary

The tests live in `platform:paper`, while the version-specific renderer remains in `versions:paper-1.21.11`. No MockBukkit dependency is exposed to production configurations.

These are server-platform smoke/integration tests, not a replacement for a real Paper server startup test.