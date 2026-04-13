# Changelog

All notable changes to CopyCoords will be documented in this file.

## [1.16.0] - 2026-04-13

### Added

- `/coordsbookmark add <name> <coords>` and `/coordbookmark add <name> <coords>` now support explicit coordinates when creating bookmarks.
  - Accepts space-separated coordinates like `100 64 200`.
  - Accepts bracket coordinates like `[100, 64, 200]`.
  - Accepts `X:100 Y:64 Z:200` style coordinates.
  - Keeps the old `add <name>` behavior and still uses your current position when coordinates are omitted.

### Fixed

- Fixed the bookmark Xaero export action failing for bookmarks that stored equivalent dimension IDs in legacy or raw forms instead of Xaero's expected Overworld, Nether, or End IDs.
- Fixed incoming chat coordinate detection registration on Minecraft `26.1.2`, which could fail at runtime because chat receive compat reflected against Fabric's concrete event implementation instead of the public event interface.
- Fixed malformed `/coordsbookmark add <name> <coords>` and `/coordbookmark add <name> <coords>` input falling back to the player's current position instead of reporting an invalid coordinate or dimension error.
- Fixed `/convertcoords` silently substituting the player's current coordinates when given invalid coordinate tokens.
- Fixed bookmark import crashing or partially importing data when the JSON array contained null or malformed bookmark entries.
- Fixed Windows clipboard writes using the JVM default charset instead of an explicit Unicode-safe encoding.
- Fixed `/msgcoords` failing without showing the underlying chat command send reason when the send call returned `false`.

## [1.15.0] - 2026-04-10

### Added

- Support for Minecraft `26.1.2`.
- Automatic coordinate detection in incoming chat messages.
  - Detects common coordinate formats such as `100 64 200`, `[100, 64, 200]`, and `X:100 Y:64 Z:200`.
  - Shows a follow-up action line so detected coordinates can be copied, inserted into chat, bookmarked, or converted between Overworld and Nether when the source dimension is known.
- New config options for chat coordinate detection:
  - `Detect coordinates from chat`
  - `Max detected coordinate sets per message`
- New Localizations:
  - New `LOLCAT` localization (`lol_us`).
  - New `Pirate Speak` localization (`en_pt`).
  - New `Shakespearean English` localization (`en_sh`).
  - Also made the mod description translatable in Mod Menu and added translations for all shipped locales.
- Xaero waypoint export for bookmarks.
  - Use `/coordsbookmark xaero add <bookmark>` to export to the current Xaero world/server when CopyCoords can infer it.
  - Use `/coordsbookmark xaero add <bookmark> target <path>` to export to a specific Xaero folder or waypoint file.

### Removed

- Removed the temporary `/copycoords status`, `/copycoords hintunbound`, and `/copycoords config showInstantChatSendUnboundHint` debug utility commands.

## [1.14.0] - 2026-04-06

### Added

- Support for Minecraft `26.1` and `26.1.1`.


## [1.13.0] - 2026-03-21

### Added

- New config option: `Show instant send key unbound hint`.
  - Controls whether the chat hint is shown when the Instant Chat Send keybind is unbound.
  - Default value is `true`.
- New `/copycoords` utility commands:
  - `/copycoords status` to display instant chat related settings and instant-send keybind state.
  - `/copycoords hintunbound <true|false>` and `/copycoords config showInstantChatSendUnboundHint <true|false>` to change the hint option in-game.

### Changed

- Removed Fabric API version constraints from manifest dependencies.
  - `Fabric API` is still required, but no minimum version is forced.
- Localized instant-chat config option labels/tooltips in Mod Menu via language keys.
- Added missing translation keys for the new instant-send hint option across all shipped locales.

## [1.12.5] - 2026-03-20

### Added

- `/msgcoords` name suggestion

### Fixed

- Fixed the mod failing to start on Java 17 for Minecraft versions below 1.20.5.

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
