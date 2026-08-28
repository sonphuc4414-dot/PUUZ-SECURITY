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

    public static void register() {
        // Kept intentionally empty; class loading performs one registration.
    }

    private MapShieldKeybind() {
    }
}
