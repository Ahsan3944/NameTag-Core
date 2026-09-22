# Configuration

NameTag-Core uses a human-readable `configuration.yml` file with a schema version.

The configuration service is platform-independent. Paper and Fabric load it during bootstrap, and the reload lifecycle uses a separate API contract.

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
chatFormat: "[{item}{tag}] {player}: {message}"
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

### Chat placeholders

`chatFormat` supports `{item}`, `{tag}`, `{tags}`, `{tag_id}`, `{tag_priority}`, `{tag_prefix}`, `{tag_suffix}`, `{tag_meta:key}`, `{player}` and `{message}`. Unknown placeholders remain literal text. `{item}` renders the configured Minecraft item sprite before the tag text; if an existing configuration omits `{item}`, the platform chat renderers inject it before `{tag}`/`{tags}` automatically. The Paper adapter applies the active tag's supported color and text style to the `{tag}` component. See [CHAT.md](CHAT.md) for platform rendering details.

## Persistence and recovery

The configuration uses the same atomic YAML file-store foundation as tag/assignment persistence:

- temporary file during save;
- atomic move when supported;
- backup file;
- recovery from the backup when the primary YAML cannot be loaded;
- schema-version validation.

The current configuration service exposes one immutable snapshot at runtime. Reload builds and validates a complete replacement snapshot before atomically publishing it.

## Reload

`/nametag reload` is available through the common command layer and requires `nametag.reload` (or `nametag.admin`).

Reload behavior is deliberately fail-safe:

1. Read the current YAML storage.
2. Validate schema and all typed configuration values.
3. Build a complete immutable replacement snapshot.
4. Publish the replacement only after every validation step succeeds.
5. If loading or validation fails, keep the previous runtime snapshot unchanged.

If the primary YAML is unreadable, the existing file-store recovery path may load the backup. If the configuration is semantically invalid, reload reports the failure and does not replace the active snapshot.

## Database storage

The platform creates `storage.yml` beside the existing tag and assignment files.

Default:

```yaml
schemaVersion: 1
type: yaml
jdbcUrl: ""
username: ""
password: ""
migrateYaml: true
```

Supported `type` values are `yaml`, `sqlite`, `mysql`, `mariadb`, and `postgresql`.

- `yaml`: keeps the existing human-readable `tags.yml` and `assignments.yml` provider.
- `sqlite`: uses `jdbc:sqlite:<path>`; when `jdbcUrl` is blank, the database is `nametag.db` in the platform data directory.
- `mysql`, `mariadb`, `postgresql`: require a JDBC URL and normally a database username/password.
- `migrateYaml`: when enabled, a new empty database imports the existing YAML tags and assignments once. A database that already contains data is never overwritten by this migration, and the migration is recorded as complete so the YAML files are not re-imported on later restarts.

The JDBC schema currently uses version 1. Startup rejects a newer database schema rather than attempting an unsafe downgrade. Repository writes are transactional and each operation closes its JDBC connection after completion.

Paper resolves the JDBC drivers through the plugin's declared libraries. Fabric bundles the supported drivers in the mod JAR.
