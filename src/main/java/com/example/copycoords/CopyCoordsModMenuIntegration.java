package com.example.copycoords;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class CopyCoordsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            if (CopyCoords.config == null) {
                CopyCoords.config = CopyCoordsConfig.load();
            }
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.literal("CopyCoords Configuration"));

            ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.literal("Copy coordinates to clipboard"),
                            CopyCoords.config.copyToClipboard)
                    .setDefaultValue(true)
                    .setTooltip(Component.literal("If enabled, the /copycoords command will copy the coordinates to your system clipboard."))
                    .setSaveConsumer(newValue -> CopyCoords.config.copyToClipboard = newValue)
                    .build());

            builder.setSavingRunnable(() -> CopyCoords.config.save());

            return builder.build();
        };
    }
}