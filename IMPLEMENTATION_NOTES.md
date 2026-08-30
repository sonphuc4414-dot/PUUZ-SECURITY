# PUUZ SECURITY 1.2.5 — Money History implementation

## Feature
- Optional HUD widget: Money History.
- Optional Balance row, independently toggleable.
- Sent/received transaction visibility is independently toggleable.
- Position presets and size/scale presets are exposed in the Settings GUI.
- Visible entry count controls only HUD presentation; storage is unlimited.
- Local transaction history is persistent and never uploaded.

## Storage
- `config/puuz-security/money-history.json`
- JSON object stores `transactions` and `lastKnownBalance`.
- No automatic retention limit is implemented.
- User-only `Clear History` removes all stored records.

## Detection
- Outgoing: Fabric `ClientSendMessageEvents.COMMAND`, parsing `/pay <player> <amount>`.
- Incoming: Fabric `ClientReceiveMessageEvents.GAME`, parsing common `paid you`, `sent you`, `gave you`, `received ... from` patterns.
- Balance: common `balance`, `bal`, and `money` response patterns.

## HUD
- Implemented through `InGameHud.render` tail injection for the fixed 1.21.11 DrawContext API.
- Uses a lightweight time-based entry animation.
- Does not allocate or perform I/O every frame.
