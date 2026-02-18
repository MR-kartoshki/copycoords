package com.example.copycoords;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

// Integrates with Mod Menu to provide a GUI for mod configuration
@Environment(EnvType.CLIENT)
public class CopyCoordsModMenuIntegration implements ModMenuApi {
    // Create and return the mod configuration GUI screen
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            // Load config if not already loaded
            if (CopyCoords.config == null) {
                CopyCoords.config = CopyCoordsConfig.load();
            }
            // Build the configuration GUI
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("CopyCoords Configuration"));

            // Create General settings category
            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // Add toggle option for clipboard copying feature
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("Copy coordinates to clipboard"),
                            CopyCoords.config.copyToClipboard)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("If enabled, the /copycoords command will copy the coordinates to your system clipboard."))
                    .setSaveConsumer(newValue -> CopyCoords.config.copyToClipboard = newValue)
                    .build());

            // Add toggle option for copying converted coordinates
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("Copy converted coordinates to clipboard"),
                            CopyCoords.config.copyConvertedToClipboard)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("If enabled, the /convertcoords command will copy the converted coordinates to your system clipboard."))
                    .setSaveConsumer(newValue -> CopyCoords.config.copyConvertedToClipboard = newValue)
                    .build());

            // Save config to file when changes are applied
            builder.setSavingRunnable(() -> CopyCoords.config.save());

            return builder.build();
        };
    }
}
