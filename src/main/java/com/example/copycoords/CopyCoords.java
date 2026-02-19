package com.example.copycoords;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

// Main mod class that initializes the mod and registers the /copycoords command
public class CopyCoords implements ClientModInitializer {
    public static CopyCoordsConfig config;
    public static CopyCoordsDataStore dataStore;

    private static final String OVERWORLD_ID = Level.OVERWORLD.toString();
    private static final String NETHER_ID = Level.NETHER.toString();
    private static final String END_ID = Level.END.toString();

    // Initialize the mod client-side by loading config and registering commands/keybinds
    @Override
    @SuppressWarnings("null")
    public void onInitializeClient() {
        config = CopyCoordsConfig.load();
        dataStore = CopyCoordsDataStore.load();
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

            SuggestionProvider<FabricClientCommandSource> bookmarkSuggestions = (ctx, sb) -> {
                for (String name : dataStore.getBookmarkNames()) {
                    sb.suggest(name);
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

                // Register /coordshistory
                LiteralArgumentBuilder<FabricClientCommandSource> history = ClientCommandManager.literal("coordshistory");
                history.executes(context -> executeHistoryList());
                history.then(ClientCommandManager.literal("list")
                    .executes(context -> executeHistoryList()));
                history.then(ClientCommandManager.literal("clear")
                    .executes(context -> executeHistoryClear()));
                history.then(ClientCommandManager.literal("copy")
                    .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> executeHistoryCopy(IntegerArgumentType.getInteger(context, "index")))));
                dispatcher.register(history);

                // Register /coordbookmark
                LiteralArgumentBuilder<FabricClientCommandSource> bookmark = ClientCommandManager.literal("coordbookmark");
                bookmark.executes(context -> executeBookmarkList());
                bookmark.then(ClientCommandManager.literal("list")
                    .executes(context -> executeBookmarkList()));
                bookmark.then(ClientCommandManager.literal("add")
                    .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(context -> executeBookmarkAdd(StringArgumentType.getString(context, "name")))));
                bookmark.then(ClientCommandManager.literal("copy")
                    .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                        .suggests(bookmarkSuggestions)
                        .executes(context -> executeBookmarkCopy(StringArgumentType.getString(context, "name")))));
                bookmark.then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                        .suggests(bookmarkSuggestions)
                        .executes(context -> executeBookmarkRemove(StringArgumentType.getString(context, "name")))));
                dispatcher.register(bookmark);

                // Register /distcalc for calculating distance between two coordinate sets
                LiteralArgumentBuilder<FabricClientCommandSource> distcalc = ClientCommandManager.literal("distcalc");
                
                RequiredArgumentBuilder<FabricClientCommandSource, String> x1Arg =
                    ClientCommandManager.argument("x1", StringArgumentType.word())
                        .then(ClientCommandManager.argument("y1", StringArgumentType.word())
                            .then(ClientCommandManager.argument("z1", StringArgumentType.word())
                                .then(ClientCommandManager.argument("x2", StringArgumentType.word())
                                    .then(ClientCommandManager.argument("y2", StringArgumentType.word())
                                        .then(ClientCommandManager.argument("z2", StringArgumentType.word())
                                            .executes(context -> executeDistanceCalc(context)))))));
                
                distcalc.then(x1Arg);
                
                // Also allow /distcalc <bookmark1> <bookmark2> format
                RequiredArgumentBuilder<FabricClientCommandSource, String> bm1Arg =
                    ClientCommandManager.argument("bookmark1", StringArgumentType.greedyString())
                        .suggests(bookmarkSuggestions)
                        .then(ClientCommandManager.argument("bookmark2", StringArgumentType.greedyString())
                            .suggests(bookmarkSuggestions)
                            .executes(context -> executeDistanceCalcBookmarks(context)));
                
                distcalc.then(ClientCommandManager.literal("bookmarks")
                    .then(bm1Arg));
                
                dispatcher.register(distcalc);
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
        String dimensionId = getDimensionId(player);
        String coordString = formatCoordinates(x, y, z, dimensionId);

        // Print coordinates to chat
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.coords_printed", coordString));

        // Copy to clipboard if enabled in config
        if (config.copyToClipboard) {
            copyToClipboardWithFeedback(coordString, x, y, z, dimensionId);
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

        String dimensionId = getDimensionIdForGoal(goal);
        String coordString = formatCoordinates((int) converted[0], (int) converted[1], (int) converted[2], dimensionId);
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.converted", coordString));

        if (config.copyConvertedToClipboard) {
            copyToClipboardWithFeedback(coordString, (int) converted[0], (int) converted[1], (int) converted[2], dimensionId);
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

        String dimensionId = getDimensionIdForGoal(goal);
        String out = formatCoordinates((int) converted[0], (int) converted[1], (int) converted[2], dimensionId);
        Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.converted", out));

        if (config.copyConvertedToClipboard) {
            copyToClipboardWithFeedback(out, (int) converted[0], (int) converted[1], (int) converted[2], dimensionId);
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
    private static double parseCoordinate(String input, double playerCoord) {
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

    static String getDimensionId(Player player) {
        return player.level().dimension().toString();
    }

    static String getDimensionIdForGoal(String goal) {
        if (goal.equals("nether")) {
            return NETHER_ID;
        }
        return OVERWORLD_ID;
    }

    static String getDimensionNameFromId(String dimensionId) {
        if (dimensionId == null) {
            return "Unknown";
        }
        if (dimensionId.equals(OVERWORLD_ID)) {
            return "Overworld";
        } else if (dimensionId.equals(NETHER_ID)) {
            return "Nether";
        } else if (dimensionId.equals(END_ID)) {
            return "End";
        }
        return dimensionId;
    }

    // Helper method to format coordinates with optional dimension
    static String formatCoordinates(int x, int y, int z, String dimensionId) {
        CoordinateFormat format = CoordinateFormat.fromId(CopyCoords.config.coordinateFormat);
        String coordString = format.format(x, y, z);
        if (CopyCoords.config.showDimensionInCoordinates) {
            coordString += " (" + getDimensionNameFromId(dimensionId) + ")";
        }
        return coordString;
    }

    static void addHistoryEntry(int x, int y, int z, String dimensionId) {
        if (dataStore != null) {
            dataStore.addHistoryEntry(x, y, z, dimensionId);
        }
    }

    private void copyToClipboardWithFeedback(String text, int x, int y, int z, String dimensionId) {
        try {
            // Copy to clipboard using cross-platform utility
            ClipboardUtils.copyToClipboard(text);
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copied"));
            addHistoryEntry(x, y, z, dimensionId);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copy_failed", errorMsg));
        }
    }

    private int executeHistoryList() {
        List<CopyCoordsDataStore.HistoryEntry> history = dataStore.getHistory();
        if (history.isEmpty()) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("History is empty."));
            return 0;
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Recent coordinates (click to copy):"));

        for (int i = 0; i < history.size(); i++) {
            CopyCoordsDataStore.HistoryEntry entry = history.get(i);
            String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);
            int index = i + 1;
            final int clickIndex = index;
                        ClickEvent clickEvent = buildClickEvent("/coordshistory copy " + clickIndex);
                        HoverEvent hoverEvent = buildHoverEvent(Component.literal("Copy to clipboard"));
                        Component line = Component.literal(index + ") " + coordString)
                            .withStyle(style -> applyEvents(style, clickEvent, hoverEvent));
            Minecraft.getInstance().gui.getChat().addMessage(line);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeHistoryCopy(int index) {
        List<CopyCoordsDataStore.HistoryEntry> history = dataStore.getHistory();
        if (index < 1 || index > history.size()) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Invalid history index: " + index));
            return 0;
        }

        CopyCoordsDataStore.HistoryEntry entry = history.get(index - 1);
        String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);
        try {
            ClipboardUtils.copyToClipboard(coordString);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Copied history entry " + index + " to clipboard."));
            addHistoryEntry(entry.x, entry.y, entry.z, entry.dimensionId);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copy_failed", errorMsg));
            return 0;
        }
    }

    private int executeHistoryClear() {
        dataStore.clearHistory();
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("History cleared."));
        return Command.SINGLE_SUCCESS;
    }

    private int executeBookmarkAdd(String name) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark name cannot be empty."));
            return 0;
        }

        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();
        String dimensionId = getDimensionId(player);

        if (!dataStore.addBookmark(trimmed, x, y, z, dimensionId)) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark already exists: " + trimmed));
            return 0;
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark added: " + trimmed));
        return Command.SINGLE_SUCCESS;
    }

    private int executeBookmarkList() {
        List<CopyCoordsDataStore.BookmarkEntry> bookmarks = dataStore.getBookmarks();
        if (bookmarks.isEmpty()) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("No bookmarks yet."));
            return 0;
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmarks (click to copy):"));
        for (CopyCoordsDataStore.BookmarkEntry entry : bookmarks) {
            String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);
            String command = "/coordbookmark copy " + quoteArgument(entry.name);
                        ClickEvent clickEvent = buildClickEvent(command);
                        HoverEvent hoverEvent = buildHoverEvent(Component.literal("Copy to clipboard"));
                        Component line = Component.literal(entry.name + " - " + coordString)
                            .withStyle(style -> applyEvents(style, clickEvent, hoverEvent));
            Minecraft.getInstance().gui.getChat().addMessage(line);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeBookmarkCopy(String name) {
        CopyCoordsDataStore.BookmarkEntry entry = dataStore.getBookmark(name);
        if (entry == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark not found: " + name));
            return 0;
        }

        String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);
        try {
            ClipboardUtils.copyToClipboard(coordString);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Copied bookmark '" + entry.name + "' to clipboard."));
            addHistoryEntry(entry.x, entry.y, entry.z, entry.dimensionId);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copy_failed", errorMsg));
            return 0;
        }
    }

    private int executeBookmarkRemove(String name) {
        if (dataStore.removeBookmark(name)) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark removed: " + name));
            return Command.SINGLE_SUCCESS;
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark not found: " + name));
        return 0;
    }

    private String quoteArgument(String value) {
        String escaped = value.replace("\"", "\\\"");
        if (escaped.contains(" ")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static net.minecraft.network.chat.Style applyEvents(net.minecraft.network.chat.Style style,
                                                               ClickEvent clickEvent,
                                                               HoverEvent hoverEvent) {
        if (clickEvent != null) {
            style = style.withClickEvent(clickEvent);
        }
        if (hoverEvent != null) {
            style = style.withHoverEvent(hoverEvent);
        }
        return style;
    }

    private static ClickEvent buildClickEvent(String command) {
        try {
            Method runCommand = ClickEvent.class.getDeclaredMethod("runCommand", String.class);
            return (ClickEvent) runCommand.invoke(null, command);
        } catch (Exception ignored) {
        }

        try {
            Class<?> runCommandClass = Class.forName("net.minecraft.network.chat.ClickEvent$RunCommand");
            return (ClickEvent) runCommandClass.getConstructor(String.class).newInstance(command);
        } catch (Exception ignored) {
        }

        try {
            Constructor<ClickEvent> ctor = ClickEvent.class.getConstructor(ClickEvent.Action.class, String.class);
            return ctor.newInstance(ClickEvent.Action.RUN_COMMAND, command);
        } catch (Exception ignored) {
        }

        return null;
    }

    private static HoverEvent buildHoverEvent(Component text) {
        try {
            Method showText = HoverEvent.class.getDeclaredMethod("showText", Component.class);
            return (HoverEvent) showText.invoke(null, text);
        } catch (Exception ignored) {
        }

        try {
            Class<?> showTextClass = Class.forName("net.minecraft.network.chat.HoverEvent$ShowText");
            return (HoverEvent) showTextClass.getConstructor(Component.class).newInstance(text);
        } catch (Exception ignored) {
        }

        try {
            Constructor<HoverEvent> ctor = HoverEvent.class.getConstructor(HoverEvent.Action.class, Component.class);
            return ctor.newInstance(HoverEvent.Action.SHOW_TEXT, text);
        } catch (Exception ignored) {
        }

        return null;
    }

    // Execute /distcalc <x1> <y1> <z1> <x2> <y2> <z2> to calculate distance between two coordinate sets
    private int executeDistanceCalc(CommandContext<FabricClientCommandSource> context) {
        try {
            int x1 = parseCoordOrPlayerCoord(StringArgumentType.getString(context, "x1"), Minecraft.getInstance().player, "x");
            int y1 = parseCoordOrPlayerCoord(StringArgumentType.getString(context, "y1"), Minecraft.getInstance().player, "y");
            int z1 = parseCoordOrPlayerCoord(StringArgumentType.getString(context, "z1"), Minecraft.getInstance().player, "z");
            int x2 = parseCoordOrPlayerCoord(StringArgumentType.getString(context, "x2"), Minecraft.getInstance().player, "x");
            int y2 = parseCoordOrPlayerCoord(StringArgumentType.getString(context, "y2"), Minecraft.getInstance().player, "y");
            int z2 = parseCoordOrPlayerCoord(StringArgumentType.getString(context, "z2"), Minecraft.getInstance().player, "z");

            DistanceCalculator.DistanceResult result = DistanceCalculator.calculate(x1, y1, z1, x2, y2, z2);
            
            // Format and display result
            String message = "§6Distance Calculator§r: From [" + x1 + ", " + y1 + ", " + z1 + "] to [" + x2 + ", " + y2 + ", " + z2 + "]";
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(message));
            
            String resultMessage = "§2" + DistanceCalculator.formatResult(result, true);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(resultMessage));
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§cError calculating distance: " + e.getMessage()));
            return 0;
        }
    }

    // Execute /distcalc bookmarks <bookmark1> <bookmark2> to calculate distance between bookmarks
    private int executeDistanceCalcBookmarks(CommandContext<FabricClientCommandSource> context) {
        try {
            String bm1Name = StringArgumentType.getString(context, "bookmark1");
            String bm2Name = StringArgumentType.getString(context, "bookmark2");
            
            CopyCoordsDataStore.BookmarkEntry bm1 = dataStore.getBookmark(bm1Name);
            CopyCoordsDataStore.BookmarkEntry bm2 = dataStore.getBookmark(bm2Name);
            
            if (bm1 == null) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§cBookmark not found: " + bm1Name));
                return 0;
            }
            if (bm2 == null) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§cBookmark not found: " + bm2Name));
                return 0;
            }
            
            DistanceCalculator.DistanceResult result = DistanceCalculator.calculate(bm1.x, bm1.y, bm1.z, bm2.x, bm2.y, bm2.z);
            
            // Format and display result
            String message = "§6Distance Calculator§r: From '" + bm1Name + "' to '" + bm2Name + "'";
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(message));
            
            String coordMessage = "  From [" + bm1.x + ", " + bm1.y + ", " + bm1.z + "] to [" + bm2.x + ", " + bm2.y + ", " + bm2.z + "]";
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(coordMessage));
            
            String resultMessage = "§2" + DistanceCalculator.formatResult(result, true);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(resultMessage));
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§cError calculating distance: " + e.getMessage()));
            return 0;
        }
    }

    // Helper method to parse coordinates that can be absolute numbers, ~, or player position
    private static int parseCoordOrPlayerCoord(String input, Player player, String coordType) {
        if (player == null) {
            throw new IllegalArgumentException("Player not found");
        }
        
        double playerCoord;
        if ("x".equals(coordType)) {
            playerCoord = player.getX();
        } else if ("y".equals(coordType)) {
            playerCoord = player.getY();
        } else if ("z".equals(coordType)) {
            playerCoord = player.getZ();
        } else {
            throw new IllegalArgumentException("Invalid coordinate type: " + coordType);
        }
        
        return (int) Math.floor(parseCoordinate(input, playerCoord));
    }
}
