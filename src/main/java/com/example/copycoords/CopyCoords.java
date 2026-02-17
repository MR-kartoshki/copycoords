package com.example.copycoords;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

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
        // Register the /copycoords and /convertcoords commands with Brigadier
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("copycoords");
            builder.executes(context -> executeCopyCoords(context));
            dispatcher.register(builder);

            // Register /convertcoords <goal> <pos>
            LiteralArgumentBuilder<FabricClientCommandSource> conv = ClientCommandManager.literal("convertcoords");
            
            // Suggest goal dimension values
            SuggestionProvider<FabricClientCommandSource> dimSuggestions = (ctx, sb) -> {
                sb.suggest("overworld");
                sb.suggest("nether");
                return sb.buildFuture();
            };

            // Coordinate suggestions: suggest common patterns
            SuggestionProvider<FabricClientCommandSource> coordSuggestions = (ctx, sb) -> {
                String remaining = sb.getRemaining().toLowerCase();
                if (remaining.isEmpty() || remaining.equals("~")) {
                    sb.suggest("~ ~ ~", Component.literal("Use current position"));
                }
                return sb.buildFuture();
            };

            // Coordinates can use absolute numbers or ~ for relative (e.g., "100 64 200" or "~ ~ ~")
            RequiredArgumentBuilder<FabricClientCommandSource, String> coordArg = 
                ClientCommandManager.argument("coordinates", StringArgumentType.greedyString())
                    .suggests(coordSuggestions)
                    .executes(context -> executeConvertCoords(context));

            RequiredArgumentBuilder<FabricClientCommandSource, String> goalArg = 
                ClientCommandManager.argument("goal", StringArgumentType.word())
                    .suggests(dimSuggestions)
                    .then(coordArg)
                    .executes(context -> executeConvertCoords(context));

            conv.then(goalArg);
            dispatcher.register(conv);
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
                Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "echo " + coordString + " | clip.exe"});
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

    // Execute the /convertcoords command
    private int executeConvertCoords(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String goal = StringArgumentType.getString(context, "goal").toLowerCase();
        
        double x, y, z;
        
        // Try to get coordinates from the optional "coordinates" argument
        try {
            String coordInput = StringArgumentType.getString(context, "coordinates");
            // Parse coordinates from input string (e.g., "~ ~ ~" or "100 64 200" or "~10 ~ ~20")
            String[] parts = coordInput.trim().split("\\s+");
            
            x = parseCoordinate(parts.length > 0 ? parts[0] : "~", player.getX());
            y = parseCoordinate(parts.length > 1 ? parts[1] : "~", player.getY());
            z = parseCoordinate(parts.length > 2 ? parts[2] : "~", player.getZ());
        } catch (Exception e) {
            // If parsing fails, use player position
            x = Math.floor(player.getX());
            y = Math.floor(player.getY());
            z = Math.floor(player.getZ());
        }

        // Determine conversion direction: if goal is nether, convert from overworld -> nether (divide by 8)
        double rx = x;
        double rz = z;
        if (goal.equals("nether")) {
            rx = Math.floor(x / 8.0);
            rz = Math.floor(z / 8.0);
        } else if (goal.equals("overworld")) {
            rx = Math.floor(x * 8.0);
            rz = Math.floor(z * 8.0);
        } else {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Unknown goal dimension: " + goal + " (use overworld or nether)"));
            return 0;
        }

        long ix = (long) rx;
        long iy = Math.round(y);
        long iz = (long) rz;

        String out = ix + " " + iy + " " + iz;
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.converted", out));

        // Copy converted coordinates to clipboard if enabled in config
        if (config.copyConvertedToClipboard) {
            try {
                // Use Windows clip.exe to copy to system clipboard
                Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "echo " + out + " | clip.exe"});
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

    // Helper method to parse coordinate strings that support ~ for relative coordinates
    private double parseCoordinate(String input, double playerCoord) {
        if (input.equals("~")) {
            // Relative to player coordinate
            return playerCoord;
        } else if (input.startsWith("~")) {
            // Relative with offset (e.g., ~10, ~-5)
            try {
                double offset = Double.parseDouble(input.substring(1));
                return playerCoord + offset;
            } catch (NumberFormatException e) {
                return playerCoord;
            }
        } else {
            // Absolute coordinate
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                return playerCoord;
            }
        }
    }
}