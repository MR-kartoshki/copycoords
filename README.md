# CopyCoords - Stonecutter Multi-Version Fabric Mod

A Stonecutter-based multi-Minecraft-version Fabric mod that provides coordinate utilities.

## Project Structure

```
stonecutter-migrated/
├── build.gradle                      # Main Stonecutter + Loom build file
├── settings.gradle                   # Project settings
├── gradle.properties                 # Version and dependency config
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties  # Gradle version specifications
├── src/
│   └── main/
│       ├── java/com/example/copycoords/
│       │   ├── CopyCoords.java                      # Main mod class
│       │   ├── CopyCoordsBind.java                  # Keybinding handler
│       │   ├── CopyCoordsConfig.java                # Configuration manager
│       │   └── CopyCoordsModMenuIntegration.java    # Mod Menu integration
│       └── resources/
│           ├── fabric.mod.json                      # Fabric mod metadata
│           ├── pack.mcmeta                          # Resource pack metadata
│           └── assets/copycoords/lang/
│               └── en_us.json                       # English language file
└── README.md                         # This file
```

## Supported Minecraft Versions

This project builds for Minecraft versions **1.21.1** and **1.21.11**.

- Default/active version for development: **1.21.11**
- Change the active version via environment variable: `MC_VERSION=1.21.1 ./gradlew.bat build`

## Build Instructions

### Prerequisites

- Java 21 or later (for Minecraft 1.21.x)
- Gradle 9.3.1 or later (or use the included Gradle wrapper)

### Build for Active Version (1.21.11)

```bash
./gradlew.bat build
```

This creates a single JAR file for version 1.21.11 in `build/libs/`.

### Build for Specific Version

```bash
./gradlew.bat build -PMC_VERSION=1.21.4
```

Replace `1.21.1` with any supported version.

### Build for All Supported Versions

```bash
./gradlew.bat buildAllVersions
```

This creates JAR files for all supported Minecraft versions.

### Run Client for Development (1.21.11)

```bash
./gradlew.bat runClient
```

This launches the Minecraft client with the mod loaded for development/testing.

### Run Tests

```bash
./gradlew.bat test
```

Test for all versions:
```bash
./gradlew.bat testAllVersions
```

## Version Configuration

The build system supports the following Minecraft versions with auto-configured dependencies:

| MC Version | Fabric Loader | Fabric API       | Cloth Config        | Mod Menu       |
|-----------|---------------|------------------|---------------------|----------------|
| 1.21.1    | 0.18.4        | 0.116.8+1.21.1   | 15.0.140            | 11.0.3         |
| 1.21.11   | 0.18.4        | 0.141.3+1.21.11  | 11.0.99             | 17.0.0-beta.2  |

## Features

- **Get Coordinates**: Use `/copycoords` command or press **C** (default keybind) to get your current coordinates
- **Convert Coordinates**: Use `/convertcoords <goal> [x y z]` to convert between Overworld and Nether coordinates
- **Send Coordinates**: Use `/msgcoords <player> [goal]` to send coordinates to another player
- **Clipboard Integration**: Automatically copy coordinates to your system clipboard (Windows only, uses `clip.exe`)
- **Mod Menu Integration**: Configure clipboard options through Mod Menu
- **Multi-Version Support**: One codebase, multiple compiled versions

## Configuration

The mod saves its configuration to `config/copycoords.json`:

```json
{
  "copyToClipboard": true,
  "copyConvertedToClipboard": true
}
```

You can also configure it via the Mod Menu in-game.

## Mod Behavior

The mod behavior is **identical across all supported versions**. The Minecraft API used by this mod is stable across 1.21.1–1.21.11, so no version-specific code is required.

- Commands and keybindings work the same
- Clipboard operations use Windows `clip.exe` (version-agnostic)
- Chat integration is unchanged
- Configuration format is unchanged

## Development

### Code Layout

- **CopyCoords.java**: Main entry point; registers commands and initializes the mod
- **CopyCoordsBind.java**: Handles keybinding registration and execution
- **CopyCoordsConfig.java**: JSON-based configuration management
- **CopyCoordsModMenuIntegration.java**: Mod Menu screen factory

### Adding a New Version

To add support for a new Minecraft version (e.g., 1.21.12):

1. Update the `gameVersions` map in `build.gradle` with the appropriate dependency versions
2. Run `./gradlew.bat build -PMC_VERSION=1.21.12`
3. Add the new version to the README table above

### Common Tasks

**Update all dependencies to use the new Fabric Loader version:**
```gradle
// Edit build.gradle and update the gameVersions map
```

**Clean build artifacts:**
```bash
./gradlew.bat clean
```

**Generate IDE configuration (IntelliJ/Eclipse):**
```bash
./gradlew.bat genSources // Already run by default
```

## License

MIT License - See the original repository for details.

## Credits

Original mod by freddy._.fazbear (Discord) and MR-Kartoshki (GitHub)  
Multi-version Stonecutter adaptation following the same mod behavior.

---

For issues, contributions, or questions, visit the [original repository](https://github.com/MR-kartoshki/copycoords).
