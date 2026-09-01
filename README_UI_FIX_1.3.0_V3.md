# PUUZ SECURITY 1.3.1 UI – Responsive Fix V3

## UI fixes
- Compact screens no longer use an 11-tab multi-row grid that can consume the whole viewport.
- Compact mode uses a fixed-height previous/current/next navigation row so the active settings page always keeps a usable viewport.
- Sidebar mode is enabled only when there is enough vertical space for navigation and content.
- Content controls remain interactable when partially intersecting the scroll viewport.
- Scroll target is hard-clamped to 0..maxContentScroll and eased toward the target.
- Tab navigation remains fixed while page content scrolls independently.

## Compatibility
Base remains the single-version Minecraft 1.21.11 PUUZ SECURITY 1.3.1 source.
