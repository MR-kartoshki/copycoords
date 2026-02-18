package com.example.copycoords;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

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

            // Optional goal dimension for /copycoords
            RequiredArgumentBuilder<FabricClientCommandSource, String> copyGoalArg =
                ClientCommandManager.argument("goal", StringArgumentType.word())
                    .suggests(dimSuggestions)
                    .executes(context -> executeCopyCoordsWithGoal(context));
            builder.then(copyGoalArg);
            dispatcher.register(builder);

            // Register /msgcoords <player> [goal]
            LiteralArgumentBuilder<FabricClientCommandSource> msg = ClientCommandManager.literal("msgcoords");
            RequiredArgumentBuilder<FabricClientCommandSource, String> playerArg =
                ClientCommandManager.argument("player", StringArgumentType.word())
                    .executes(context -> executeMsgCoords(context));

            RequiredArgumentBuilder<FabricClientCommandSource, String> msgGoalArg =
                ClientCommandManager.argument("goal", StringArgumentType.word())
                    .suggests(dimSuggestions)
                    .executes(context -> executeMsgCoordsWithGoal(context));

            playerArg.then(msgGoalArg);
            msg.then(playerArg);
            dispatcher.register(msg);
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
            copyToClipboardWithFeedback(coordString);
        }

        return Command.SINGLE_SUCCESS;
    }

    // Execute /copycoords with a goal dimension to convert current coordinates
    private int executeCopyCoordsWithGoal(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String goal = StringArgumentType.getString(context, "goal").toLowerCase();
        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();

        long[] converted = convertCurrentCoordsToGoal(player, goal, x, y, z);
        if (converted == null) {
            return 0;
        }

        String coordString = converted[0] + " " + converted[1] + " " + converted[2];
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.converted", coordString));

        if (config.copyConvertedToClipboard) {
            copyToClipboardWithFeedback(coordString);
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

        // Try to get coordinates from the optional "coordinates" argument (supports ~ relative syntax)
        try {
            String coordInput = StringArgumentType.getString(context, "coordinates");
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

        // Round/convert to integers in the same way other commands do
        int ix = (int) Math.floor(x);
        int iy = (int) Math.round(y);
        int iz = (int) Math.floor(z);

        long[] converted = convertCurrentCoordsToGoal(player, goal, ix, iy, iz);
        if (converted == null) {
            return 0;
        }

        String out = converted[0] + " " + converted[1] + " " + converted[2];
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.converted", out));

        if (config.copyConvertedToClipboard) {
            copyToClipboardWithFeedback(out);
        }

        return Command.SINGLE_SUCCESS;
    }

    // Execute /msgcoords <player> using current coordinates
    private int executeMsgCoords(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String target = StringArgumentType.getString(context, "player");
        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();
        String coordString = x + " " + y + " " + z;

        return sendCoordsMessage(target, coordString);
    }

    // Execute /msgcoords <player> <goal> using converted coordinates
    private int executeMsgCoordsWithGoal(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String target = StringArgumentType.getString(context, "player");
        String goal = StringArgumentType.getString(context, "goal").toLowerCase();
        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();

        long[] converted = convertCurrentCoordsToGoal(player, goal, x, y, z);
        if (converted == null) {
            return 0;
        }

        String coordString = converted[0] + " " + converted[1] + " " + converted[2];
        return sendCoordsMessage(target, coordString);
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

    private long[] convertCurrentCoordsToGoal(Player player, String goal, int x, int y, int z) {
        if (!goal.equals("overworld") && !goal.equals("nether")) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.unknown_goal", goal));
            return null;
        }

        if (!player.level().dimension().equals(Level.OVERWORLD) && !player.level().dimension().equals(Level.NETHER)) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.unsupported_dimension"));
            return null;
        }

        double rx = x;
        double rz = z;

        if (player.level().dimension().equals(Level.OVERWORLD) && goal.equals("nether")) {
            rx = Math.floor(x / 8.0);
            rz = Math.floor(z / 8.0);
        } else if (player.level().dimension().equals(Level.NETHER) && goal.equals("overworld")) {
            rx = Math.floor(x * 8.0);
            rz = Math.floor(z * 8.0);
        }

        return new long[]{(long) rx, y, (long) rz};
    }

    private int sendCoordsMessage(String target, String coordString) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.msg_failed", target, "no server connection"));
            return 0;
        }

        try {
            connection.sendCommand("msg " + target + " " + coordString);
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.msg_sent", target, coordString));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.msg_failed", target, errorMsg));
            return 0;
        }
    }

    private void copyToClipboardWithFeedback(String text) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", "echo " + text + " | clip.exe"});
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copied"));
            } else {
                Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copy_failed", "clip.exe returned " + exitCode));
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copy_failed", errorMsg));
        }
    }
}