# Compatibility Matrix

## Current baseline

| Component | Supported baseline |
|---|---|
| Minecraft | 1.21.11 |
| Java | 21 |
| Fabric Loader | 0.18.1 |
| Fabric API | 0.141.3+1.21.11 |
| Fabric Loom | 1.14.10 |
| Paper API | 1.21.11-R0.1-SNAPSHOT |

The build toolchain targets Java 21. Minecraft 1.21.11 requires Java 21 or newer on Fabric, and Paper documents Java 21 for Minecraft 1.20 through 1.21.11. The project intentionally keeps the current pinned versions in Gradle rather than silently changing dependencies during a stability pass.

## Platform coverage

| Area | Fabric 1.21.11 | Paper 1.21.11 |
|---|---:|---:|
| Commands | Yes | Yes |
| Permissions | Yes | Yes |
| Persistence | Yes | Yes |
| Active-tag resolution | Yes | Yes |
| Nameplate | Yes | Yes |
| Chat | Yes | Yes |
| Glitch effect | Yes | Yes |
| Player lifecycle | Yes | Yes |
| Automated integration coverage | Server GameTests | Integration tests |
| Rendering regression coverage | Chat + lifecycle/nameplate paths | Chat + lifecycle/nameplate paths |

## Future-version policy

Minecraft 26.1+ is a separate porting target. Fabric documents that 26.1 is the first unobfuscated release and that 1.21.11-or-earlier mods require at least recompilation for 26.1. NameTag-Core therefore does not claim 26.1 compatibility until a dedicated version adapter is implemented and the regression suite passes.

Sources: Fabric's 1.21.11 release notes and current Java requirements; Paper's current getting-started requirements.
