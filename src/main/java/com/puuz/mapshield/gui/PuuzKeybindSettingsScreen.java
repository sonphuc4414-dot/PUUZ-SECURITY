package com.puuz.mapshield.gui;

import com.puuz.mapshield.PuuzMapShieldClient;
import com.puuz.mapshield.keybind.MapShieldKeybind;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Dedicated PUUZ keybind editor so users jump directly to PUUZ bindings. */
public final class PuuzKeybindSettingsScreen extends Screen {
    private static final int BG = 0xFF14161B;
    private static final int SURFACE = 0xFF1B1E25;
    private static final int BORDER = 0xFF343844;
    private static final int TEXT = 0xFFE5E4EA;
    private static final int MUTED = 0xFF9A98A4;
    private static final int ACCENT = 0xFF7C5CBF;

    private static final KeyBinding[] BINDINGS = {
            MapShieldKeybind.SETTINGS,
            MapShieldKeybind.TOGGLE,
            MapShieldKeybind.PIN,
            MapShieldKeybind.UNPIN,
            MapShieldKeybind.QUICK_PAY,
            MapShieldKeybind.QUICK_PAY_AMOUNT
    };

    private final Screen parent;
    private KeyBinding listening;

    public PuuzKeybindSettingsScreen(Screen parent) {
        super(Text.literal("PUUZ SECURITY - Phím tắt"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        clearChildren();

        int panelW = Math.min(760, Math.max(340, width - 32));
        int panelX = (width - panelW) / 2;
        int panelY = Math.max(18, (height - Math.min(560, height - 28)) / 2);
        int y = panelY + 84;
        int rowH = 38;
        int gap = 8;

        for (KeyBinding binding : BINDINGS) {
            int buttonW = Math.min(170, Math.max(120, panelW / 4));
            int nameW = panelW - buttonW - 44;
            String readable = readableName(binding);

            addDrawableChild(ButtonWidget.builder(Text.literal(readable), b -> startListening(binding))
                    .dimensions(panelX + 12, y, Math.max(150, nameW), rowH)
                    .build());

            addDrawableChild(ButtonWidget.builder(
                            listening == binding ? Text.literal("Nhấn phím...") : binding.getBoundKeyLocalizedText(),
                            b -> startListening(binding))
                    .dimensions(panelX + panelW - buttonW - 12, y, buttonW, rowH)
                    .build());

            y += rowH + gap;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Đặt lại tất cả"), b -> {
            for (KeyBinding binding : BINDINGS) {
                binding.setBoundKey(binding.getDefaultKey());
            }
            save();
            listening = null;
            rebuild();
        }).dimensions(panelX, Math.min(height - 44, y + 8), 130, 30).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Quay lại"), b -> close())
                .dimensions(panelX + panelW - 120, Math.min(height - 44, y + 8), 120, 30)
                .build());
    }

    private String readableName(KeyBinding binding) {
        return switch (binding.getId()) {
            case "key.puuz_map_shield.settings" -> "Mở Settings";
            case "key.puuz_map_shield.toggle" -> "Bật / tắt Map Shield";
            case "key.puuz_map_shield.pin" -> "Cho phép Map Art";
            case "key.puuz_map_shield.unpin" -> "Chặn lại Map Art";
            case "key.puuz_map_shield.quick_pay" -> "Quick Pay";
            case "key.puuz_map_shield.quick_pay_amount" -> "Nhập tiền Quick Pay";
            default -> binding.getId();
        };
    }

    private void startListening(KeyBinding binding) {
        listening = binding;
        rebuild();
    }

    private void rebuild() {
        init();
    }

    private void save() {
        if (client != null) {
            client.options.write();
            KeyBinding.updateKeysByCode();
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listening == null) {
            return super.keyPressed(input);
        }

        if (input.isEscape()) {
            listening = null;
            rebuild();
            return true;
        }

        listening.setBoundKey(InputUtil.fromKeyCode(input));
        save();
        listening = null;
        rebuild();
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int button = click.button();
        if (listening != null && button >= 0) {
            listening.setBoundKey(InputUtil.Type.MOUSE.createFromCode(button));
            save();
            listening = null;
            rebuild();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void close() {
        if (listening != null) {
            listening = null;
            rebuild();
            return;
        }
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BG);
        int panelW = Math.min(760, Math.max(340, width - 32));
        int panelX = (width - panelW) / 2;
        int panelH = Math.min(560, Math.max(340, height - 28));
        int panelY = (height - panelH) / 2;

        context.fill(panelX + 4, panelY + 6, panelX + panelW + 4, panelY + panelH + 6, 0x55000000);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, SURFACE);
        context.fill(panelX, panelY + 68, panelX + panelW, panelY + 69, BORDER);

        Identifier logo = Identifier.of(PuuzMapShieldClient.MOD_ID, "icon.png");
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, logo,
                panelX + 18, panelY + 18, 0, 0, 34, 34, 34, 34);
        context.drawTextWithShadow(textRenderer, Text.literal("PUUZ SECURITY"), panelX + 62, panelY + 20, TEXT);
        context.drawTextWithShadow(textRenderer, Text.literal("Phím tắt"), panelX + 62, panelY + 38, MUTED);
        context.drawTextWithShadow(textRenderer, Text.literal("Tùy chỉnh trực tiếp các phím của PUUZ SECURITY."), panelX + 18, panelY + panelH - 30, MUTED);

        super.render(context, mouseX, mouseY, delta);
    }
}
