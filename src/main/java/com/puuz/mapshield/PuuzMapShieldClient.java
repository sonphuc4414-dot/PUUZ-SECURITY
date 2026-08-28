package com.puuz.mapshield;

import com.puuz.mapshield.config.MapShieldConfig;
import com.puuz.mapshield.keybind.MapShieldKeybind;
import com.puuz.mapshield.update.UpdateChecker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

/** Client-only entrypoint for PUUZ Map Shield. */
public final class PuuzMapShieldClient implements ClientModInitializer {
    public static final String MOD_ID = "puuz_map_shield";
    public static final String CREATOR = "_PhucHoang_";
    public static final Identifier SAFE_MAP_TEXTURE =
            Identifier.of(MOD_ID, "textures/misc/map_hidden.png");

    @Override
    public void onInitializeClient() {
        MapShieldConfig.load();
        MapShieldKeybind.register();
        UpdateChecker.start(MinecraftClient.getInstance());
        ClientTickEvents.END_CLIENT_TICK.register(PuuzMapShieldClient::onClientTick);
    }

    private static void onClientTick(MinecraftClient client) {
        UpdateChecker.tick(client);
        while (MapShieldKeybind.TOGGLE.wasPressed()) {
            boolean enabled = !MapShieldConfig.isEnabled();
            MapShieldConfig.setEnabled(enabled);
            MapShieldConfig.save();

            if (client.player != null) {
                client.player.sendMessage(buildStatusMessage(enabled), true);
            }
        }
    }

    private static Text buildStatusMessage(boolean enabled) {
        // Unicode small-caps keeps the status compact without adding a custom font
        // asset, which helps the exact same JAR behave on PC and Zalith/Android.
        String title = "ᴘᴜᴜᴢ ᴍᴀᴘ sʜɪᴇʟᴅ";
        if (enabled) {
            return Text.literal("◆ ")
                    .formatted(Formatting.DARK_AQUA)
                    .append(Text.literal(title).formatted(Formatting.AQUA, Formatting.BOLD))
                    .append(Text.literal("  •  ").formatted(Formatting.GRAY))
                    .append(Text.literal("✓ ᴏɴ").formatted(Formatting.GREEN, Formatting.BOLD));
        }
        return Text.literal("◆ ")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.literal(title).formatted(Formatting.GRAY, Formatting.BOLD))
                .append(Text.literal("  •  ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal("× ᴏғғ").formatted(Formatting.RED, Formatting.BOLD));
    }
}
