# Caching

NameTag-Core caches only resolved NameTag results, not persistent tag or assignment data.

## Cache layers

The common service currently uses two bounded LRU caches:

- Legacy active-tag cache (ActiveTagCache) — 1024 player UUID entries by default.
- Contextual multi-tag cache (ContextualTagCache) — 2048 (player UUID, world, X, Y, Z) entries by default.

Both caches are implementation details of DefaultTagService and are never the source of truth.

## Legacy active-tag cache

The legacy activeTag(UUID) API uses the 1024-entry ActiveTagCache.
- Empty results are cacheable.
- Entries are evicted with LRU semantics.
- Mutations that can change resolution invalidate the affected player or clear the cache.

Automatic permission-derived roles are intentionally not cached when they are resolved without an explicit assignment, because permission providers can change independently of NameTag-Core mutations.

## Contextual multi-tag cache

The contextual activeTags(UUID, TagResolutionContext) API uses the 2048-entry ContextualTagCache.

The key contains:
- player UUID;
- world identifier;
- block X;
- block Y;
- block Z.

This keeps different contextual positions independent while preventing unbounded growth.

Contextual results are cached only when they come from explicit, non-expiring assignments and contain at least one resolved tag. Permission-derived automatic-role results and expiring assignments are deliberately resolved again instead of being cached.

## Invalidation

The service invalidates cached results whenever a mutation can change active-tag resolution:
- player assignment changes;
- active tag changes;
- player tags are cleared;
- a tag is deleted;
- a tag is updated;
- cache state is cleared during relevant service mutations.

A tag update clears both caches because changing priority, enabled state, effect, metadata or scope can affect resolution for many players and contexts.

## Safety

Both bounded LRU implementations synchronize structural access because LinkedHashMap is not thread-safe for concurrent mutation.

Persistent repositories remain the source of truth. Caches are acceleration layers only and are never written to disk.

## Current limitation

Cache capacities are implementation defaults rather than user-facing configuration values. Exposing cache tuning through configuration will be considered only after the behavior is stable and benchmarked.