<div align="center">

# CopyCoords

**A client-side Fabric mod for copying, converting, and sharing coordinates.**

[![Modrinth](https://img.shields.io/modrinth/dt/copycoordsmod?color=00AF5C&label=Downloads&logo=modrinth)](https://modrinth.com/mod/copycoordsmod)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/MR-kartoshki/copycoords)](https://github.com/MR-kartoshki/copycoords/releases)

</div>

---

## Features

- `/cc` copies your current coordinates instantly
- Overworld ↔ Nether conversion with the 8:1 ratio
- Send coordinates to a player with `/msgcoords`
- Distance calculator between two positions or saved bookmarks
- Clickable coordinates in chat
- Full coordinate history — copy, reinsert, or remove past entries
- Bookmarks with import/export support
- Map links for Dynmap, BlueMap, or any custom web map
- Configurable format, decimal precision, and custom templates

**Keybinds** (all rebindable in Controls):
- Copy current coordinates
- Copy converted coordinates
- Copy coordinates with dimension label
- Instant chat send

---

## Requirements

| Dependency | Link |
|------------|------|
| Fabric Loader | [fabricmc.net](https://fabricmc.net) |
| Fabric API | [Modrinth](https://modrinth.com/mod/fabric-api) |
| Cloth Config | [Modrinth](https://modrinth.com/mod/cloth-config) |
| Mod Menu | [Modrinth](https://modrinth.com/mod/modmenu) |

Supports Minecraft `1.19` through `26.1.1`. Each jar targets a specific MC version.

---

## Installation

1. Install Fabric Loader.
2. Download all four dependencies above **for your exact MC version.**
   Mixing versions breaks things. `Fabric API 0.58.5+1.19.1` is incompatible with MC `1.19`.
3. Drop `copycoords+<mc-version>-<mod-version>.jar` into your `mods` folder.
4. Launch.

---

## Commands

| Command | Description |
|---------|-------------|
| `/cc` | Copy current coordinates |
| `/copycoords [overworld\|nether]` | Copy for a specific dimension |
| `/convertcoords [dim] [x] [y] [z]` | Convert between Overworld and Nether |
| `/msgcoords [player]` | Send your coords to a player |
| `/distcalc [x1] [y1] [z1] [x2] [y2] [z2]` | Distance between two points |
| `/distcalc bookmarks [b1] [b2]` | Distance between two bookmarks |
| `/coordshistory` | View coordinate history |
| `/coordsbookmark add/list/copy/remove` | Manage bookmarks |
| `/coordsbookmark export/import [file]` | Backup or restore bookmarks |

See in-game command suggestions for the full list.

---

## Configuration

Open **Mod Menu** and select CopyCoords, or edit the config files directly:
```
.minecraft/config/copycoords/copycoords.json
.minecraft/config/copycoords/copycoords-data.json
```

**Template placeholders:** `{x}` `{y}` `{z}` `{dimension}` `{dimName}`

**Map link placeholders:** `{x}` `{y}` `{z}` `{world}` `{worldEncoded}` `{dimension}` `{dimensionEncoded}`

> **Instant Chat Send** is unbound by default. Set it in Controls before use.
> If a send fails, CopyCoords prints the reason in chat.

---

## License

[AGPL-3.0](LICENSE)
