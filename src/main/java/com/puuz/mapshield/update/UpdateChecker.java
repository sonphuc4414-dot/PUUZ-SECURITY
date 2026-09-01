package com.puuz.mapshield.update;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.puuz.mapshield.PuuzMapShieldClient;
import com.puuz.mapshield.config.MapShieldConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Best-effort GitHub Releases update checker for PUUZ SECURITY.
 *
 * Network and disk work are performed away from Minecraft's client thread.
 * Any failure is ignored so update checking can never break gameplay.
 */
public final class UpdateChecker {
    private static final String GITHUB_REPOSITORY =
            "sonphuc4414-dot/PUUZ-SECURITY";

    private static final String GITHUB_API =
            "https://api.github.com/repos/sonphuc4414-dot/PUUZ-SECURITY/releases/latest";

    private static final String FALLBACK_RELEASES_URL =
            "https://github.com/sonphuc4414-dot/PUUZ-SECURITY/releases";

    private static final Duration CONNECT_TIMEOUT =
            Duration.ofSeconds(5);

    private static final Duration REQUEST_TIMEOUT =
            Duration.ofSeconds(8);

    private static final long INITIAL_DELAY_SECONDS = 10L;
    private static final long CHECK_INTERVAL_HOURS = 12L;

    private static final Gson GSON = new Gson();

    private static final ScheduledExecutorService EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(
                        runnable,
                        "PUUZ-Security-UpdateChecker"
                );
                thread.setDaemon(true);
                return thread;
            });

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

    private static final AtomicBoolean STARTED =
            new AtomicBoolean(false);

    private static final AtomicBoolean CHECK_RUNNING =
            new AtomicBoolean(false);

    private static final AtomicReference<UpdateInfo> PENDING_UPDATE =
            new AtomicReference<>();

    private static final Path CACHE_FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("puuz-security-update.json");

    private static volatile long lastCheckAt = 0L;
    private static volatile String lastNotifiedVersion = "";

    private UpdateChecker() {
    }

    /**
     * Starts the checker once. Safe to call multiple times.
     */
    public static void start(MinecraftClient client) {
        if (client == null || !MapShieldConfig.isUpdateCheckerEnabled()) {
            return;
        }

        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                loadCache();
                sleepQuietly(INITIAL_DELAY_SECONDS, TimeUnit.SECONDS);
                requestCheckInternal();
            } catch (Exception ignored) {
                // Never affect Minecraft startup.
            }
        });

        EXECUTOR.scheduleWithFixedDelay(
                UpdateChecker::requestCheckInternal,
                INITIAL_DELAY_SECONDS + CHECK_INTERVAL_HOURS * 3600L,
                CHECK_INTERVAL_HOURS * 3600L,
                TimeUnit.SECONDS
        );
    }

    /**
     * Called from the client tick.
     *
     * No network or file I/O happens here.
     */
    public static void tick(MinecraftClient client) {
        // The client entrypoint consumes PENDING_UPDATE safely on this thread.
    }

    /**
     * Returns and removes the pending update.
     */
    public static UpdateInfo consumePending() {
        UpdateInfo update = PENDING_UPDATE.getAndSet(null);

        if (update != null) {
            lastNotifiedVersion = update.version();
            EXECUTOR.execute(UpdateChecker::saveCache);
        }

        return update;
    }

    /**
     * Optional manual check for testing or future UI use.
     */
    public static void checkNow() {
        requestCheckInternal();
    }

    /**
     * Public compatibility entry point used by the settings GUI.
     * The actual HTTP request is still executed on the background executor.
     */
    public static void requestCheck() {
        requestCheckInternal();
    }

    private static void requestCheckInternal() {
        if (!MapShieldConfig.isUpdateCheckerEnabled()) {
            return;
        }

        if (!CHECK_RUNNING.compareAndSet(false, true)) {
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                UpdateInfo update = queryLatestRelease();
                if (update != null && shouldNotify(update)) {
                    PENDING_UPDATE.compareAndSet(null, update);
                }
                lastCheckAt = System.currentTimeMillis();
                saveCache();
            } catch (Exception ignored) {
                // Update checking is strictly best-effort.
            } finally {
                CHECK_RUNNING.set(false);
            }
        });
    }

    private static boolean shouldNotify(UpdateInfo update) {
        String current = normalizeVersion(getCurrentVersion());
        if (current == null || !isNewer(update.version(), current)) {
            return false;
        }

        if (update.version().equals(lastNotifiedVersion)) {
            return false;
        }

        return PENDING_UPDATE.get() == null;
    }

    private static UpdateInfo queryLatestRelease()
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header(
                        "User-Agent",
                        "PUUZ-Security/" + getCurrentVersion()
                )
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() != 200) {
            return null;
        }

        try {
            JsonObject root = GSON.fromJson(
                    response.body(),
                    JsonObject.class
            );

            if (root == null || !root.has("tag_name")) {
                return null;
            }

            String version = normalizeVersion(
                    root.get("tag_name").getAsString()
            );

            if (version == null) {
                return null;
            }

            String releaseUrl = FALLBACK_RELEASES_URL;

            if (root.has("html_url")
                    && !root.get("html_url").isJsonNull()) {

                String candidate =
                        root.get("html_url").getAsString();

                if (candidate != null && !candidate.isBlank()) {
                    releaseUrl = candidate;
                }
            }

            return new UpdateInfo(
                    version,
                    releaseUrl
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String getCurrentVersion() {
        try {
            return FabricLoader.getInstance()
                    .getModContainer(PuuzMapShieldClient.MOD_ID)
                    .map(container -> container.getMetadata()
                            .getVersion()
                            .getFriendlyString())
                    .orElse("0.0.0");
        } catch (Exception ignored) {
            return "0.0.0";
        }
    }

    private static String normalizeVersion(String value) {
        if (value == null) {
            return null;
        }

        String version = value.trim();

        while (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }

        int dash = version.indexOf('-');
        if (dash >= 0) {
            version = version.substring(0, dash);
        }

        int plus = version.indexOf('+');
        if (plus >= 0) {
            version = version.substring(0, plus);
        }

        int[] parsed = parseVersion(version);
        if (parsed == null) {
            return null;
        }

        return parsed[0] + "." + parsed[1] + "." + parsed[2];
    }

    private static boolean isNewer(String remote, String local) {
        int[] remoteParts = parseVersion(remote);
        int[] localParts = parseVersion(local);

        if (remoteParts == null || localParts == null) {
            return false;
        }

        for (int i = 0; i < 3; i++) {
            if (remoteParts[i] != localParts[i]) {
                return remoteParts[i] > localParts[i];
            }
        }

        return false;
    }

    private static int[] parseVersion(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] parts = value.trim().split("\\.");
        if (parts.length < 1 || parts.length > 3) {
            return null;
        }

        int[] result = {0, 0, 0};

        try {
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].matches("\\d+")) {
                    return null;
                }
                result[i] = Integer.parseInt(parts[i]);
            }
            return result;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void loadCache() {
        try {
            if (!Files.isRegularFile(CACHE_FILE)) {
                return;
            }

            JsonObject root = GSON.fromJson(
                    Files.readString(
                            CACHE_FILE,
                            StandardCharsets.UTF_8
                    ),
                    JsonObject.class
            );

            if (root == null) {
                return;
            }

            if (root.has("lastCheckAt")) {
                lastCheckAt = root.get("lastCheckAt").getAsLong();
            }

            if (root.has("lastNotifiedVersion")) {
                lastNotifiedVersion =
                        root.get("lastNotifiedVersion").getAsString();
            }
        } catch (Exception ignored) {
            lastCheckAt = 0L;
            lastNotifiedVersion = "";
        }
    }

    private static void saveCache() {
        try {
            Path parent = CACHE_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            JsonObject root = new JsonObject();
            root.addProperty("lastCheckAt", lastCheckAt);
            root.addProperty("lastNotifiedVersion", lastNotifiedVersion);

            Path temp = CACHE_FILE.resolveSibling(
                    CACHE_FILE.getFileName() + ".tmp"
            );

            Files.writeString(
                    temp,
                    GSON.toJson(root),
                    StandardCharsets.UTF_8
            );

            try {
                Files.move(
                        temp,
                        CACHE_FILE,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (Exception ignored) {
                Files.move(
                        temp,
                        CACHE_FILE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (Exception ignored) {
            // Cache failure is harmless.
        }
    }

    private static void sleepQuietly(
            long duration,
            TimeUnit unit
    ) {
        try {
            unit.sleep(duration);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public static String getRepository() {
        return GITHUB_REPOSITORY;
    }

    public static String getReleasesPage() {
        return FALLBACK_RELEASES_URL;
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }
}
