# Fabric 1.21.11 Player Lifecycle

Fabric 1.21.11 nameplate lifecycle handling uses Fabric's ServerPlayerEvents.JOIN and ServerPlayerEvents.LEAVE events.

## Behavior

- On JOIN, the renderer immediately resolves the player's persisted active tag and restores scoreboard-team membership.
- On LEAVE, the renderer removes the player from the runtime scoreboard team.
- Persistent tag assignments are not modified by lifecycle events.
- The periodic server-tick renderer remains the steady-state reconciliation path.
- The JOIN hook avoids waiting for the next periodic tick after reconnect.
- The LEAVE hook cleans runtime membership before the player is removed from the server.

Fabric documents ServerPlayerEvents.JOIN as running after the player has fully loaded into the world, and LEAVE before the player is saved and removed. This matches the lifecycle boundary required by the renderer.
