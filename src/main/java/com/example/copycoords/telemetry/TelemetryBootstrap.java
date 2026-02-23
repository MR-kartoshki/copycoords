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
                System.out.println("CopyCoords: Telemetry not sent! (disabled)");
                return;
            }

            final long now = System.currentTimeMillis();
            if (now - cfg.lastSent < RATE_LIMIT_MS) {
                System.out.println("CopyCoords: Telemetry not sent! (rate limited)");
                return;
            }

            // `getModContainer("minecraft")` returns the version baked into the
            // development jar, which in my workspace is always 1.21.11.  That means
            // when running the mod against an older or newer game the telemetry
            // payload would still claim 1.21.11.  The loader provides a direct
            // getter for the *running* game version which is what we really want.
            String minecraftVersion = FabricLoader.getInstance()
                    .getGameVersion()
                    .getFriendlyString();
            // log so we can verify in the game log which version was detected
            System.out.println("CopyCoords: detected Minecraft version " + minecraftVersion);

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
                        System.out.println("CopyCoords: Telemetry sent!");
                    })
                    .exceptionally(ex -> {
                        System.out.println("CopyCoords: Telemetry not sent! (" + ex.getMessage() + ")");
                        return null;
                    });
        } catch (Exception ex) {
            System.out.println("CopyCoords: Telemetry not sent! (initialization error: " + ex.getMessage() + ")");
        }
    }
}
