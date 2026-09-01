# PUUZ SECURITY – AI / Developer Handoff Notes

## Current baseline
- Project: PUUZ SECURITY / PUUZ Map Shield
- Minecraft: 1.21.11
- Fabric client-side mod
- Internal mod id must remain `puuz_map_shield` for config compatibility.
- Public branding: `PUUZ SECURITY`.
- Default logo/icon is the user-provided PUUZ SECURITY image.

## v1.2.x behavior
- Hide Map Art with per-server allowed Map IDs.
- Password Shield defaults: `/l`, `/login`, `/dn`, `/dk`, `/reg`, `/register`.
- Users can add custom protected commands.
- Update Checker reads GitHub latest release from `sonphuc4414-dot/PUUZ-SECURITY`.
- Settings opened by keybind and Mod Menu integration.

## GUI principles
- Light, soft, calm UI; no cyberpunk/gaming styling.
- No blur pass: avoid `applyBlur()`/`renderBackground()` blur paths because they can conflict with other renderers.
- Use standard Minecraft widgets for interaction; do not implement legacy `mouseClicked(double,double,int)` overrides.
- Layout must be computed from current screen size and must not overlap at compact sizes.
- Hover/press animation should be subtle and frame-rate independent.
- Keep text contrast comfortable rather than pure white on saturated backgrounds.
- Appearance settings support accent/text color presets and optional local PNG/JPG backgrounds.
- Background images are selected from `config/puuz-security/backgrounds` or by path in the Appearance tab.

## Password Shield architecture
- `ChatScreenMixin` targets `ChatScreen`, not a generic chat-mod API.
- `ChatScreen.format(String,int)` masks displayed command arguments.
- `ChatScreen.render(...)` TAIL overlay draws masked text above the normal chat field, so common chat animation/rendering mods are less likely to reveal the plaintext.
- The real `TextFieldWidget.getText()` is never replaced; only the rendered representation is masked.
- If a third-party mod renders after this mixin, ordering can still vary; keep mixin priority low enough to overlay typical default-priority render injectors.

## Important safety rules
- No secrets/PAT/API keys in client code.
- Update checker failures must never block startup or gameplay.
- Network and disk work should stay off Minecraft's render/client thread where practical.
- Do not force-push release tags unless intentionally replacing history.

## GUI V5
- Settings screen uses vanilla widget input and custom visual layer; do not override mapping-sensitive mouseClicked/renderWidget methods.
- Open/close animation is time-based and avoids blur.
- Responsive layout supports sidebar on wide screens and multi-row horizontal tabs on narrow screens.
- Appearance supports accent/text presets and selectable PNG/JPG backgrounds from config/puuz-security/backgrounds.
- Background list has refresh, apply-path, and clear-background actions.

## GUI responsive redesign
- Settings screen uses vanilla ButtonWidget/TextFieldWidget for input and focus handling.
- Layout is calculated from actual screen width/height; compact mode uses multi-row top tabs.
- Appearance page supports accent/text presets and PNG/JPG background selection from config/puuz-security/backgrounds.
- No blur rendering is used; this avoids frame blur conflicts with other UI mods.
- Open/close visuals use a short easing animation without moving input hitboxes.

## 1.2.0 Map Tooltip Privacy Patch
- Added `mapTooltipPreviewBlocked` config, default `true`.
- Added an optional `@Pseudo` mixin for EnhancedTooltips `MapTooltipComponent.drawImage`.
- The mixin cancels only the map image preview; tooltip text such as Map, ID, Scaling and Level remains visible.
- No dependency on EnhancedTooltips is added, so PUUZ remains loadable without it.
- This is intentionally independent of Password Shield/login handling.

## 1.3.0 premium HUD/image upgrade
- Money History now has multiple visual styles: text-only, soft card, glass, outline, pill.
- Added opacity presets and retained position/size controls.
- Added custom Map Shield hide image with live preview and one-click restore of the default image.
- Settings wheel scroll is page-wide rather than restricted to a narrow viewport so lower controls are reachable even when the pointer is over another part of the menu.
