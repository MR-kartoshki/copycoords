package com.example.copycoords;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import com.mojang.blaze3d.platform.InputConstants;

// Handles keybinding registration and execution for copying coordinates via keyboard shortcut
public class CopyCoordsBind {
    // The key mapping for the copy coordinates keybind
    private static KeyMapping copyKeyBinding;

    // Register the keybind and set up the tick event listener
    public static void register() {
        // Create a custom keybind category for this mod
        Object copyCategory = createKeyCategory("copycoords:copycoords");
        // Register the keybind with default key 'C'
        copyKeyBinding = KeyBindingHelper.registerKeyBinding(
            createKeyMapping("key.copycoords.copy", GLFW.GLFW_KEY_C, copyCategory)
        );

        // Listen for key presses each client tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (copyKeyBinding.consumeClick()) {
                executeKeybindCopy(client);
            }
        });
    }

    // Execute coordinate copying when keybind is pressed
    private static void executeKeybindCopy(Minecraft minecraft) {
        // Ensure player exists before accessing position
        if (minecraft.player == null) {
            return;
        }

        // Get player's current block coordinates
        int x = minecraft.player.blockPosition().getX();
        int y = minecraft.player.blockPosition().getY();
        int z = minecraft.player.blockPosition().getZ();
        
        // Format coordinates according to config
        CoordinateFormat format = CoordinateFormat.fromId(CopyCoords.config.coordinateFormat);
        String coordString = format.format(x, y, z);
        if (CopyCoords.config.showDimensionInCoordinates) {
            coordString += " (" + getDimensionName(minecraft.player) + ")";
        }

        try {
            // Copy coordinates to clipboard using cross-platform utility
            ClipboardUtils.copyToClipboard(coordString);
            // Notify player of successful copy
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.copied", coordString));
        } catch (Exception e) {
            // Handle clipboard copy errors
            String errorMsg = e.getMessage();
            // Use exception class name if no message is available
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.failed", errorMsg));
        }
    }

    // Use reflection to stay compatible across 1.21.1 and 1.21.11 keybinding APIs
    private static Object createKeyCategory(String id) {
        try {
            Class<?> categoryClass = Class.forName("net.minecraft.client.KeyMapping$Category");
            Class<?> identifierClass;
            try {
                identifierClass = Class.forName("net.minecraft.resources.Identifier");
            } catch (ClassNotFoundException e) {
                identifierClass = Class.forName("net.minecraft.util.Identifier");
            }
            Method tryParse = identifierClass.getMethod("tryParse", String.class);
            Object identifier = tryParse.invoke(null, id);
            Method register = categoryClass.getMethod("register", identifierClass);
            return register.invoke(null, identifier);
        } catch (Exception e) {
            // Fall back to a category string if the Category API is unavailable
            return "key.categories.copycoords";
        }
    }

    private static KeyMapping createKeyMapping(String translationKey, int keyCode, Object category) {
        String categoryKey = "key.categories.copycoords";

        for (Constructor<?> ctor : KeyMapping.class.getConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            try {
                if (params.length == 3
                    && params[0] == String.class
                    && params[1] == int.class
                    && params[2] == String.class) {
                    return (KeyMapping) ctor.newInstance(translationKey, keyCode, categoryKey);
                }
                if (params.length == 3
                    && params[0] == String.class
                    && params[1] == int.class
                    && params[2].getName().endsWith("KeyMapping$Category")) {
                    return (KeyMapping) ctor.newInstance(translationKey, keyCode, category);
                }
                if (params.length == 4
                    && params[0] == String.class
                    && params[1].getName().equals("com.mojang.blaze3d.platform.InputConstants$Type")
                    && params[2] == int.class) {
                    if (params[3] == String.class) {
                        return (KeyMapping) ctor.newInstance(translationKey, InputConstants.Type.KEYSYM, keyCode, categoryKey);
                    }
                    if (params[3].getName().endsWith("KeyMapping$Category")) {
                        return (KeyMapping) ctor.newInstance(translationKey, InputConstants.Type.KEYSYM, keyCode, category);
                    }
                }
            } catch (Exception ignored) {
                // Try next constructor
            }
        }

        throw new IllegalStateException("No compatible KeyMapping constructor found.");
    }

    // Helper method to get dimension name from player's current dimension
    private static String getDimensionName(Player player) {
        if (player.level().dimension().equals(Level.OVERWORLD)) {
            return "Overworld";
        } else if (player.level().dimension().equals(Level.NETHER)) {
            return "Nether";
        } else if (player.level().dimension().equals(Level.END)) {
            return "End";
        }
        // Fallback for unknown dimensions
        return player.level().dimension().toString();
    }
}
