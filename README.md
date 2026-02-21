# CopyCoords

A simple mod that makes sharing Minecraft coordinates less of a hassle. Copy, convert, send - that's it.

Whether you're coordinating builds across dimensions, sharing base locations in chat, or just tired of manually typing coordinates, CopyCoords saves you a bunch of clicks.

## Features

- `/copycoords` (alias: `/cc`) - Get your current coordinates in chat. Optionally copies to clipboard.
- **Keybinds** for lightning-fast coordinate copying:
  - **C key** (default) - Quick copy without opening chat
  - **V key** (default) - Copy converted coordinates (Overworld ↔ Nether)
  - **B key** (default) - Copy with dimension name always included
- **Clickable Coordinates** - Click any coordinate in chat to re-copy it to your clipboard. Hover to see the tooltip.
- `/convertcoords` - Convert coordinates between Overworld and Nether (8:1 scale).
- `/msgcoords` - Send your coords to another player, with optional dimension conversion.
- `/distcalc` - Calculate distance and direction between two coordinate sets, with bearing and cardinal directions.
- **Works on Windows, Mac, and Linux** - Clipboard support built in for all platforms.
- **Shows current dimension** - See which world you're in when you copy (Overworld, Nether, End).
- **Multiple coordinate formats** - Space-separated, brackets, or XYZ labels. Pick your style.
- **8 languages** - English, Spanish, French, German, Chinese, Japanese, Portuguese, Russian.
- **Customizable in-game** - Open Mod Menu to tweak settings without editing JSON.
- **Optional map links** - Add clickable Dynmap/BlueMap/web-map links next to coordinates (manual config, disabled by default).

###### This mod collects anonymous usage statistics. No personal data is collected. This can be disabled in Mod Menu or by editing `config/copycoords/telemetry.json`.

## Coordinate Conversion

Wanna check your Nether coordinates in the Overworld? Use `/convertcoords`.

```
/convertcoords [goal] [x] [y] [z]
```

- **goal** - `overworld` or `nether`
- **coordinates** - Numbers or `~` for relative (e.g., `~ ~1 ~-5`)

Examples:
- `/convertcoords nether 100 64 200` - Convert those Overworld coords to Nether
- `/convertcoords overworld ~ ~ ~` - Convert your current position from Nether to Overworld

The conversion is standard Minecraft (8:1 scale on X/Z), and Y stays the same.

## Commands

- `/copycoords` (alias: `/cc`) - Print your current coordinates
- `/copycoords [dimension]` - Print and convert to another dimension
- `/convertcoords [dimension] [x] [y] [z]` - Convert specific coordinates
- `/msgcoords [player]` - Send your coordinates to another player
- `/msgcoords [player] [dimension]` - Send converted coordinates to a player
- `/distcalc [x1] [y1] [z1] [x2] [y2] [z2]` - Calculate distance between two coordinate sets
- `/distcalc bookmarks [bookmark1] [bookmark2]` - Calculate distance between saved bookmarks
- `/coordbookmark export <file>` - write current bookmarks to disk as JSON
- `/coordbookmark import <file>` - load bookmarks from a previously-exported JSON file
  - If bookmark names contain spaces, wrap them in quotes (e.g., `/distcalc bookmarks "Base One" "Base Two"`)

## Chat-pasting option

A new configuration flag `pasteToChatInput` is available (disabled by default). When enabled, any coordinates copied by a command or keybind will appear in the chat input box automatically instead of being copied to the clipboard. This makes it easy to edit or send them just by pressing `Enter`.

You can toggle the behaviour via Mod Menu (see the configuration section below).

## Keybinds

All keybinds can be customized in **Options → Controls → Key Binds → CopyCoords**

- **C key** (default) - Copy your current coordinates
- **V key** (default) - Copy converted coordinates to the opposite dimension
  - In Overworld: copies Nether coordinates
  - In Nether: copies Overworld coordinates
  - Shows an error in the End or other dimensions
- **B key** (default) - Copy coordinates with dimension name always included
  - Overrides the `showDimensionInCoordinates` config for this copy
  - Useful when you want to be explicit about which dimension you're in

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.x
2. Install the dependencies: Fabric API, Cloth Config, Mod Menu
3. Drop `copycoords-<version>.jar` into your `mods` folder
4. Launch the game and enjoy

## Configuration

Main config is at `%APPDATA%/.minecraft/config/copycoords/copycoords.json`.

Data/history config is at `%APPDATA%/.minecraft/config/copycoords/copycoords-data.json`.

Telemetry config is at `%APPDATA%/.minecraft/config/copycoords/telemetry.json`.

Most options are easiest to configure with Mod Menu.

```json
{
  "copyToClipboard": true,
  "copyConvertedToClipboard": true,
  "showDimensionInCoordinates": true,
  "pasteToChatInput": false,
  "coordinateFormat": "space",
  "mapLinksEnabled": false,
  "dynmapUrlTemplate": "http://localhost:8123/?world={world}&map=flat&x={x}&y={y}&z={z}",
  "bluemapUrlTemplate": "http://localhost:8100/#world:{world}:{x}:{y}:{z}:150:0:0:0:0:perspective",
  "webMapUrlTemplate": ""
}
```

**What each option does:**

- `copyToClipboard` - Auto-copy when you use `/copycoords`
- `copyConvertedToClipboard` - Auto-copy when you use `/convertcoords`
- `showDimensionInCoordinates` - Include dimension name in output (e.g., `100 64 200 (Overworld)`)
- `pasteToChatInput` - If set to `true`, coordinates will be placed in the chat input box instead of the clipboard. Useful for quick sharing; press Enter to send immediately.
- `coordinateFormat` - How to display coords:
  - `"space"` - `100 64 200`
  - `"bracket"` - `[100, 64, 200]`
  - `"xyz"` - `X:100 Y:64 Z:200`
- `mapLinksEnabled` - If `true`, appends clickable map links to coordinate chat output.
- `dynmapUrlTemplate` - URL template used for Dynmap links (only shown if Dynmap mod is present).
- `bluemapUrlTemplate` - URL template used for BlueMap links (only shown if BlueMap mod is present).
- `webMapUrlTemplate` - Generic custom map URL template (always optional).

### Map link template placeholders

Available placeholders for map URL templates:

- `{x}` `{y}` `{z}` - block coordinates
- `{world}` - default world slug from dimension (`world`, `world_nether`, `world_the_end`)
- `{worldEncoded}` - URL-encoded world value
- `{dimension}` - raw dimension id string
- `{dimensionEncoded}` - URL-encoded dimension id

Notes:

- Map links are off by default and require `mapLinksEnabled: true`.
- Invalid or non-http(s) URLs are ignored safely.
- If Dynmap or BlueMap is not installed, their corresponding links are skipped without errors.

### Telemetry config

Telemetry settings are stored separately in `config/copycoords/telemetry.json`.

```json
{
  "enabled": true,
  "lastSent": 0
}
```

- `enabled` - opt out by setting to `false` (also available in Mod Menu).
- `lastSent` - internal rate-limit timestamp managed automatically.

## Languages & Platforms

CopyCoords supports 8 languages. The game language is detected automatically:

- 🇬🇧 English
- 🇪🇸 Spanish
- 🇫🇷 French
- 🇩🇪 German
- 🇨🇳 Chinese
- 🇯🇵 Japanese
- 🇧🇷 Portuguese
- 🇷🇺 Russian

**Platform support:**

- **Windows** - Clipboard works out of the box
- **Mac** - Clipboard works out of the box
- **Linux** - Install `xclip` with `sudo apt install xclip` (Ubuntu/Debian)

## License

MIT — see the `LICENSE` file.

## Credits

- MR-Kartoshki (GitHub)
- freddy._.fazbear (Discord)

Suggestions or feature requests are welcome — open an issue or send a message.