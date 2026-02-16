# CopyCoords

A simple Fabric mod for Minecraft 1.21.11 that adds a `/copycoords` command to quickly display and copy your current coordinates to the system clipboard.

## Features

- **Command**: Type `/copycoords` to print your current coordinates in chat
- **Auto-Copy**: Optionally automatically copy coordinates to your system clipboard when using the command
- **Keybind**: Press **C** (default) to instantly copy your coordinates without typing a command
- **Configurable**: Toggle clipboard copying on/off via Mod Menu
- **Localized**: Full translation support for multiple languages

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.11
2. Download and install the required dependencies:
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [Cloth Config](https://modrinth.com/mod/cloth-config)
   - [Mod Menu](https://modrinth.com/mod/modmenu)
3. Place `copycoords-x.x.x.jar` in your `.minecraft/mods` folder

## Usage

- **Command**: `/copycoords` - Displays coordinates in chat and optionally copies them
- **Keybind**: Press **C** - Instantly copies coordinates to clipboard with a chat notification
- **Configuration**: Open Mod Menu and click CopyCoords → General → Toggle "Copy coordinates to clipboard"

## Configuration

The mod stores its configuration in `%APPDATA%/.minecraft/config/copycoords.json`:

```json
{
  "copyToClipboard": true
}
```

Set `"copyToClipboard": false` to only display coordinates without auto-copying.

## License

MIT License - See LICENSE file for details

## Author

- MR-Kartoshki on GitHub
- freddy._.fazbear on discord
- freddyfazbear0000 on CurseForge
- MR-kartoshki on Modrinth
#### (feel free to message me on discord)