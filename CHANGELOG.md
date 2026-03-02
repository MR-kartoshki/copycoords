# Changelog

All notable changes to CopyCoords will be documented in this file.

## [1.11.0] - 2026-02-26

### Added

- Bookmark import/export command using .json files in game root directory `/coordsbookmark import`, `/coordsbookmark export`

### Changed

- Bookmark command is now `/coordsbookmark` (added an s) with the same subcommands and behavior as before.

## [1.10.3] - 2026-02-25

### Added

- New config option: `decimalPlaces` (range `0-10`, default `2`) to control coordinate precision.

### Changed

- Coordinate output now always uses fixed precision with trailing zeros (for example, `100.00` when precision is `2`).
- Coordinate template placeholders `{x}`, `{y}`, and `{z}` now use the same configured precision.
- "Copy with dimension" keybind output now uses the shared coordinate formatter for consistent precision across versions.

## [1.10.2] - 2026-02-24

### Fixed

- `/copycoords`, `/msgcoords`, and instant-send keybind coordinate output now uses live player position (`getX/getY/getZ`) instead of block-position integers.
- Removed local `"Sent coordinates to chat:"` output for the instant-send keybind.
- Removed local `/msgcoords` success echo (`"Sent coordinates to %s: %s"`).
- `coordbookmark list` clickable copy entries now execute reliably, including bookmark names with spaces/quotes.

### Changed

- `coordbookmark add` now stores decimal coordinates.

## [1.10.1] - 2026-02-24

### Fixed

- Instant chat send now uses direct chat sending instead of reflection, preventing `sendChatMessage(java.lang.String)` reflection failures in-game.

## [1.10.0] - 2026-02-24

### Added

- New keybind: **Instant Chat Send**.
  - Sends your current coordinates directly to server chat.
  - Works even when `instantChatEnabled` is disabled.
  - Not bound to a key by default

## [1.9.1] - 2026-02.23

### Fixed (warning: nerdy)

- Telemetry version string could be wrong in development (the jar
  metadata sometimes reports the IDE’s target or a long mapping identifier).
  We now clean up the value from the `minecraft` mod container so the server
  sees the real running game version.
- Telemetry backend updated to use a synchronous sender with reusable
  helpers, matching the implementation from MonkeyLib538 for consistency and
  easier cross‑loader porting.

## [1.9.0] - 2026-02.23

### Added

- Instant‑chat option
  - new config toggle (instantChatEnabled) and `/cc` / `/copycoords` behaviour: when turned on the command output is also sent to the server chat.

### Fixed

- Telemetry always reported version **1.21.11** because it read the version baked into the development jar; now uses `FabricLoader.getGameVersion()` so reports the actual running game version.

## [1.8.1] - 2026-02-23

### Added

- Support for Minecraft versions **1.20.5** and **1.20.6** (I forgor)
- Telemetry now prints messages to the game log saying “Telemetry sent!” on success or “Telemetry not sent!” when it’s disabled, rate‑limited, or failed.
- Added missing localization entries for the coordinate template UI and new options, all supported languages now contain them.

## [1.8.0] - 2026-02-21

### Added

- **Custom coordinate template** – users can now specify a format string that overrides the built‑in `coordinateFormat`.
  - Placeholders: `{x}`, `{y}`, `{z}`, `{dimension}`, `{dimName}`.
  - The template field shows a live preview in its tooltip while editing.

## [1.7.0] - 2026-02-21

### Added

- **Optional map links** for coordinate output.
  - New optional templates for Dynmap, BlueMap, and generic web maps.
  - Available in Mod Menu and disabled by default.
- **Telemetry**.
  - Check Notes for more

### Changed

- **Config and data files moved** under `config/copycoords/`:
  - `copycoords.json`
  - `copycoords-data.json`
- Existing files in the old `config/` root are migrated automatically.

### Notes

- Telemetry is optional and can be disabled at any time.
- Map links are skipped automatically if Dynmap/BlueMap are not installed.

## [1.6.1] - 2026-02-21

### Fixed

- **Clickable coordinates** – clicking coordinates in chat now properly copies the value to clipboard on versions 1.21 through 1.21.4 (overrides were updated to use direct click/hover constructors).

## [1.6.0] - 2026-02-20

### Added

- **Paste-to-chat option** — new config toggle allowing coordinates to be placed directly into the chat input box instead of (or in addition to) copying to clipboard.  Works with keybinds and commands.

### Fixed

- **Keybinds not registering for 1.21.9+** - fixed an issue where keybinds just wouldnt work in 1.21.9+ versions of the mod.

## [1.5.1] - 2026-02-20

### Fixed

- **Minecraft crash** – keybinding registration failure on several versions was resolved, preventing startup crashes.

## [1.5.0] - 2026-02-20

### Added

- **Additional Keybinds** — two new keybinds for faster coordinate workflows
  - **V key** (default) - Copy coordinates converted to opposite dimension (Overworld ↔ Nether)
  - **B key** (default) - Copy coordinates with dimension name always shown
  - All keybinds are fully customizable in Controls settings
- **Clickable Chat Coordinates** — coordinate output in chat is now clickable
  - Click any coordinate in chat to re-copy it to your clipboard
  - Hover over coordinates to see "Click to copy coordinates" tooltip
  - Works with `/copycoords`, `/convertcoords`, keybinds, history, and bookmarks
  - All three keybinds now display clickable coordinates
  - `/cc` — alias for `/copycoords` (shortcut)

### Fixed

- **Minecraft 1.21.11 Crash** — Fixed keybinding registration crash on 1.21.11 instances
  - Improved KeyMapping constructor compatibility detection with better fallback handling
  - Changed error messages to help identify constructor availability issues
  - Keybinds now register gracefully if some fail

## [1.4.0] - 2026-02-19

### Added

- **Cross-Platform Clipboard Support** — clipboard functions now work on Windows, macOS, and Linux
- Windows: Uses `clip.exe` (built-in)
- macOS: Uses `pbcopy` (built-in)
- Linux: Uses `xclip` or `xsel` (install via package manager)
- **Dimension Indicator** — coordinates now optionally show which dimension you're in (Overworld, Nether, End)
  - New `showDimensionInCoordinates` config option (enabled by default)
  - Toggleable in Mod Menu
- **Configurable Coordinate Format** — choose how coordinates are displayed
  - Space-separated: `100 64 200` (default)
  - Bracket-comma: `[100, 64, 200]`
  - XYZ labels: `X:100 Y:64 Z:200`
  - New `coordinateFormat` config option
  - Format selector in Mod Menu
- **Multi-Language Support** — now available in 8 languages
  - English, Spanish, French, German
  - Chinese (Simplified), Japanese, Portuguese (Brazilian), Russian
  - Automatic language detection based on game settings
- **Distance Calculator** — calculate distance and direction between two coordinate sets
  - `/distcalc [x1] [y1] [z1] [x2] [y2] [z2]` - Calculate distance between absolute coordinates
  - `/distcalc bookmarks [bookmark1] [bookmark2]` - Calculate distance between saved bookmarks
  - Shows horizontal, vertical, and total 3D distance
  - Provides bearing in degrees and cardinal direction (16-point compass)
  - Displays Manhattan distance for reference

### Fixed

- **Client startup keybind crash on some 1.21.11 environments**
  - Expanded `KeyMapping` constructor compatibility handling in keybind registration

## [1.3.1] - 2026-02-18

### Added

- Multi-version build support for all Minecraft 1.21.x versions (1.21-1.21.11)

## [1.3.0] - 2026-02-18

### Added

- Optional `/copycoords [goal]` to convert current coordinates to a target dimension
- `/msgcoords [player] [goal]` to send coordinates to another player with optional conversion

### Changed

- Updated Mod Menu Mod description and authors to match those stated in the README
- `/convertcoords` now uses the same conversion logic as `/copycoords [goal]` (dimension-aware conversion and `~` relative coordinates); output and clipboard behavior now match the mod settings

## [1.2.1] - 2026-02-17

### Added

- `/convertcoords` now copies converted coordinates to clipboard (enabled by default)
- New `copyConvertedToClipboard` config option to toggle clipboard copying for converted coordinates
- Mod Menu toggle for the new converted coordinates clipboard setting
- Automated release pipeline (Modrinth, CurseForge, GitHub Releases)

### Changed

- Updated README with `/convertcoords` clipboard docs and new config example
