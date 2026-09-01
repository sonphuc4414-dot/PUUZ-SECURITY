# PUUZ SECURITY 1.3.1 — Responsive UI V4

## Adaptive navigation

The Settings screen now selects navigation automatically from the actual Minecraft viewport size:

- **SIDEBAR**: large/comfortable windows show all tabs permanently in a left sidebar.
- **ALL_TABS**: medium windows show all 11 tabs in a shallow two-row quick-select strip, preserving a large settings viewport.
- **COMPACT**: small or short windows use previous/current/next arrows so navigation never consumes the whole screen.

The mode is recalculated whenever the Minecraft window is resized.

## Small-screen behavior

The panel is clamped to the real viewport, and the settings content has its own scroll region. Navigation and footer remain fixed while the settings page scrolls.

## Compatibility note

This release keeps the existing Minecraft 1.21.11 single-version base. It does not add any new Minecraft-version API dependencies.
