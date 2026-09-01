package com.puuz.mapshield.keybind;

import com.puuz.mapshield.PuuzMapShieldClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Standard Minecraft keybind; users can change it in Options -> Controls. */
public final class MapShieldKeybind {
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
            Identifier.of(PuuzMapShieldClient.MOD_ID, "category")
    );

    public static final KeyBinding TOGGLE = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.puuz_map_shield.toggle",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_F8,
                    CATEGORY
            )
    );

    public static final KeyBinding PIN = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.puuz_map_shield.pin",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_P,
                    CATEGORY
            )
    );

    public static final KeyBinding SETTINGS = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.puuz_map_shield.settings",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    CATEGORY
            )
    );

    /**
     * Optional quick-pay action. Default is NONE so PUUZ SECURITY never
     * silently reserves a gameplay key. Hold Shift while pressing it.
     */
    public static final KeyBinding QUICK_PAY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.puuz_map_shield.quick_pay",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    CATEGORY
            )
    );

    /**
     * Optional future-friendly amount shortcut. Default is NONE.
     * It is intentionally independent from QUICK_PAY so users can bind it
     * without changing the default target-selection behaviour.
     */
    public static final KeyBinding QUICK_PAY_AMOUNT = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.puuz_map_shield.quick_pay_amount",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    CATEGORY
            )
    );

    public static final KeyBinding UNPIN = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    "key.puuz_map_shield.unpin",
                    InputUtil.Type.KEYSYM,
                    GLFW.GLFW_KEY_U,
                    CATEGORY
            )
    );

    public static void register() {
        // Kept intentionally empty; class loading performs one registration.
    }

    private MapShieldKeybind() {
    }
}
