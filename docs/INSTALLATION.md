# Installation

## Build

The repository includes a pinned Gradle Wrapper, so a system-wide Gradle installation is not required.

Linux/macOS:
`./gradlew build --no-daemon`

Windows:
`gradlew.bat build --no-daemon`

## Paper 1.21.11

1. Build the project with the repository's Gradle Wrapper.
2. Use the generated Paper platform artifact for the 1.21.11 server.
3. Place the plugin JAR in the server's `plugins/` directory.
4. Start Paper 1.21.11 with Java 21.
5. The plugin creates its configuration and persistence files under the plugin data directory.
6. Configure `configuration.yml` if required.
7. Use `/nametag list` to verify command registration.

## Fabric 1.21.11

1. Build the project with the repository's Gradle Wrapper.
2. Use the generated Fabric 1.21.11 platform artifact.
3. Install it on a Fabric 1.21.11 dedicated server together with the required Fabric API dependency.
4. Start the server with Java 21.
5. Fabric stores NameTag-Core configuration and persistence under `config/nametag-core/`.
6. Use `/nametag list` to verify command registration.
7. **Native item nameplate icons:** Fabric 1.21.11 item icons are inserted directly into the scoreboard/nameplate `Text` as a vanilla atlas text object. The icon therefore follows the same NameTag text layout and is also available anywhere the same team prefix is rendered, including the TAB player list. No separate client-side NameTag-Core installation is required for this native atlas icon path.

## Configuration

See [CONFIGURATION.md](CONFIGURATION.md) for defaults, validation, file locations, atomic persistence and reload behavior.

## Important compatibility rule

Do not install a 1.21.11 build into a different Minecraft major/minor target. Future Minecraft versions require their own adapter and regression pass.
