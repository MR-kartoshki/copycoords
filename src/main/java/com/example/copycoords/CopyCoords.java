package com.example.copycoords;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CopyCoords implements ClientModInitializer {
    public static CopyCoordsConfig config;
    public static CopyCoordsDataStore dataStore;

    private static final String OVERWORLD_ID = "minecraft:overworld";
    private static final String NETHER_ID = "minecraft:the_nether";
    private static final String END_ID = "minecraft:the_end";
    private static final String DETECTED_COORDINATE_PREFIX = "Detected coordinates: ";
    private static final String DETECTED_CONVERTED_PREFIX = "Converted detected coordinates: ";
    private static final String DETECTED_UNKNOWN_DIMENSION_TOKEN = "unknown";
    private static final int RECENT_LOCAL_MESSAGE_LIMIT = 40;
    private static final String COORD_NUMBER_PATTERN = "-?\\d+(?:\\.\\d+)?";
    private static final String COORD_ARGUMENT_PATTERN = "(?:~(?:" + COORD_NUMBER_PATTERN + ")?|" + COORD_NUMBER_PATTERN + ")";
    private static final Pattern COORD_ARGUMENT_INPUT_PATTERN = Pattern.compile("^(?:" + COORD_ARGUMENT_PATTERN + ")$");
    private static final Pattern BOOKMARK_ADD_COORD_LABEL_PATTERN = Pattern.compile("(?i)(?<!\\w)[xyz]\\s*[:=]");
    private static final Pattern BOOKMARK_ADD_XYZ_PATTERN = Pattern.compile(
            "(?is)^(?<name>.+?)\\s+(?<coords>(?:(?<!\\w)x\\s*[:=]\\s*" + COORD_NUMBER_PATTERN
                    + "\\s*[,; ]+y\\s*[:=]\\s*" + COORD_NUMBER_PATTERN
                    + "\\s*[,; ]+z\\s*[:=]\\s*" + COORD_NUMBER_PATTERN + "))(?:\\s+(?<dimension>.+?))?\\s*$");
    private static final Pattern BOOKMARK_ADD_BRACKET_PATTERN = Pattern.compile(
            "(?is)^(?<name>.+?)\\s+(?<coords>\\[\\s*" + COORD_NUMBER_PATTERN
                    + "\\s*,\\s*" + COORD_NUMBER_PATTERN
                    + "\\s*,\\s*" + COORD_NUMBER_PATTERN + "\\s*\\])(?:\\s+(?<dimension>.+?))?\\s*$");
    private static final Pattern BOOKMARK_ADD_SPACE_PATTERN = Pattern.compile(
            "(?is)^(?<name>.+?)\\s+(?<x>" + COORD_ARGUMENT_PATTERN + ")\\s+(?<y>" + COORD_ARGUMENT_PATTERN
                    + ")\\s+(?<z>" + COORD_ARGUMENT_PATTERN + ")(?:\\s+(?<dimension>.+?))?\\s*$");
    private static final Deque<String> RECENT_LOCAL_MESSAGES = new ArrayDeque<>();

    static void sendSystemMessage(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || message == null) {
            return;
        }
        rememberLocalMessage(message.getString());
        PlayerMessageCompat.send(mc.player, message);
    }

    @Override
    @SuppressWarnings("null")
    public void onInitializeClient() {
        config = CopyCoordsConfig.load();
        dataStore = CopyCoordsDataStore.load();
        CopyCoordsBind.register();
        ChatReceiveCompat.register(CopyCoords::handleIncomingChatText);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandCompat.literal("copycoords");
            builder.executes(context -> executeCopyCoords(context));

            LiteralArgumentBuilder<FabricClientCommandSource> conv = ClientCommandCompat.literal("convertcoords");

            SuggestionProvider<FabricClientCommandSource> dimSuggestions = (ctx, sb) -> {
                sb.suggest("overworld");
                sb.suggest("nether");
                return sb.buildFuture();
            };

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
                    if (name.contains(" ") || name.contains("\"")) {
                        sb.suggest(quoteForBrigadier(name));
                    }
                }
                return sb.buildFuture();
            };

            SuggestionProvider<FabricClientCommandSource> playerSuggestions = (pctx, psb) ->
                SharedSuggestionProvider.suggest(pctx.getSource().getOnlinePlayerNames(), psb);

            RequiredArgumentBuilder<FabricClientCommandSource, String> coordArg = 
                ClientCommandCompat.argument("coordinates", StringArgumentType.greedyString())
                    .suggests(coordSuggestions)
                    .executes(context -> executeConvertCoords(context));

            RequiredArgumentBuilder<FabricClientCommandSource, String> goalArg = 
                ClientCommandCompat.argument("goal", StringArgumentType.word())
                    .suggests(dimSuggestions)
                    .then(coordArg)
                    .executes(context -> executeConvertCoords(context));

            conv.then(goalArg);
            dispatcher.register(conv);

            RequiredArgumentBuilder<FabricClientCommandSource, String> copyGoalArg =
                ClientCommandCompat.argument("goal", StringArgumentType.word())
                    .suggests(dimSuggestions)
                    .executes(context -> executeCopyCoordsWithGoal(context));
            builder.then(copyGoalArg);
            builder.then(buildDetectedCommand(dimSuggestions));
            dispatcher.register(builder);

            LiteralArgumentBuilder<FabricClientCommandSource> cc = ClientCommandCompat.literal("cc");
            cc.executes(context -> executeCopyCoords(context));
            cc.then(copyGoalArg);
            dispatcher.register(cc);

            LiteralArgumentBuilder<FabricClientCommandSource> copycoordinates = ClientCommandCompat.literal("copycoordinates");
            copycoordinates.executes(context -> executeCopyCoords(context));
            copycoordinates.then(copyGoalArg);
            dispatcher.register(copycoordinates);

            LiteralArgumentBuilder<FabricClientCommandSource> msg = ClientCommandCompat.literal("msgcoords");
            RequiredArgumentBuilder<FabricClientCommandSource, String> playerArg =
                ClientCommandCompat.argument("player", StringArgumentType.word())
                    .suggests(playerSuggestions)
                    .executes(context -> executeMsgCoords(context));

            RequiredArgumentBuilder<FabricClientCommandSource, String> msgGoalArg =
                ClientCommandCompat.argument("goal", StringArgumentType.word())
                    .suggests(dimSuggestions)
                    .executes(context -> executeMsgCoordsWithGoal(context));

            playerArg.then(msgGoalArg);
            msg.then(playerArg);
            dispatcher.register(msg);

                LiteralArgumentBuilder<FabricClientCommandSource> history = ClientCommandCompat.literal("coordshistory");
                history.executes(context -> executeHistoryList());
                history.then(ClientCommandCompat.literal("list")
                    .executes(context -> executeHistoryList()));
                history.then(ClientCommandCompat.literal("clear")
                    .executes(context -> executeHistoryClear()));
                history.then(ClientCommandCompat.literal("copy")
                    .then(ClientCommandCompat.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> executeHistoryCopy(IntegerArgumentType.getInteger(context, "index")))));
                history.then(ClientCommandCompat.literal("remove")
                    .then(ClientCommandCompat.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> executeHistoryRemove(IntegerArgumentType.getInteger(context, "index")))));
                history.then(ClientCommandCompat.literal("menu")
                    .then(ClientCommandCompat.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> executeHistoryMenu(IntegerArgumentType.getInteger(context, "index")))));
                dispatcher.register(history);

                dispatcher.register(buildBookmarkCommand("coordsbookmark", bookmarkSuggestions));
                dispatcher.register(buildBookmarkCommand("coordbookmark", bookmarkSuggestions));

                LiteralArgumentBuilder<FabricClientCommandSource> distcalc = ClientCommandCompat.literal("distcalc");
                
                RequiredArgumentBuilder<FabricClientCommandSource, Integer> x1Arg =
                    ClientCommandCompat.argument("x1", IntegerArgumentType.integer())
                        .then(ClientCommandCompat.argument("y1", IntegerArgumentType.integer())
                            .then(ClientCommandCompat.argument("z1", IntegerArgumentType.integer())
                                .then(ClientCommandCompat.argument("x2", IntegerArgumentType.integer())
                                    .then(ClientCommandCompat.argument("y2", IntegerArgumentType.integer())
                                        .then(ClientCommandCompat.argument("z2", IntegerArgumentType.integer())
                                            .executes(context -> executeDistanceCalc(context)))))));
                
                distcalc.then(x1Arg);

                RequiredArgumentBuilder<FabricClientCommandSource, String> bm1Arg =
                    ClientCommandCompat.argument("bookmark1", StringArgumentType.string())
                        .suggests(bookmarkSuggestions)
                        .then(ClientCommandCompat.argument("bookmark2", StringArgumentType.string())
                            .suggests(bookmarkSuggestions)
                            .executes(context -> executeDistanceCalcBookmarks(context)));

                distcalc.then(ClientCommandCompat.literal("bookmarks")
                    .then(bm1Arg));
                
                dispatcher.register(distcalc);
        });
    }

    private static boolean sendChatLine(ClientPacketListener connection, String line) {
        try {
            boolean sent = ChatSendCompat.sendChat(Minecraft.getInstance(), connection, line);
            if (!sent) {
                reportInstantSendFailure(Minecraft.getInstance(), "chat send failed", ChatSendCompat.getLastFailureReason());
            }
            return sent;
        } catch (Throwable error) {
            reportInstantSendFailure(Minecraft.getInstance(), "chat send failed", error);
            return false;
        }
    }

    private static void maybeInstantSendCommandOutput(String text) {
        sendCommandOutputToChat(text, false);
    }

    public static boolean sendCommandOutputToChat(String text, boolean forceSend) {
        if (text == null || text.isBlank()) {
            return false;
        }

        if (!forceSend && (config == null || !config.instantChatEnabled)) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }

        ClientPacketListener connection = client.getConnection();
        if (connection == null) {
            return false;
        }

        String trimmed = text.trim();
        if (trimmed.startsWith("/")) {
            String command = trimmed.substring(1).trim();
            if (command.isEmpty()) {
                return false;
            }
            try {
                boolean sent = ChatSendCompat.sendCommand(client, connection, command);
                if (!sent) {
                    reportInstantSendFailure(client, "command send failed", ChatSendCompat.getLastFailureReason());
                }
                return sent;
            } catch (Throwable error) {
                reportInstantSendFailure(client, "command send failed", error);
                return false;
            }
        }

        return sendChatLine(connection, trimmed);
    }

    private static void reportInstantSendFailure(Minecraft client, String prefix, Throwable error) {
        String details = "unknown";
        if (error != null && error.getMessage() != null && !error.getMessage().isBlank()) {
            details = error.getMessage();
        } else if (error != null) {
            details = error.getClass().getSimpleName();
        }
        reportInstantSendFailure(client, prefix, details);
    }

    private static void reportInstantSendFailure(Minecraft client, String prefix, String details) {
        if (client == null || client.player == null) {
            return;
        }

        if (details == null || details.isBlank()) {
            details = "unknown";
        }

        sendSystemMessage(Component.literal("[CopyCoords] Instant send failed: " + prefix + " (" + details + ")"));
    }

    private int executeCopyCoords(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;

        if (player == null) {
            sendSystemMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        int iz = (int) Math.floor(z);
        String dimensionId = getDimensionId(player);
        String coordString = formatCoordinates(x, y, z, dimensionId);

        postCoordinateMessage("Your current coordinates are: ", coordString, ix, iy, iz, dimensionId);
        maybeInstantSendCommandOutput(coordString);

        if (config != null && config.copyToClipboard) {
            copyToClipboardWithFeedback(coordString, ix, iy, iz, dimensionId);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeCopyCoordsWithGoal(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            sendSystemMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String goal = StringArgumentType.getString(context, "goal").toLowerCase();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        double[] converted = convertCurrentCoordsToGoal(player, goal, x, y, z);
        if (converted == null) {
            return 0;
        }

        String dimensionId = getDimensionIdForGoal(goal);
        String coordString = formatCoordinates(converted[0], converted[1], converted[2], dimensionId);

        int cx = (int) Math.floor(converted[0]);
        int cy = (int) Math.floor(converted[1]);
        int cz = (int) Math.floor(converted[2]);
        postCoordinateMessage("Converted coordinates: ", coordString, cx, cy, cz, dimensionId);
        maybeInstantSendCommandOutput(coordString);

        if (config != null && config.copyConvertedToClipboard) {
            copyToClipboardWithFeedback(coordString, cx, cy, cz, dimensionId);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeConvertCoords(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            sendSystemMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String goal = StringArgumentType.getString(context, "goal").toLowerCase();
        
        double x, y, z;

        String coordInput = StringArgumentType.getString(context, "coordinates");
        String[] parts = coordInput.trim().split("\\s+");

        try {
            x = parseCoordinate(parts.length > 0 ? parts[0] : "~", player.getX());
            y = parseCoordinate(parts.length > 1 ? parts[1] : "~", player.getY());
            z = parseCoordinate(parts.length > 2 ? parts[2] : "~", player.getZ());
        } catch (IllegalArgumentException e) {
            sendSystemMessage(Component.translatable("message.copycoords.command.invalid_coordinates", e.getMessage()));
            return 0;
        }

        double[] converted = convertCurrentCoordsToGoal(player, goal, x, y, z);
        if (converted == null) {
            return 0;
        }

        String dimensionId = getDimensionIdForGoal(goal);
        String out = formatCoordinates(converted[0], converted[1], converted[2], dimensionId);

        int cx = (int) Math.floor(converted[0]);
        int cy = (int) Math.floor(converted[1]);
        int cz = (int) Math.floor(converted[2]);
        postCoordinateMessage("Converted coordinates: ", out, cx, cy, cz, dimensionId);

        if (config != null && config.copyConvertedToClipboard) {
            copyToClipboardWithFeedback(out, cx, cy, cz, dimensionId);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeMsgCoords(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            sendSystemMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String target = StringArgumentType.getString(context, "player");
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        String dimensionId = getDimensionId(player);
        String coordString = formatCoordinates(x, y, z, dimensionId);

        return sendCoordsMessage(target, coordString);
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildDetectedCommand(
            SuggestionProvider<FabricClientCommandSource> dimSuggestions) {
        LiteralArgumentBuilder<FabricClientCommandSource> detected = ClientCommandCompat.literal("detected");

        detected.then(ClientCommandCompat.literal("copy")
                .then(ClientCommandCompat.argument("x", DoubleArgumentType.doubleArg())
                        .then(ClientCommandCompat.argument("y", DoubleArgumentType.doubleArg())
                                .then(ClientCommandCompat.argument("z", DoubleArgumentType.doubleArg())
                                        .then(ClientCommandCompat.argument("dimension", StringArgumentType.word())
                                                .executes(context -> executeDetectedCopy(
                                                        DoubleArgumentType.getDouble(context, "x"),
                                                        DoubleArgumentType.getDouble(context, "y"),
                                                        DoubleArgumentType.getDouble(context, "z"),
                                                        StringArgumentType.getString(context, "dimension"))))))));

        detected.then(ClientCommandCompat.literal("bookmark")
                .then(ClientCommandCompat.argument("x", DoubleArgumentType.doubleArg())
                        .then(ClientCommandCompat.argument("y", DoubleArgumentType.doubleArg())
                                .then(ClientCommandCompat.argument("z", DoubleArgumentType.doubleArg())
                                        .then(ClientCommandCompat.argument("dimension", StringArgumentType.word())
                                                .then(ClientCommandCompat.argument("name", StringArgumentType.greedyString())
                                                        .executes(context -> executeDetectedBookmark(
                                                                DoubleArgumentType.getDouble(context, "x"),
                                                                DoubleArgumentType.getDouble(context, "y"),
                                                                DoubleArgumentType.getDouble(context, "z"),
                                                                StringArgumentType.getString(context, "dimension"),
                                                                StringArgumentType.getString(context, "name")))))))));

        detected.then(ClientCommandCompat.literal("convert")
                .then(ClientCommandCompat.argument("source", StringArgumentType.word())
                        .suggests(dimSuggestions)
                        .then(ClientCommandCompat.argument("goal", StringArgumentType.word())
                                .suggests(dimSuggestions)
                                .then(ClientCommandCompat.argument("x", DoubleArgumentType.doubleArg())
                                        .then(ClientCommandCompat.argument("y", DoubleArgumentType.doubleArg())
                                                .then(ClientCommandCompat.argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(context -> executeDetectedConvert(
                                                                StringArgumentType.getString(context, "source"),
                                                                StringArgumentType.getString(context, "goal"),
                                                                DoubleArgumentType.getDouble(context, "x"),
                                                                DoubleArgumentType.getDouble(context, "y"),
                                                                DoubleArgumentType.getDouble(context, "z")))))))));

        return detected;
    }

    private LiteralArgumentBuilder<FabricClientCommandSource> buildBookmarkCommand(
            String rootCommand,
            SuggestionProvider<FabricClientCommandSource> bookmarkSuggestions) {
        LiteralArgumentBuilder<FabricClientCommandSource> bookmark = ClientCommandCompat.literal(rootCommand);
        bookmark.executes(context -> executeBookmarkList());
        bookmark.then(ClientCommandCompat.literal("list")
                .executes(context -> executeBookmarkList()));
        bookmark.then(ClientCommandCompat.literal("add")
                .then(ClientCommandCompat.argument("name", StringArgumentType.greedyString())
                        .executes(context -> executeBookmarkAdd(StringArgumentType.getString(context, "name")))));
        bookmark.then(ClientCommandCompat.literal("copy")
                .then(ClientCommandCompat.argument("name", StringArgumentType.string())
                        .suggests(bookmarkSuggestions)
                        .executes(context -> executeBookmarkCopy(StringArgumentType.getString(context, "name")))));
        bookmark.then(ClientCommandCompat.literal("remove")
                .then(ClientCommandCompat.argument("name", StringArgumentType.string())
                        .suggests(bookmarkSuggestions)
                        .executes(context -> executeBookmarkRemove(StringArgumentType.getString(context, "name")))));
        bookmark.then(ClientCommandCompat.literal("export")
                .then(ClientCommandCompat.argument("file", StringArgumentType.greedyString())
                        .executes(ctx -> executeBookmarkExport(StringArgumentType.getString(ctx, "file")))));
        bookmark.then(ClientCommandCompat.literal("import")
                .then(ClientCommandCompat.argument("file", StringArgumentType.greedyString())
                        .executes(ctx -> executeBookmarkImport(StringArgumentType.getString(ctx, "file")))));
        bookmark.then(ClientCommandCompat.literal("xaero")
                .then(ClientCommandCompat.literal("add")
                        .then(ClientCommandCompat.argument("name", StringArgumentType.string())
                                .suggests(bookmarkSuggestions)
                                .executes(ctx -> executeBookmarkXaeroAdd(StringArgumentType.getString(ctx, "name")))
                                .then(ClientCommandCompat.literal("target")
                                        .then(ClientCommandCompat.argument("path", StringArgumentType.greedyString())
                                                .executes(ctx -> executeBookmarkXaeroAddToTarget(
                                                        StringArgumentType.getString(ctx, "name"),
                                                        StringArgumentType.getString(ctx, "path"))))))));
        return bookmark;
    }

    private int executeMsgCoordsWithGoal(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            sendSystemMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String target = StringArgumentType.getString(context, "player");
        String goal = StringArgumentType.getString(context, "goal").toLowerCase();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        double[] converted = convertCurrentCoordsToGoal(player, goal, x, y, z);
        if (converted == null) {
            return 0;
        }

        String dimensionId = getDimensionIdForGoal(goal);
        String coordString = formatCoordinates(converted[0], converted[1], converted[2], dimensionId);
        return sendCoordsMessage(target, coordString);
    }

    private static double parseCoordinate(String input, double playerCoord) {
        if (input.equals("~")) {

            return playerCoord;
        } else if (input.startsWith("~")) {

            try {
                double offset = Double.parseDouble(input.substring(1));
                return playerCoord + offset;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid relative coordinate: " + input);
            }
        } else {

            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid coordinate: " + input);
            }
        }
    }

    private double[] convertCurrentCoordsToGoal(Player player, String goal, double x, double y, double z) {
        if (!goal.equals("overworld") && !goal.equals("nether")) {
            sendSystemMessage(Component.translatable("message.copycoords.command.unknown_goal", goal));
            return null;
        }

        if (!PlayerLevelCompat.isInDimension(player, Level.OVERWORLD) && !PlayerLevelCompat.isInDimension(player, Level.NETHER)) {
            sendSystemMessage(Component.translatable("message.copycoords.command.unsupported_dimension"));
            return null;
        }

        double rx = x;
        double rz = z;

        if (PlayerLevelCompat.isInDimension(player, Level.OVERWORLD) && goal.equals("nether")) {
            rx = x / 8.0;
            rz = z / 8.0;
        } else if (PlayerLevelCompat.isInDimension(player, Level.NETHER) && goal.equals("overworld")) {
            rx = x * 8.0;
            rz = z * 8.0;
        }


        return new double[]{rx, y, rz};
    }

    private int sendCoordsMessage(String target, String coordString) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            sendSystemMessage(Component.translatable("message.copycoords.command.msg_failed", target, "no server connection"));
            return 0;
        }

        try {
            boolean sent = ChatSendCompat.sendCommand(Minecraft.getInstance(), connection, "msg " + target + " " + coordString);
            if (!sent) {
                sendSystemMessage(Component.translatable(
                        "message.copycoords.command.msg_failed",
                        target,
                        ChatSendCompat.getLastFailureReason()));
                return 0;
            }
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            sendSystemMessage(Component.translatable("message.copycoords.command.msg_failed", target, errorMsg));
            return 0;
        }
    }

    static String getDimensionId(Player player) {
        return normalizeDimensionId(PlayerLevelCompat.getDimensionId(player));
    }

    static String normalizeDimensionId(String dimensionId) {
        if (dimensionId == null) {
            return null;
        }
        String normalized = ChatCoordinateParser.normalizeDimensionHint(dimensionId);
        return normalized != null ? normalized : dimensionId;
    }

    static String getDimensionIdForGoal(String goal) {
        if (goal.equals("nether")) {
            return NETHER_ID;
        }
        return OVERWORLD_ID;
    }

    static String getDimensionNameFromId(String dimensionId) {
        dimensionId = normalizeDimensionId(dimensionId);
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

    static String formatCoordinates(int x, int y, int z, String dimensionId) {
        CopyCoordsConfig currentConfig = config;
        if (currentConfig != null && currentConfig.coordinateTemplate != null && !currentConfig.coordinateTemplate.isBlank()) {
            String result = currentConfig.coordinateTemplate;
            result = result.replace("{x}", String.valueOf(x));
            result = result.replace("{y}", String.valueOf(y));
            result = result.replace("{z}", String.valueOf(z));
            result = result.replace("{dimension}", dimensionId == null ? "" : dimensionId);
            result = result.replace("{dimName}", getDimensionNameFromId(dimensionId));
            return result;
        }

        String xs = String.valueOf(x);
        String ys = String.valueOf(y);
        String zs = String.valueOf(z);

        CoordinateFormat format = currentConfig == null
                ? CoordinateFormat.SPACE_SEPARATED
                : CoordinateFormat.fromId(currentConfig.coordinateFormat);
        String coordString;
        switch (format) {
            case SPACE_SEPARATED -> coordString = xs + " " + ys + " " + zs;
            case BRACKET_COMMA -> coordString = "[" + xs + ", " + ys + ", " + zs + "]";
            case XYZ_LABEL -> coordString = "X:" + xs + " Y:" + ys + " Z:" + zs;
            default -> coordString = xs + " " + ys + " " + zs;
        }
        if (currentConfig != null && currentConfig.showDimensionInCoordinates) {
            coordString += " (" + getDimensionNameFromId(dimensionId) + ")";
        }
        return coordString;
    }

    static String formatCoordinates(double x, double y, double z, String dimensionId) {
        CopyCoordsConfig currentConfig = config;
        if (currentConfig != null && currentConfig.coordinateTemplate != null && !currentConfig.coordinateTemplate.isBlank()) {
            return applyTemplate(currentConfig.coordinateTemplate, x, y, z, dimensionId);
        }

        String xs = formatCoordinateValue(x);
        String ys = formatCoordinateValue(y);
        String zs = formatCoordinateValue(z);

        CoordinateFormat format = currentConfig == null
                ? CoordinateFormat.SPACE_SEPARATED
                : CoordinateFormat.fromId(currentConfig.coordinateFormat);
        String coordString;
        switch (format) {
            case SPACE_SEPARATED -> coordString = xs + " " + ys + " " + zs;
            case BRACKET_COMMA -> coordString = "[" + xs + ", " + ys + ", " + zs + "]";
            case XYZ_LABEL -> coordString = "X:" + xs + " Y:" + ys + " Z:" + zs;
            default -> coordString = xs + " " + ys + " " + zs;
        }
        if (currentConfig != null && currentConfig.showDimensionInCoordinates) {
            coordString += " (" + getDimensionNameFromId(dimensionId) + ")";
        }
        return coordString;
    }

    private static int getCoordinateDecimalPlaces() {
        if (config == null) {
            return CopyCoordsConfig.DEFAULT_DECIMAL_PLACES;
        }
        return CopyCoordsConfig.clampDecimalPlaces(config.decimalPlaces);
    }

    static String formatCoordinateValue(double value) {
        int decimalPlaces = getCoordinateDecimalPlaces();
        return String.format(Locale.ROOT, "%." + decimalPlaces + "f", value);
    }



    public static String applyTemplate(String template, double x, double y, double z, String dimensionId) {
        String result = template;
        result = result.replace("{x}", formatCoordinateValue(x));
        result = result.replace("{y}", formatCoordinateValue(y));
        result = result.replace("{z}", formatCoordinateValue(z));
        result = result.replace("{dimension}", dimensionId == null ? "" : dimensionId);
        result = result.replace("{dimName}", getDimensionNameFromId(dimensionId));
        return result;
    }

    public static String previewForTemplate(String template) {
        if (template == null || template.isBlank()) return "";

        return applyTemplate(template, 100, 64, 200, OVERWORLD_ID);
    }

    static void addHistoryEntry(int x, int y, int z, String dimensionId) {
        if (dataStore != null) {
            dataStore.addHistoryEntry(x, y, z, dimensionId);
        }
    }

    private static boolean ensureDataStoreAvailable() {
        if (dataStore != null) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        sendSystemMessage(Component.literal("Data store is not available yet."));
        return false;
    }

    public static void openChatWithText(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        ChatScreenOpener.open(mc, text);
    }

    private void copyToClipboardWithFeedback(String text, int x, int y, int z, String dimensionId) {
        copyFormattedCoordinatesWithFeedback(text, x, y, z, dimensionId);
    }

    private static int copyFormattedCoordinatesWithFeedback(String text, int x, int y, int z, String dimensionId) {
        try {
            if (config != null && config.pasteToChatInput) {
                openChatWithText(text);
            } else {
                ClipboardUtils.copyToClipboard(text);
                sendSystemMessage(Component.translatable("message.copycoords.command.copied"));
            }
            addHistoryEntry(x, y, z, dimensionId);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            sendSystemMessage(Component.translatable("message.copycoords.command.copy_failed", errorMsg));
            return 0;
        }
    }

    private int executeHistoryList() {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        List<CopyCoordsDataStore.HistoryEntry> history = dataStore.getHistory();
        if (history.isEmpty()) {
            sendSystemMessage(Component.literal("History is empty."));
            return 0;
        }

        sendSystemMessage(Component.literal("Recent coordinates (click entry to copy, use [menu] for actions):"));

        for (int i = 0; i < history.size(); i++) {
            CopyCoordsDataStore.HistoryEntry entry = history.get(i);
            String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);
            int index = i + 1;
            final int clickIndex = index;
            ClickEvent clickEvent = buildClickEvent("/coordshistory copy " + clickIndex);
            HoverEvent hoverEvent = buildHoverEvent(Component.literal("Copy this entry to clipboard. Shift-click to insert into chat."));
            net.minecraft.network.chat.MutableComponent line = Component.literal(index + ") " + coordString)
                    .withStyle(style -> applyEvents(style.withInsertion(coordString), clickEvent, hoverEvent));
            line.append(Component.literal(" "));
            line.append(buildActionChip("copy", buildClickEvent("/coordshistory copy " + clickIndex), "Copy this entry to clipboard"));
            line.append(Component.literal(" "));
            line.append(buildActionChip("insert", buildSuggestCommandEvent(coordString), "Insert this entry into chat input"));
            line.append(Component.literal(" "));
            line.append(buildActionChip("remove", buildClickEvent("/coordshistory remove " + clickIndex), "Remove this entry from history"));
            line.append(Component.literal(" "));
            line.append(buildActionChip("menu", buildClickEvent("/coordshistory menu " + clickIndex), "Open quick actions for this entry"));
            appendMapLinks(line, entry.x, entry.y, entry.z, entry.dimensionId);
            sendSystemMessage(line);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeHistoryCopy(int index) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        List<CopyCoordsDataStore.HistoryEntry> history = dataStore.getHistory();
        if (index < 1 || index > history.size()) {
            sendSystemMessage(Component.literal("Invalid history index: " + index));
            return 0;
        }

        CopyCoordsDataStore.HistoryEntry entry = history.get(index - 1);
        String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);
        try {
            ClipboardUtils.copyToClipboard(coordString);
            sendSystemMessage(Component.literal("Copied history entry " + index + " to clipboard."));
            addHistoryEntry(entry.x, entry.y, entry.z, entry.dimensionId);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            sendSystemMessage(Component.translatable("message.copycoords.command.copy_failed", errorMsg));
            return 0;
        }
    }

    private int executeHistoryRemove(int index) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        if (!dataStore.removeHistoryEntry(index - 1)) {
            sendSystemMessage(Component.literal("Invalid history index: " + index));
            return 0;
        }

        sendSystemMessage(Component.literal("Removed history entry " + index + "."));
        return Command.SINGLE_SUCCESS;
    }

    private int executeHistoryMenu(int index) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        List<CopyCoordsDataStore.HistoryEntry> history = dataStore.getHistory();
        if (index < 1 || index > history.size()) {
            sendSystemMessage(Component.literal("Invalid history index: " + index));
            return 0;
        }

        CopyCoordsDataStore.HistoryEntry entry = history.get(index - 1);
        String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);

        net.minecraft.network.chat.MutableComponent title = Component.literal("History entry " + index + ": " + coordString);
        appendMapLinks(title, entry.x, entry.y, entry.z, entry.dimensionId);
        sendSystemMessage(title);

        net.minecraft.network.chat.MutableComponent actions = Component.literal("Actions: ");
        actions.append(buildActionChip("copy", buildClickEvent("/coordshistory copy " + index), "Copy this entry to clipboard"));
        actions.append(Component.literal(" "));
        actions.append(buildActionChip("insert", buildSuggestCommandEvent(coordString), "Insert this entry into chat input"));
        actions.append(Component.literal(" "));
        actions.append(buildActionChip("remove", buildClickEvent("/coordshistory remove " + index), "Remove this entry from history"));
        actions.append(Component.literal(" "));
        actions.append(buildActionChip("clear_all", buildClickEvent("/coordshistory clear"), "Clear all history entries"));
        sendSystemMessage(actions);

        return Command.SINGLE_SUCCESS;
    }

    private int executeHistoryClear() {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        dataStore.clearHistory();
        sendSystemMessage(Component.literal("History cleared."));
        return Command.SINGLE_SUCCESS;
    }

    private int executeBookmarkAdd(String input) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            sendSystemMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        BookmarkAddRequest request;
        try {
            request = parseBookmarkAddRequest(input, player);
        } catch (IllegalArgumentException e) {
            sendSystemMessage(Component.translatable("message.copycoords.bookmark.add_failed", e.getMessage()));
            return 0;
        }
        if (request == null || request.name.isEmpty()) {
            sendSystemMessage(Component.literal("Bookmark name cannot be empty."));
            return 0;
        }

        if (!dataStore.addBookmark(request.name, request.x, request.y, request.z, request.dimensionId)) {
            sendSystemMessage(Component.literal("Bookmark already exists: " + request.name));
            return 0;
        }

        sendSystemMessage(Component.literal("Bookmark added: " + request.name));
        return Command.SINGLE_SUCCESS;
    }

    private BookmarkAddRequest parseBookmarkAddRequest(String input, Player player) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        BookmarkAddRequest explicitCoords = tryParseBookmarkAddRequest(trimmed, player, BOOKMARK_ADD_XYZ_PATTERN);
        if (explicitCoords != null) {
            return explicitCoords;
        }

        explicitCoords = tryParseBookmarkAddRequest(trimmed, player, BOOKMARK_ADD_BRACKET_PATTERN);
        if (explicitCoords != null) {
            return explicitCoords;
        }

        explicitCoords = tryParseBookmarkAddSpaceRequest(trimmed, player);
        if (explicitCoords != null) {
            return explicitCoords;
        }

        if (looksLikeExplicitBookmarkAddInput(trimmed)) {
            throw new IllegalArgumentException("Invalid coordinate or dimension input. Use <name> <x> <y> <z> [dimension].");
        }

        return new BookmarkAddRequest(trimmed, player.getX(), player.getY(), player.getZ(), getDimensionId(player));
    }

    private BookmarkAddRequest tryParseBookmarkAddRequest(String input, Player player, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        if (!matcher.matches()) {
            return null;
        }

        String name = matcher.group("name").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Bookmark name cannot be empty.");
        }

        String dimensionId = parseBookmarkAddDimension(matcher.group("dimension"), getDimensionId(player));
        if (dimensionId == null && matcher.group("dimension") != null) {
            throw new IllegalArgumentException("Unknown bookmark dimension: " + matcher.group("dimension").trim());
        }

        List<ChatCoordinateParser.DetectedCoordinate> detections = ChatCoordinateParser.detect(matcher.group("coords"), 1);
        if (detections.isEmpty()) {
            throw new IllegalArgumentException("Invalid bookmark coordinates.");
        }

        ChatCoordinateParser.DetectedCoordinate detection = detections.get(0);
        return new BookmarkAddRequest(name, detection.x, detection.y, detection.z, dimensionId);
    }

    private BookmarkAddRequest tryParseBookmarkAddSpaceRequest(String input, Player player) {
        Matcher matcher = BOOKMARK_ADD_SPACE_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return null;
        }

        String name = matcher.group("name").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Bookmark name cannot be empty.");
        }

        String dimensionId = parseBookmarkAddDimension(matcher.group("dimension"), getDimensionId(player));
        if (dimensionId == null && matcher.group("dimension") != null) {
            throw new IllegalArgumentException("Unknown bookmark dimension: " + matcher.group("dimension").trim());
        }

        double x = parseCoordinate(matcher.group("x"), player.getX());
        double y = parseCoordinate(matcher.group("y"), player.getY());
        double z = parseCoordinate(matcher.group("z"), player.getZ());
        if (!isValidBookmarkCoordinate(x, y, z)) {
            throw new IllegalArgumentException("Bookmark coordinates are out of range.");
        }

        return new BookmarkAddRequest(name, x, y, z, dimensionId);
    }

    private static boolean looksLikeExplicitBookmarkAddInput(String input) {
        if (input.indexOf('[') >= 0 || input.indexOf(']') >= 0 || BOOKMARK_ADD_COORD_LABEL_PATTERN.matcher(input).find()) {
            return true;
        }

        String[] parts = input.trim().split("\\s+");
        if (parts.length < 4) {
            return false;
        }

        if (hasCoordinateLikeSuffix(parts, 3, 2)) {
            return true;
        }

        return hasCoordinateLikeSuffix(parts, 4, 3);
    }

    private static boolean hasCoordinateLikeSuffix(String[] parts, int suffixLength, int requiredCoordinateTokens) {
        if (parts.length <= suffixLength) {
            return false;
        }

        int start = parts.length - suffixLength;
        int coordinateTokenCount = Math.min(3, suffixLength);
        int matches = 0;
        for (int i = start; i < start + coordinateTokenCount; i++) {
            if (COORD_ARGUMENT_INPUT_PATTERN.matcher(parts[i]).matches()) {
                matches++;
            }
        }
        return matches >= requiredCoordinateTokens;
    }

    private String parseBookmarkAddDimension(String rawDimension, String defaultDimensionId) {
        if (rawDimension == null || rawDimension.isBlank()) {
            return defaultDimensionId;
        }

        String normalized = ChatCoordinateParser.normalizeDimensionHint(rawDimension);
        if (normalized != null) {
            return normalized;
        }

        String trimmed = rawDimension.trim();
        return trimmed.contains(":") ? trimmed : null;
    }

    private static boolean isValidBookmarkCoordinate(double x, double y, double z) {
        return Math.abs(x) <= 30_000_000
                && Math.abs(z) <= 30_000_000
                && y >= -2048
                && y <= 2048;
    }

    private static final class BookmarkAddRequest {
        final String name;
        final double x;
        final double y;
        final double z;
        final String dimensionId;

        private BookmarkAddRequest(String name, double x, double y, double z, String dimensionId) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimensionId = dimensionId;
        }
    }

    private int executeBookmarkList() {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        List<CopyCoordsDataStore.BookmarkEntry> bookmarks = dataStore.getBookmarks();
        if (bookmarks.isEmpty()) {
            sendSystemMessage(Component.literal("No bookmarks yet."));
            return 0;
        }

        sendSystemMessage(Component.literal("Bookmarks (click to copy):"));
        for (CopyCoordsDataStore.BookmarkEntry entry : bookmarks) {
            String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);
            String command = "/coordsbookmark copy " + quoteForBrigadier(entry.name);
            ClickEvent clickEvent = buildClickEvent(command);
            HoverEvent hoverEvent = buildHoverEvent(Component.literal("Copy to clipboard"));
            net.minecraft.network.chat.MutableComponent line = Component.literal(entry.name + " - " + coordString)
                    .withStyle(style -> applyEvents(style, clickEvent, hoverEvent));
            appendMapLinks(line,
                    (int) Math.floor(entry.x),
                    (int) Math.floor(entry.y),
                    (int) Math.floor(entry.z),
                    entry.dimensionId);
            line.append(Component.literal(" "));
            line.append(buildActionChip(
                    "xaero",
                    buildClickEvent("/coordsbookmark xaero add " + quoteForBrigadier(entry.name)),
                    "Export this bookmark to Xaero waypoints"));
            sendSystemMessage(line);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeBookmarkCopy(String name) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        CopyCoordsDataStore.BookmarkEntry entry = dataStore.getBookmark(name);
        if (entry == null) {
            sendSystemMessage(Component.literal("Bookmark not found: " + name));
            return 0;
        }

        String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);
        try {
            ClipboardUtils.copyToClipboard(coordString);
            sendSystemMessage(Component.literal("Copied bookmark '" + entry.name + "' to clipboard."));
            addHistoryEntry(
                    (int) Math.floor(entry.x),
                    (int) Math.floor(entry.y),
                    (int) Math.floor(entry.z),
                    entry.dimensionId);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            sendSystemMessage(Component.translatable("message.copycoords.command.copy_failed", errorMsg));
            return 0;
        }
    }

    private int executeBookmarkRemove(String name) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        if (dataStore.removeBookmark(name)) {
            sendSystemMessage(Component.literal("Bookmark removed: " + name));
            return Command.SINGLE_SUCCESS;
        }

        sendSystemMessage(Component.literal("Bookmark not found: " + name));
        return 0;
    }

    private int executeBookmarkExport(String file) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        try {
            Path path = resolveBookmarkTransferPath(file, false);
            if (dataStore.exportBookmarks(path)) {
                sendSystemMessage(Component.translatable("message.copycoords.bookmark.exported", path.toString()));
                return Command.SINGLE_SUCCESS;
            }
            sendSystemMessage(Component.translatable("message.copycoords.bookmark.export_failed", "see log for details"));
        } catch (Exception e) {
            sendSystemMessage(Component.translatable("message.copycoords.bookmark.export_failed", e.getMessage()));
        }
        return 0;
    }

    private int executeBookmarkImport(String file) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        try {
            Path path = resolveBookmarkTransferPath(file, true);
            if (dataStore.importBookmarks(path)) {
                sendSystemMessage(Component.translatable("message.copycoords.bookmark.imported", path.toString()));
                return Command.SINGLE_SUCCESS;
            }
            sendSystemMessage(Component.translatable("message.copycoords.bookmark.import_failed", "see log for details"));
        } catch (Exception e) {
            sendSystemMessage(Component.translatable("message.copycoords.bookmark.import_failed", e.getMessage()));
        }
        return 0;
    }

    private int executeBookmarkXaeroAdd(String name) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }

        CopyCoordsDataStore.BookmarkEntry entry = dataStore.getBookmark(name);
        if (entry == null) {
            sendSystemMessage(Component.literal("Bookmark not found: " + name));
            return 0;
        }

        try {
            XaeroWaypointExporter.XaeroExportResult result =
                    XaeroWaypointExporter.exportToCurrentTarget(Minecraft.getInstance(), entry);
            sendSystemMessage(Component.literal(
                    "Exported bookmark '" + entry.name + "' to Xaero waypoints in " + result.targetPath));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendSystemMessage(Component.literal("Failed to export bookmark to Xaero waypoints: " + e.getMessage()));
            return 0;
        }
    }

    private int executeBookmarkXaeroAddToTarget(String name, String path) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }

        CopyCoordsDataStore.BookmarkEntry entry = dataStore.getBookmark(name);
        if (entry == null) {
            sendSystemMessage(Component.literal("Bookmark not found: " + name));
            return 0;
        }

        try {
            XaeroWaypointExporter.XaeroExportResult result =
                    XaeroWaypointExporter.exportToTarget(entry, path);
            sendSystemMessage(Component.literal(
                    "Exported bookmark '" + entry.name + "' to Xaero target " + result.targetPath));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendSystemMessage(Component.literal("Failed to export bookmark to Xaero target: " + e.getMessage()));
            return 0;
        }
    }

    private static Path resolveBookmarkTransferPath(String file, boolean forImport) {
        String trimmed = file == null ? "" : file.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("File path cannot be empty.");
        }

        Path rawPath = Path.of(trimmed);
        if (hasJsonExtension(rawPath)) {
            return rawPath;
        }

        if (forImport && Files.exists(rawPath)) {
            return rawPath;
        }

        return Path.of(trimmed + ".json");
    }

    private static boolean hasJsonExtension(Path path) {
        Path fileName = path.getFileName();
        String name = fileName == null ? path.toString() : fileName.toString();
        return name.toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private static String quoteForBrigadier(String value) {
        String escaped = value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
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
        return ChatEventFactory.runCommand(command);
    }

    private static ClickEvent buildSuggestCommandEvent(String command) {
        return ChatEventFactory.suggestCommand(command);
    }

    private static HoverEvent buildHoverEvent(Component text) {
        return ChatEventFactory.showText(text);
    }

    private static net.minecraft.network.chat.MutableComponent buildActionChip(String label,
                                                                               ClickEvent clickEvent,
                                                                               String hoverText) {
        HoverEvent hoverEvent = buildHoverEvent(Component.literal(hoverText));
        return Component.literal("[" + label + "]")
                .withStyle(style -> applyEvents(style, clickEvent, hoverEvent));
    }

    private static void handleIncomingChatText(String text) {
        if (config == null || !config.chatCoordinateDetectionEnabled || text == null || text.isBlank()) {
            return;
        }

        String trimmed = text.trim();
        if (shouldIgnoreIncomingChatText(trimmed)) {
            return;
        }

        List<ChatCoordinateParser.DetectedCoordinate> detections = ChatCoordinateParser.detect(
                trimmed,
                CopyCoordsConfig.clampChatCoordinateDetectionMaxPerMessage(config.chatCoordinateDetectionMaxPerMessage));

        for (ChatCoordinateParser.DetectedCoordinate detection : detections) {
            sendSystemMessage(buildDetectedCoordinateMessage(detection));
        }
    }

    private static boolean shouldIgnoreIncomingChatText(String text) {
        return consumeRememberedLocalMessage(text)
                || text.startsWith(DETECTED_COORDINATE_PREFIX)
                || text.startsWith(DETECTED_CONVERTED_PREFIX)
                || text.startsWith("Your current coordinates are:")
                || text.startsWith("Converted coordinates:")
                || text.startsWith("Copied coordinates to clipboard:")
                || text.startsWith("Copied converted coordinates to clipboard:")
                || text.startsWith("Copied coordinates with dimension to clipboard:");
    }

    private static void rememberLocalMessage(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        synchronized (RECENT_LOCAL_MESSAGES) {
            RECENT_LOCAL_MESSAGES.addLast(text);
            while (RECENT_LOCAL_MESSAGES.size() > RECENT_LOCAL_MESSAGE_LIMIT) {
                RECENT_LOCAL_MESSAGES.removeFirst();
            }
        }
    }

    private static boolean consumeRememberedLocalMessage(String text) {
        synchronized (RECENT_LOCAL_MESSAGES) {
            return RECENT_LOCAL_MESSAGES.removeFirstOccurrence(text);
        }
    }

    private static net.minecraft.network.chat.MutableComponent buildDetectedCoordinateMessage(
            ChatCoordinateParser.DetectedCoordinate detection) {
        String coordString = formatCoordinates(detection.x, detection.y, detection.z, detection.dimensionId);
        String copyCommand = buildDetectedCopyCommand(detection.x, detection.y, detection.z, detection.dimensionId);
        net.minecraft.network.chat.MutableComponent message = Component.literal(DETECTED_COORDINATE_PREFIX);
        message.append(Component.literal(coordString)
                .withStyle(style -> applyEvents(
                        style.withInsertion(coordString),
                        buildClickEvent(copyCommand),
                        buildHoverEvent(Component.literal("Copy detected coordinates")))));
        message.append(Component.literal(" "));
        message.append(buildActionChip("copy", buildClickEvent(copyCommand), "Copy to clipboard and add to history"));
        message.append(Component.literal(" "));
        message.append(buildActionChip("insert", buildSuggestCommandEvent(coordString), "Insert detected coordinates into chat input"));
        message.append(Component.literal(" "));
        message.append(buildActionChip(
                "bookmark",
                buildSuggestCommandEvent(buildDetectedBookmarkSuggestion(detection.x, detection.y, detection.z, detection.dimensionId)),
                "Insert a bookmark command for these coordinates"));
        appendDetectedConvertChip(message, detection);
        if (detection.dimensionId != null) {
            appendMapLinks(
                    message,
                    (int) Math.floor(detection.x),
                    (int) Math.floor(detection.y),
                    (int) Math.floor(detection.z),
                    detection.dimensionId);
        }
        return message;
    }

    private static void appendDetectedConvertChip(net.minecraft.network.chat.MutableComponent message,
                                                  ChatCoordinateParser.DetectedCoordinate detection) {
        String sourceDimensionId = detection.dimensionId;
        if (sourceDimensionId == null) {
            return;
        }

        if (sourceDimensionId.equals(OVERWORLD_ID)) {
            message.append(Component.literal(" "));
            message.append(buildActionChip(
                    "to_nether",
                    buildClickEvent(buildDetectedConvertCommand(sourceDimensionId, "nether", detection.x, detection.y, detection.z)),
                    "Convert detected coordinates to Nether scale"));
        } else if (sourceDimensionId.equals(NETHER_ID)) {
            message.append(Component.literal(" "));
            message.append(buildActionChip(
                    "to_overworld",
                    buildClickEvent(buildDetectedConvertCommand(sourceDimensionId, "overworld", detection.x, detection.y, detection.z)),
                    "Convert detected coordinates to Overworld scale"));
        }
    }

    private static String buildDetectedCopyCommand(double x, double y, double z, String dimensionId) {
        return "/copycoords detected copy "
                + ChatCoordinateParser.toCommandNumber(x) + " "
                + ChatCoordinateParser.toCommandNumber(y) + " "
                + ChatCoordinateParser.toCommandNumber(z) + " "
                + encodeDetectedDimensionArgument(dimensionId);
    }

    private static String buildDetectedBookmarkSuggestion(double x, double y, double z, String dimensionId) {
        return "/copycoords detected bookmark "
                + ChatCoordinateParser.toCommandNumber(x) + " "
                + ChatCoordinateParser.toCommandNumber(y) + " "
                + ChatCoordinateParser.toCommandNumber(z) + " "
                + encodeDetectedDimensionArgument(dimensionId) + " ";
    }

    private static String buildDetectedConvertCommand(String sourceDimensionId, String goal, double x, double y, double z) {
        return "/copycoords detected convert "
                + encodeDetectedDimensionArgument(sourceDimensionId) + " "
                + goal + " "
                + ChatCoordinateParser.toCommandNumber(x) + " "
                + ChatCoordinateParser.toCommandNumber(y) + " "
                + ChatCoordinateParser.toCommandNumber(z);
    }

    private static String encodeDetectedDimensionArgument(String dimensionId) {
        return dimensionId == null || dimensionId.isBlank() ? DETECTED_UNKNOWN_DIMENSION_TOKEN : dimensionId;
    }

    private static String decodeDetectedDimensionArgument(String token) {
        if (token == null || token.isBlank() || token.equalsIgnoreCase(DETECTED_UNKNOWN_DIMENSION_TOKEN)) {
            return null;
        }

        String normalized = ChatCoordinateParser.normalizeDimensionHint(token);
        return normalized == null ? token : normalized;
    }

    private int executeDetectedCopy(double x, double y, double z, String dimensionToken) {
        String dimensionId = decodeDetectedDimensionArgument(dimensionToken);
        String coordString = formatCoordinates(x, y, z, dimensionId);
        return copyFormattedCoordinatesWithFeedback(
                coordString,
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.floor(z),
                dimensionId);
    }

    private int executeDetectedBookmark(double x, double y, double z, String dimensionToken, String name) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }

        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            sendSystemMessage(Component.literal("Bookmark name cannot be empty."));
            return 0;
        }

        String dimensionId = decodeDetectedDimensionArgument(dimensionToken);
        if (!dataStore.addBookmark(trimmed, x, y, z, dimensionId)) {
            sendSystemMessage(Component.literal("Bookmark already exists: " + trimmed));
            return 0;
        }

        sendSystemMessage(Component.literal("Bookmark added from detected coordinates: " + trimmed));
        return Command.SINGLE_SUCCESS;
    }

    private int executeDetectedConvert(String sourceToken, String goal, double x, double y, double z) {
        String sourceDimensionId = decodeDetectedDimensionArgument(sourceToken);
        String normalizedGoal = goal == null ? "" : goal.trim().toLowerCase(Locale.ROOT);

        if (!normalizedGoal.equals("overworld") && !normalizedGoal.equals("nether")) {
            sendSystemMessage(Component.translatable("message.copycoords.command.unknown_goal", goal));
            return 0;
        }

        if (!OVERWORLD_ID.equals(sourceDimensionId) && !NETHER_ID.equals(sourceDimensionId)) {
            sendSystemMessage(Component.literal("Detected coordinates can only be converted when the source dimension is Overworld or Nether."));
            return 0;
        }

        double convertedX = x;
        double convertedZ = z;
        if (OVERWORLD_ID.equals(sourceDimensionId) && normalizedGoal.equals("nether")) {
            convertedX = x / 8.0;
            convertedZ = z / 8.0;
        } else if (NETHER_ID.equals(sourceDimensionId) && normalizedGoal.equals("overworld")) {
            convertedX = x * 8.0;
            convertedZ = z * 8.0;
        }

        String targetDimensionId = getDimensionIdForGoal(normalizedGoal);
        String out = formatCoordinates(convertedX, y, convertedZ, targetDimensionId);
        int floorX = (int) Math.floor(convertedX);
        int floorY = (int) Math.floor(y);
        int floorZ = (int) Math.floor(convertedZ);
        sendSystemMessage(buildCoordinateMessage(DETECTED_CONVERTED_PREFIX, out, floorX, floorY, floorZ, targetDimensionId));

        if (config != null && config.copyConvertedToClipboard) {
            return copyFormattedCoordinatesWithFeedback(out, floorX, floorY, floorZ, targetDimensionId);
        }

        return Command.SINGLE_SUCCESS;
    }

    private void postCoordinateMessage(String prefix, String coordString, int x, int y, int z, String dimensionId) {
        sendSystemMessage(buildCoordinateMessage(prefix, coordString, x, y, z, dimensionId));
    }

    static net.minecraft.network.chat.MutableComponent buildCoordinateMessage(String prefix, String coordString, int x, int y, int z, String dimensionId) {
        net.minecraft.network.chat.MutableComponent message = Component.literal(prefix);
        net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, x, y, z, dimensionId);
        message.append(clickableCoord);
        appendMapLinks(message, x, y, z, dimensionId);
        return message;
    }

    static void appendMapLinks(net.minecraft.network.chat.MutableComponent message, int x, int y, int z, String dimensionId) {
        if (config == null || !config.mapLinksEnabled) {
            return;
        }

        tryAppendLink(message, "Dynmap", config.dynmapUrlTemplate, x, y, z, dimensionId, FabricLoader.getInstance().isModLoaded("dynmap"));
        tryAppendLink(message, "BlueMap", config.bluemapUrlTemplate, x, y, z, dimensionId, FabricLoader.getInstance().isModLoaded("bluemap"));
        tryAppendLink(message, "Map", config.webMapUrlTemplate, x, y, z, dimensionId, true);
    }

    private static void tryAppendLink(net.minecraft.network.chat.MutableComponent message,
                                      String label,
                                      String template,
                                      int x,
                                      int y,
                                      int z,
                                      String dimensionId,
                                      boolean available) {
        if (!available || template == null || template.isBlank()) {
            return;
        }

        try {
            String world = getMapWorldName(dimensionId);
            String encodedWorld = URLEncoder.encode(world, StandardCharsets.UTF_8);
            String encodedDimension = URLEncoder.encode(dimensionId == null ? "" : dimensionId, StandardCharsets.UTF_8);
            String url = template.trim()
                    .replace("{x}", Integer.toString(x))
                    .replace("{y}", Integer.toString(y))
                    .replace("{z}", Integer.toString(z))
                    .replace("{world}", world)
                    .replace("{worldEncoded}", encodedWorld)
                    .replace("{dimension}", dimensionId == null ? "" : dimensionId)
                    .replace("{dimensionEncoded}", encodedDimension);

            if (!(url.startsWith("http://") || url.startsWith("https://"))) {
                return;
            }

            ClickEvent clickEvent = buildOpenUrlClickEvent(url);
            HoverEvent hoverEvent = buildHoverEvent(Component.literal("Open " + label + " link"));
            net.minecraft.network.chat.MutableComponent link = Component.literal(" [" + label + "]")
                    .withStyle(style -> applyEvents(style, clickEvent, hoverEvent));
            message.append(link);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid map link URL for template '" + label + "': " + e.getMessage());
        }
    }

    private static String getMapWorldName(String dimensionId) {
        dimensionId = normalizeDimensionId(dimensionId);
        if (dimensionId == null) {
            return "world";
        }
        if (dimensionId.equals(NETHER_ID)) {
            return "world_nether";
        }
        if (dimensionId.equals(END_ID)) {
            return "world_the_end";
        }
        return "world";
    }

    private static ClickEvent buildOpenUrlClickEvent(String url) {
        return ChatEventFactory.openUrl(url);
    }

    private int executeDistanceCalc(CommandContext<FabricClientCommandSource> context) {
        try {
            int x1 = IntegerArgumentType.getInteger(context, "x1");
            int y1 = IntegerArgumentType.getInteger(context, "y1");
            int z1 = IntegerArgumentType.getInteger(context, "z1");
            int x2 = IntegerArgumentType.getInteger(context, "x2");
            int y2 = IntegerArgumentType.getInteger(context, "y2");
            int z2 = IntegerArgumentType.getInteger(context, "z2");

            DistanceCalculator.DistanceResult result = DistanceCalculator.calculate(x1, y1, z1, x2, y2, z2);

            String message = "§6Distance Calculator§r: From ["
                    + formatCoordinateValue(x1) + ", "
                    + formatCoordinateValue(y1) + ", "
                    + formatCoordinateValue(z1) + "] to ["
                    + formatCoordinateValue(x2) + ", "
                    + formatCoordinateValue(y2) + ", "
                    + formatCoordinateValue(z2) + "]";
            sendSystemMessage(Component.literal(message));
            
            String resultMessage = "§2" + DistanceCalculator.formatResult(result, true);
            sendSystemMessage(Component.literal(resultMessage));
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendSystemMessage(Component.literal("§cError calculating distance: " + e.getMessage()));
            return 0;
        }
    }

    private int executeDistanceCalcBookmarks(CommandContext<FabricClientCommandSource> context) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        try {
            String bm1Name = StringArgumentType.getString(context, "bookmark1");
            String bm2Name = StringArgumentType.getString(context, "bookmark2");
            
            CopyCoordsDataStore.BookmarkEntry bm1 = dataStore.getBookmark(bm1Name);
            CopyCoordsDataStore.BookmarkEntry bm2 = dataStore.getBookmark(bm2Name);
            
            if (bm1 == null) {
                sendSystemMessage(Component.literal("§cBookmark not found: " + bm1Name));
                return 0;
            }
            if (bm2 == null) {
                sendSystemMessage(Component.literal("§cBookmark not found: " + bm2Name));
                return 0;
            }
            
            DistanceCalculator.DistanceResult result = DistanceCalculator.calculate(
                    (int) Math.floor(bm1.x),
                    (int) Math.floor(bm1.y),
                    (int) Math.floor(bm1.z),
                    (int) Math.floor(bm2.x),
                    (int) Math.floor(bm2.y),
                    (int) Math.floor(bm2.z));

            String message = "§6Distance Calculator§r: From '" + bm1Name + "' to '" + bm2Name + "'";
            sendSystemMessage(Component.literal(message));
            
            String coordMessage = "  From ["
                    + formatCoordinateValue(bm1.x) + ", "
                    + formatCoordinateValue(bm1.y) + ", "
                    + formatCoordinateValue(bm1.z) + "] to ["
                    + formatCoordinateValue(bm2.x) + ", "
                    + formatCoordinateValue(bm2.y) + ", "
                    + formatCoordinateValue(bm2.z) + "]";
            sendSystemMessage(Component.literal(coordMessage));
            
            String resultMessage = "§2" + DistanceCalculator.formatResult(result, true);
            sendSystemMessage(Component.literal(resultMessage));
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            sendSystemMessage(Component.literal("§cError calculating distance: " + e.getMessage()));
            return 0;
        }
    }

}

