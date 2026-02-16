package com.example.copycoords;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
// ModConfigProvider and ConfigScreen are no longer used. Cloth Config 11
// constructs config screens via ConfigBuilder directly.
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Provides integration with the Mod Menu mod.  When Mod Menu requests this
 * mod's configuration screen, the {@link #getModConfigScreenFactory()} method
 * builds a configuration screen using Cloth Config.  The screen exposes
 * a boolean toggle for copying coordinates to the clipboard and saves
 * changes back to {@link CopyCoordsConfig} when closed.
 */
@Environment(EnvType.CLIENT)
public class CopyCoordsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            // Ensure the configuration is loaded; Mod Menu may request the
            // screen before the regular client entrypoint has called
            // `CopyCoordsConfig.load()`.
            if (CopyCoords.config == null) {
                CopyCoords.config = CopyCoordsConfig.load();
            }
            // Create a new config builder each time the config screen is opened.
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("CopyCoords Configuration"));

            // General category for our single option.  Categories group
            // configuration entries together.
            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

            // Entry builder for constructing individual config options.
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // Boolean toggle for copying coordinates to clipboard.  When the
            // user changes the value, update the config instance accordingly.
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("Copy coordinates to clipboard"),
                            CopyCoords.config.copyToClipboard)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("If enabled, the /copycoords command will copy the coordinates to your system clipboard."))
                    .setSaveConsumer(newValue -> CopyCoords.config.copyToClipboard = newValue)
                    .build());

            // When the user clicks "Done" on the config screen, save the
            // configuration back to disk.
            builder.setSavingRunnable(() -> CopyCoords.config.save());

            return builder.build();
        };
    }
}