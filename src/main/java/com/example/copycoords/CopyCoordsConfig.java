package com.example.copycoords;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple configuration class storing the user's preferences for the
 * CopyCoords mod.  This class is responsible for reading and writing
 * a JSON file in the config directory.  The AutoConfig dependency has
 * been removed to avoid build issues on newer versions of Minecraft.
 */
public class CopyCoordsConfig {

    /**
     * When true, the coordinates will be copied to the system clipboard after
     * running the /copycoords command.  Default value is {@code true} so
     * players do not need to toggle it manually after installing.
     */
    public boolean copyToClipboard = true;

    /**
     * Gson instance used for serialization and deserialization.  Use
     * pretty printing for readability.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Cached path to the configuration file.  Set when loading the config.
     */
    private static Path configPath;

    /**
     * Loads the configuration from disk.  If the file does not exist, a new
     * configuration with default values is created and saved.  This method
     * should be called during mod initialisation.
     *
     * @return an instance of {@link CopyCoordsConfig} populated from disk or
     *         containing default values when the file is missing or invalid
     */
    public static CopyCoordsConfig load() {
        // Determine the config directory using FabricLoader.  The config dir
        // resides in the game directory and is safe to write user data to.
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
                // Log the exception and fall back to defaults
                System.err.println("Failed to read copycoords configuration: " + e.getMessage());
            }
        }

        // File does not exist or could not be read; use defaults and save a new file
        CopyCoordsConfig config = new CopyCoordsConfig();
        config.save();
        return config;
    }

    /**
     * Saves the current configuration to disk.  The parent directories are
     * created as necessary.  Any IO exceptions are printed to the standard
     * error stream.
     */
    public void save() {
        if (configPath == null) {
            // If save is called before load, determine the path now
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