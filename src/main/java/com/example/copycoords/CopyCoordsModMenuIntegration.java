package com.example.copycoords;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.example.copycoords.telemetry.TelemetryConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
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
                        TelemetryConfig telemetryConfig = TelemetryConfig.loadOrCreate();
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

            // custom template overrides the above formats; we show preview in the tooltip
            String initialPreview = CopyCoords.previewForTemplate(CopyCoords.config.coordinateTemplate);
            final Object[] templateEntryRef = new Object[1];
            Object templateEntryObj = entryBuilder.startStrField(
                            Component.literal("Coordinate template"),
                            CopyCoords.config.coordinateTemplate)
                    .setDefaultValue("")
                    .setTooltip(Component.literal("Empty to use Coordinate format above. Placeholders: {x},{y},{z},{dimension},{dimName}" +
                            (initialPreview.isEmpty() ? "" : "\nPreview: " + initialPreview)))
                    .setSaveConsumer(newValue -> {
                        CopyCoords.config.coordinateTemplate = newValue;
                        // attempt to update tooltip via reflection
                        try {
                            Object entry = templateEntryRef[0];
                            entry.getClass()
                                    .getMethod("setTooltip", Component.class)
                                    .invoke(entry, Component.literal("Empty to use Coordinate format above. Placeholders: {x},{y},{z},{dimension},{dimName}" +
                                            (CopyCoords.previewForTemplate(newValue).isEmpty() ? "" : "\nPreview: " + CopyCoords.previewForTemplate(newValue))));
                        } catch (Throwable ignored) {
                            // ignore; tooltip will be correct when screen reopened
                        }
                    })
                    .build();
            templateEntryRef[0] = templateEntryObj;
            general.addEntry((AbstractConfigListEntry<?>)templateEntryObj);


            // Paste to chat input option
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("Paste coordinates into chat input"),
                            CopyCoords.config.pasteToChatInput)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("If enabled, copied coordinates will be placed into the chat input box (without sending)."))
                    .setSaveConsumer(newValue -> CopyCoords.config.pasteToChatInput = newValue)
                    .build());

            ConfigCategory mapLinks = builder.getOrCreateCategory(Component.literal("Map Links"));

            mapLinks.addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("Enable map links"),
                            CopyCoords.config.mapLinksEnabled)
                    .setDefaultValue(false)
                    .setTooltip(Component.literal("If enabled, coordinate chat output includes clickable map links."))
                    .setSaveConsumer(newValue -> CopyCoords.config.mapLinksEnabled = newValue)
                    .build());

            mapLinks.addEntry(entryBuilder.startStrField(
                            Component.literal("Dynmap URL template"),
                            CopyCoords.config.dynmapUrlTemplate)
                    .setDefaultValue("http://localhost:8123/?world={world}&map=flat&x={x}&y={y}&z={z}")
                    .setTooltip(Component.literal("Shown only when Dynmap is installed. Placeholders: {x} {y} {z} {world} {worldEncoded} {dimension} {dimensionEncoded}"))
                    .setSaveConsumer(newValue -> CopyCoords.config.dynmapUrlTemplate = newValue)
                    .build());

            mapLinks.addEntry(entryBuilder.startStrField(
                            Component.literal("BlueMap URL template"),
                            CopyCoords.config.bluemapUrlTemplate)
                    .setDefaultValue("http://localhost:8100/#world:{world}:{x}:{y}:{z}:150:0:0:0:0:perspective")
                    .setTooltip(Component.literal("Shown only when BlueMap is installed. Placeholders: {x} {y} {z} {world} {worldEncoded} {dimension} {dimensionEncoded}"))
                    .setSaveConsumer(newValue -> CopyCoords.config.bluemapUrlTemplate = newValue)
                    .build());

            mapLinks.addEntry(entryBuilder.startStrField(
                            Component.literal("Custom web map URL template"),
                            CopyCoords.config.webMapUrlTemplate)
                    .setDefaultValue("")
                    .setTooltip(Component.literal("Optional always-available map link. Use http(s) URL and placeholders like {x}, {y}, {z}, {world}."))
                    .setSaveConsumer(newValue -> CopyCoords.config.webMapUrlTemplate = newValue)
                    .build());

            ConfigCategory telemetry = builder.getOrCreateCategory(Component.literal("Telemetry"));

            telemetry.addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("Enable telemetry"),
                            telemetryConfig.enabled)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("If enabled, CopyCoords sends a minimal anonymous ping at most once every 24 hours."))
                    .setSaveConsumer(newValue -> telemetryConfig.enabled = newValue)
                    .build());

            // Save config to file when changes are applied
            builder.setSavingRunnable(() -> {
                CopyCoords.config.save();
                telemetryConfig.save();
            });

            return builder.build();
        };
    }
}
