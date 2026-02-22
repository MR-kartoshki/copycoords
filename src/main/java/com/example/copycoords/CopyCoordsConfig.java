package com.example.copycoords;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Manages configuration settings for the mod, including clipboard copy preferences
public class CopyCoordsConfig {

    public boolean copyToClipboard = true;
    public boolean copyConvertedToClipboard = true;
    public boolean showDimensionInCoordinates = true;
    public boolean pasteToChatInput = false; // if true, paste coords into chat input instead of (or in addition to) clipboard
    public String coordinateFormat = "space"; // "space", "bracket", or "xyz"
    public String coordinateTemplate = ""; // optional custom format, overrides coordinateFormat when nonempty
    public boolean mapLinksEnabled = false;
    public String dynmapUrlTemplate = "http://localhost:8123/?world={world}&map=flat&x={x}&y={y}&z={z}";
    public String bluemapUrlTemplate = "http://localhost:8100/#world:{world}:{x}:{y}:{z}:150:0:0:0:0:perspective";
    public String webMapUrlTemplate = "";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(CoordinateFormat.class, (JsonDeserializer<CoordinateFormat>) (json, typeOfT, context) -> {
                String value = json.getAsString();
                return CoordinateFormat.fromId(value);
            })
            .create();

    private static Path configPath;

    private static Path getScopedConfigPath() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return configDir.resolve("copycoords").resolve("copycoords.json");
    }

    private static Path getLegacyConfigPath() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return configDir.resolve("copycoords.json");
    }

    // Load configuration from file or create default if not found
    public static CopyCoordsConfig load() {
        configPath = getScopedConfigPath();
        Path legacyPath = getLegacyConfigPath();

        // Try to read existing config file
        Path readPath = Files.exists(configPath) ? configPath : legacyPath;
        if (Files.exists(readPath)) {
            try {
                String json = Files.readString(readPath);
                CopyCoordsConfig config = GSON.fromJson(json, CopyCoordsConfig.class);
                if (config != null) {
                    if (!configPath.equals(readPath)) {
                        config.save();
                    }
                    return config;
                }
            } catch (IOException e) {
                System.err.println("Failed to read copycoords configuration: " + e.getMessage());
            }
        }

        // Create and save default config if file doesn't exist
        CopyCoordsConfig config = new CopyCoordsConfig();
        config.save();
        return config;
    }

    // Save current configuration to file
    public void save() {
        if (configPath == null) {
            configPath = getScopedConfigPath();
        }
        try {
            // Ensure config directory exists before writing
            Files.createDirectories(configPath.getParent());
            // Convert config object to JSON and write to file
            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            System.err.println("Failed to save copycoords configuration: " + e.getMessage());
        }
    }
}
