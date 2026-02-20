package com.example.copycoords;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

public class CopyCoordsBind {
    private static KeyMapping copyKeyBinding;
    private static KeyMapping copyConvertedKeyBinding;
    private static KeyMapping copyWithDimensionKeyBinding;

    private static final String CATEGORY_KEY = "key.categories.copycoords";

    private static boolean REGISTERED = false;

    public static void register() {
        if (REGISTERED) {
            return;
        }
        REGISTERED = true;

        copyKeyBinding = KeyBindingHelper.registerKeyBinding(
                createKeyMapping("key.copycoords.copy", GLFW.GLFW_KEY_C));
        copyConvertedKeyBinding = KeyBindingHelper.registerKeyBinding(
                createKeyMapping("key.copycoords.copy_converted", GLFW.GLFW_KEY_V));
        copyWithDimensionKeyBinding = KeyBindingHelper.registerKeyBinding(
                createKeyMapping("key.copycoords.copy_with_dimension", GLFW.GLFW_KEY_B));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (copyKeyBinding != null) {
                while (copyKeyBinding.consumeClick()) {
                    executeKeybindCopy(client);
                }
            }
            if (copyConvertedKeyBinding != null) {
                while (copyConvertedKeyBinding.consumeClick()) {
                    executeKeybindCopyConverted(client);
                }
            }
            if (copyWithDimensionKeyBinding != null) {
                while (copyWithDimensionKeyBinding.consumeClick()) {
                    executeKeybindCopyWithDimension(client);
                }
            }
        });
    }

    /**
     * Legacy constructor for 1.21.0-1.21.8: category is a plain String.
     * Overridden by per-version sources in 1.21.9+.
     */
    private static KeyMapping createKeyMapping(String translationKey, int keyCode) {
        return new KeyMapping(translationKey, InputConstants.Type.KEYSYM, keyCode, CATEGORY_KEY);
    }

    @SuppressWarnings("null")
    private static void executeKeybindCopy(Minecraft minecraft) {
        net.minecraft.world.entity.player.Player player = minecraft.player;
        if (player == null) {
            return;
        }

        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();

        String dimensionId = CopyCoords.getDimensionId(player);
        String coordString = CopyCoords.formatCoordinates(x, y, z, dimensionId);

        try {
            if (CopyCoords.config != null && CopyCoords.config.pasteToChatInput) {
                try {
                    CopyCoords.openChatWithText(coordString);
                } catch (Throwable t) {
                    ClipboardUtils.copyToClipboard(coordString);
                    minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Copied coordinates to clipboard: ").append(ClickableCoordinateComponent.createClickableCoordinate(coordString, x, y, z, dimensionId)));
                }
            } else {
                ClipboardUtils.copyToClipboard(coordString);
                net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal("Copied coordinates to clipboard: ");
                net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, x, y, z, dimensionId);
                minecraft.gui.getChat().addMessage(message.append(clickableCoord));
            }
            CopyCoords.addHistoryEntry(x, y, z, dimensionId);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.failed", errorMsg));
        }
    }

    @SuppressWarnings("null")
    private static void executeKeybindCopyConverted(Minecraft minecraft) {
        net.minecraft.world.entity.player.Player player = minecraft.player;
        if (player == null) {
            return;
        }

        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();

        String goal;
        if (player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            goal = "nether";
        } else if (player.level().dimension().equals(net.minecraft.world.level.Level.NETHER)) {
            goal = "overworld";
        } else {
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.command.unsupported_dimension"));
            return;
        }

        double rx = x;
        double rz = z;
        if (goal.equals("nether")) {
            rx = Math.floor(x / 8.0);
            rz = Math.floor(z / 8.0);
        } else if (goal.equals("overworld")) {
            rx = Math.floor(x * 8.0);
            rz = Math.floor(z * 8.0);
        }

        int convertedX = (int) rx;
        int convertedZ = (int) rz;
        String convertedDimensionId = CopyCoords.getDimensionIdForGoal(goal);
        String coordString = CopyCoords.formatCoordinates(convertedX, y, convertedZ, convertedDimensionId);

        try {
            if (CopyCoords.config != null && CopyCoords.config.pasteToChatInput) {
                try {
                    CopyCoords.openChatWithText(coordString);
                } catch (Throwable t) {
                    ClipboardUtils.copyToClipboard(coordString);
                    minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Copied converted coordinates to clipboard: ").append(ClickableCoordinateComponent.createClickableCoordinate(coordString, convertedX, y, convertedZ, convertedDimensionId)));
                }
            } else {
                ClipboardUtils.copyToClipboard(coordString);
                net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal("Copied converted coordinates to clipboard: ");
                net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, convertedX, y, convertedZ, convertedDimensionId);
                minecraft.gui.getChat().addMessage(message.append(clickableCoord));
            }
            CopyCoords.addHistoryEntry(convertedX, y, convertedZ, convertedDimensionId);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.failed", errorMsg));
        }
    }

    @SuppressWarnings("null")
    private static void executeKeybindCopyWithDimension(Minecraft minecraft) {
        net.minecraft.world.entity.player.Player player = minecraft.player;
        if (player == null) {
            return;
        }

        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();

        String dimensionId = CopyCoords.getDimensionId(player);

        CoordinateFormat format = CoordinateFormat.fromId(CopyCoords.config.coordinateFormat);
        String coordString = format.format(x, y, z) + " (" + CopyCoords.getDimensionNameFromId(dimensionId) + ")";

        try {
            if (CopyCoords.config != null && CopyCoords.config.pasteToChatInput) {
                try {
                    CopyCoords.openChatWithText(coordString);
                } catch (Throwable t) {
                    ClipboardUtils.copyToClipboard(coordString);
                    minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Copied coordinates with dimension to clipboard: ").append(ClickableCoordinateComponent.createClickableCoordinate(coordString, x, y, z, dimensionId)));
                }
            } else {
                ClipboardUtils.copyToClipboard(coordString);
                net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal("Copied coordinates with dimension to clipboard: ");
                net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, x, y, z, dimensionId);
                minecraft.gui.getChat().addMessage(message.append(clickableCoord));
            }
            CopyCoords.addHistoryEntry(x, y, z, dimensionId);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.failed", errorMsg));
        }
    }
}
