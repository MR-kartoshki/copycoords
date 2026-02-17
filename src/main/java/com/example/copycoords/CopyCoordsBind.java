package com.example.copycoords;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

// Handles keybinding registration and execution for copying coordinates via keyboard shortcut
public class CopyCoordsBind {
    // The key mapping for the copy coordinates keybind
    private static KeyMapping copyKeyBinding;

    // Register the keybind and set up the tick event listener
    public static void register() {
        // Create a custom keybind category for this mod
        KeyMapping.Category copyCategory = KeyMapping.Category.register(Identifier.tryParse("copycoords:copycoords"));
        // Register the keybind with default key 'C'
        copyKeyBinding = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.copycoords.copy", GLFW.GLFW_KEY_C, copyCategory)
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
        String coordString = x + " " + y + " " + z;

        try {
            // Copy coordinates to clipboard using Windows clip.exe
            Process process = Runtime.getRuntime().exec("cmd.exe /c echo " + coordString + " | clip.exe");
            process.waitFor();
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
}
