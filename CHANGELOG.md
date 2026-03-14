# Changelog

All notable changes to CopyCoords will be documented in this file.

## [1.12.4] - 2026-03-15

### Fixed

- Fixed coordinates always showing `.0` decimal suffix when using the copy-with-dimension keybind, the copy-converted keybind, and history commands. Block-position coordinates (which are always integers) are now formatted without decimal places.

- Fixed `/msg` coordinate command ignoring the configured coordinate format, custom template, and dimension display settings.

## [1.12.3] - 2026-03-14

### Fixed 

- Fixed name-mapping mismatch at runtime. This caused the instant send to chat keybind to not work.

## [1.12.2] - 2026-03-14

### Fixed

- Fixed a bug where the instant send to chat keybind would not work.

## [1.12.1] - 2026-03-13

### Added

- Support for Minecraft `1.19` through `1.19.4`

### Removed

- Telemetry and all related config/UI code

## [1.12.0] - 2026-03-02

### Added

- New history commands:
  - `/coordshistory remove <index>` to remove a single history entry
  - `/coordshistory menu <index>` to open quick actions for a specific entry
- `coordshistory list` entries now include clickable action chips:
  - `[copy]`, `[insert]`, `[remove]`, and `[menu]`
- History quick menu now includes a `[clear_all]` action for one-click full history clearing

### Changed

- History entries now support click-to-insert behavior for chat input (`[insert]` and suggest-command action)
- Bookmark commands now support both roots:
  - `/coordsbookmark`
  - `/coordbookmark`

## [1.11.0] - 2026-02-26

### Added

- Bookmark import/export command using `.json` files in the game root directory

### Changed

- Bookmark command root is now `/coordsbookmark` with `/coordbookmark` kept as an alias
