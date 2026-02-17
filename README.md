# CopyCoords

CopyCoords is a small Fabric mod for Minecraft 1.21.11 that makes it quick and easy to get, share, and convert coordinates.

Whether you're pasting a location into chat, coordinating building projects across dimensions, or just bookmarking a base, CopyCoords saves a few clicks.

## Features

- `/copycoords` — prints your current block coordinates in chat and can copy them to your system clipboard.
- Keybind — press **C** (default) to copy your coordinates instantly.
- `/convertcoords` — convert coordinates between the Overworld and the Nether.
- Configurable — toggle automatic clipboard copying in Mod Menu.
- Localized — supports translations for chat messages.

## Convert coordinates between dimensions

Use `/convertcoords [goal] [x] [y] [z]` to convert coordinates between Overworld and Nether.

- `goal` — `overworld` or `nether` (autocomplete is supported).
- `x y z` — coordinates to convert; you can use relative coordinates with `~` (for example `~ ~1 ~-2`).
- Examples:
  - `/convertcoords nether 100 64 200` converts Overworld 100 64 200 → Nether (X/Z divided by 8).
  - `/convertcoords overworld ~ ~ ~` converts your current position from Nether → Overworld (X/Z multiplied by 8).

Notes: Conversion uses the standard 8:1 X/Z scaling between Overworld and Nether. Y is preserved and rounded to the nearest block.

## Usage

- `/copycoords` — print and optionally copy current coordinates.
- `/convertcoords [goal] [x] [y] [z]` — convert coordinates between dimensions.
- Keybind — press **C** to copy coordinates without typing.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11: https://fabricmc.net/use/installer/.
2. Install required dependencies (Fabric API, Cloth Config, Mod Menu).
3. Drop `copycoords-<version>.jar` into your `mods` folder.

## Configuration

Config file: `%APPDATA%/.minecraft/config/copycoords.json`

```json
{
  "copyToClipboard": true
}
```

Set `copyToClipboard` to `false` to only show coordinates in chat.

## License

MIT — see the `LICENSE` file.

## Credits

- MR-Kartoshki (GitHub)
- freddy._.fazbear (Discord)

Suggestions or feature requests are welcome — open an issue or send a message.