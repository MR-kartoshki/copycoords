# Changelog

All notable changes to CopyCoords will be documented in this file.

## [1.6.0] - 2026-02-20

### Added
- **Decimal precision option** — configurable number of decimal places for coordinate output (0 = integer only).

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
