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
    private static final int APP_BG = 0xFF13151A;
    private static final int PANEL = 0xFF181B22;
    private static final int PANEL_SOFT = 0xFF111318;
    private static final int BORDER = 0x142C3038;
    private static final int SUCCESS = 0xFF45D17A;
    private static final int DANGER = 0xFFE5504A;
    private static final int SHADOW = 0x8A000000;
    private static final int WHITE = 0xFFDDE0E8;
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
    private TextFieldWidget menuSurfaceHexField;
    private TextFieldWidget menuCardHexField;
    private TextFieldWidget menuBorderHexField;
    private TextFieldWidget gradientStartHexField;
    private TextFieldWidget gradientEndHexField;
    private TextFieldWidget textGradientStartHexField;
    private TextFieldWidget textGradientEndHexField;
    private TextFieldWidget hideMapImageField;
    private TextFieldWidget passwordCurtainHexField;
    private TextFieldWidget passwordMaskHexField;

    private Identifier backgroundTextureId;
    private NativeImageBackedTexture backgroundTexture;
    private int backgroundWidth;
    private int backgroundHeight;
    private final List<Path> backgroundFiles = new ArrayList<>();
    private final List<Path> hideMapImageFiles = new ArrayList<>();

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
        refreshHideMapImageFiles();
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

        Layout layout = Layout.calculate(width, height);
        targetContentScroll = Math.max(0.0, Math.min(targetContentScroll, maxContentScroll));
        double delta = targetContentScroll - contentScroll;
        if (Math.abs(delta) > 0.05) {
            double previous = contentScroll;
            contentScroll += delta * 0.35;
            if (Math.abs(targetContentScroll - contentScroll) < 0.08) {
                contentScroll = targetContentScroll;
            }
            shiftContentElements((int) Math.round(previous - contentScroll));
            updateWidgetVisibility(layout);
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
        menuSurfaceHexField = null;
        menuCardHexField = null;
        menuBorderHexField = null;
        gradientStartHexField = null;
        gradientEndHexField = null;
        textGradientStartHexField = null;
        textGradientEndHexField = null;
        hideMapImageField = null;
        passwordCurtainHexField = null;
        passwordMaskHexField = null;

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
        if (l.navMode == NavigationMode.COMPACT) {
            // Small/short windows: never waste the settings viewport on a
            // multi-row navigation grid. Use previous/current/next controls.
            int y = l.tabsY;
            int gap = 8;
            int arrowW = Math.max(34, Math.min(44, l.contentWidth / 7));
            int centerW = Math.max(120, l.contentWidth - arrowW * 2 - gap * 2);

            addFixedButton(
                    l.contentX, y, arrowW, 32, Text.literal("‹"),
                    b -> selectRelativeTab(-1), BORDER, false
            );
            addFixedButton(
                    l.contentX + arrowW + gap, y, centerW, 32,
                    Text.literal("●  " + selectedTab.label),
                    b -> cycleToNextTab(), MapShieldConfig.getAccentColor(), false
            );
            addFixedButton(
                    l.contentX + arrowW + gap + centerW + gap, y, arrowW, 32, Text.literal("›"),
                    b -> selectRelativeTab(1), BORDER, false
            );
            return;
        }

        if (l.navMode == NavigationMode.ALL_TABS) {
            // Medium windows: show every tab for one-click navigation, but keep
            // the strip shallow (two rows max) so the settings viewport remains
            // large enough to edit controls comfortably.
            int gap = 6;
            int cols = 6; // Always two rows for the 11-tab set.
            int rows = (Tab.values().length + cols - 1) / cols;
            int rowH = 28;
            int cellW = (l.contentWidth - gap * (cols - 1)) / cols;
            int x0 = l.contentX;
            int y0 = l.tabsY;

            for (int i = 0; i < Tab.values().length; i++) {
                Tab tab = Tab.values()[i];
                int col = i % cols;
                int row = i / cols;
                int x = x0 + col * (cellW + gap);
                int y = y0 + row * (rowH + gap);
                addFixedButton(
                        x, y, cellW, rowH,
                        Text.literal(tab == selectedTab ? "●  " + tab.shortLabel : tab.shortLabel),
                        b -> selectTab(tab),
                        tab == selectedTab ? MapShieldConfig.getAccentColor() : BORDER,
                        false
                );
            }
            return;
        }

        // Large windows: persistent sidebar with all tabs visible.
        int x = l.sidebarX + 12;
        int y = l.sidebarY + 42;
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

    private void selectRelativeTab(int delta) {
        Tab[] tabs = Tab.values();
        int next = (selectedTab.ordinal() + delta) % tabs.length;
        if (next < 0) next += tabs.length;
        selectTab(tabs[next]);
    }

    private void cycleToNextTab() {
        selectRelativeTab(1);
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
            case TOOLTIP -> buildTooltip(x, bodyY, w, l);
            case MONEY -> buildMoney(x, bodyY, w, l);
            case QUICK_PAY -> buildQuickPay(x, bodyY, w, l);
            case APPEARANCE -> buildAppearance(x, bodyY, w, l);
            case HUD -> buildHud(x, bodyY, w, l);
            case UPDATES -> buildUpdates(x, bodyY, w, l);
            case CONTROLS -> buildControls(x, bodyY, w, l);
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
        labels.add(new Label(x, sectionY + 18, "Xem, thêm và chọn ảnh ngay tại đây. Ảnh được lưu trong thư mục map-hidden.", false));

        int previewSize = Math.min(128, Math.max(92, w / 4));
        int previewX = x;
        int previewY = sectionY + 38;
        drawPreviewRequests.add(new PreviewRequest(previewX, previewY, previewSize));

        int panelX = x + previewSize + 14;
        int panelW = Math.max(160, w - previewSize - 14);
        int buttonGap = 8;
        int addW = Math.max(100, (panelW - buttonGap) / 2);
        addStyledButton(panelX, previewY, addW, 30, Text.literal("＋ Thêm ảnh"), b -> chooseHideMapImage(), MapShieldConfig.getAccentColor(), false);
        addStyledButton(panelX + addW + buttonGap, previewY, panelW - addW - buttonGap, 30, Text.literal("Quét lại"), b -> {
            refreshHideMapImageFiles();
            rebuildRequested = true;
            setNotice("Đã cập nhật danh sách ảnh Map Art.");
        }, BORDER, false);

        int selectedY = previewY + 38;
        hideMapImageField = addDrawableChild(new TextFieldWidget(textRenderer, panelX, selectedY, panelW, 28, Text.literal("Ảnh đang chọn")));
        hideMapImageField.setMaxLength(512);
        hideMapImageField.setText(MapShieldConfig.getHideMapImagePath());
        hideMapImageField.setPlaceholder(Text.literal("Chưa chọn ảnh — đang dùng ảnh mặc định"));

        int actionY = selectedY + 36;
        int actionW = Math.max(90, (panelW - buttonGap * 2) / 3);
        addStyledButton(panelX, actionY, actionW, 28, Text.literal("Áp dụng"), b -> applyHideMapImage(), MapShieldConfig.getAccentColor(), false);
        addStyledButton(panelX + actionW + buttonGap, actionY, actionW, 28, Text.literal("Mở thư mục"), b -> openHideMapFolder(), BORDER, false);
        addStyledButton(panelX + (actionW + buttonGap) * 2, actionY, panelW - (actionW + buttonGap) * 2, 28, Text.literal("Mặc định"), b -> resetHideMapImage(), BORDER, false);

        int listY = previewY + Math.max(previewSize + 12, 136);
        labels.add(new Label(x, listY, "Ảnh trong thư mục map-hidden", true));
        int listTop = listY + 18;
        int listCols = l.compact ? 1 : 2;
        int rowH = 32;
        int listGap = 8;
        int listW = Math.max(120, (w - listGap * (listCols - 1)) / listCols);
        int visible = Math.min(hideMapImageFiles.size(), l.compact ? 6 : 8);

        if (visible == 0) {
            addInfoCard(x, listTop, w, 54, "Chưa có ảnh", "Nhấn ＋ Thêm ảnh để chọn PNG/JPG từ máy. Ảnh sẽ được sao chép vào map-hidden.");
        } else {
            for (int i = 0; i < visible; i++) {
                Path file = hideMapImageFiles.get(i);
                int col = i % listCols;
                int row = i / listCols;
                boolean selected = samePath(MapShieldConfig.getHideMapImagePath(), file);
                String name = file.getFileName().toString();
                if (name.length() > 26) name = name.substring(0, 23) + "...";
                addStyledButton(
                        x + col * (listW + listGap),
                        listTop + row * rowH,
                        listW, 28,
                        Text.literal((selected ? "✓ " : "") + name),
                        b -> selectHideMapImage(file),
                        selected ? MapShieldConfig.getAccentColor() : BORDER,
                        false
                );
            }
        }

        int listRows = Math.max(1, (visible + listCols - 1) / listCols);
        int footerY = listTop + listRows * rowH + 8;
        addInfoCard(x, footerY, w, l.compact ? 82 : 96, "Phím tắt", "F8  ·  Bật/tắt Map Shield\nP  ·  Cho phép Map Art hiện tại\nU  ·  Gỡ cho phép Map Art hiện tại");
    }

    private final List<PreviewRequest> drawPreviewRequests = new ArrayList<>();
    private record PreviewRequest(int x, int y, int size) {}

    private static boolean samePath(String configured, Path file) {
        if (configured == null || configured.isBlank() || file == null) return false;
        try {
            return Path.of(configured).toAbsolutePath().normalize().equals(file.toAbsolutePath().normalize());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private Path hideMapImagesDirectory() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("puuz-security")
                .resolve("map-hidden");
    }

    private void refreshHideMapImageFiles() {
        hideMapImageFiles.clear();
        Path folder = hideMapImagesDirectory();
        try {
            Files.createDirectories(folder);
            try (var stream = Files.list(folder)) {
                stream.filter(Files::isRegularFile)
                        .filter(PuuzSecuritySettingsScreen::isSupportedImage)
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(hideMapImageFiles::add);
            }
        } catch (IOException ignored) {
        }
    }

    private void selectHideMapImage(Path file) {
        if (file == null || !isSupportedImage(file)) return;
        MapShieldConfig.setHideMapImagePath(file.toAbsolutePath().toString());
        MapShieldConfig.save();
        boolean loaded = MapHideTextureManager.reload();
        if (hideMapImageField != null) hideMapImageField.setText(file.toAbsolutePath().toString());
        setNotice(loaded ? "Đã chọn ảnh Map Art." : "Không đọc được ảnh; đang dùng ảnh mặc định.");
        refreshHideMapImageFiles();
        rebuildRequested = true;
    }

    private void chooseHideMapImage() {
        Thread picker = new Thread(() -> {
            try {
                java.awt.FileDialog dialog = new java.awt.FileDialog((java.awt.Frame) null, "Chọn ảnh Map Art", java.awt.FileDialog.LOAD);
                dialog.setFilenameFilter((dir, name) -> {
                    String lower = name.toLowerCase(Locale.ROOT);
                    return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
                });
                dialog.setVisible(true);
                String file = dialog.getFile();
                String directory = dialog.getDirectory();
                dialog.dispose();
                if (file == null || directory == null) return;

                Path selected = Path.of(directory, file);
                Path targetDir = hideMapImagesDirectory();
                Files.createDirectories(targetDir);
                Path target = targetDir.resolve(selected.getFileName().toString()).normalize();
                Files.copy(selected, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                MinecraftClient.getInstance().execute(() -> selectHideMapImage(target));
            } catch (Exception ignored) {
                MinecraftClient.getInstance().execute(() -> setNotice("Không mở được hộp chọn ảnh."));
            }
        }, "PUUZ-ImagePicker");
        picker.setDaemon(true);
        picker.start();
    }

    private void applyHideMapImage() {
        if (hideMapImageField == null) return;
        String value = hideMapImageField.getText().trim();
        MapShieldConfig.setHideMapImagePath(value);
        MapShieldConfig.save();
        boolean loaded = MapHideTextureManager.reload();
        refreshHideMapImageFiles();
        setNotice(loaded ? "Đã áp dụng ảnh Map Shield." : (value.isBlank() ? "Đã khôi phục ảnh mặc định." : "Không đọc được ảnh; đang dùng ảnh mặc định."));
        rebuildRequested = true;
    }

    private void openHideMapFolder() {
        try {
            Path folder = hideMapImagesDirectory();
            Files.createDirectories(folder);
            net.minecraft.util.Util.getOperatingSystem().open(folder.toFile());
            refreshHideMapImageFiles();
            setNotice("Đã mở thư mục map-hidden.");
        } catch (Exception ignored) {
            setNotice("Không mở được thư mục map-hidden.");
        }
    }

    private void resetHideMapImage() {
        MapShieldConfig.setHideMapImagePath("");
        MapShieldConfig.save();
        MapHideTextureManager.reload();
        refreshHideMapImageFiles();
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

        int colorY = buttonY + (l.compact ? 128 : 146);
        labels.add(new Label(x, colorY, "Màu khung che login", true));
        int colorW = Math.max(120, (w - 8) / 2);
        passwordCurtainHexField = addColorInput(x, colorY + 18, colorW, "Màn che #RRGGBBAA", MapShieldConfig.getPasswordCurtainColor());
        passwordMaskHexField = addColorInput(x + colorW + 8, colorY + 18, colorW, "Khung chat #RRGGBB", MapShieldConfig.getPasswordMaskColor());
        addStyledButton(x, colorY + 54, w, 30, Text.literal("Áp dụng màu che login"), b -> applyPasswordMaskColors(), MapShieldConfig.getAccentColor(), false);
    }

    private void buildTooltip(int x, int y, int w, Layout l) {
        int cardH = l.compact ? 50 : 56;
        addToggleCard(x, y, w, cardH, "Bảo vệ chú giải", "Chặn Map Art xuất hiện trong tooltip được hỗ trợ.", MapShieldConfig.isMapTooltipPreviewBlocked(), MapShieldConfig.getAccentColor(), this::toggleMapTooltipPreview);
        addInfoCard(x, y + cardH + 12, w, l.compact ? 82 : 96, "Chỉ bảo vệ hình ảnh", "Phần chữ của tooltip vẫn được giữ lại.\nPUUZ chỉ can thiệp phần preview Map Art khi lớp bảo vệ được bật.");
    }

    private void buildQuickPay(int x, int y, int w, Layout l) {
        addInfoCard(x, y, w, l.compact ? 70 : 82, "Thanh toán nhanh", "Ngắm vào người chơi rồi dùng Shift + phím Quick Pay để chuẩn bị lệnh /pay.");
        addInfoCard(x, y + (l.compact ? 82 : 94), w, l.compact ? 78 : 90, "Luôn cần xác nhận", "PUUZ không tự chọn số tiền và không tự gửi tiền.\nNgười chơi vẫn kiểm tra và gửi lệnh.");
        addStyledButton(x, y + (l.compact ? 172 : 196), w, 34, Text.literal("Mở phần phím tắt"), b -> openControls(), MapShieldConfig.getAccentColor(), false);
    }

    private void buildHud(int x, int y, int w, Layout l) {
        addInfoCard(x, y, w, l.compact ? 70 : 82, "HUD", "Khu vực này dùng để điều chỉnh các thành phần hiển thị nổi của PUUZ SECURITY.");
        addToggleCard(x, y + (l.compact ? 82 : 94), w, l.compact ? 50 : 56, "Hiển thị Money History", "Hiện bảng lịch sử giao dịch trên HUD.", MapShieldConfig.isMoneyHistoryEnabled(), MapShieldConfig.getAccentColor(), b -> toggleMoneyHistory());
        int base = y + (l.compact ? 144 : 164);
        addInfoCard(x, base, w, l.compact ? 86 : 100, "Trình chỉnh HUD", "Ẩn menu để chỉnh trực tiếp HUD trên màn hình. Kéo khung để di chuyển và kéo góc để đổi kích thước.");
        addStyledButton(x, base + (l.compact ? 98 : 112), w, 34, Text.literal("Chỉnh sửa HUD trực tiếp"), b -> openHudEditor(), MapShieldConfig.getAccentColor(), false);
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

        int menuSectionY = applyRowY + 42;
        labels.add(new Label(x, menuSectionY, "Màu giao diện menu", true));
        int themeFieldY = menuSectionY + 18;
        int themeCols = l.compact ? 1 : 3;
        int themeW = Math.max(120, (w - fieldGap * (themeCols - 1)) / themeCols);

        menuSurfaceHexField = addColorInput(x, themeFieldY, themeW, "Mặt menu", MapShieldConfig.getMenuSurfaceColor());
        menuCardHexField = addColorInput(x + (themeCols > 1 ? themeW + fieldGap : 0), themeFieldY + (themeCols > 1 ? 0 : 34), themeW, "Card menu", MapShieldConfig.getMenuCardColor());
        menuBorderHexField = addColorInput(x + (themeCols > 2 ? 2 * (themeW + fieldGap) : 0), themeFieldY + (themeCols > 2 ? 0 : (themeCols > 1 ? 34 : 68)), themeW, "Viền menu", MapShieldConfig.getMenuBorderColor());

        int presetY = themeFieldY + (themeCols > 1 ? 40 : 110);
        labels.add(new Label(x, presetY, "Mẫu màu giao diện", true));
        String[] presetNames = {"PUUZ", "Than", "Tím mềm", "Xanh dịu", "Sương"};
        int[][] presets = {
                {0xFF1B1B21, 0xFF23232C, 0xFF34303F},
                {0xFF18191E, 0xFF202228, 0xFF30333B},
                {0xFF1E1A24, 0xFF292333, 0xFF3A3048},
                {0xFF192025, 0xFF222E35, 0xFF31434B},
                {0xFF1D2222, 0xFF27302F, 0xFF3A4542}
        };
        int presetGap = 7;
        int presetW = Math.max(70, (w - presetGap * 4) / 5);
        for (int i = 0; i < presetNames.length; i++) {
            int idx = i;
            addStyledButton(x + i * (presetW + presetGap), presetY + 18, presetW, 28,
                    Text.literal(presetNames[i]), b -> applyMenuPreset(presets[idx]), presets[i][2], false);
        }

        int gradientY = presetY + 56;
        labels.add(new Label(x, gradientY, "Nền chuyển màu", true));
        addToggleRow(x, gradientY + 18, Math.min(w, 260), 34, "Chuyển màu nền", MapShieldConfig.isMenuGradientEnabled(), MapShieldConfig.getAccentColor(), b -> {
            MapShieldConfig.setMenuGradientEnabled(!MapShieldConfig.isMenuGradientEnabled());
            MapShieldConfig.save();
            rebuildRequested = true;
        });
        gradientStartHexField = addColorInput(x, gradientY + 58, themeW, "Màu bắt đầu", MapShieldConfig.getMenuGradientStartColor());
        gradientEndHexField = addColorInput(x + (themeCols > 1 ? themeW + fieldGap : 0), gradientY + 58, themeW, "Màu kết thúc", MapShieldConfig.getMenuGradientEndColor());

        int textGradientY = gradientY + 100;
        labels.add(new Label(x, textGradientY, "Màu chữ chuyển sắc", true));
        addToggleRow(x, textGradientY + 18, Math.min(w, 260), 34, "Chuyển sắc chữ", MapShieldConfig.isTextGradientEnabled(), MapShieldConfig.getAccentColor(), b -> {
            MapShieldConfig.setTextGradientEnabled(!MapShieldConfig.isTextGradientEnabled());
            MapShieldConfig.save();
            rebuildRequested = true;
        });
        textGradientStartHexField = addColorInput(x, textGradientY + 58, themeW, "Chữ bắt đầu", MapShieldConfig.getTextGradientStartColor());
        textGradientEndHexField = addColorInput(x + (themeCols > 1 ? themeW + fieldGap : 0), textGradientY + 58, themeW, "Chữ kết thúc", MapShieldConfig.getTextGradientEndColor());

        int applyThemeY = textGradientY + 98;
        addStyledButton(x, applyThemeY, w, 30, Text.literal("Áp dụng màu menu"), b -> applyMenuTheme(), MapShieldConfig.getAccentColor(), false);

        int imageSectionY = applyThemeY + 42;
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

    private TextFieldWidget addColorInput(int x, int y, int w, String placeholder, int color) {
        TextFieldWidget field = addDrawableChild(new TextFieldWidget(textRenderer, x, y, w, 28, Text.literal(placeholder)));
        field.setMaxLength(9);
        field.setText(toHex(color));
        field.setPlaceholder(Text.literal("#RRGGBB"));
        return field;
    }

    private void applyMenuPreset(int[] preset) {
        MapShieldConfig.setMenuSurfaceColor(preset[0]);
        MapShieldConfig.setMenuCardColor(preset[1]);
        MapShieldConfig.setMenuBorderColor(preset[2]);
        MapShieldConfig.setMenuGradientStartColor(preset[0]);
        MapShieldConfig.setMenuGradientEndColor(preset[1]);
        MapShieldConfig.save();
        rebuildRequested = true;
        setNotice("Đã áp dụng mẫu màu giao diện.");
    }

    private void applyMenuTheme() {
        Integer surface = parseHexColor(menuSurfaceHexField == null ? "" : menuSurfaceHexField.getText());
        Integer card = parseHexColor(menuCardHexField == null ? "" : menuCardHexField.getText());
        Integer border = parseHexColor(menuBorderHexField == null ? "" : menuBorderHexField.getText());
        Integer start = parseHexColor(gradientStartHexField == null ? "" : gradientStartHexField.getText());
        Integer end = parseHexColor(gradientEndHexField == null ? "" : gradientEndHexField.getText());
        Integer textStart = parseHexColor(textGradientStartHexField == null ? "" : textGradientStartHexField.getText());
        Integer textEnd = parseHexColor(textGradientEndHexField == null ? "" : textGradientEndHexField.getText());
        if (surface == null || card == null || border == null || start == null || end == null || textStart == null || textEnd == null) {
            setNotice("Màu không hợp lệ. Dùng dạng #RRGGBB hoặc #RRGGBBAA.");
            return;
        }
        MapShieldConfig.setMenuSurfaceColor(surface);
        MapShieldConfig.setMenuCardColor(card);
        MapShieldConfig.setMenuBorderColor(border);
        MapShieldConfig.setMenuGradientStartColor(start);
        MapShieldConfig.setMenuGradientEndColor(end);
        MapShieldConfig.setTextGradientStartColor(textStart);
        MapShieldConfig.setTextGradientEndColor(textEnd);
        MapShieldConfig.save();
        setNotice("Đã áp dụng màu giao diện menu.");
        rebuildRequested = true;
    }

    private void openHudEditor() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.setScreen(new PuuzHudEditorScreen(this));
        }
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
                "Client-side Fabric mod tập trung vào Map Art và bảo vệ mật khẩu khi live.\n\nPhiên bản: 1.3.1\nTrạng thái: Big Update\n\nDesigned for a clean, calm and personal setup experience.");
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

        button.setAlpha(0.0f);
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
        TextFieldWidget[] themeFields = {
                menuSurfaceHexField, menuCardHexField, menuBorderHexField,
                gradientStartHexField, gradientEndHexField,
                textGradientStartHexField, textGradientEndHexField
        };
        for (TextFieldWidget field : themeFields) {
            if (field != null) {
                maxBottom = Math.max(maxBottom, field.getY() + field.getHeight());
            }
        }
        if (passwordCurtainHexField != null) maxBottom = Math.max(maxBottom, passwordCurtainHexField.getY() + passwordCurtainHexField.getHeight());
        if (passwordMaskHexField != null) maxBottom = Math.max(maxBottom, passwordMaskHexField.getY() + passwordMaskHexField.getHeight());

        maxContentScroll = Math.max(0.0, maxBottom - l.contentBottom + 16.0);
        contentScroll = Math.max(0.0, Math.min(contentScroll, maxContentScroll));
    }

    private boolean isWidgetFullyInside(int y, int h) {
        return y >= contentViewportTop && y + h <= contentViewportBottom;
    }

    private boolean isWidgetIntersectingViewport(int y, int h) {
        return y < contentViewportBottom && y + h > contentViewportTop;
    }

    private void updateWidgetVisibility(Layout l) {
        for (int i = 0; i < interactiveButtons.size(); i++) {
            ButtonWidget button = interactiveButtons.get(i);
            VisualButton visual = visualButtons.get(i);
            button.visible = visual.fixed || visual.sidebar || isWidgetIntersectingViewport(button.getY(), button.getHeight());
        }
        if (commandField != null) {
            commandField.visible = isWidgetIntersectingViewport(commandField.getY(), commandField.getHeight());
        }
        if (backgroundField != null) {
            backgroundField.visible = isWidgetIntersectingViewport(backgroundField.getY(), backgroundField.getHeight());
        }
        if (accentHexField != null) {
            accentHexField.visible = isWidgetIntersectingViewport(accentHexField.getY(), accentHexField.getHeight());
        }
        if (textHexField != null) {
            textHexField.visible = isWidgetIntersectingViewport(textHexField.getY(), textHexField.getHeight());
        }
        if (backgroundHexField != null) {
            backgroundHexField.visible = isWidgetIntersectingViewport(backgroundHexField.getY(), backgroundHexField.getHeight());
        }
        TextFieldWidget[] themeFields = {
                menuSurfaceHexField, menuCardHexField, menuBorderHexField,
                gradientStartHexField, gradientEndHexField,
                textGradientStartHexField, textGradientEndHexField
        };
        for (TextFieldWidget field : themeFields) {
            if (field != null) field.visible = isWidgetIntersectingViewport(field.getY(), field.getHeight());
        }
        if (passwordCurtainHexField != null) passwordCurtainHexField.visible = isWidgetIntersectingViewport(passwordCurtainHexField.getY(), passwordCurtainHexField.getHeight());
        if (passwordMaskHexField != null) passwordMaskHexField.visible = isWidgetIntersectingViewport(passwordMaskHexField.getY(), passwordMaskHexField.getHeight());
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
            client.setScreen(new PuuzKeybindSettingsScreen(this));
        }
    }

    private void applyPasswordMaskColors() {
        Integer curtain = parseHexColor(passwordCurtainHexField == null ? "" : passwordCurtainHexField.getText());
        Integer mask = parseHexColor(passwordMaskHexField == null ? "" : passwordMaskHexField.getText());
        if (curtain == null || mask == null) {
            setNotice("Màu không hợp lệ. Dùng #RRGGBB hoặc #RRGGBBAA.");
            return;
        }
        MapShieldConfig.setPasswordCurtainColor(curtain);
        MapShieldConfig.setPasswordMaskColor(mask);
        MapShieldConfig.setPasswordMaskBorderColor(MapShieldConfig.getAccentColor());
        MapShieldConfig.save();
        setNotice("Đã áp dụng màu che login.");
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
        int top = MapShieldConfig.isMenuGradientEnabled()
                ? MapShieldConfig.getMenuGradientStartColor()
                : MapShieldConfig.getMenuSurfaceColor();
        int bottom = MapShieldConfig.isMenuGradientEnabled()
                ? MapShieldConfig.getMenuGradientEndColor()
                : MapShieldConfig.getMenuSurfaceColor();
        if (!MapShieldConfig.isMenuGradientEnabled()) {
            context.fill(0, 0, width, height, top);
            return;
        }
        int steps = Math.max(16, Math.min(48, height));
        for (int i = 0; i < steps; i++) {
            float t = steps <= 1 ? 0.0f : (float) i / (steps - 1);
            int y0 = i * height / steps;
            int y1 = (i + 1) * height / steps + 1;
            context.fill(0, y0, width, y1, blend(top, bottom, t));
        }
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

        context.fill(0, 0, width, height, 0x6613151A);
    }

    private void drawMainPanel(DrawContext context, Layout l, float progress) {
        int lift = Math.round((1.0f - progress) * 7.0f);
        int top = l.top + lift;
        int bottom = l.bottom + lift;

        rounded(context, l.left + 4, top + 5, l.right + 4, bottom + 5, 16, SHADOW);
        rounded(context, l.left, top, l.right, bottom, 16, panelColor());
        // Very subtle top wash keeps the menu dimensional without making it bright.
        int wash = withAlpha(MapShieldConfig.getAccentColor(), 14);
        context.fill(l.left + 18, top + 2, l.right - 18, top + 3, wash);
        stroke(context, l.left, top, l.right, bottom, 16, MapShieldConfig.getMenuBorderColor());

        int accent = MapShieldConfig.getAccentColor();
        context.fill(l.left + 18, bottom - 3, Math.min(l.left + 124, l.right - 22), bottom - 1, accent);
    }

    private void drawHeader(DrawContext context, Layout l, float progress) {
        int text = MapShieldConfig.getTextColor();
        int accent = MapShieldConfig.getAccentColor();
        int offset = Math.round((1.0f - progress) * -4.0f);

        Identifier logo = Identifier.of("puuz_map_shield", "icon.png");
        context.drawTexture(RenderPipelines.GUI_TEXTURED, logo, l.left + 20, l.top + 12 + offset, 0.0f, 0.0f, 28, 28, 28, 28);
        drawText(context, l.left + 56, l.top + 13 + offset, "PUUZ SECURITY", text);
        drawText(context, l.left + 56, l.top + 34 + offset, "Cài đặt nhẹ nhàng · riêng tư · theo cách của bạn", mutedTextColor());
        drawTextRight(context, l.right - 24, l.top + 21 + offset, "v1.3.1", accent);

        context.fill(l.left + 20, l.top + 62 + offset, l.right - 20, l.top + 63 + offset, BORDER);
    }

    private void drawSidebar(DrawContext context, Layout l, float progress) {
        if (l.navMode != NavigationMode.SIDEBAR) {
            return;
        }

        rounded(context, l.sidebarX, l.sidebarY, l.sidebarX + l.sidebarWidth, l.sidebarBottom, 12, sidebarColor());
        stroke(context, l.sidebarX, l.sidebarY, l.sidebarX + l.sidebarWidth, l.sidebarBottom, 12, BORDER);
        drawText(context, l.sidebarX + 14, l.sidebarY + 6, "CÀI ĐẶT", mutedTextColor());
        drawText(context, l.sidebarX + 14, l.sidebarY + 23, "PUUZ SECURITY", secondaryTextColor());
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
            stroke(context, card.x, top, card.x + card.w, top + card.h, 12, MapShieldConfig.getMenuBorderColor());

            drawText(context, card.x + 14, top + 11, card.title, MapShieldConfig.getTextColor());
            context.fill(card.x + 14, top + 28, Math.min(card.x + 74, card.x + card.w - 14), top + 29,
                    withAlpha(MapShieldConfig.getAccentColor(), 70));
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
            int border = blend(MapShieldConfig.getMenuBorderColor(), accent, 0.20f + visual.hover * 0.55f);

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
                ? withAlpha(MapShieldConfig.getMenuSurfaceColor(), 226)
                : MapShieldConfig.getMenuSurfaceColor();
    }

    private int cardColor() {
        return backgroundTextureId != null
                ? withAlpha(MapShieldConfig.getMenuCardColor(), 224)
                : MapShieldConfig.getMenuCardColor();
    }

    private int sidebarColor() {
        return backgroundTextureId != null
                ? withAlpha(blend(MapShieldConfig.getMenuSurfaceColor(), MapShieldConfig.getMenuCardColor(), 0.45f), 222)
                : MapShieldConfig.getMenuSurfaceColor();
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
        int bg = withAlpha(MapShieldConfig.getMenuSurfaceColor(), 210 + Math.round(35 * alpha));
        rounded(context, x, y, x + w, y + 30, 9, bg);
        stroke(context, x, y, x + w, y + 30, 9, MapShieldConfig.getMenuBorderColor());
        center(context, notice, width / 2, y + 8, MapShieldConfig.getAccentColor());
    }

    private void drawText(DrawContext context, int x, int y, String value, int color) {
        if (MapShieldConfig.isTextGradientEnabled() && value.length() > 1) {
            drawGradientText(context, x, y, value);
            return;
        }
        context.drawTextWithShadow(textRenderer, Text.literal(value), x, y, color);
    }

    private void drawGradientText(DrawContext context, int x, int y, String value) {
        int total = Math.max(1, textRenderer.getWidth(value));
        int cursor = x;
        for (int i = 0; i < value.length(); i++) {
            String ch = value.substring(i, i + 1);
            int width = Math.max(1, textRenderer.getWidth(ch));
            float t = Math.max(0.0f, Math.min(1.0f, (float) (cursor - x) / total));
            int color = blend(MapShieldConfig.getTextGradientStartColor(), MapShieldConfig.getTextGradientEndColor(), t);
            context.drawTextWithShadow(textRenderer, Text.literal(ch), cursor, y, color);
            cursor += width;
        }
    }

    private void drawTextRight(DrawContext context, int right, int y, String value, int color) {
        drawText(context, right - textRenderer.getWidth(value), y, value, color);
    }

    private void center(DrawContext context, String value, int centerX, int y, int color) {
        int x = centerX - textRenderer.getWidth(value) / 2;
        drawText(context, x, y, value, color);
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
        // Wheel-down arrives as a negative amount. Increase scroll state while
        // moving the page content upward. Both ends are hard-clamped.
        if (verticalAmount == 0.0 || maxContentScroll <= 0.0) {
            return true;
        }

        final double step = 12.0;
        final double previous = contentScroll;
        final double requested = previous - (verticalAmount > 0.0 ? step : -step);
        final double next = Math.max(0.0, Math.min(requested, maxContentScroll));

        if (next == previous) {
            return true;
        }

        targetContentScroll = next;
        return true;
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
        TextFieldWidget[] themeFields = {
                menuSurfaceHexField, menuCardHexField, menuBorderHexField,
                gradientStartHexField, gradientEndHexField,
                textGradientStartHexField, textGradientEndHexField
        };
        for (TextFieldWidget field : themeFields) {
            if (field != null) field.setY(field.getY() + deltaY);
        }
        if (passwordCurtainHexField != null) passwordCurtainHexField.setY(passwordCurtainHexField.getY() + deltaY);
        if (passwordMaskHexField != null) passwordMaskHexField.setY(passwordMaskHexField.getY() + deltaY);

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
        GENERAL("Tổng quan", "Tổng quan", "Thiết lập nhanh cho PUUZ SECURITY.", "Tổng quan"),
        MAP_ART("Map Shield", "Map Shield", "Kiểm soát cách Map Art hiển thị.", "Map"),
        PASSWORD("Password Shield", "Password Shield", "Bảo vệ mật khẩu khi nhập command.", "Password"),
        TOOLTIP("Tooltip Protection", "Tooltip Protection", "Bảo vệ Map Art trong tooltip.", "Tooltip"),
        MONEY("Money History", "Money History", "Lịch sử giao dịch trên HUD.", "Money"),
        QUICK_PAY("Quick Pay", "Quick Pay", "Chuẩn bị lệnh thanh toán nhanh.", "Quick Pay"),
        APPEARANCE("Giao diện", "Giao diện", "Màu sắc, text và ảnh nền.", "Giao diện"),
        HUD("HUD", "HUD", "Điều chỉnh hiển thị HUD.", "HUD"),
        CONTROLS("Phím tắt", "Phím tắt", "Quản lý phím tắt PUUZ SECURITY.", "Phím tắt"),
        UPDATES("Cập nhật", "Cập nhật", "Quản lý cập nhật từ GitHub.", "Update"),
        ABOUT("Giới thiệu", "Giới thiệu", "Thông tin về PUUZ SECURITY.", "About");

        final String label;
        final String title;
        final String subtitle;
        final String shortLabel;

        Tab(String label, String title, String subtitle) {
            this(label, title, subtitle, label);
        }

        Tab(String label, String title, String subtitle, String shortLabel) {
            this.label = label;
            this.title = title;
            this.subtitle = subtitle;
            this.shortLabel = shortLabel;
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

    private enum NavigationMode {
        SIDEBAR,
        ALL_TABS,
        COMPACT
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
        final NavigationMode navMode;
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
                NavigationMode navMode
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
            this.navMode = navMode;
            this.compact = navMode == NavigationMode.COMPACT;
        }

        static Layout calculate(int screenWidth, int screenHeight) {
            // Always keep the application panel inside the actual Minecraft
            // viewport. All navigation decisions are based on usable panel
            // width/height, not a fixed GUI scale.
            int availableWidth = Math.max(260, screenWidth - 8);
            int availableHeight = Math.max(260, screenHeight - 8);
            int marginX = screenWidth < 700 ? 8 : 16;
            int marginY = screenHeight < 500 ? 8 : 14;

            int panelWidth = Math.min(1180, Math.max(260, screenWidth - marginX * 2));
            int panelHeight = Math.min(760, Math.max(260, screenHeight - marginY * 2));
            panelWidth = Math.min(panelWidth, availableWidth);
            panelHeight = Math.min(panelHeight, availableHeight);

            int left = Math.max(4, (screenWidth - panelWidth) / 2);
            int top = Math.max(4, (screenHeight - panelHeight) / 2);
            int right = Math.min(screenWidth - 4, left + panelWidth);
            int bottom = Math.min(screenHeight - 4, top + panelHeight);
            panelWidth = right - left;
            panelHeight = bottom - top;

            // SIDEBAR: enough room to display every tab vertically while
            // retaining a substantial content viewport.
            if (panelWidth >= 700 && panelHeight >= 500) {
                int sidebarWidth = Math.max(188, Math.min(216, Math.round(panelWidth * 0.22f)));
                int sidebarX = left;
                int sidebarY = top + 72;
                int sidebarBottom = bottom - 42;
                int contentX = sidebarX + sidebarWidth + 1;
                int contentY = top + 72;
                int contentWidth = Math.max(260, right - contentX - 1);
                return new Layout(left, top, right, bottom,
                        sidebarX, sidebarY, sidebarWidth, sidebarBottom,
                        contentX, contentY, contentWidth, bottom - 42, 0, NavigationMode.SIDEBAR);
            }

            // ALL_TABS: medium landscape windows get a shallow 2-row tab strip.
            // Keep at least ~150px of content height; otherwise fall back to arrows.
            int contentX = left + 14;
            int contentWidth = Math.max(220, panelWidth - 28);
            int allTabCols = Math.max(3, Math.min(6, contentWidth / 104));
            int allTabRows = (Tab.values().length + allTabCols - 1) / allTabCols;
            int allTabHeight = allTabRows * 28 + Math.max(0, allTabRows - 1) * 6;
            int allTabContentY = top + 70 + allTabHeight + 14;
            int allTabContentBottom = bottom - 44;
            boolean allTabsFit = panelWidth >= 520
                    && panelHeight >= 390
                    && allTabContentBottom - allTabContentY >= 150;

            if (allTabsFit) {
                return new Layout(left, top, right, bottom,
                        0, 0, 0, 0,
                        contentX, allTabContentY, contentWidth, allTabContentBottom,
                        top + 70, NavigationMode.ALL_TABS);
            }

            // COMPACT: very small/short windows use a 3-control navigation row.
            int tabsY = top + 70;
            int compactContentY = tabsY + 42;
            int compactContentBottom = Math.max(compactContentY + 90, bottom - 44);
            return new Layout(left, top, right, bottom,
                    0, 0, 0, 0,
                    contentX, compactContentY, contentWidth, compactContentBottom,
                    tabsY, NavigationMode.COMPACT);
        }
    }
}
