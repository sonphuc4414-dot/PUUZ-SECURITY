package com.puuz.mapshield.gui;

import com.puuz.mapshield.PuuzMapShieldClient;
import com.puuz.mapshield.config.MapShieldConfig;
import com.puuz.mapshield.update.UpdateChecker;
import com.puuz.mapshield.money.MoneyHistory;
import com.puuz.mapshield.money.MoneyHistoryStyle;
import com.puuz.mapshield.map.MapHideTextureManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ControlsOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Responsive, soft-light settings UI for PUUZ SECURITY.
 *
 * Design goals:
 * - Never overlap controls across supported screen sizes.
 * - Use vanilla widgets for all input/focus/click handling.
 * - Avoid blur so it remains compatible with other UI/visual mods.
 * - Keep visuals light, calm and smooth rather than cyber/gaming themed.
 */
public final class PuuzSecuritySettingsScreen extends Screen {
    private static final int APP_BG = 0xFFF3F0F6;
    private static final int PANEL = 0xFFFEFCFF;
    private static final int PANEL_SOFT = 0xFFF8F5FA;
    private static final int BORDER = 0xFFD7CFDE;
    private static final int SUCCESS = 0xFF739B86;
    private static final int DANGER = 0xFFB57C87;
    private static final int SHADOW = 0x26000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final long OPEN_ANIMATION_MS = 260L;

    private final Screen parent;
    private Tab selectedTab = Tab.GENERAL;

    private final List<ButtonWidget> interactiveButtons = new ArrayList<>();
    private final List<VisualButton> visualButtons = new ArrayList<>();
    private final List<InfoCard> cards = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();

    private TextFieldWidget commandField;
    private TextFieldWidget backgroundField;
    private TextFieldWidget accentHexField;
    private TextFieldWidget textHexField;
    private TextFieldWidget backgroundHexField;
    private TextFieldWidget hideMapImageField;

    private Identifier backgroundTextureId;
    private NativeImageBackedTexture backgroundTexture;
    private int backgroundWidth;
    private int backgroundHeight;
    private final List<Path> backgroundFiles = new ArrayList<>();

    // Scroll state for the active settings page. Widgets are rebuilt at this
    // offset so their hitboxes stay aligned with the visual content.
    private double contentScroll = 0.0;
    private double targetContentScroll = 0.0;
    private double maxContentScroll = 0.0;
    private int contentViewportTop;
    private int contentViewportBottom;
    private int contentViewportLeftX;
    private int contentViewportRightX;

    private long animationStartedAt;
    private boolean closing;
    private boolean rebuildRequested;
    private String notice = "";
    private long noticeUntil;

    public PuuzSecuritySettingsScreen(Screen parent) {
        super(Text.literal("PUUZ SECURITY"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        if (animationStartedAt == 0L) {
            animationStartedAt = System.currentTimeMillis();
        }
        closing = false;
        refreshBackgroundFiles();
        loadBackgroundTexture();
        rebuildLayout();
    }

    @Override
    public void tick() {
        super.tick();

        if (rebuildRequested && !closing) {
            rebuildRequested = false;
            rebuildLayout();
        }

        if (Math.abs(targetContentScroll - contentScroll) > 0.05) {
            double oldScroll = contentScroll;
            contentScroll += (targetContentScroll - contentScroll) * 0.28;
            if (Math.abs(targetContentScroll - contentScroll) < 0.08) {
                contentScroll = targetContentScroll;
            }

            int delta = (int) Math.round(oldScroll - contentScroll);
            if (delta != 0) {
                shiftContentElements(delta);
                updateWidgetVisibility(Layout.calculate(width, height));
            }
        }

        if (closing && animationProgress() <= 0.0f) {
            disposeBackgroundTexture();
            MinecraftClient.getInstance().setScreen(parent);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        rebuildLayout();
    }

    private void rebuildLayout() {
        clearChildren();
        interactiveButtons.clear();
        visualButtons.clear();
        cards.clear();
        labels.clear();
        drawPreviewRequests.clear();
        commandField = null;
        backgroundField = null;
        accentHexField = null;
        textHexField = null;
        backgroundHexField = null;
        hideMapImageField = null;

        Layout l = Layout.calculate(width, height);
        contentViewportTop = l.contentY;
        contentViewportBottom = l.contentBottom;
        contentViewportLeftX = l.contentX;
        contentViewportRightX = l.right - 10;
        buildTabs(l);
        buildPage(l);
        updateContentScrollBounds(l);
        if (contentScroll > 0.0) {
            shiftContentElements(-(int) Math.round(contentScroll));
        }
        targetContentScroll = Math.max(0.0, Math.min(targetContentScroll, maxContentScroll));
        contentScroll = Math.max(0.0, Math.min(contentScroll, maxContentScroll));
        updateWidgetVisibility(l);
        buildFooter(l);
    }

    private void buildTabs(Layout l) {
        if (l.compact) {
            int columns = l.contentWidth >= 560 ? 4 : (l.contentWidth >= 420 ? 3 : 2);
            int gap = 7;
            int tabW = Math.max(86, (l.contentWidth - gap * (columns - 1)) / columns);
            int tabH = 28;
            int startY = l.tabsY;

            for (int i = 0; i < Tab.values().length; i++) {
                Tab tab = Tab.values()[i];
                int col = i % columns;
                int row = i / columns;
                addFixedButton(
                        l.contentX + col * (tabW + gap),
                        startY + row * (tabH + gap),
                        tabW,
                        tabH,
                        Text.literal(tab == selectedTab ? "● " + tab.label : tab.label),
                        b -> selectTab(tab),
                        tab == selectedTab ? MapShieldConfig.getAccentColor() : BORDER,
                        true
                );
            }
            return;
        }

        int x = l.sidebarX + 12;
        int y = l.sidebarY + 58;
        int w = l.sidebarWidth - 24;
        int h = 30;
        int gap = 7;

        for (Tab tab : Tab.values()) {
            addFixedButton(
                    x,
                    y,
                    w,
                    h,
                    Text.literal(tab == selectedTab ? "●  " + tab.label : tab.label),
                    b -> selectTab(tab),
                    tab == selectedTab ? MapShieldConfig.getAccentColor() : BORDER,
                    true
            );
            y += h + gap;
        }
    }

    private void buildPage(Layout l) {
        int x = l.contentX + 8;
        int y = l.contentY + 6;
        int w = l.contentWidth - 16;

        labels.add(new Label(x, y, selectedTab.title, true));
        labels.add(new Label(x, y + 19, selectedTab.subtitle, false));

        int bodyY = y + 42;
        switch (selectedTab) {
            case GENERAL -> buildGeneral(x, bodyY, w, l);
            case MAP_ART -> buildMapArt(x, bodyY, w, l);
            case PASSWORD -> buildPassword(x, bodyY, w, l);
            case APPEARANCE -> buildAppearance(x, bodyY, w, l);
            case UPDATES -> buildUpdates(x, bodyY, w, l);
            case CONTROLS -> buildControls(x, bodyY, w, l);
            case MONEY -> buildMoney(x, bodyY, w, l);
            case ABOUT -> buildAbout(x, bodyY, w, l);
        }
    }

    private void buildGeneral(int x, int y, int w, Layout l) {
        int gap = 10;
        int cardH = l.compact ? 48 : 54;
        int halfGap = 10;
        int halfW = (w - halfGap) / 2;

        addToggleCard(x, y, halfW, cardH, "Ẩn Map Art", "Giấu Map Art phía client.", MapShieldConfig.isEnabled(), MapShieldConfig.getAccentColor(), this::toggleMapShield);
        addToggleCard(x + halfW + halfGap, y, w - halfW - halfGap, cardH, "Che mật khẩu", "Ẩn tham số login/register.", MapShieldConfig.isPasswordMaskEnabled(), SUCCESS, this::togglePassword);
        addToggleCard(x, y + cardH + gap, halfW, cardH, "Tự động cập nhật", "Theo dõi GitHub Releases.", MapShieldConfig.isUpdateCheckerEnabled(), MapShieldConfig.getAccentColor(), this::toggleUpdates);
        addStyledButton(x + halfW + halfGap, y + cardH + gap, w - halfW - halfGap, cardH,
                Text.literal("Mở Minecraft Controls"), b -> openControls(), MapShieldConfig.getAccentColor(), false);

        addInfoCard(x, y + 2 * (cardH + gap) + 8, w, l.compact ? 78 : 88,
                "Thiết lập nhanh",
                "Mọi thay đổi có thể chỉnh lại trong các mục bên trái.\nPUUZ SECURITY ưu tiên giao diện nhẹ, rõ ràng và không làm ảnh hưởng gameplay.");
    }

    private void buildMapArt(int x, int y, int w, Layout l) {
        int cardH = l.compact ? 50 : 56;
        addToggleCard(x, y, w, cardH, "Hide Map Art", "Bật/tắt lớp bảo vệ Map Art phía client.", MapShieldConfig.isEnabled(), MapShieldConfig.getAccentColor(), this::toggleMapShield);
        addToggleCard(x, y + cardH + 10, w, cardH, "Chặn Map Art trong Tooltip", "Ẩn riêng preview Map Art của tooltip hỗ trợ.", MapShieldConfig.isMapTooltipPreviewBlocked(), MapShieldConfig.getAccentColor(), this::toggleMapTooltipPreview);

        int sectionY = y + 2 * (cardH + 10) + 8;
        labels.add(new Label(x, sectionY, "Ảnh khi Map Art bị ẩn", true));
        labels.add(new Label(x, sectionY + 18, "Để trống để dùng ảnh ẩn mặc định của PUUZ SECURITY.", false));

        int previewSize = Math.min(118, Math.max(84, w / 4));
        int previewX = x;
        int previewY = sectionY + 34;
        drawPreviewRequests.add(new PreviewRequest(previewX, previewY, previewSize));

        int fieldX = x + previewSize + 12;
        int fieldW = Math.max(120, w - previewSize - 12);
        hideMapImageField = addDrawableChild(new TextFieldWidget(textRenderer, fieldX, previewY, fieldW, 30, Text.literal("PNG/JPG path")));
        hideMapImageField.setMaxLength(512);
        hideMapImageField.setText(MapShieldConfig.getHideMapImagePath());
        hideMapImageField.setPlaceholder(Text.literal("Đường dẫn ảnh thay thế"));

        addStyledButton(fieldX, previewY + 38, fieldW, 30, Text.literal("Áp dụng ảnh này"), b -> applyHideMapImage(), MapShieldConfig.getAccentColor(), false);
        int half = Math.max(80, (fieldW - 8) / 2);
        addStyledButton(fieldX, previewY + 76, half, 28, Text.literal("Mở thư mục"), b -> openHideMapFolder(), BORDER, false);
        addStyledButton(fieldX + half + 8, previewY + 76, fieldW - half - 8, 28, Text.literal("Khôi phục mặc định"), b -> resetHideMapImage(), BORDER, false);

        int infoY = previewY + Math.max(112, previewSize + 10);
        addInfoCard(x, infoY, w, l.compact ? 76 : 88, "Xem trước ảnh hide", "Ảnh bên trái là ảnh PUUZ sẽ dùng để thay Map Art bị chặn.\nNút khôi phục sẽ trở về ảnh mặc định.");
        addInfoCard(x, infoY + (l.compact ? 88 : 100), w, l.compact ? 86 : 98, "Phím tắt", "F8  ·  Bật/tắt Map Shield\nP  ·  Cho phép Map Art hiện tại\nU  ·  Gỡ cho phép Map Art hiện tại");
    }

    private final List<PreviewRequest> drawPreviewRequests = new ArrayList<>();
    private record PreviewRequest(int x, int y, int size) {}

    private void applyHideMapImage() {
        if (hideMapImageField == null) return;
        String value = hideMapImageField.getText().trim();
        MapShieldConfig.setHideMapImagePath(value);
        MapShieldConfig.save();
        boolean loaded = MapHideTextureManager.reload();
        setNotice(loaded ? "Đã áp dụng ảnh Map Shield." : (value.isBlank() ? "Đã khôi phục ảnh mặc định." : "Không đọc được ảnh; đang dùng ảnh mặc định."));
        rebuildRequested = true;
    }

    private void openHideMapFolder() {
        try {
            Path folder = FabricLoader.getInstance().getConfigDir().resolve("puuz-security").resolve("map-hidden");
            Files.createDirectories(folder);
            net.minecraft.util.Util.getOperatingSystem().open(folder.toFile());
            setNotice("Đã mở thư mục map-hidden.");
        } catch (Exception ignored) {
            setNotice("Không mở được thư mục map-hidden.");
        }
    }

    private void resetHideMapImage() {
        MapShieldConfig.setHideMapImagePath("");
        MapShieldConfig.save();
        MapHideTextureManager.reload();
        if (hideMapImageField != null) hideMapImageField.setText("");
        setNotice("Đã khôi phục ảnh hide mặc định.");
        rebuildRequested = true;
    }

    private void toggleMapTooltipPreview(ButtonWidget button) {
        MapShieldConfig.setMapTooltipPreviewBlocked(!MapShieldConfig.isMapTooltipPreviewBlocked());
        MapShieldConfig.save();
        setNotice(MapShieldConfig.isMapTooltipPreviewBlocked() ? "Đã chặn Map Art preview trong tooltip." : "Đã cho phép Map Art preview trong tooltip.");
        rebuildRequested = true;
    }

    private void buildPassword(int x, int y, int w, Layout l) {
        int cardH = l.compact ? 50 : 56;
        addToggleCard(x, y, w, cardH, "Password Shield", "Che phần mật khẩu trong command đã chọn.", MapShieldConfig.isPasswordMaskEnabled(), SUCCESS, this::togglePassword);

        int rowY = y + cardH + 12;
        int addW = Math.min(88, Math.max(74, w / 5));
        int fieldW = Math.max(100, w - addW - 8);

        commandField = addDrawableChild(new TextFieldWidget(
                textRenderer,
                x,
                rowY,
                fieldW,
                30,
                Text.literal("/login")
        ));
        commandField.setMaxLength(64);
        commandField.setPlaceholder(Text.literal("Thêm command, ví dụ /auth"));

        addStyledButton(
                x + fieldW + 8,
                rowY,
                addW,
                30,
                Text.literal("Thêm"),
                b -> addCommand(),
                MapShieldConfig.getAccentColor(),
                false
        );

        int buttonY = rowY + 40;
        int gap = 8;
        int half = (w - gap) / 2;
        addStyledButton(x, buttonY, half, 30, Text.literal("Xóa lệnh cuối"), b -> removeLastCommand(), DANGER, false);
        addStyledButton(x + half + gap, buttonY, w - half - gap, 30, Text.literal("Khôi phục mặc định"), b -> resetCommands(), MapShieldConfig.getAccentColor(), false);

        String commands = String.join("   ", MapShieldConfig.getPasswordCommands());
        String body = "Đang bảo vệ: " + commands + "\nVí dụ:  /login ********";
        addInfoCard(x, buttonY + 42, w, l.compact ? 76 : 92, "Command đang bảo vệ", body);
    }

    private void buildAppearance(int x, int y, int w, Layout l) {
        addInfoCard(
                x,
                y,
                w,
                l.compact ? 66 : 74,
                "Giao diện cá nhân hóa",
                "Màu sắc và ảnh nền áp dụng ngay. Cuộn để xem thêm."
        );

        int sectionY = y + (l.compact ? 80 : 88);
        int gap = 8;
        int cols = l.compact ? 2 : 4;
        int rowH = 36;
        int swatchW = Math.max(74, (w - gap * (cols - 1)) / cols);

        labels.add(new Label(x, sectionY, "Màu nhấn", true));

        int[] accentColors = {
                0xFF8E78B8, 0xFF7D93B8, 0xFF78A08B,
                0xFFC39982, 0xFF7E9FA3, 0xFF9B86A9
        };
        String[] accentNames = {
                "Tím mềm", "Xanh dịu", "Xanh lá",
                "Đào ấm", "Xanh sương", "Tím khói"
        };

        for (int i = 0; i < accentColors.length; i++) {
            int col = i % cols;
            int row = i / cols;
            addColorButton(
                    x + col * (swatchW + gap),
                    sectionY + 18 + row * rowH,
                    swatchW,
                    30,
                    accentNames[i],
                    accentColors[i],
                    true
            );
        }

        int accentRows = (accentColors.length + cols - 1) / cols;
        int textSectionY = sectionY + 18 + accentRows * rowH + 12;
        labels.add(new Label(x, textSectionY, "Màu chữ", true));

        int[] textColors = {
                0xFF56515D, 0xFF56636B, 0xFF56675C,
                0xFF665951, 0xFF625D68, 0xFF4F5E63
        };
        String[] textNames = {
                "Slate", "Xám xanh", "Rêu dịu",
                "Nâu ấm", "Mận nhạt", "Xanh than"
        };

        for (int i = 0; i < textColors.length; i++) {
            int col = i % cols;
            int row = i / cols;
            addColorButton(
                    x + col * (swatchW + gap),
                    textSectionY + 18 + row * rowH,
                    swatchW,
                    30,
                    textNames[i],
                    textColors[i],
                    false
            );
        }

        int textRows = (textColors.length + cols - 1) / cols;
        int bgSectionY = textSectionY + 18 + textRows * rowH + 12;
        labels.add(new Label(x, bgSectionY, "Màu nền", true));

        int[] backgroundColors = {
                0xFFF1EEF5, 0xFFEFF3F5,
                0xFFF1F5EF, 0xFFF7F0EA
        };
        String[] backgroundNames = {
                "Lavender", "Sương xanh", "Xanh kem", "Peach"
        };

        for (int i = 0; i < backgroundColors.length; i++) {
            int col = i % cols;
            int row = i / cols;
            addBackgroundColorButton(
                    x + col * (swatchW + gap),
                    bgSectionY + 18 + row * rowH,
                    swatchW,
                    30,
                    backgroundNames[i],
                    backgroundColors[i]
            );
        }

        int backgroundRows = (backgroundColors.length + cols - 1) / cols;
        int customY = bgSectionY + 18 + backgroundRows * rowH + 14;
        labels.add(new Label(x, customY, "Màu tùy chỉnh (Hex)", true));

        int fieldGap = 8;
        int fieldCols = l.compact ? 1 : 3;
        int fieldW = Math.max(120, (w - fieldGap * (fieldCols - 1)) / fieldCols);
        int fieldY = customY + 18;

        accentHexField = addDrawableChild(new TextFieldWidget(
                textRenderer, x, fieldY, fieldW, 28,
                Text.literal("Accent #RRGGBB")
        ));
        accentHexField.setMaxLength(9);
        accentHexField.setText(toHex(MapShieldConfig.getAccentColor()));
        accentHexField.setPlaceholder(Text.literal("#8E78B8"));

        textHexField = addDrawableChild(new TextFieldWidget(
                textRenderer,
                x + (fieldCols > 1 ? fieldW + fieldGap : 0),
                fieldY + (fieldCols > 1 ? 0 : 34),
                fieldW,
                28,
                Text.literal("Text #RRGGBB")
        ));
        textHexField.setMaxLength(9);
        textHexField.setText(toHex(MapShieldConfig.getTextColor()));
        textHexField.setPlaceholder(Text.literal("#56515D"));

        int bgFieldX = x;
        int bgFieldY = fieldY + (fieldCols > 1 ? 0 : 68);
        if (fieldCols > 2) {
            bgFieldX = x + 2 * (fieldW + fieldGap);
            bgFieldY = fieldY;
        }
        backgroundHexField = addDrawableChild(new TextFieldWidget(
                textRenderer,
                bgFieldX,
                bgFieldY,
                fieldW,
                28,
                Text.literal("Background #RRGGBB")
        ));
        backgroundHexField.setMaxLength(9);
        backgroundHexField.setText(toHex(MapShieldConfig.getBackgroundColor()));
        backgroundHexField.setPlaceholder(Text.literal("#F1EEF5"));

        int applyRowY = fieldY + (fieldCols > 1 ? 34 : 102);
        addStyledButton(
                x,
                applyRowY,
                w,
                30,
                Text.literal("Áp dụng màu tùy chỉnh"),
                b -> applyCustomColors(),
                MapShieldConfig.getAccentColor(),
                false
        );

        int imageSectionY = applyRowY + 42;
        labels.add(new Label(x, imageSectionY, "Ảnh nền", true));
        int rowY = imageSectionY + 18;
        int folderW = Math.min(150, Math.max(110, w / 3));
        int inputW = Math.max(120, w - folderW - 8);

        addStyledButton(
                x,
                rowY,
                folderW,
                30,
                Text.literal("Mở thư mục ảnh"),
                b -> openBackgroundFolder(),
                MapShieldConfig.getAccentColor(),
                false
        );

        backgroundField = addDrawableChild(new TextFieldWidget(
                textRenderer,
                x + folderW + 8,
                rowY,
                inputW,
                30,
                Text.literal("PNG/JPG")
        ));
        backgroundField.setMaxLength(512);
        backgroundField.setText(MapShieldConfig.getBackgroundPath());
        backgroundField.setPlaceholder(
                Text.literal("Đường dẫn PNG/JPG hoặc /path/to/image.png")
        );

        addStyledButton(
                x,
                rowY + 38,
                folderW,
                30,
                Text.literal("Áp dụng ảnh"),
                b -> applyBackgroundPath(),
                MapShieldConfig.getAccentColor(),
                false
        );

        addStyledButton(
                x + folderW + 8,
                rowY + 38,
                inputW,
                30,
                Text.literal("Quét lại thư mục"),
                b -> {
                    refreshBackgroundFiles();
                    rebuildLayout();
                    setNotice("Đã quét lại thư mục ảnh.");
                },
                BORDER,
                false
        );

        int listY = rowY + 76;
        int listGap = 8;
        int listCols = l.compact ? 1 : 2;
        int listW = Math.max(120, (w - listGap * (listCols - 1)) / listCols);
        int visible = Math.min(backgroundFiles.size(), l.compact ? 8 : 12);

        for (int i = 0; i < visible; i++) {
            Path file = backgroundFiles.get(i);
            int col = i % listCols;
            int row = i / listCols;
            boolean selected = MapShieldConfig.getBackgroundPath()
                    .equals(file.toAbsolutePath().toString());
            addStyledButton(
                    x + col * (listW + listGap),
                    listY + row * 32,
                    listW,
                    28,
                    Text.literal(selected
                            ? "✓ " + file.getFileName()
                            : file.getFileName().toString()),
                    b -> selectBackground(file),
                    selected
                            ? MapShieldConfig.getAccentColor()
                            : BORDER,
                    false
            );
        }

        int listRows = Math.max(1, (visible + listCols - 1) / listCols);
        int clearY = listY + listRows * 32 + 4;
        addStyledButton(
                x,
                clearY,
                w,
                28,
                Text.literal("Bỏ ảnh nền"),
                b -> {
                    MapShieldConfig.setBackgroundPath("");
                    MapShieldConfig.save();
                    disposeBackgroundTexture();
                    if (backgroundField != null) {
                        backgroundField.setText("");
                    }
                    setNotice("Đã trở về nền màu.");
                },
                BORDER,
                false
        );

        addInfoCard(
                x,
                clearY + 38,
                w,
                l.compact ? 70 : 78,
                "Mẹo",
                "Ảnh được tự scale để phủ toàn bộ cửa sổ. Nếu ảnh nằm trong backgrounds, hãy bấm Quét lại sau khi chép ảnh vào."
        );
    }

    private void applyCustomColors() {
        Integer accent = parseHexColor(accentHexField == null ? "" : accentHexField.getText());
        Integer text = parseHexColor(textHexField == null ? "" : textHexField.getText());
        Integer background = parseHexColor(backgroundHexField == null ? "" : backgroundHexField.getText());

        if (accent == null || text == null || background == null) {
            setNotice("Màu phải ở dạng #RRGGBB hoặc RRGGBBAA.");
            return;
        }

        MapShieldConfig.setAccentColor(accent);
        MapShieldConfig.setTextColor(text);
        MapShieldConfig.setBackgroundColor(background);
        MapShieldConfig.save();

        rebuildRequested = true;
        setNotice("Đã áp dụng màu tùy chỉnh.");
    }

    private static Integer parseHexColor(String value) {
        if (value == null) {
            return null;
        }

        String s = value.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }

        if (!(s.length() == 6 || s.length() == 8)) {
            return null;
        }

        try {
            long parsed = Long.parseLong(s, 16);
            if (s.length() == 6) {
                return (int) parsed | 0xFF000000;
            }
            return (int) parsed;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String toHex(int color) {
        return String.format("#%06X", color & 0x00FFFFFF);
    }

    private void buildUpdates(int x, int y, int w, Layout l) {
        int cardH = l.compact ? 50 : 56;
        addToggleCard(x, y, w, cardH,
                "Tự động kiểm tra cập nhật",
                "Kiểm tra GitHub Releases ở background.",
                MapShieldConfig.isUpdateCheckerEnabled(),
                MapShieldConfig.getAccentColor(),
                this::toggleUpdates);

        addStyledButton(x, y + cardH + 12, w, 34,
                Text.literal("Kiểm tra ngay"),
                b -> {
                    if (!MapShieldConfig.isUpdateCheckerEnabled()) {
                        setNotice("Hãy bật Update Checker trước.");
                        return;
                    }
                    UpdateChecker.requestCheck();
                    setNotice("Đang kiểm tra phiên bản mới…");
                },
                MapShieldConfig.getAccentColor(), false);

        addInfoCard(x, y + cardH + 58, w, l.compact ? 86 : 96,
                "Nguồn cập nhật",
                "Repository: sonphuc4414-dot/PUUZ-SECURITY\nAPI: /releases/latest\nKhông cần PAT hoặc token.");
    }

    private void buildControls(int x, int y, int w, Layout l) {
        addInfoCard(x, y, w, l.compact ? 94 : 108,
                "Phím tắt của Minecraft",
                "Mọi keybind của PUUZ SECURITY đều được quản lý bằng Minecraft Controls.\nMặc định: K = Settings · F8 = Map Shield · P/U = Map Art.");
        addStyledButton(x, y + (l.compact ? 108 : 122), w, 34,
                Text.literal("Mở Minecraft Controls"), b -> openControls(), MapShieldConfig.getAccentColor(), false);
    }

    private void buildMoney(int x, int y, int w, Layout l) {
        int rowH = l.compact ? 42 : 46;
        int gap = 8;

        addToggleRow(x, y, w, rowH, "Money History", MapShieldConfig.isMoneyHistoryEnabled(), MapShieldConfig.getAccentColor(), b -> toggleMoneyHistory());
        addToggleRow(x, y + rowH + gap, w, rowH, "Hiện Balance", MapShieldConfig.isMoneyBalanceVisible(), SUCCESS, b -> toggleMoneyBalance());
        addToggleRow(x, y + (rowH + gap) * 2, w, rowH, "Hiện tiền đã trả", MapShieldConfig.isMoneySentVisible(), 0xFF9B7180, b -> toggleMoneySent());
        addToggleRow(x, y + (rowH + gap) * 3, w, rowH, "Hiện tiền nhận", MapShieldConfig.isMoneyReceivedVisible(), SUCCESS, b -> toggleMoneyReceived());

        int styleY = y + (rowH + gap) * 4 + 10;
        labels.add(new Label(x, styleY, "Kiểu khung Money History", true));
        MoneyHistoryStyle[] styles = MoneyHistoryStyle.values();
        int styleGap = 7;
        int styleW = Math.max(88, (w - styleGap * 2) / 3);
        for (int i = 0; i < styles.length; i++) {
            int col = i % 3;
            int row = i / 3;
            MoneyHistoryStyle style = styles[i];
            boolean selected = MoneyHistoryStyle.from(MapShieldConfig.getMoneyStyle()) == style;
            addStyledButton(x + col * (styleW + styleGap), styleY + 18 + row * 34, styleW, 28,
                    Text.literal((selected ? "✓ " : "") + style.label()), b -> {
                        MapShieldConfig.setMoneyStyle(style.name());
                        MapShieldConfig.save();
                        setNotice("Kiểu HUD: " + style.label());
                        rebuildRequested = true;
                    }, selected ? MapShieldConfig.getAccentColor() : BORDER, false);
        }

        int opacityY = styleY + 18 + ((styles.length + 2) / 3) * 34 + 8;
        labels.add(new Label(x, opacityY, "Độ trong suốt", true));
        int opW = Math.max(84, (w - 16) / 3);
        addStyledButton(x, opacityY + 18, opW, 30, Text.literal("60%"), b -> setMoneyOpacity(60), MapShieldConfig.getAccentColor(), false);
        addStyledButton(x + opW + 8, opacityY + 18, opW, 30, Text.literal("80%"), b -> setMoneyOpacity(80), MapShieldConfig.getAccentColor(), false);
        addStyledButton(x + 2 * (opW + 8), opacityY + 18, Math.max(70, w - 2 * (opW + 8)), 30, Text.literal("100%"), b -> setMoneyOpacity(100), MapShieldConfig.getAccentColor(), false);

        int posY = opacityY + 60;
        labels.add(new Label(x, posY, "Vị trí HUD", true));
        int bw = Math.max(82, (w - 16) / 3);
        addStyledButton(x, posY + 18, bw, 30, Text.literal("↖ Góc trái"), b -> setMoneyPosition(0.02f, 0.03f), MapShieldConfig.getAccentColor(), false);
        addStyledButton(x + bw + 8, posY + 18, bw, 30, Text.literal("↗ Góc phải"), b -> setMoneyPosition(0.73f, 0.03f), MapShieldConfig.getAccentColor(), false);
        addStyledButton(x, posY + 54, bw, 30, Text.literal("↙ Trái dưới"), b -> setMoneyPosition(0.02f, 0.72f), MapShieldConfig.getAccentColor(), false);
        addStyledButton(x + bw + 8, posY + 54, bw, 30, Text.literal("↘ Phải dưới"), b -> setMoneyPosition(0.73f, 0.72f), MapShieldConfig.getAccentColor(), false);
        addStyledButton(x + 2 * (bw + 8), posY + 18, Math.max(70, w - 2 * (bw + 8)), 66, Text.literal("Tâm"), b -> setMoneyPosition(0.50f, 0.40f), MapShieldConfig.getAccentColor(), false);

        int sizeY = posY + 98;
        labels.add(new Label(x, sizeY, "Kích thước", true));
        int sizeW = Math.max(80, (w - 16) / 3);
        addStyledButton(x, sizeY + 18, sizeW, 30, Text.literal("Compact"), b -> setMoneySize(210, 96, 0.85f), MapShieldConfig.getAccentColor(), false);
        addStyledButton(x + sizeW + 8, sizeY + 18, sizeW, 30, Text.literal("Balanced"), b -> setMoneySize(240, 118, 1.0f), MapShieldConfig.getAccentColor(), false);
        addStyledButton(x + 2 * (sizeW + 8), sizeY + 18, Math.max(70, w - 2 * (sizeW + 8)), 30, Text.literal("Large"), b -> setMoneySize(300, 150, 1.15f), MapShieldConfig.getAccentColor(), false);

        int clearY = sizeY + 62;
        addStyledButton(x, clearY, w, 30, Text.literal("Xóa toàn bộ lịch sử giao dịch"), b -> {
            MoneyHistory.clearAllHistory();
            setNotice("Đã xóa toàn bộ lịch sử local.");
        }, DANGER, false);
        addInfoCard(x, clearY + 40, w, l.compact ? 70 : 82, "Lưu trữ local", "Lịch sử giao dịch được lưu mãi trên máy người dùng và không có giới hạn số record.\nĐếm hiện tại: " + MoneyHistory.getTotalStoredTransactions());
    }

    private void toggleMoneyHistory() {
        MapShieldConfig.setMoneyHistoryEnabled(!MapShieldConfig.isMoneyHistoryEnabled());
        MapShieldConfig.save();
        setNotice(MapShieldConfig.isMoneyHistoryEnabled() ? "Money History đã bật." : "Money History đã tắt.");
        rebuildRequested = true;
    }
    private void toggleMoneyBalance() { MapShieldConfig.setMoneyBalanceVisible(!MapShieldConfig.isMoneyBalanceVisible()); MapShieldConfig.save(); rebuildRequested = true; }
    private void toggleMoneySent() { MapShieldConfig.setMoneySentVisible(!MapShieldConfig.isMoneySentVisible()); MapShieldConfig.save(); rebuildRequested = true; }
    private void toggleMoneyReceived() { MapShieldConfig.setMoneyReceivedVisible(!MapShieldConfig.isMoneyReceivedVisible()); MapShieldConfig.save(); rebuildRequested = true; }
    private void setMoneyOpacity(int value) { MapShieldConfig.setMoneyOpacity(value); MapShieldConfig.save(); setNotice("Độ trong suốt: " + value + "%."); rebuildRequested = true; }

    private void addToggleRow(int x, int y, int w, int h, String title, boolean enabled, int accent, ButtonWidget.PressAction action) {
        int stateW = Math.min(88, Math.max(72, w / 5));
        addStyledButton(x, y, w - stateW - 8, h, Text.literal(title), action, accent, false);
        addStyledButton(x + w - stateW, y, stateW, h, Text.literal(enabled ? "BẬT" : "TẮT"), action, enabled ? accent : BORDER, false);
    }

    private void setMoneyPosition(float x, float y) {
        MapShieldConfig.setMoneyX(x);
        MapShieldConfig.setMoneyY(y);
        MapShieldConfig.save();
        setNotice("Đã đổi vị trí Money History.");
    }

    private void setMoneySize(int width, int height, float scale) {
        MapShieldConfig.setMoneyWidth(width);
        MapShieldConfig.setMoneyHeight(height);
        MapShieldConfig.setMoneyScale(scale);
        MapShieldConfig.save();
        setNotice("Đã đổi kích thước Money History.");
    }

    private void buildAbout(int x, int y, int w, Layout l) {
        addInfoCard(x, y, w, l.compact ? 120 : 136,
                "PUUZ SECURITY",
                "Client-side Fabric mod tập trung vào Map Art và bảo vệ mật khẩu khi live.\n\nPhiên bản: 1.2.0\nTrạng thái: Big Update\n\nDesigned for a clean, calm and personal setup experience.");
        addStyledButton(x, y + (l.compact ? 134 : 150), w, 34,
                Text.literal("Mở GitHub"), b -> net.minecraft.util.Util.getOperatingSystem().open("https://github.com/sonphuc4414-dot/PUUZ-SECURITY"), MapShieldConfig.getAccentColor(), false);
    }

    private void buildFooter(Layout l) {
        int gap = 8;
        int buttonW = Math.min(120, Math.max(90, (l.contentWidth - gap) / 4));
        int y = l.bottom - 40;
        int right = l.right - 16;

        addFixedButton(
                right - buttonW * 2 - gap,
                y,
                buttonW,
                30,
                Text.literal("Đóng"),
                b -> beginClose(),
                BORDER,
                false
        );

        addFixedButton(
                right - buttonW,
                y,
                buttonW,
                30,
                Text.literal("Lưu"),
                b -> saveAll(),
                MapShieldConfig.getAccentColor(),
                false
        );
    }

    private void addToggleCard(int x, int y, int w, int h, String title, String subtitle, boolean enabled, int accent, ToggleAction action) {
        int actionW = Math.min(82, Math.max(72, w / 5));
        int titleW = Math.max(1, w - actionW - 8);

        addStyledButton(
                x,
                y,
                titleW,
                h,
                Text.literal(title),
                b -> action.toggle(b),
                accent,
                false
        );

        addStyledButton(
                x + titleW + 8,
                y,
                actionW,
                h,
                Text.literal(enabled ? "BẬT" : "TẮT"),
                b -> action.toggle(b),
                enabled ? accent : BORDER,
                false
        );

        int subtitleY = y + h - 15;
        labels.add(new Label(x + 12, subtitleY, subtitle, false));
    }

    private void addInfoCard(int x, int y, int w, int h, String title, String body) {
        if (h < 58) {
            h = 58;
        }
        cards.add(new InfoCard(x, y, w, h, title, body));
    }

    private void addBackgroundColorButton(int x, int y, int w, int h, String name, int color) {
        boolean selected = MapShieldConfig.getBackgroundColor() == (color | 0xFF000000);
        addStyledButton(
                x,
                y,
                w,
                h,
                Text.literal(selected ? "✓ " + name : name),
                b -> {
                    MapShieldConfig.setBackgroundColor(color);
                    MapShieldConfig.save();
                    setNotice("Đã chọn nền " + name + ".");
                    rebuildRequested = true;
                },
                selected
                        ? MapShieldConfig.getAccentColor()
                        : color,
                false
        );
    }

    private void addColorButton(int x, int y, int w, int h, String name, int color, boolean accent) {
        int current = accent
                ? MapShieldConfig.getAccentColor()
                : MapShieldConfig.getTextColor();
        boolean selected = current == (color | 0xFF000000);
        addStyledButton(
                x,
                y,
                w,
                h,
                Text.literal(selected ? "✓ " + name : name),
                b -> {
                    if (accent) {
                        MapShieldConfig.setAccentColor(color);
                    } else {
                        MapShieldConfig.setTextColor(color);
                    }
                    MapShieldConfig.save();
                    setNotice("Đã chọn " + name + ".");
                    rebuildRequested = true;
                },
                selected
                        ? MapShieldConfig.getAccentColor()
                        : color,
                false
        );
    }

    private ButtonWidget addStyledButton(int x, int y, int w, int h, Text message, ButtonWidget.PressAction action, int accent, boolean sidebar) {
        return addStyledButtonInternal(x, y, w, h, message, action, accent, sidebar, false);
    }

    private ButtonWidget addFixedButton(int x, int y, int w, int h, Text message, ButtonWidget.PressAction action, int accent, boolean sidebar) {
        return addStyledButtonInternal(x, y, w, h, message, action, accent, sidebar, true);
    }

    private ButtonWidget addStyledButtonInternal(int x, int y, int w, int h, Text message, ButtonWidget.PressAction action, int accent, boolean sidebar, boolean fixed) {
        w = Math.max(40, w);
        h = Math.max(26, h);

        ButtonWidget button = ButtonWidget.builder(message, action)
                .dimensions(x, y, w, h)
                .build();

        button.setAlpha(1.0f);
        interactiveButtons.add(button);
        visualButtons.add(new VisualButton(button, accent, sidebar, fixed));
        return addDrawableChild(button);
    }

    private void updateContentScrollBounds(Layout l) {
        int maxBottom = l.contentY;

        for (ButtonWidget button : interactiveButtons) {
            if (button.getY() >= l.contentY) {
                maxBottom = Math.max(maxBottom, button.getY() + button.getHeight());
            }
        }

        for (InfoCard card : cards) {
            maxBottom = Math.max(maxBottom, card.y + card.h);
        }

        for (Label label : labels) {
            if (label.y >= l.contentY) {
                maxBottom = Math.max(maxBottom, label.y + 12);
            }
        }

        if (commandField != null) {
            maxBottom = Math.max(maxBottom, commandField.getY() + commandField.getHeight());
        }
        if (backgroundField != null) {
            maxBottom = Math.max(maxBottom, backgroundField.getY() + backgroundField.getHeight());
        }
        if (accentHexField != null) {
            maxBottom = Math.max(maxBottom, accentHexField.getY() + accentHexField.getHeight());
        }
        if (textHexField != null) {
            maxBottom = Math.max(maxBottom, textHexField.getY() + textHexField.getHeight());
        }
        if (backgroundHexField != null) {
            maxBottom = Math.max(maxBottom, backgroundHexField.getY() + backgroundHexField.getHeight());
        }

        maxContentScroll = Math.max(0.0, maxBottom - l.contentBottom + 16.0);
        contentScroll = Math.max(0.0, Math.min(contentScroll, maxContentScroll));
    }

    private boolean isWidgetFullyInside(int y, int h) {
        return y >= contentViewportTop && y + h <= contentViewportBottom;
    }

    private void updateWidgetVisibility(Layout l) {
        for (int i = 0; i < interactiveButtons.size(); i++) {
            ButtonWidget button = interactiveButtons.get(i);
            VisualButton visual = visualButtons.get(i);
            button.visible = visual.fixed || visual.sidebar || isWidgetFullyInside(button.getY(), button.getHeight());
        }
        if (commandField != null) {
            commandField.visible = isWidgetFullyInside(commandField.getY(), commandField.getHeight());
        }
        if (backgroundField != null) {
            backgroundField.visible = isWidgetFullyInside(backgroundField.getY(), backgroundField.getHeight());
        }
        if (accentHexField != null) {
            accentHexField.visible = isWidgetFullyInside(accentHexField.getY(), accentHexField.getHeight());
        }
        if (textHexField != null) {
            textHexField.visible = isWidgetFullyInside(textHexField.getY(), textHexField.getHeight());
        }
        if (backgroundHexField != null) {
            backgroundHexField.visible = isWidgetFullyInside(backgroundHexField.getY(), backgroundHexField.getHeight());
        }
    }

    private void selectTab(Tab tab) {
        if (closing || tab == selectedTab) {
            return;
        }
        selectedTab = tab;
        contentScroll = 0.0;
        targetContentScroll = 0.0;
        animationStartedAt = System.currentTimeMillis();
        rebuildLayout();
    }

    private void toggleMapShield(ButtonWidget button) {
        MapShieldConfig.setEnabled(!MapShieldConfig.isEnabled());
        MapShieldConfig.save();
        setNotice(MapShieldConfig.isEnabled() ? "Hide Map Art đã bật." : "Hide Map Art đã tắt.");
        rebuildRequested = true;
    }

    private void togglePassword(ButtonWidget button) {
        MapShieldConfig.setPasswordMaskEnabled(!MapShieldConfig.isPasswordMaskEnabled());
        MapShieldConfig.save();
        setNotice(MapShieldConfig.isPasswordMaskEnabled() ? "Password Shield đã bật." : "Password Shield đã tắt.");
        rebuildRequested = true;
    }

    private void toggleUpdates(ButtonWidget button) {
        MapShieldConfig.setUpdateCheckerEnabled(!MapShieldConfig.isUpdateCheckerEnabled());
        MapShieldConfig.save();
        if (MapShieldConfig.isUpdateCheckerEnabled()) {
            UpdateChecker.requestCheck();
        }
        setNotice(MapShieldConfig.isUpdateCheckerEnabled() ? "Update Checker đã bật." : "Update Checker đã tắt.");
        rebuildRequested = true;
    }

    private void openControls() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new ControlsOptionsScreen(this, client.options));
        }
    }

    private void addCommand() {
        if (commandField == null) {
            return;
        }
        String value = commandField.getText().trim();
        if (MapShieldConfig.addPasswordCommand(value)) {
            MapShieldConfig.save();
            commandField.setText("");
            setNotice("Đã thêm command: " + value);
        } else {
            setNotice("Command không hợp lệ hoặc đã tồn tại.");
        }
        rebuildLayout();
    }

    private void removeLastCommand() {
        if (MapShieldConfig.removeLastPasswordCommand()) {
            MapShieldConfig.save();
            setNotice("Đã xóa command cuối.");
        } else {
            setNotice("Danh sách command đang trống.");
        }
        rebuildLayout();
    }

    private void resetCommands() {
        MapShieldConfig.resetPasswordCommands();
        MapShieldConfig.save();
        setNotice("Đã khôi phục command mặc định.");
        rebuildLayout();
    }

    private void saveAll() {
        applyBackgroundPath();
        MapShieldConfig.save();
        setNotice("Đã lưu cài đặt.");
    }

    private void setNotice(String text) {
        notice = text == null ? "" : text;
        noticeUntil = System.currentTimeMillis() + 2200L;
    }

    private void refreshBackgroundFiles() {
        backgroundFiles.clear();
        Path folder = backgroundsDirectory();
        try {
            Files.createDirectories(folder);
            try (var stream = Files.list(folder)) {
                stream.filter(Files::isRegularFile)
                        .filter(PuuzSecuritySettingsScreen::isSupportedImage)
                        .sorted(Comparator.comparing(
                                p -> p.getFileName().toString().toLowerCase(Locale.ROOT)
                        ))
                        .limit(24)
                        .forEach(backgroundFiles::add);
            }
        } catch (IOException ignored) {
        }
    }

    private void openBackgroundFolder() {
        try {
            Path folder = backgroundsDirectory();
            Files.createDirectories(folder);
            net.minecraft.util.Util.getOperatingSystem().open(folder.toFile());
            setNotice("Đặt PNG/JPG vào thư mục backgrounds rồi bấm Quét lại.");
        } catch (Exception ignored) {
            setNotice("Không mở được thư mục backgrounds.");
        }
    }

    private void applyBackgroundPath() {
        if (backgroundField == null) {
            return;
        }

        String path = backgroundField.getText().trim();
        MapShieldConfig.setBackgroundPath(path);
        MapShieldConfig.save();
        loadBackgroundTexture();

        if (path.isBlank()) {
            setNotice("Đã dùng nền màu mặc định.");
        } else if (backgroundTextureId != null) {
            setNotice("Đã áp dụng ảnh nền.");
        } else {
            setNotice("Không đọc được ảnh nền.");
        }
    }

    private void selectBackground(Path file) {
        String absolute = file.toAbsolutePath().toString();
        MapShieldConfig.setBackgroundPath(absolute);
        MapShieldConfig.save();
        if (backgroundField != null) {
            backgroundField.setText(absolute);
        }
        loadBackgroundTexture();
        setNotice("Đã chọn: " + file.getFileName());
        rebuildLayout();
    }

    private void loadBackgroundTexture() {
        disposeBackgroundTexture();

        String configured = MapShieldConfig.getBackgroundPath();
        if (configured == null || configured.isBlank()) {
            return;
        }

        Path path;
        try {
            path = Path.of(configured);
        } catch (Exception ignored) {
            return;
        }

        if (!Files.isRegularFile(path) || !isSupportedImage(path)) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        try (InputStream input = Files.newInputStream(path)) {
            NativeImage image = NativeImage.read(input);
            if (image.getWidth() <= 0 || image.getHeight() <= 0 || image.getWidth() > 4096 || image.getHeight() > 4096) {
                image.close();
                return;
            }

            backgroundWidth = image.getWidth();
            backgroundHeight = image.getHeight();
            backgroundTexture = new NativeImageBackedTexture(
                    () -> "PUUZ Security GUI Background",
                    image
            );
            backgroundTextureId = Identifier.of(
                    PuuzMapShieldClient.MOD_ID,
                    "gui_background"
            );

            client.getTextureManager().registerTexture(
                    backgroundTextureId,
                    backgroundTexture
            );
        } catch (Exception ignored) {
            backgroundTextureId = null;
            backgroundTexture = null;
            backgroundWidth = 0;
            backgroundHeight = 0;
        }
    }

    private void disposeBackgroundTexture() {
        if (backgroundTextureId != null) {
            try {
                MinecraftClient.getInstance()
                        .getTextureManager()
                        .destroyTexture(backgroundTextureId);
            } catch (Exception ignored) {
            }
        }
        backgroundTextureId = null;
        backgroundTexture = null;
        backgroundWidth = 0;
        backgroundHeight = 0;
    }

    private static boolean isSupportedImage(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private static Path backgroundsDirectory() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("puuz-security")
                .resolve("backgrounds");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float progress = animationProgress();
        Layout l = Layout.calculate(width, height);

        drawBase(context);
        drawBackgroundImage(context);
        drawMainPanel(context, l, progress);
        drawHeader(context, l, progress);
        drawSidebar(context, l, progress);

        context.enableScissor(l.contentX, l.contentY, l.right - 10, l.contentBottom);
        drawLabels(context, l, progress);
        drawCards(context, l, progress);
        drawPreviewImages(context);
        context.disableScissor();

        super.render(context, mouseX, mouseY, delta);

        context.enableScissor(l.contentX, l.contentY, l.right - 10, l.contentBottom);
        drawVisualButtons(context, mouseX, mouseY, progress, false);
        context.disableScissor();
        drawVisualButtons(context, mouseX, mouseY, progress, true);
        drawScrollbar(context, l);
        drawNotice(context, progress);
    }

    private void drawBase(DrawContext context) {
        context.fill(0, 0, width, height, MapShieldConfig.getBackgroundColor());
    }

    private void drawBackgroundImage(DrawContext context) {
        if (backgroundTextureId == null || backgroundWidth <= 0 || backgroundHeight <= 0) {
            return;
        }

        float scale = Math.max((float) width / backgroundWidth, (float) height / backgroundHeight);
        int drawW = Math.max(1, Math.round(backgroundWidth * scale));
        int drawH = Math.max(1, Math.round(backgroundHeight * scale));
        int x = (width - drawW) / 2;
        int y = (height - drawH) / 2;

        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                backgroundTextureId,
                x,
                y,
                0.0f,
                0.0f,
                drawW,
                drawH,
                drawW,
                drawH
        );

        context.fill(0, 0, width, height, 0x1FF7F3F8);
    }

    private void drawMainPanel(DrawContext context, Layout l, float progress) {
        int lift = Math.round((1.0f - progress) * 7.0f);
        int top = l.top + lift;
        int bottom = l.bottom + lift;

        rounded(context, l.left + 4, top + 5, l.right + 4, bottom + 5, 16, SHADOW);
        rounded(context, l.left, top, l.right, bottom, 16, panelColor());
        stroke(context, l.left, top, l.right, bottom, 16, BORDER);

        int accent = MapShieldConfig.getAccentColor();
        context.fill(l.left + 18, bottom - 3, l.left + 94, bottom - 1, accent);
    }

    private void drawHeader(DrawContext context, Layout l, float progress) {
        int text = MapShieldConfig.getTextColor();
        int accent = MapShieldConfig.getAccentColor();
        int offset = Math.round((1.0f - progress) * -4.0f);

        drawText(context, l.left + 24, l.top + 15 + offset, "PUUZ SECURITY", text);
        drawText(context, l.left + 24, l.top + 36 + offset, "Cài đặt nhẹ nhàng · riêng tư · theo cách của bạn", mutedTextColor());
        drawTextRight(context, l.right - 24, l.top + 21 + offset, "v1.2.0", accent);

        context.fill(l.left + 20, l.top + 62 + offset, l.right - 20, l.top + 63 + offset, BORDER);
    }

    private void drawSidebar(DrawContext context, Layout l, float progress) {
        if (l.compact) {
            return;
        }

        rounded(context, l.sidebarX, l.sidebarY, l.sidebarX + l.sidebarWidth, l.sidebarBottom, 12, sidebarColor());
        stroke(context, l.sidebarX, l.sidebarY, l.sidebarX + l.sidebarWidth, l.sidebarBottom, 12, BORDER);
        drawText(context, l.sidebarX + 14, l.sidebarY + 14, "CÀI ĐẶT", MapShieldConfig.getTextColor());
        drawText(context, l.sidebarX + 14, l.sidebarY + 33, "PUUZ SECURITY", secondaryTextColor());
    }

    private void drawLabels(DrawContext context, Layout l, float progress) {
        int offset = Math.round((1.0f - progress) * 5.0f);
        int compactOffset = l.compact ? 0 : offset;
        for (Label label : labels) {
            int color = label.title ? MapShieldConfig.getTextColor() : mutedTextColor();
            drawText(context, label.x, label.y + compactOffset, label.text, color);
        }
    }

    private void drawCards(DrawContext context, Layout l, float progress) {
        int offset = Math.round((1.0f - progress) * 6.0f);
        for (InfoCard card : cards) {
            int top = card.y + offset;
            rounded(context, card.x, top, card.x + card.w, top + card.h, 12, cardColor());
            stroke(context, card.x, top, card.x + card.w, top + card.h, 12, BORDER);

            drawText(context, card.x + 14, top + 11, card.title, MapShieldConfig.getTextColor());
            String[] lines = card.body.split("\\n", -1);
            int lineY = top + 31;
            for (String line : lines) {
                drawText(context, card.x + 14, lineY, line, mutedTextColor());
                lineY += 15;
                if (lineY > top + card.h - 8) {
                    break;
                }
            }
        }
    }

    private void drawVisualButtons(DrawContext context, int mouseX, int mouseY, float progress, boolean fixedOnly) {
        for (VisualButton visual : visualButtons) {
            ButtonWidget button = visual.button;
            if (!button.visible || visual.fixed != fixedOnly) {
                continue;
            }

            boolean hovered = mouseX >= button.getX()
                    && mouseX < button.getX() + button.getWidth()
                    && mouseY >= button.getY()
                    && mouseY < button.getY() + button.getHeight();
            boolean focused = button.isFocused();

            float target = hovered || focused ? 1.0f : 0.0f;
            visual.hover += (target - visual.hover) * 0.24f;

            int x = button.getX();
            int y = button.getY();
            int expand = Math.round(visual.hover * 2.0f);

            int base = visual.sidebar ? sidebarColor() : panelColor();
            int accent = visual.accent;
            int hoverColor = withAlpha(accent, 10 + Math.round(26 * visual.hover));
            int border = blend(BORDER, accent, 0.20f + visual.hover * 0.55f);

            rounded(context, x - expand, y - expand, x + button.getWidth() + expand, y + button.getHeight() + expand, 9, base);
            rounded(context, x - expand, y - expand, x + button.getWidth() + expand, y + button.getHeight() + expand, 9, hoverColor);
            stroke(context, x - expand, y - expand, x + button.getWidth() + expand, y + button.getHeight() + expand, 9, border);

            String text = button.getMessage().getString();
            int textColor = MapShieldConfig.getTextColor();
            center(context, text, x + button.getWidth() / 2, y + Math.max(7, (button.getHeight() - 9) / 2), textColor);
        }
    }

    private void drawPreviewImages(DrawContext context) {
        for (PreviewRequest request : drawPreviewRequests) {
            Identifier id = MapHideTextureManager.getActiveTexture();
            rounded(context, request.x() - 2, request.y() - 2, request.x() + request.size() + 2, request.y() + request.size() + 2, 10, 0xFFDDD5E3);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, id, request.x(), request.y(), 0.0f, 0.0f, request.size(), request.size(), request.size(), request.size());
        }
    }

    private void drawScrollbar(DrawContext context, Layout l) {
        if (maxContentScroll <= 0.0) {
            return;
        }

        int x = l.right - 8;
        int top = l.contentY;
        int bottom = l.contentBottom;
        int height = Math.max(1, bottom - top);
        int thumb = Math.max(24, (int) Math.round(height * (height / (height + maxContentScroll))));
        int travel = Math.max(0, height - thumb);
        int y = top + (int) Math.round(travel * (contentScroll / Math.max(1.0, maxContentScroll)));

        context.fill(x, top, x + 2, bottom, 0x225B5662);
        context.fill(x, y, x + 2, y + thumb, MapShieldConfig.getAccentColor());
    }

    private int panelColor() {
        return backgroundTextureId != null
                ? 0xB8FEFCFF
                : 0xF2FEFCFF;
    }

    private int cardColor() {
        return backgroundTextureId != null
                ? 0xB0FFFFFF
                : 0xF7FFFFFF;
    }

    private int sidebarColor() {
        return backgroundTextureId != null
                ? 0xB0F8F5FA
                : 0xF4F8F5FA;
    }

    private int mutedTextColor() {
        return blend(MapShieldConfig.getTextColor(), 0xFF8C8592, 0.42f);
    }

    private int secondaryTextColor() {
        return blend(MapShieldConfig.getTextColor(), 0xFFA29BAA, 0.62f);
    }

    private void drawNotice(DrawContext context, float progress) {
        if (notice.isBlank() || System.currentTimeMillis() >= noticeUntil) {
            return;
        }

        float alpha = Math.min(1.0f, Math.max(0.0f, (noticeUntil - System.currentTimeMillis()) / 450.0f));
        int w = Math.min(440, Math.max(180, width - 28));
        int x = (width - w) / 2;
        int y = height - 72;
        int bg = withAlpha(PANEL, 210 + Math.round(35 * alpha));
        rounded(context, x, y, x + w, y + 30, 9, bg);
        stroke(context, x, y, x + w, y + 30, 9, BORDER);
        center(context, notice, width / 2, y + 8, MapShieldConfig.getAccentColor());
    }

    private void drawText(DrawContext context, int x, int y, String value, int color) {
        context.drawTextWithShadow(textRenderer, Text.literal(value), x, y, color);
    }

    private void drawTextRight(DrawContext context, int right, int y, String value, int color) {
        drawText(context, right - textRenderer.getWidth(value), y, value, color);
    }

    private void center(DrawContext context, String value, int centerX, int y, int color) {
        context.drawTextWithShadow(
                textRenderer,
                Text.literal(value),
                centerX - textRenderer.getWidth(value) / 2,
                y,
                color
        );
    }

    private float animationProgress() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - animationStartedAt);
        float progress = Math.min(1.0f, elapsed / (float) OPEN_ANIMATION_MS);
        return closing ? 1.0f - easeOut(progress) : easeOut(progress);
    }

    private void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        animationStartedAt = System.currentTimeMillis();
        for (ButtonWidget button : interactiveButtons) {
            button.active = false;
        }
        if (commandField != null) {
            commandField.active = false;
        }
        if (backgroundField != null) {
            backgroundField.active = false;
        }
    }

    private static int blend(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        return 0xFF000000
                | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8)
                | (int) (ab + (bb - ab) * t);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0x00FFFFFF);
    }

    private static float easeOut(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    private static void rounded(DrawContext context, int left, int top, int right, int bottom, int radius, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        int r = Math.min(radius, Math.min(right - left, bottom - top) / 2);
        context.fill(left + r, top, right - r, bottom, color);
        context.fill(left, top + r, right, bottom - r, color);
        context.fill(left + 2, top + 1, left + r, top + r, color);
        context.fill(right - r, top + 1, right - 2, top + r, color);
        context.fill(left + 2, bottom - r, left + r, bottom - 1, color);
        context.fill(right - r, bottom - r, right - 2, bottom - 1, color);
    }

    private static void stroke(DrawContext context, int left, int top, int right, int bottom, int radius, int color) {
        if (right <= left || bottom <= top) {
            return;
        }
        int r = Math.min(radius, Math.min(right - left, bottom - top) / 2);
        context.fill(left + r, top, right - r, top + 1, color);
        context.fill(left + r, bottom - 1, right - r, bottom, color);
        context.fill(left, top + r, left + 1, bottom - r, color);
        context.fill(right - 1, top + r, right, bottom - r, color);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (maxContentScroll > 0.0 && Math.abs(verticalAmount) > 0.0) {
            targetContentScroll -= verticalAmount * 72.0;
            targetContentScroll = Math.max(
                    0.0,
                    Math.min(targetContentScroll, maxContentScroll)
            );

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
    }

    /**
     * Moves only page-content elements. Sidebar, tabs and footer stay fixed.
     * This avoids rebuilding widgets on every wheel event, which caused
     * visible flicker, focus loss and click jitter.
     */
    private void shiftContentElements(int deltaY) {
        if (deltaY == 0) {
            return;
        }

        for (int i = 0; i < interactiveButtons.size(); i++) {
            VisualButton visual = visualButtons.get(i);
            if (visual.fixed || visual.sidebar) {
                continue;
            }

            ButtonWidget button = interactiveButtons.get(i);
            button.setY(button.getY() + deltaY);
        }

        if (commandField != null) {
            commandField.setY(commandField.getY() + deltaY);
        }

        if (backgroundField != null) {
            backgroundField.setY(backgroundField.getY() + deltaY);
        }
        if (accentHexField != null) {
            accentHexField.setY(accentHexField.getY() + deltaY);
        }
        if (textHexField != null) {
            textHexField.setY(textHexField.getY() + deltaY);
        }
        if (backgroundHexField != null) {
            backgroundHexField.setY(backgroundHexField.getY() + deltaY);
        }

        for (int i = 0; i < labels.size(); i++) {
            Label label = labels.get(i);
            labels.set(
                    i,
                    new Label(
                            label.x(),
                            label.y() + deltaY,
                            label.text(),
                            label.title()
                    )
            );
        }

        for (int i = 0; i < cards.size(); i++) {
            InfoCard card = cards.get(i);
            cards.set(
                    i,
                    new InfoCard(
                            card.x(),
                            card.y() + deltaY,
                            card.w(),
                            card.h(),
                            card.title(),
                            card.body()
                    )
            );
        }
    }

    @Override
    public void close() {
        beginClose();
    }

    @Override
    public void removed() {
        disposeBackgroundTexture();
        super.removed();
    }

    private enum Tab {
        GENERAL("Tổng quan", "Tổng quan", "Thiết lập nhanh cho PUUZ SECURITY."),
        MAP_ART("Map Art", "Map Art", "Kiểm soát cách Map Art hiển thị."),
        PASSWORD("Mật khẩu", "Password Shield", "Bảo vệ mật khẩu khi nhập command."),
        APPEARANCE("Giao diện", "Giao diện", "Màu sắc, text và ảnh nền."),
        UPDATES("Cập nhật", "Update Checker", "Quản lý cập nhật từ GitHub."),
        CONTROLS("Phím tắt", "Keybinds", "Quản lý phím tắt PUUZ SECURITY."),
        MONEY("Tiền", "Money History", "Lịch sử giao dịch trên HUD."),
        ABOUT("Giới thiệu", "Giới thiệu", "Thông tin về PUUZ SECURITY.");

        final String label;
        final String title;
        final String subtitle;

        Tab(String label, String title, String subtitle) {
            this.label = label;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private static final class VisualButton {
        final ButtonWidget button;
        final int accent;
        final boolean sidebar;
        final boolean fixed;
        float hover;

        VisualButton(ButtonWidget button, int accent, boolean sidebar, boolean fixed) {
            this.button = button;
            this.accent = accent;
            this.sidebar = sidebar;
            this.fixed = fixed;
        }
    }

    private record InfoCard(int x, int y, int w, int h, String title, String body) {
    }

    private record Label(int x, int y, String text, boolean title) {
    }

    @FunctionalInterface
    private interface ToggleAction {
        void toggle(ButtonWidget button);
    }

    private static final class Layout {
        final int left;
        final int top;
        final int right;
        final int bottom;
        final int sidebarX;
        final int sidebarY;
        final int sidebarWidth;
        final int sidebarBottom;
        final int contentX;
        final int contentY;
        final int contentWidth;
        final int contentBottom;
        final int tabsY;
        final boolean compact;

        private Layout(
                int left,
                int top,
                int right,
                int bottom,
                int sidebarX,
                int sidebarY,
                int sidebarWidth,
                int sidebarBottom,
                int contentX,
                int contentY,
                int contentWidth,
                int contentBottom,
                int tabsY,
                boolean compact
        ) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.sidebarX = sidebarX;
            this.sidebarY = sidebarY;
            this.sidebarWidth = sidebarWidth;
            this.sidebarBottom = sidebarBottom;
            this.contentX = contentX;
            this.contentY = contentY;
            this.contentWidth = contentWidth;
            this.contentBottom = contentBottom;
            this.tabsY = tabsY;
            this.compact = compact;
        }

        static Layout calculate(int screenWidth, int screenHeight) {
            int panelWidth = Math.min(1220, Math.max(240, screenWidth - 8));
            int panelHeight = Math.min(760, Math.max(240, screenHeight - 8));

            int left = Math.max(5, (screenWidth - panelWidth) / 2);
            int top = Math.max(5, (screenHeight - panelHeight) / 2);
            int right = left + panelWidth;
            int bottom = top + panelHeight;

            boolean compact = panelWidth < 760 || panelHeight < 520;

            if (compact) {
                int contentX = left + 14;
                int contentWidth = Math.max(170, panelWidth - 28);
                int tabsY = top + 72;
                int tabRows = contentWidth >= 560 ? 2 : (contentWidth >= 420 ? 3 : 4);
                int contentY = tabsY + tabRows * 35 + 8;
                return new Layout(
                        left,
                        top,
                        right,
                        bottom,
                        0,
                        0,
                        0,
                        0,
                        contentX,
                        contentY,
                        contentWidth,
                        bottom - 52,
                        tabsY,
                        true
                );
            }

            int sidebarWidth = Math.min(206, Math.max(190, panelWidth / 5));
            int sidebarX = left + 12;
            int sidebarY = top + 76;
            int sidebarBottom = bottom - 52;
            int contentX = sidebarX + sidebarWidth + 14;
            int contentY = top + 76;
            int contentWidth = Math.max(220, right - contentX - 14);

            return new Layout(
                    left,
                    top,
                    right,
                    bottom,
                    sidebarX,
                    sidebarY,
                    sidebarWidth,
                    sidebarBottom,
                    contentX,
                    contentY,
                    contentWidth,
                    bottom - 52,
                    0,
                    false
            );
        }
    }
}
