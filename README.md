# CopyCoords

CopyCoords is a client-side Fabric mod that makes coordinates faster to copy, convert, and share.

## Supported Minecraft Versions

- `1.19` - `1.21.11`

Each release jar is built for a specific Minecraft version.

## Features

- `/copycoords` and `/cc` show your current coordinates
- `/convertcoords` converts Overworld <-> Nether coordinates using the 8:1 ratio
- `/msgcoords` sends your coordinates to another player
- `/distcalc` calculates distance between two positions or bookmarks
- Clickable coordinates in chat
- Coordinate history with copy, insert, remove, and menu actions
- Bookmark add/list/copy/remove/import/export commands
- Configurable coordinate formatting, decimal precision, and templates
- Optional map links for Dynmap, BlueMap, or custom web maps
- Keybinds for:
  - copying current coordinates
  - copying converted coordinates
  - copying coordinates with dimension
  - instant chat send

## Installation

1. Install Fabric Loader for your Minecraft version.
2. Install the required dependencies for that same version:
   - Fabric API
   - Cloth Config
   - Mod Menu
3. Put the matching `copycoords+<mc-version>-<mod-version>.jar` in your `mods` folder.
4. Launch the game.

## Commands

- `/copycoords`
- `/copycoords [overworld|nether]`
- `/convertcoords [overworld|nether] [x] [y] [z]`
- `/msgcoords [player]`
- `/msgcoords [player] [overworld|nether]`
- `/distcalc [x1] [y1] [z1] [x2] [y2] [z2]`
- `/distcalc bookmarks [bookmark1] [bookmark2]`
- `/coordshistory`
- `/coordshistory list`
- `/coordshistory copy <index>`
- `/coordshistory remove <index>`
- `/coordshistory menu <index>`
- `/coordshistory clear`
- `/coordsbookmark add <name>`
- `/coordsbookmark list`
- `/coordsbookmark copy <name>`
- `/coordsbookmark remove <name>`
- `/coordsbookmark export <file>`
- `/coordsbookmark import <file>`
- `/coordbookmark ...` alias for bookmark commands

## Configuration

Use Mod Menu for configuration.

Config files:

- `%APPDATA%/.minecraft/config/copycoords/copycoords.json`
- `%APPDATA%/.minecraft/config/copycoords/copycoords-data.json`

Main options:

- `copyToClipboard`
- `copyConvertedToClipboard`
- `showDimensionInCoordinates`
- `pasteToChatInput`
- `instantChatEnabled`
- `coordinateFormat`
- `decimalPlaces`
- `coordinateTemplate`
- `mapLinksEnabled`
- `dynmapUrlTemplate`
- `bluemapUrlTemplate`
- `webMapUrlTemplate`

Instant chat send notes:

- The Instant Chat Send keybind is unbound by default. Set it in Controls before use.
- If sending fails, CopyCoords now shows an in-chat failure reason to help with troubleshooting.

Template placeholders:

- `{x}`
- `{y}`
- `{z}`
- `{dimension}`
- `{dimName}`

Map link placeholders:

- `{x}`
- `{y}`
- `{z}`
- `{world}`
- `{worldEncoded}`
- `{dimension}`
- `{dimensionEncoded}`

## License

AGPL v3. See `LICENSE`.

## Credits

- MR-Kartoshki
- freddy._.fazbear
