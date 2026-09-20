# Paper Player Lifecycle

Paper 1.21.11 now handles player join/quit transitions explicitly for nameplate state.

## Join

`PlayerJoinEvent` triggers an immediate nameplate refresh after the join event reaches `MONITOR` priority.

This avoids waiting for the periodic renderer tick before applying an existing active tag.

## Quit

`PlayerQuitEvent` removes the player's scoreboard-team membership immediately.

The periodic renderer remains responsible for steady-state reconciliation and also removes stale UUID entries.

## Lifecycle safety

The listener is registered by `Paper2111Adapter.start()` and unregistered by `stop()`.

The renderer's scheduled task is also cancelled during adapter shutdown, so the lifecycle listener cannot outlive the renderer.

## Scope

This lifecycle layer does not change persistent assignments. Player tags remain stored by UUID and are available when the player reconnects.

The lifecycle hooks only synchronize runtime nameplate state with the server's online-player lifecycle.