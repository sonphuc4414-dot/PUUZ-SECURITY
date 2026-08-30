package com.puuz.mapshield.money;

import com.puuz.mapshield.PuuzMapShieldClient;
import com.puuz.mapshield.config.MapShieldConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Lightweight HUD renderer; no per-frame allocations beyond the visible lines. */
public final class MoneyHistoryHud {
    private static long lastTransactionCount;
    private static long animationStart;

    private MoneyHistoryHud() {}

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!MapShieldConfig.isMoneyHistoryEnabled()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        List<MoneyHistory.Transaction> transactions = MoneyHistory.getForCurrentServer();
        long count = transactions.size();
        if (count != lastTransactionCount) {
            lastTransactionCount = count;
            animationStart = System.currentTimeMillis();
        }

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        float scale = MapShieldConfig.getMoneyScale();
        int width = Math.round(MapShieldConfig.getMoneyWidth() * scale);
        int height = Math.round(MapShieldConfig.getMoneyHeight() * scale);
        int x = Math.round((screenW - width) * MapShieldConfig.getMoneyX());
        int y = Math.round((screenH - height) * MapShieldConfig.getMoneyY());
        x = Math.max(4, Math.min(x, screenW - width - 4));
        y = Math.max(4, Math.min(y, screenH - height - 4));

        float appear = Math.min(1.0f, (System.currentTimeMillis() - animationStart) / 220.0f);
        int slide = Math.round((1.0f - easeOut(appear)) * 7.0f);
        y += slide;

        int accent = MapShieldConfig.getAccentColor();
        int muted = withAlpha(MapShieldConfig.getTextColor(), 175);
        int text = MapShieldConfig.getTextColor();
        MoneyHistoryStyle style = MoneyHistoryStyle.from(MapShieldConfig.getMoneyStyle());
        int opacity = MapShieldConfig.getMoneyOpacity();

        if (style != MoneyHistoryStyle.TEXT) {
            int panel = (Math.round(255 * (opacity / 100.0f)) << 24) | 0x00FCFBFE;
            int radius = style == MoneyHistoryStyle.PILL ? 18 : 12;
            rounded(context, x + 3, y + 4, x + width + 3, y + height + 4, radius, 0x19000000);
            rounded(context, x, y, x + width, y + height, radius, panel);
            if (style == MoneyHistoryStyle.OUTLINE || style == MoneyHistoryStyle.GLASS) {
                stroke(context, x, y, x + width, y + height, withAlpha(accent, 210));
            } else {
                stroke(context, x, y, x + width, y + height, withAlpha(0xFFD8D0E0, 220));
            }
            context.fill(x + 14, y + height - 2, x + Math.min(width - 14, 88), y + height, accent);
        }

        context.drawTextWithShadow(client.textRenderer,
                Text.literal("ᴘᴀʏᴍᴇɴᴛ ʜɪsᴛᴏʀʏ").formatted(Formatting.BOLD),
                x + 14, y + 10, text);

        int lineY = y + 28;
        if (MapShieldConfig.isMoneyBalanceVisible()) {
            String balance = MoneyHistory.getBalance();
            String balanceText = balance.isBlank() ? "—" : balance;
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("ʙᴀʟᴀɴᴄᴇ").formatted(Formatting.BOLD), x + 14, lineY, muted);
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal(balanceText), x + width - 14 - client.textRenderer.getWidth(balanceText), lineY, accent);
            lineY += 17;
        }

        int shown = 0;
        for (int i = transactions.size() - 1; i >= 0 && shown < MapShieldConfig.getMoneyVisibleEntries(); i--) {
            MoneyHistory.Transaction tx = transactions.get(i);
            boolean sent = MoneyHistory.TransactionDirection.SENT.name().equals(tx.direction());
            if (sent && !MapShieldConfig.isMoneySentVisible()) continue;
            if (!sent && !MapShieldConfig.isMoneyReceivedVisible()) continue;

            String arrow = sent ? "↑" : "↓";
            String name = MapShieldConfig.isMoneyShowNames() ? tx.player() : "transaction";
            String amount = MapShieldConfig.isMoneyShowAmounts() ? tx.amount() : "";
            String left = arrow + "  " + name;
            int color = sent ? 0xFF8B6873 : 0xFF5E8E76;
            context.drawTextWithShadow(client.textRenderer, left, x + 14, lineY, text);
            if (!amount.isBlank()) {
                context.drawTextWithShadow(client.textRenderer, amount,
                        x + width - 14 - client.textRenderer.getWidth(amount), lineY, color);
            }
            lineY += 16;
            shown++;
            if (lineY > y + height - 14) break;
        }

        if (transactions.isEmpty()) {
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("No transactions yet"), x + 14, lineY, muted);
        }
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static float easeOut(float t) {
        float i = 1.0f - t;
        return 1.0f - i * i * i;
    }

    private static void rounded(DrawContext c, int l, int t, int r, int b, int rad, int color) {
        int rr = Math.min(rad, Math.min(r - l, b - t) / 2);
        c.fill(l + rr, t, r - rr, b, color);
        c.fill(l, t + rr, r, b - rr, color);
    }

    private static void stroke(DrawContext c, int l, int t, int r, int b, int color) {
        c.fill(l + 10, t, r - 10, t + 1, color);
        c.fill(l + 10, b - 1, r - 10, b, color);
        c.fill(l, t + 10, l + 1, b - 10, color);
        c.fill(r - 1, t + 10, r, b - 10, color);
    }
}
