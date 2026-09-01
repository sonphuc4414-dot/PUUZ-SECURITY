package com.puuz.mapshield.gui;

import com.puuz.mapshield.config.MapShieldConfig;
import com.puuz.mapshield.money.MoneyHistory;
import com.puuz.mapshield.money.MoneyHistoryStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Full-screen HUD editor: the settings menu is hidden while the player drags
 * or resizes the Money History widget over the live Minecraft world.
 */
public final class PuuzHudEditorScreen extends Screen {
    private final Screen parent;
    private boolean dragging;
    private boolean resizing;
    private double grabX;
    private double grabY;
    private int startX;
    private int startY;
    private int startW;
    private int startH;

    public PuuzHudEditorScreen(Screen parent) {
        super(Text.literal("PUUZ HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Lưu"), b -> closeEditor())
                .dimensions(width - 196, height - 42, 86, 28).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Hủy"), b -> cancelEditor())
                .dimensions(width - 100, height - 42, 86, 28).build());
    }

    private int hudWidth() {
        return Math.max(160, Math.min(MapShieldConfig.getMoneyWidth(), width - 32));
    }

    private int hudHeight() {
        return Math.max(80, Math.min(MapShieldConfig.getMoneyHeight(), height - 72));
    }

    private int hudX() {
        int w = hudWidth();
        return Math.max(8, Math.min(Math.round((width - w) * MapShieldConfig.getMoneyX()), width - w - 8));
    }

    private int hudY() {
        int h = hudHeight();
        return Math.max(8, Math.min(Math.round((height - h) * MapShieldConfig.getMoneyY()), height - h - 54));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Intentionally do not draw a Screen background: the live Minecraft world remains visible.
        int x = hudX();
        int y = hudY();
        int w = hudWidth();
        int h = hudHeight();

        // Gentle editor overlay.
        context.fill(0, 0, width, 42, 0x660F1014);
        context.fill(0, height - 54, width, height, 0x660F1014);
        context.drawTextWithShadow(textRenderer, Text.literal("PUUZ HUD • Kéo để di chuyển • Kéo góc để đổi kích thước"), 14, 14, MapShieldConfig.getTextColor());

        drawHudPreview(context, x, y, w, h);
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawHudPreview(DrawContext context, int x, int y, int w, int h) {
        int accent = MapShieldConfig.getAccentColor();
        MoneyHistoryStyle style = MoneyHistoryStyle.from(MapShieldConfig.getMoneyStyle());

        if (style != MoneyHistoryStyle.TEXT) {
            int bg = (Math.round(230 * (MapShieldConfig.getMoneyOpacity() / 100.0f)) << 24)
                    | (MapShieldConfig.getMenuCardColor() & 0x00FFFFFF);
            rounded(context, x, y, x + w, y + h, style == MoneyHistoryStyle.PILL ? 18 : 12, bg);
            stroke(context, x, y, x + w, y + h, accent);
        }

        context.drawTextWithShadow(textRenderer, Text.literal("ᴘᴀʏᴍᴇɴᴛ ʜɪsᴛᴏʀʏ"), x + 14, y + 12, MapShieldConfig.getTextColor());
        int line = y + 31;
        if (MapShieldConfig.isMoneyBalanceVisible()) {
            context.drawTextWithShadow(textRenderer, Text.literal("ʙᴀʟᴀɴᴄᴇ"), x + 14, line, withAlpha(MapShieldConfig.getTextColor(), 175));
            context.drawTextWithShadow(textRenderer, Text.literal("$125,420"), x + w - 82, line, accent);
            line += 17;
        }
        context.drawTextWithShadow(textRenderer, Text.literal("↑ Steve"), x + 14, line, MapShieldConfig.getTextColor());
        context.drawTextWithShadow(textRenderer, Text.literal("-$500"), x + w - 58, line, 0xFFB77E8B);
        line += 16;
        context.drawTextWithShadow(textRenderer, Text.literal("↓ Alex"), x + 14, line, MapShieldConfig.getTextColor());
        context.drawTextWithShadow(textRenderer, Text.literal("+$1,250"), x + w - 70, line, 0xFF77A989);

        // Resize handle.
        context.fill(x + w - 10, y + h - 10, x + w - 3, y + h - 3, withAlpha(accent, 220));
        context.fill(x - 1, y - 1, x + w + 1, y + 1, withAlpha(accent, 130));
        context.fill(x - 1, y + h - 1, x + w + 1, y + h + 1, withAlpha(accent, 130));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, doubled);
        }
        double mx = click.x();
        double my = click.y();
        int x = hudX(), y = hudY(), w = hudWidth(), h = hudHeight();
        if (mx >= x + w - 18 && mx <= x + w + 4 && my >= y + h - 18 && my <= y + h + 4) {
            resizing = true;
            startX = x;
            startY = y;
            startW = w;
            startH = h;
            grabX = mx;
            grabY = my;
            return true;
        }
        if (mx >= x && mx <= x + w && my >= y && my <= y + h) {
            dragging = true;
            startX = x;
            startY = y;
            grabX = mx;
            grabY = my;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (!dragging && !resizing) return super.mouseDragged(click, offsetX, offsetY);
        if (resizing) {
            int w = Math.max(160, Math.min(width - 16, startW + (int) Math.round(click.x() - grabX)));
            int h = Math.max(80, Math.min(height - 70, startH + (int) Math.round(click.y() - grabY)));
            MapShieldConfig.setMoneyWidth(w);
            MapShieldConfig.setMoneyHeight(h);
        } else {
            int newX = startX + (int) Math.round(click.x() - grabX);
            int newY = startY + (int) Math.round(click.y() - grabY);
            int w = hudWidth(), h = hudHeight();
            float nx = width == w ? 0.0f : (float) Math.max(8, Math.min(newX, width - w - 8)) / (float) (width - w);
            float ny = height == h ? 0.0f : (float) Math.max(8, Math.min(newY, height - h - 54)) / (float) (height - h - 54);
            MapShieldConfig.setMoneyX(nx);
            MapShieldConfig.setMoneyY(ny);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging || resizing) {
            dragging = false;
            resizing = false;
            MapShieldConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    private void closeEditor() {
        MapShieldConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void cancelEditor() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void close() {
        closeEditor();
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }
    private static void rounded(DrawContext c, int l, int t, int r, int b, int rad, int color) {
        int rr = Math.min(rad, Math.min(r - l, b - t) / 2);
        c.fill(l + rr, t, r - rr, b, color);
        c.fill(l, t + rr, r, b - rr, color);
    }
    private static void stroke(DrawContext c, int l, int t, int r, int b, int color) {
        c.fill(l + 10, t, r - 10, t + 1, withAlpha(color, 190));
        c.fill(l + 10, b - 1, r - 10, b, withAlpha(color, 190));
        c.fill(l, t + 10, l + 1, b - 10, withAlpha(color, 190));
        c.fill(r - 1, t + 10, r, b - 10, withAlpha(color, 190));
    }
}
