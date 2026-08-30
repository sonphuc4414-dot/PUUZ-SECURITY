# PUUZ SECURITY 1.2.5 — Money History

- Optional HUD widget for payment history.
- `/pay <player> <amount>` sent by the player is recorded locally.
- Incoming server game messages are parsed for common `paid you` / `received ... from` forms.
- Balance is parsed from common `balance`, `bal`, or `money` server messages.
- History storage is unlimited and local-only; no automatic deletion.
- Visible HUD entry count is only a display preference, not a storage cap.
- The user can clear the whole local history explicitly.
- HUD position uses normalized X/Y, size uses width/height plus scale.
- Enable Money History and Show Balance are separate toggles.
- Sent/Received, names, amounts are independently toggleable.
- HUD renders at the end of vanilla InGameHud.render and uses a short time-based entrance animation.
- No networking is added for this feature.
