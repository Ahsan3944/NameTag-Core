# Configuration

NameTag-Core uses a human-readable `configuration.yml` file with a schema version.

The configuration service is platform-independent. Paper and Fabric load it during bootstrap, while reload lifecycle behavior remains a separate milestone.

## File location

### Paper
The file is created under the plugin data directory:

`configuration.yml`

### Fabric
The file is created under:

`config/nametag-core/configuration.yml`

## Default configuration

A missing configuration file is created with these defaults:

```yaml
schemaVersion: 1
nameplateEnabled: true
chatEnabled: true
chatFormat: "[{tag}] {player}: {message}"
defaultTagPriority: 0
defaultTagEnabled: true
defaultTagChatEnabled: true
defaultGlitchIntensity: 45
defaultGlitchSpeedMs: 80
```

## Validation

The configuration service validates values before exposing the immutable runtime snapshot.

- `nameplateEnabled`: boolean.
- `chatEnabled`: boolean.
- `chatFormat`: non-blank string.
- `defaultTagPriority`: integer from `-1000000` to `1000000`.
- `defaultTagEnabled`: boolean.
- `defaultTagChatEnabled`: boolean.
- `defaultGlitchIntensity`: integer from `0` to `100`.
- `defaultGlitchSpeedMs`: integer from `30` to `2000`.

Invalid typed values are rejected rather than silently coerced.

## Persistence and recovery

The configuration uses the same atomic YAML file-store foundation as tag/assignment persistence:

- temporary file during save;
- atomic move when supported;
- backup file;
- recovery from the backup when the primary YAML cannot be loaded;
- schema-version validation.

The current configuration service loads one immutable snapshot at startup. It does not mutate live configuration while commands or renderers are running.

## Reload boundary

`/nametag reload` is intentionally not implemented by this milestone.

A future reload service will load and validate a new snapshot first, then replace the runtime snapshot only after successful validation. This prevents a malformed configuration from partially replacing live state.
