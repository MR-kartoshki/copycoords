package com.example.copycoords;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CopyCoordsConfig {

    public boolean copyToClipboard = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath;

    public static CopyCoordsConfig load() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        configPath = configDir.resolve("copycoords.json");

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

        CopyCoordsConfig config = new CopyCoordsConfig();
        config.save();
        return config;
    }

    public void save() {
        if (configPath == null) {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            configPath = configDir.resolve("copycoords.json");
        }
        try {
            Files.createDirectories(configPath.getParent());
            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            System.err.println("Failed to save copycoords configuration: " + e.getMessage());
        }
    }
}