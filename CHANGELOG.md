# Changelog

All notable changes to CopyCoords will be documented in this file.

## [1.3.2] - 2026-02-18

### Added
- Cross-Platform Clipboard Support - clipboard functions now support linux and mac.

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
