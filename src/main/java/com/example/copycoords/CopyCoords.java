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
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class CopyCoords implements ClientModInitializer {
    public static CopyCoordsConfig config;
    public static CopyCoordsDataStore dataStore;

    private static final String OVERWORLD_ID = Level.OVERWORLD.toString();
    private static final String NETHER_ID = Level.NETHER.toString();
    private static final String END_ID = Level.END.toString();

    @Override
    @SuppressWarnings("null")
    public void onInitializeClient() {
        config = CopyCoordsConfig.load();
        dataStore = CopyCoordsDataStore.load();
        CopyCoordsBind.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal("copycoords");
            builder.executes(context -> executeCopyCoords(context));

            LiteralArgumentBuilder<FabricClientCommandSource> conv = ClientCommandManager.literal("convertcoords");

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

            RequiredArgumentBuilder<FabricClientCommandSource, String> copyGoalArg =
                ClientCommandManager.argument("goal", StringArgumentType.word())
                    .suggests(dimSuggestions)
                    .executes(context -> executeCopyCoordsWithGoal(context));
            builder.then(copyGoalArg);
            dispatcher.register(builder);

            LiteralArgumentBuilder<FabricClientCommandSource> cc = ClientCommandManager.literal("cc");
            cc.executes(context -> executeCopyCoords(context));
            cc.then(copyGoalArg);
            dispatcher.register(cc);

            LiteralArgumentBuilder<FabricClientCommandSource> copycoordinates = ClientCommandManager.literal("copycoordinates");
            copycoordinates.executes(context -> executeCopyCoords(context));
            copycoordinates.then(copyGoalArg);
            dispatcher.register(copycoordinates);

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

                LiteralArgumentBuilder<FabricClientCommandSource> history = ClientCommandManager.literal("coordshistory");
                history.executes(context -> executeHistoryList());
                history.then(ClientCommandManager.literal("list")
                    .executes(context -> executeHistoryList()));
                history.then(ClientCommandManager.literal("clear")
                    .executes(context -> executeHistoryClear()));
                history.then(ClientCommandManager.literal("copy")
                    .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> executeHistoryCopy(IntegerArgumentType.getInteger(context, "index")))));
                history.then(ClientCommandManager.literal("remove")
                    .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> executeHistoryRemove(IntegerArgumentType.getInteger(context, "index")))));
                history.then(ClientCommandManager.literal("menu")
                    .then(ClientCommandManager.argument("index", IntegerArgumentType.integer(1))
                        .executes(context -> executeHistoryMenu(IntegerArgumentType.getInteger(context, "index")))));
                dispatcher.register(history);

                dispatcher.register(buildBookmarkCommand("coordsbookmark", bookmarkSuggestions));
                dispatcher.register(buildBookmarkCommand("coordbookmark", bookmarkSuggestions));

                LiteralArgumentBuilder<FabricClientCommandSource> distcalc = ClientCommandManager.literal("distcalc");
                
                RequiredArgumentBuilder<FabricClientCommandSource, Integer> x1Arg =
                    ClientCommandManager.argument("x1", IntegerArgumentType.integer())
                        .then(ClientCommandManager.argument("y1", IntegerArgumentType.integer())
                            .then(ClientCommandManager.argument("z1", IntegerArgumentType.integer())
                                .then(ClientCommandManager.argument("x2", IntegerArgumentType.integer())
                                    .then(ClientCommandManager.argument("y2", IntegerArgumentType.integer())
                                        .then(ClientCommandManager.argument("z2", IntegerArgumentType.integer())
                                            .executes(context -> executeDistanceCalc(context)))))));
                
                distcalc.then(x1Arg);

                RequiredArgumentBuilder<FabricClientCommandSource, String> bm1Arg =
                    ClientCommandManager.argument("bookmark1", StringArgumentType.string())
                        .suggests(bookmarkSuggestions)
                        .then(ClientCommandManager.argument("bookmark2", StringArgumentType.string())
                            .suggests(bookmarkSuggestions)
                            .executes(context -> executeDistanceCalcBookmarks(context)));

                distcalc.then(ClientCommandManager.literal("bookmarks")
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

        client.gui.getChat().addMessage(Component.literal("[CopyCoords] Instant send failed: " + prefix + " (" + details + ")"));
    }

    private int executeCopyCoords(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;

        if (player == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
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
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
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
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
            return 0;
        }

        String goal = StringArgumentType.getString(context, "goal").toLowerCase();
        
        double x, y, z;

        String coordInput = StringArgumentType.getString(context, "coordinates");
        String[] parts = coordInput.trim().split("\\s+");

        x = parseCoordinate(parts.length > 0 ? parts[0] : "~", player.getX());
        y = parseCoordinate(parts.length > 1 ? parts[1] : "~", player.getY());
        z = parseCoordinate(parts.length > 2 ? parts[2] : "~", player.getZ());

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
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
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

    private LiteralArgumentBuilder<FabricClientCommandSource> buildBookmarkCommand(
            String rootCommand,
            SuggestionProvider<FabricClientCommandSource> bookmarkSuggestions) {
        LiteralArgumentBuilder<FabricClientCommandSource> bookmark = ClientCommandManager.literal(rootCommand);
        bookmark.executes(context -> executeBookmarkList());
        bookmark.then(ClientCommandManager.literal("list")
                .executes(context -> executeBookmarkList()));
        bookmark.then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(context -> executeBookmarkAdd(StringArgumentType.getString(context, "name")))));
        bookmark.then(ClientCommandManager.literal("copy")
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                        .suggests(bookmarkSuggestions)
                        .executes(context -> executeBookmarkCopy(StringArgumentType.getString(context, "name")))));
        bookmark.then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("name", StringArgumentType.string())
                        .suggests(bookmarkSuggestions)
                        .executes(context -> executeBookmarkRemove(StringArgumentType.getString(context, "name")))));
        bookmark.then(ClientCommandManager.literal("export")
                .then(ClientCommandManager.argument("file", StringArgumentType.greedyString())
                        .executes(ctx -> executeBookmarkExport(StringArgumentType.getString(ctx, "file")))));
        bookmark.then(ClientCommandManager.literal("import")
                .then(ClientCommandManager.argument("file", StringArgumentType.greedyString())
                        .executes(ctx -> executeBookmarkImport(StringArgumentType.getString(ctx, "file")))));
        return bookmark;
    }

    private int executeMsgCoordsWithGoal(CommandContext<FabricClientCommandSource> context) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.no_player"));
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
                return playerCoord;
            }
        } else {

            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                return playerCoord;
            }
        }
    }

    private double[] convertCurrentCoordsToGoal(Player player, String goal, double x, double y, double z) {
        if (!goal.equals("overworld") && !goal.equals("nether")) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.unknown_goal", goal));
            return null;
        }

        if (!PlayerLevelCompat.isInDimension(player, Level.OVERWORLD) && !PlayerLevelCompat.isInDimension(player, Level.NETHER)) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.unsupported_dimension"));
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
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.msg_failed", target, "no server connection"));
            return 0;
        }

        try {
            return ChatSendCompat.sendCommand(Minecraft.getInstance(), connection, "msg " + target + " " + coordString)
                    ? Command.SINGLE_SUCCESS
                    : 0;
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
        return PlayerLevelCompat.getDimensionId(player);
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
        mc.gui.getChat().addMessage(Component.literal("Data store is not available yet."));
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
        try {
            if (config != null && config.pasteToChatInput) {
                openChatWithText(text);
            } else {
                ClipboardUtils.copyToClipboard(text);
                Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copied"));
            }
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
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        List<CopyCoordsDataStore.HistoryEntry> history = dataStore.getHistory();
        if (history.isEmpty()) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("History is empty."));
            return 0;
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Recent coordinates (click entry to copy, use [menu] for actions):"));

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
            Minecraft.getInstance().gui.getChat().addMessage(line);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeHistoryCopy(int index) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
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

    private int executeHistoryRemove(int index) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        if (!dataStore.removeHistoryEntry(index - 1)) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Invalid history index: " + index));
            return 0;
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Removed history entry " + index + "."));
        return Command.SINGLE_SUCCESS;
    }

    private int executeHistoryMenu(int index) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        List<CopyCoordsDataStore.HistoryEntry> history = dataStore.getHistory();
        if (index < 1 || index > history.size()) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Invalid history index: " + index));
            return 0;
        }

        CopyCoordsDataStore.HistoryEntry entry = history.get(index - 1);
        String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);

        net.minecraft.network.chat.MutableComponent title = Component.literal("History entry " + index + ": " + coordString);
        appendMapLinks(title, entry.x, entry.y, entry.z, entry.dimensionId);
        Minecraft.getInstance().gui.getChat().addMessage(title);

        net.minecraft.network.chat.MutableComponent actions = Component.literal("Actions: ");
        actions.append(buildActionChip("copy", buildClickEvent("/coordshistory copy " + index), "Copy this entry to clipboard"));
        actions.append(Component.literal(" "));
        actions.append(buildActionChip("insert", buildSuggestCommandEvent(coordString), "Insert this entry into chat input"));
        actions.append(Component.literal(" "));
        actions.append(buildActionChip("remove", buildClickEvent("/coordshistory remove " + index), "Remove this entry from history"));
        actions.append(Component.literal(" "));
        actions.append(buildActionChip("clear_all", buildClickEvent("/coordshistory clear"), "Clear all history entries"));
        Minecraft.getInstance().gui.getChat().addMessage(actions);

        return Command.SINGLE_SUCCESS;
    }

    private int executeHistoryClear() {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        dataStore.clearHistory();
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("History cleared."));
        return Command.SINGLE_SUCCESS;
    }

    private int executeBookmarkAdd(String name) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
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

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        String dimensionId = getDimensionId(player);

        if (!dataStore.addBookmark(trimmed, x, y, z, dimensionId)) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark already exists: " + trimmed));
            return 0;
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark added: " + trimmed));
        return Command.SINGLE_SUCCESS;
    }

    private int executeBookmarkList() {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        List<CopyCoordsDataStore.BookmarkEntry> bookmarks = dataStore.getBookmarks();
        if (bookmarks.isEmpty()) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("No bookmarks yet."));
            return 0;
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmarks (click to copy):"));
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
            Minecraft.getInstance().gui.getChat().addMessage(line);
        }

        return Command.SINGLE_SUCCESS;
    }

    private int executeBookmarkCopy(String name) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        CopyCoordsDataStore.BookmarkEntry entry = dataStore.getBookmark(name);
        if (entry == null) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark not found: " + name));
            return 0;
        }

        String coordString = formatCoordinates(entry.x, entry.y, entry.z, entry.dimensionId);
        try {
            ClipboardUtils.copyToClipboard(coordString);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Copied bookmark '" + entry.name + "' to clipboard."));
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
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.command.copy_failed", errorMsg));
            return 0;
        }
    }

    private int executeBookmarkRemove(String name) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        if (dataStore.removeBookmark(name)) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark removed: " + name));
            return Command.SINGLE_SUCCESS;
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Bookmark not found: " + name));
        return 0;
    }

    private int executeBookmarkExport(String file) {
        if (!ensureDataStoreAvailable()) {
            return 0;
        }
        try {
            Path path = resolveBookmarkTransferPath(file, false);
            if (dataStore.exportBookmarks(path)) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.bookmark.exported", path.toString()));
                return Command.SINGLE_SUCCESS;
            }
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.bookmark.export_failed", "see log for details"));
        } catch (Exception e) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.bookmark.export_failed", e.getMessage()));
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
                Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.bookmark.imported", path.toString()));
                return Command.SINGLE_SUCCESS;
            }
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.bookmark.import_failed", "see log for details"));
        } catch (Exception e) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.translatable("message.copycoords.bookmark.import_failed", e.getMessage()));
        }
        return 0;
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

    private void postCoordinateMessage(String prefix, String coordString, int x, int y, int z, String dimensionId) {
        Minecraft.getInstance().gui.getChat().addMessage(buildCoordinateMessage(prefix, coordString, x, y, z, dimensionId));
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
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(message));
            
            String resultMessage = "§2" + DistanceCalculator.formatResult(result, true);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(resultMessage));
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§cError calculating distance: " + e.getMessage()));
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
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§cBookmark not found: " + bm1Name));
                return 0;
            }
            if (bm2 == null) {
                Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§cBookmark not found: " + bm2Name));
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
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(message));
            
            String coordMessage = "  From ["
                    + formatCoordinateValue(bm1.x) + ", "
                    + formatCoordinateValue(bm1.y) + ", "
                    + formatCoordinateValue(bm1.z) + "] to ["
                    + formatCoordinateValue(bm2.x) + ", "
                    + formatCoordinateValue(bm2.y) + ", "
                    + formatCoordinateValue(bm2.z) + "]";
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(coordMessage));
            
            String resultMessage = "§2" + DistanceCalculator.formatResult(result, true);
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal(resultMessage));
            
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("§cError calculating distance: " + e.getMessage()));
            return 0;
        }
    }

}

