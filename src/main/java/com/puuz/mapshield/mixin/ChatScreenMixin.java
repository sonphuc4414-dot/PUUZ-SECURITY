package com.puuz.mapshield.mixin;

import com.puuz.mapshield.config.MapShieldConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Password Shield for the vanilla chat screen.
 *
 * Two layers are used deliberately:
 * 1. ChatScreen.format() masks normal TextFieldWidget rendering.
 * 2. A final ChatScreen.render() overlay covers the chat input after the
 *    screen has rendered, which makes the visual mask much more resilient
 *    to chat-animation/rendering mods that draw over the text field.
 *
 * The real TextFieldWidget text is never changed, so the server receives
 * the original command and password exactly as typed.
 */
@Mixin(value = ChatScreen.class, priority = 100)
public abstract class ChatScreenMixin {

    @Shadow protected TextFieldWidget chatField;

    @Inject(method = "format", at = @At("HEAD"), cancellable = true)
    private void puuz$maskFormattedText(
            String text,
            int firstCharacterIndex,
            CallbackInfoReturnable<OrderedText> cir
    ) {
        if (!MapShieldConfig.isPasswordMaskEnabled()) {
            return;
        }

        String masked = MapShieldConfig.maskChatInput(text);
        if (!masked.equals(text)) {
            cir.setReturnValue(Text.literal(masked).asOrderedText());
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void puuz$overlayMaskedInput(
            DrawContext context,
            int mouseX,
            int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        if (!MapShieldConfig.isPasswordMaskEnabled()) {
            return;
        }

        if (chatField == null) {
            return;
        }

        String text = chatField.getText();
        if (text == null || text.isEmpty()) {
            return;
        }

        String masked = MapShieldConfig.maskChatInput(text);
        if (masked.equals(text)) {
            return;
        }

        /*
         * Privacy curtain: while a password command is being typed, cover the
         * whole game viewport after the normal chat/HUD has rendered. This is
         * intentional: third-party chat-animation, tooltip, map-preview and
         * HUD mods can render outside ChatScreen's text field. Covering the
         * viewport prevents those overlays from exposing the password.
         *
         * The command itself remains visible in the input box below, while
         * everything behind it becomes a quiet, neutral surface.
         */
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int screenHeight = MinecraftClient.getInstance().getWindow().getScaledHeight();
        context.fill(0, 0, screenWidth, screenHeight, MapShieldConfig.getPasswordCurtainColor());

        int x = chatField.getX();
        int y = chatField.getY();
        int width = chatField.getWidth();
        int height = chatField.getHeight();

        /*
         * Cover the actual text area. The overlay is intentionally small so
         * it does not hide command suggestion UI above the field.
         */
        // Cover the real chat input bounds (plus a tiny 2px safety margin) so
        // the mask sits exactly on top of the vanilla chat field even when
        // other chat/render mods are active.
        int left = Math.max(0, x - 2);
        int right = Math.min(screenWidth, x + width + 2);
        int top = Math.max(0, y - 2);
        int bottom = Math.min(screenHeight, y + height + 2);

        int maskColor = MapShieldConfig.getPasswordMaskColor();
        context.fill(left, top, right, bottom, maskColor);
        context.fill(left, top, right, top + 1, MapShieldConfig.getPasswordMaskBorderColor());
        context.fill(left, bottom - 1, right, bottom, MapShieldConfig.getPasswordMaskBorderColor());
        context.fill(left, top, left + 1, bottom, MapShieldConfig.getPasswordMaskBorderColor());
        context.fill(right - 1, top, right, bottom, MapShieldConfig.getPasswordMaskBorderColor());

        String visible = masked;
        int maxWidth = Math.max(20, width - 8);
        if (net.minecraft.client.MinecraftClient.getInstance()
                .textRenderer.getWidth(visible) > maxWidth) {
            while (!visible.isEmpty()
                    && net.minecraft.client.MinecraftClient.getInstance()
                    .textRenderer.getWidth(visible) > maxWidth) {
                visible = visible.substring(1);
            }
        }

        context.drawText(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                Text.literal(visible),
                left + 3,
                y + Math.max(6, (height - 8) / 2),
                0xFF514C58,
                false
        );
    }
}
