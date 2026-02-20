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

            // Add toggle option for showing dimension in coordinates
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("Show dimension in coordinates"),
                            CopyCoords.config.showDimensionInCoordinates)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("If enabled, the dimension (Overworld, Nether, End) will be shown when copying coordinates."))
                    .setSaveConsumer(newValue -> CopyCoords.config.showDimensionInCoordinates = newValue)
                    .build());

            // Add selector for coordinate format
            @SuppressWarnings("null")
            CoordinateFormat currentFormat = CoordinateFormat.fromId(CopyCoords.config.coordinateFormat);
            general.addEntry(entryBuilder.startSelector(
                            Component.literal("Coordinate format"),
                            CoordinateFormat.values(),
                            currentFormat)
                    .setDefaultValue(CoordinateFormat.SPACE_SEPARATED)
                    .setTooltip(Component.literal("Choose how coordinates are displayed:\n" +
                            "Space: 100 64 200\n" +
                            "Bracket: [100, 64, 200]\n" +
                            "XYZ: X:100 Y:64 Z:200"))
                    .setSaveConsumer(newFormat -> CopyCoords.config.coordinateFormat = newFormat.getId())
                    .build());


            // Paste to chat input option
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("Paste coordinates into chat input"),
                            CopyCoords.config.pasteToChatInput)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("If enabled, copied coordinates will be placed into the chat input box (without sending)."))
                    .setSaveConsumer(newValue -> CopyCoords.config.pasteToChatInput = newValue)
                    .build());

            // Save config to file when changes are applied
            builder.setSavingRunnable(() -> CopyCoords.config.save());

            return builder.build();
        };
    }
}
