package com.example.copycoords;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Manages configuration settings for the mod, including clipboard copy preferences
public class CopyCoordsConfig {

    public boolean copyToClipboard = true;
    public boolean copyConvertedToClipboard = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath;

    // Load configuration from file or create default if not found
    public static CopyCoordsConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        configPath = configDir.resolve("copycoords.json");

        // Try to read existing config file
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                CopyCoordsConfig config = GSON.fromJson(json, CopyCoordsConfig.class);
                if (config != null) {
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
            Path configDir = FabricLoader.getInstance().getConfigDir();
            configPath = configDir.resolve("copycoords.json");
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