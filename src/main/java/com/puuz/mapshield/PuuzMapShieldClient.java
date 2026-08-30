package com.puuz.mapshield;

import com.puuz.mapshield.config.MapShieldConfig;
import com.puuz.mapshield.gui.PuuzSecuritySettingsScreen;
import com.puuz.mapshield.keybind.MapShieldKeybind;
import com.puuz.mapshield.money.MoneyHistory;
import com.puuz.mapshield.map.MapHideTextureManager;
import com.puuz.mapshield.update.UpdateChecker;
import com.puuz.mapshield.update.UpdateInfo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;

import java.util.Locale;

/** Client-only entrypoint for PUUZ SECURITY. */
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
        MoneyHistory.init();
        MapHideTextureManager.reload();

        ClientTickEvents.END_CLIENT_TICK.register(
                PuuzMapShieldClient::onClientTick
        );
    }

    private static void onClientTick(MinecraftClient client) {
        MoneyHistory.onServerContextChanged();
        if (client.player != null) {
            UpdateInfo update = UpdateChecker.consumePending();
            if (update != null) {
                showUpdateNotification(client, update);
            }
        }

        while (MapShieldKeybind.SETTINGS.wasPressed()) {
            client.setScreen(
                    new PuuzSecuritySettingsScreen(
                            client.currentScreen
                    )
            );
        }

        while (MapShieldKeybind.TOGGLE.wasPressed()) {
            boolean enabled = !MapShieldConfig.isEnabled();
            MapShieldConfig.setEnabled(enabled);
            MapShieldConfig.save();

            showStatus(
                    client,
                    enabled
                            ? "MAP SHIELD  •  ✓ ON"
                            : "MAP SHIELD  •  ✕ OFF",
                    enabled
                            ? Formatting.AQUA
                            : Formatting.GRAY,
                    enabled
                            ? Formatting.GREEN
                            : Formatting.RED
            );
        }

        while (MapShieldKeybind.PIN.wasPressed()) {
            pinCurrentMap(client);
        }

        while (MapShieldKeybind.UNPIN.wasPressed()) {
            unpinCurrentMap(client);
        }

        while (MapShieldKeybind.QUICK_PAY.wasPressed()) {
            handleQuickPay(client);
        }
    }


    /**
     * Shift + user-configured QUICK_PAY key opens a real Minecraft chat
     * command prefilled with the player currently under the crosshair.
     * The amount is deliberately left for the user to type.
     *
     * This never sends a payment by itself and never invents an amount.
     */
    private static void handleQuickPay(MinecraftClient client) {
        if (client.player == null || client.currentScreen != null) {
            return;
        }

        net.minecraft.client.util.Window window = client.getWindow();
        boolean shift = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (!shift) {
            return;
        }

        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            showStatus(client, "NO PLAYER TARGET", Formatting.GRAY, Formatting.DARK_GRAY);
            return;
        }

        if (!(hit.getEntity() instanceof PlayerEntity target)) {
            showStatus(client, "AIM AT A PLAYER", Formatting.GRAY, Formatting.DARK_GRAY);
            return;
        }

        String name = target.getGameProfile().name();
        if (name == null || name.isBlank()) {
            return;
        }

        // ChatScreen accepts initial text. We leave the cursor after the
        // player name so the user only has to type the amount and press Enter.
        client.setScreen(new ChatScreen("/pay " + name + " ", true));
    }

    private static void pinCurrentMap(MinecraftClient client) {
        if (!MapShieldConfig.isEnabled()) {
            showStatus(
                    client,
                    "PROTECTION OFF  •  PIN IGNORED",
                    Formatting.GRAY,
                    Formatting.DARK_GRAY
            );
            return;
        }

        Integer mapId = getCrosshairMapId(client);
        if (mapId == null) {
            showStatus(
                    client,
                    "NO MAP ART TARGET",
                    Formatting.GRAY,
                    Formatting.DARK_GRAY
            );
            return;
        }

        boolean changed = MapShieldConfig.addAllowedMap(
                currentServerKey(),
                mapId
        );
        MapShieldConfig.save();

        showStatus(
                client,
                changed
                        ? "ART ALLOWED  ✓"
                        : "ART ALREADY ALLOWED  ✓",
                Formatting.AQUA,
                Formatting.GREEN
        );
    }

    private static void unpinCurrentMap(MinecraftClient client) {
        Integer mapId = getCrosshairMapId(client);
        if (mapId == null) {
            showStatus(
                    client,
                    "NO MAP ART TARGET",
                    Formatting.GRAY,
                    Formatting.DARK_GRAY
            );
            return;
        }

        boolean changed = MapShieldConfig.removeAllowedMap(
                currentServerKey(),
                mapId
        );
        MapShieldConfig.save();

        showStatus(
                client,
                changed
                        ? "ART BLOCKED  ✓"
                        : "ART WAS NOT PINNED",
                Formatting.AQUA,
                changed
                        ? Formatting.RED
                        : Formatting.GRAY
        );
    }

    private static Integer getCrosshairMapId(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof EntityHitResult entityHit)) {
            return null;
        }

        if (!(entityHit.getEntity() instanceof ItemFrameEntity frame)) {
            return null;
        }

        MapIdComponent id =
                frame.getMapId(frame.getHeldItemStack());

        return id == null ? null : id.id();
    }

    public static String currentServerKey() {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo info = client.getCurrentServerEntry();

        if (info != null
                && info.address != null
                && !info.address.isBlank()) {
            return info.address.toLowerCase(Locale.ROOT);
        }

        return "singleplayer";
    }

    private static void showUpdateNotification(
            MinecraftClient client,
            UpdateInfo update
    ) {
        if (client.player == null) {
            return;
        }

        Text message = Text.literal(
                        "◆ PUUZ SECURITY  •  "
                )
                .formatted(
                        Formatting.AQUA,
                        Formatting.BOLD
                )
                .append(
                        Text.literal(
                                "có bản cập nhật mới: "
                        ).formatted(Formatting.WHITE)
                )
                .append(
                        Text.literal(
                                "v" + update.version()
                        ).formatted(
                                Formatting.GREEN,
                                Formatting.BOLD
                        )
                );

        client.player.sendMessage(message, false);
    }

    private static void showStatus(
            MinecraftClient client,
            String label,
            Formatting left,
            Formatting icon
    ) {
        if (client.player == null) {
            return;
        }

        client.player.sendMessage(
                Text.literal("◆ ")
                        .formatted(icon)
                        .append(
                                Text.literal(label)
                                        .formatted(
                                                left,
                                                Formatting.BOLD
                                        )
                        ),
                true
        );
    }
}
