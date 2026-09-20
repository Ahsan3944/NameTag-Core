# Caching

NameTag-Core caches only the resolved active NameTag result, not the persistent tag or assignment data.

## Scope

The cache is an implementation detail of the common DefaultTagService:

- bounded to 1024 player UUID entries by default;
- LRU eviction prevents unbounded growth;
- successful resolutions and empty resolutions are both cached;
- cached values are immutable domain records.

The cache is deliberately not part of the public API.

## Invalidation

The service invalidates cached results whenever a mutation can change active-tag resolution:

- player assignment changes;
- active tag changes;
- player tags are cleared;
- a tag is deleted;
- a tag is updated.

A tag update clears the active-tag cache because changing priority or enabled state can affect fallback resolution for players whose current cached result is a different tag.

## Safety

The cache is synchronized because LinkedHashMap is not thread-safe for concurrent structural access. The bounded LRU implementation uses access-order so recently used player entries remain resident.

The persistent repositories remain the source of truth. The cache is only an acceleration layer and is never written to disk.

## Current limitation

Cache capacity is currently an implementation default rather than a user-facing configuration value. Exposing cache tuning through configuration will be considered only after the behavior is stable and benchmarked.
