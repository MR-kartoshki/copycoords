# CopyCoords

CopyCoords is a client-side Fabric mod that makes coordinates faster to copy, convert, and share.

## What it does

- `/copycoords` (alias: `/cc`) shows your current coordinates
- `/convertcoords` converts Overworld ↔ Nether coordinates (8:1)
- `/msgcoords` sends your coordinates to another player
- `/distcalc` calculates distance between two positions
- Clickable coordinates in chat (click to copy)
- Keybinds for quick copy actions:
  - `C` copy current coordinates
  - `V` copy converted coordinates
  - `B` copy with dimension name
- Optional map links (Dynmap/BlueMap/custom web map)

## Installation

1. Install Fabric Loader for Minecraft 1.21.x
2. Install dependencies: Fabric API, Cloth Config, Mod Menu
3. Put `copycoords-<version>.jar` in your `mods` folder
4. Launch Minecraft

## Commands

- `/copycoords`
- `/copycoords [overworld|nether]`
- `/convertcoords [overworld|nether] [x] [y] [z]`
- `/msgcoords [player]`
- `/msgcoords [player] [overworld|nether]`
- `/distcalc [x1] [y1] [z1] [x2] [y2] [z2]`
- `/distcalc bookmarks [bookmark1] [bookmark2]`
- `/coordbookmark add <name>`
- `/coordbookmark list`
- `/coordbookmark copy <name>`
- `/coordbookmark remove <name>`
- `/coordbookmark export <file>`
- `/coordbookmark import <file>`

## Configuration

Use Mod Menu for most settings.

Config files:

- `%APPDATA%/.minecraft/config/copycoords/copycoords.json`
- `%APPDATA%/.minecraft/config/copycoords/copycoords-data.json`
- `%APPDATA%/.minecraft/config/copycoords/telemetry.json`

Main options:

- `copyToClipboard`
- `copyConvertedToClipboard`
- `showDimensionInCoordinates`
- `pasteToChatInput`
- `coordinateFormat` (`space`, `bracket`, `xyz`)

### Map links

Map links are optional and disabled by default.

- Enable in Mod Menu: `Map Links -> Enable map links`
- Configure templates:
  - `dynmapUrlTemplate`
  - `bluemapUrlTemplate`
  - `webMapUrlTemplate`

Supported placeholders:

- `{x}` `{y}` `{z}`
- `{world}` `{worldEncoded}`
- `{dimension}` `{dimensionEncoded}`

If Dynmap or BlueMap is not installed, those links are skipped automatically.

## Telemetry

CopyCoords sends minimal anonymous usage telemetry.

- No personal data is collected.
- Disable anytime in Mod Menu (`Telemetry -> Enable telemetry`) or by setting `enabled: false` in `config/copycoords/telemetry.json`.

## License

MIT. See `LICENSE`.

## Credits

- MR-Kartoshki (GitHub)
- freddy._.fazbear (Discord)
