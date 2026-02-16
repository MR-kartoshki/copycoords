package com.example.copycoords;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.example.copycoords.CopyCoordsConfig;

/**
 * The main mod class responsible for initializing the CopyCoords mod.  This
 * mod registers a client‑side command that displays the player's current
 * coordinates in chat and optionally copies them to the system clipboard.
 */
public class CopyCoords implements ClientModInitializer {

    /**
     * Loaded configuration instance.  This is populated during
     * {@link #onInitializeClient()} by reading from disk.  Mod code should
     * reference this field rather than creating new instances.
     */
    public static CopyCoordsConfig config;

    @Override
    public void onInitializeClient() {

        // Load configuration from the config directory.  A new file will be
        // created with default values if it doesn't already exist.  The
        // result is stored in a static field so it can be referenced by
        // command execution and the config screen.
        config = CopyCoordsConfig.load();

        // Register keybinding for quick coordinate copy.
        CopyCoordsBind.register();

        // Register the /copycoords command.  Client commands live entirely on
        // the client and use the ClientCommandManager class.
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder builder = ClientCommandManager.literal("copycoords");
            builder.executes(context -> executeCopyCoords(context));
            dispatcher.register(builder);
        });
    }

    /**
     * Executes the /copycoords command.  Sends a chat message containing the
     * player's current X, Y and Z block coordinates.  If the configuration
     * dictates, the coordinates are also copied to the system clipboard.
     *
     * @param context the Brigadier command context
     * @return an integer result (1 for success)
     */
    private int executeCopyCoords(CommandContext context) {
        // Retrieve the player from the client.  We need to ensure we have
        // access to the Minecraft instance.  The player may be null if not
        // in game.
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        // Build a formatted coordinate string.  We use block coordinates for
        // better readability.
        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();
        String coordString = x + " " + y + " " + z;

        // Send the coordinates to chat.
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.coords_printed", coordString));

        // If configured, also copy the coordinates to the system clipboard.
        if (config.copyToClipboard) {
            try {
                // Use Windows clip.exe command to copy to clipboard.
                // This works even in headless environments like Minecraft.
                Process process = Runtime.getRuntime().exec("cmd.exe /c echo " + coordString + " | clip.exe");
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copied"));
                } else {
                    Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copy_failed", "clip.exe returned " + exitCode));
                }
            } catch (Exception e) {
                // Log the error if clipboard access fails.
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