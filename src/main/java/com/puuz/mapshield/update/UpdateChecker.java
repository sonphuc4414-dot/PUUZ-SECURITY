package com.puuz.mapshield.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.puuz.mapshield.PuuzMapShieldClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight, fail-safe GitHub Releases checker.
 *
 * The HTTP request never runs on Minecraft's client/render thread. Any failure
 * is ignored so update checking can never prevent the mod from starting.
 */
public final class UpdateChecker {
    /** Replace OWNER with the GitHub account/org that owns the repository. */
    private static final String GITHUB_REPOSITORY = "OWNER/puuz-map-shield";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final long CHECK_INTERVAL_HOURS = 12L;


    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "PUUZ-Map-Shield-UpdateChecker");
        thread.setDaemon(true);
        return thread;
    });

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .executor(EXECUTOR)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final AtomicBoolean CHECK_IN_PROGRESS = new AtomicBoolean(false);
    private static volatile UpdateInfo pendingUpdate;

    private UpdateChecker() {
    }

    public static void start(MinecraftClient client) {
        if (!isConfigured()) {
            return;
        }

        scheduleCheck(client, 10, TimeUnit.SECONDS);
        scheduleCheck(client, CHECK_INTERVAL_HOURS, TimeUnit.HOURS);
    }

    private static void scheduleCheck(MinecraftClient client, long delay, TimeUnit unit) {
        EXECUTOR.schedule(() -> check(client), delay, unit);
    }

    private static void check(MinecraftClient client) {
        if (!CHECK_IN_PROGRESS.compareAndSet(false, true)) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + GITHUB_REPOSITORY + "/releases/latest"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "PUUZ-Map-Shield/" + getCurrentVersion())
                    .GET()
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(UpdateChecker::parseResponse)
                    .exceptionally(ignored -> null)
                    .thenAccept(info -> {
                        if (info != null) {
                            pendingUpdate = info;
                        }
                    })
                    .whenComplete((ignored, throwable) -> CHECK_IN_PROGRESS.set(false));
        } catch (Exception ignored) {
            CHECK_IN_PROGRESS.set(false);
        }
    }

    private static UpdateInfo parseResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            return null;
        }

        try {
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            String tag = root.has("tag_name") ? root.get("tag_name").getAsString() : null;
            String url = root.has("html_url") ? root.get("html_url").getAsString() : null;
            if (tag == null || url == null) {
                return null;
            }

            String latest = normalizeVersion(tag);
            if (!isNewer(latest, getCurrentVersion())) {
                return null;
            }
            return new UpdateInfo(latest, url);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeVersion(String value) {
        String version = value.trim();
        while (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        int dash = version.indexOf('-');
        if (dash >= 0) {
            version = version.substring(0, dash);
        }
        return version;
    }

    private static boolean isNewer(String remote, String local) {
        int[] remoteParts = parseVersion(remote);
        int[] localParts = parseVersion(local);
        for (int i = 0; i < 3; i++) {
            if (remoteParts[i] != localParts[i]) {
                return remoteParts[i] > localParts[i];
            }
        }
        return false;
    }

    private static int[] parseVersion(String value) {
        int[] result = new int[3];
        String[] parts = value.split("\\.");
        for (int i = 0; i < Math.min(3, parts.length); i++) {
            try {
                result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9].*", ""));
            } catch (Exception ignored) {
                result[i] = 0;
            }
        }
        return result;
    }

    private static void showUpdateNotification(MinecraftClient client, UpdateInfo info) {
        if (client.player == null || pendingUpdate != info) {
            return;
        }

        Text update = Text.literal("PUUZ Map Shield ")
                .formatted(Formatting.AQUA, Formatting.BOLD)
                .append(Text.literal("has an update: ").formatted(Formatting.WHITE))
                .append(Text.literal("v" + info.version()).formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal("  •  ").formatted(Formatting.DARK_GRAY))
                .append(Text.literal("Open").formatted(Formatting.YELLOW, Formatting.BOLD)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(info.releaseUrl())))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Open GitHub release")))));

        client.player.sendMessage(update, false);
        pendingUpdate = null;
    }

    public static void tick(MinecraftClient client) {
        UpdateInfo info = pendingUpdate;
        if (info != null && client.player != null) {
            showUpdateNotification(client, info);
        }
    }

    private static String getCurrentVersion() {
        return FabricLoader.getInstance().getModContainer(PuuzMapShieldClient.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }

    private static boolean isConfigured() {
        return !GITHUB_REPOSITORY.startsWith("OWNER/") && GITHUB_REPOSITORY.contains("/");
    }

    /** Opens the latest known release in the platform browser. */
    public static void openLatestRelease() {
        UpdateInfo info = pendingUpdate;
        if (info == null) {
            return;
        }
        try {
            Util.getOperatingSystem().open(info.releaseUrl());
        } catch (Exception ignored) {
            // Browser launch failure must never crash Minecraft.
        }
    }
}
