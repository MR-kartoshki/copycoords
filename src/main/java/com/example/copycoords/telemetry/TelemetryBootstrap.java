package com.example.copycoords.telemetry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;

public final class TelemetryBootstrap {
    private static final long RATE_LIMIT_MS = 86_400_000L;
    private static final String ENDPOINT = "https://140.86.211.122.sslip.io/ingest";

    private TelemetryBootstrap() {
    }

    public static void initAndMaybeSend() {
        try {
            TelemetryConfig cfg = TelemetryConfig.loadOrCreate();
            if (!cfg.enabled) {
                return;
            }

            final long now = System.currentTimeMillis();
            if (now - cfg.lastSent < RATE_LIMIT_MS) {
                return;
            }

            String minecraftVersion = FabricLoader.getInstance()
                    .getModContainer("minecraft")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");

            JsonObject payload = new JsonObject();
            payload.addProperty("mc", minecraftVersion);
            payload.addProperty("e", "c");
            payload.addProperty("l", "fabric");

            JsonArray mods = new JsonArray();
            mods.add("copycoords");
            payload.add("m", mods);

            TelemetrySender.send(URI.create(ENDPOINT), payload)
                    .thenRun(() -> {
                        try {
                            cfg.lastSent = now;
                            cfg.save();
                        } catch (Exception ignored) {
                        }
                    })
                    .exceptionally(ignored -> null);
        } catch (Exception ignored) {
        }
    }
}