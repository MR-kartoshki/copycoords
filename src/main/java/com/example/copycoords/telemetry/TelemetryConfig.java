package com.example.copycoords.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TelemetryConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("copycoords")
            .resolve("telemetry.json");

    public boolean enabled = true;
    public long lastSent = 0L;

    public static TelemetryConfig loadOrCreate() {
        TelemetryConfig defaults = new TelemetryConfig();

        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(CONFIG_PATH)) {
                defaults.save();
                return defaults;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                TelemetryConfig loaded = GSON.fromJson(reader, TelemetryConfig.class);
                return loaded != null ? loaded : defaults;
            }
        } catch (Exception ignored) {
            try {
                Path parent = CONFIG_PATH.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                defaults.save();
            } catch (Exception ignoredInner) {
            }
            return defaults;
        }
    }

    public void save() {
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception ignored) {
        }
    }
}