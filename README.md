# PUUZ Map Shield

Standalone **client-side Fabric mod for Minecraft 1.21.11**.

## What it does

When protection is enabled, the vanilla `MapRenderer.draw(...)` texture read is redirected to the bundled safe texture:

`assets/puuz_map_shield/textures/misc/map_hidden.png`

No map item is deleted or replaced. No NBT, map data, packets, world data, or server behavior is changed.

## Compatibility

- Minecraft **1.21.11 only**
- Fabric Loader **0.18.1+**
- Fabric API **0.141.2+1.21.11**
- Java **21+**
- Client-side only
- Works on Windows/Linux/macOS and Android launchers such as Zalith Launcher, provided the launcher runs Java 21 and Fabric 1.21.11.

Minecraft 1.21.11 is the last Yarn-mapped release; Minecraft 26.1+ is a separate target and is not supported by this project.

## Keybind

Default: **F8**

Change it in:

`Options -> Controls -> Key Binds -> PUUZ Map Shield -> Toggle Map Art Protection`

Minecraft handles conflicts and the user-selected key.

## Config

Saved automatically at:

`.minecraft/config/puuz-map-shield.json`

Example:

```json
{
  "enabled": true
}
```

## Build

The project uses Gradle wrapper and Fabric Loom 1.14.10.

```bash
./gradlew clean build
```

The remapped mod JAR is produced under `build/libs/`.

### Termux / Zalith Launcher

The same source tree can be built in Termux. Java 21 is required. The wrapper bootstraps its wrapper JAR once if it is not already present.

Do not use a random system Gradle version; use `./gradlew` from the project.

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install a compatible Fabric API for 1.21.11.
3. Put `puuz-map-shield-<version>.jar` in the `mods` directory.
4. Launch Minecraft.
5. Configure the key in Controls.

No OBS, server plugin, external application, network service, or server-side mod is required.


## PUUZ Branding

**PUUZ Map Shield**

Mod by **_PhucHoang_**.

Version 1.0.0 includes a lightweight optional GitHub Releases update checker. It runs in the background and never blocks the render/client thread.

Status format:
- `◆ ᴘᴜᴜᴢ ᴍᴀᴘ sʜɪᴇʟᴅ  •  ✓ ᴏɴ`
- `◆ ᴘᴜᴜᴢ ᴍᴀᴘ sʜɪᴇʟᴅ  •  × ᴏғғ`

The branding is client-only and does not alter gameplay, map data, networking, or server requirements.
