# CopyCoords

A simple mod that makes sharing Minecraft coordinates less of a hassle. Copy, convert, send - that's it.

Whether you're coordinating builds across dimensions, sharing base locations in chat, or just tired of manually typing coordinates, CopyCoords saves you a bunch of clicks.

## Features

- `/copycoords` - Get your current coordinates in chat. Optionally copies to clipboard.
- **C key** (default) - Quick copy keybind without opening chat.
- `/convertcoords` - Convert coordinates between Overworld and Nether (8:1 scale).
- `/msgcoords` - Send your coords to another player, with optional dimension conversion.
- `/distcalc` - Calculate distance and direction between two coordinate sets, with bearing and cardinal directions.
- **Works on Windows, Mac, and Linux** - Clipboard support built in for all platforms.
- **Shows current dimension** - See which world you're in when you copy (Overworld, Nether, End).
- **Multiple coordinate formats** - Space-separated, brackets, or XYZ labels. Pick your style.
- **8 languages** - English, Spanish, French, German, Chinese, Japanese, Portuguese, Russian.
- **Customizable in-game** - Open Mod Menu to tweak settings without editing JSON.

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

- `/copycoords` - Print your current coordinates
- `/copycoords [dimension]` - Print and convert to another dimension
- `/convertcoords [dimension] [x] [y] [z]` - Convert specific coordinates
- `/msgcoords [player]` - Send your coordinates to another player
- `/msgcoords [player] [dimension]` - Send converted coordinates to a player
- `/distcalc [x1] [y1] [z1] [x2] [y2] [z2]` - Calculate distance between two coordinate sets
- `/distcalc bookmarks [bookmark1] [bookmark2]` - Calculate distance between saved bookmarks
  - If bookmark names contain spaces, wrap them in quotes (e.g., `/distcalc bookmarks "Base One" "Base Two"`)
- **C key** - Quick copy without opening chat

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.x
2. Install the dependencies: Fabric API, Cloth Config, Mod Menu
3. Drop `copycoords-<version>.jar` into your `mods` folder
4. Launch the game and enjoy

## Configuration

Config is at `%APPDATA%/.minecraft/config/copycoords.json`. But honestly, just use Mod Menu to configure everything - it's easier.

```json
{
  "copyToClipboard": true,
  "copyConvertedToClipboard": true,
  "showDimensionInCoordinates": true,
  "coordinateFormat": "space"
}
```

**What each option does:**

- `copyToClipboard` - Auto-copy when you use `/copycoords`
- `copyConvertedToClipboard` - Auto-copy when you use `/convertcoords`
- `showDimensionInCoordinates` - Include dimension name in output (e.g., `100 64 200 (Overworld)`)
- `coordinateFormat` - How to display coords:
  - `"space"` - `100 64 200`
  - `"bracket"` - `[100, 64, 200]`
  - `"xyz"` - `X:100 Y:64 Z:200`

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