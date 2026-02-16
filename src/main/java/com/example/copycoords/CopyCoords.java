package com.example.copycoords;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

// Main mod class that initializes the mod and registers the /copycoords command
public class CopyCoords implements ClientModInitializer {
    public static CopyCoordsConfig config;

    // Initialize the mod client-side by loading config and registering commands/keybinds
    @Override
    public void onInitializeClient() {
        config = CopyCoordsConfig.load();
        CopyCoordsBind.register();
        // Register the /copycoords command with Brigadier
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("copycoords");
            builder.executes(context -> executeCopyCoords(context));
            dispatcher.register(builder);
        });
    }

    // Execute the /copycoords command to get player coordinates
    private int executeCopyCoords(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        // Verify player is in game
        if (player == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        // Extract player's block coordinates
        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();
        String coordString = x + " " + y + " " + z;

        // Print coordinates to chat
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.coords_printed", coordString));

        // Copy to clipboard if enabled in config
        if (config.copyToClipboard) {
            try {
                // Use Windows clip.exe to copy to system clipboard
                Process process = Runtime.getRuntime().exec("cmd.exe /c echo " + coordString + " | clip.exe");
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copied"));
                } else {
                    Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copy_failed", "clip.exe returned " + exitCode));
                }
            } catch (Exception e) {
                // Handle clipboard copy errors gracefully
                String errorMsg = e.getMessage();
                if (errorMsg == null || errorMsg.isEmpty()) {
                    errorMsg = e.getClass().getSimpleName();
                }
                Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copy_failed", errorMsg));
            }
        }

        return Command.SINGLE_SUCCESS;
    }
}