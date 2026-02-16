package com.example.copycoords;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

// Handles keybinding and keyboard event listening for the copy coordinates feature
public class CopyCoordsBind {
    private static KeyMapping copyKeyBinding;

    // Register the C key for copying coordinates and set up event listener
    public static void register() {
        // Create keybind category and register C key binding
        KeyMapping.Category copyCategory = KeyMapping.Category.register(Identifier.tryParse("copycoords:copycoords"));
        copyKeyBinding = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.copycoords.copy", GLFW.GLFW_KEY_C, copyCategory)
        );

        // Listen for key presses and copy coordinates when key is pressed
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (copyKeyBinding.consumeClick()) {
                executeKeybindCopy(client);
            }
        });
    }

    // Execute coordinate copy when keybind is pressed
    private static void executeKeybindCopy(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }

        // Get current player coordinates
        int x = minecraft.player.blockPosition().getX();
        int y = minecraft.player.blockPosition().getY();
        int z = minecraft.player.blockPosition().getZ();
        String coordString = x + " " + y + " " + z;

        // Copy to clipboard and notify player
        try {
            Process process = Runtime.getRuntime().exec("cmd.exe /c echo " + coordString + " | clip.exe");
            process.waitFor();
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.copied", coordString));
        } catch (Exception e) {
            // Handle clipboard copy errors
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.failed", errorMsg));
        }
    }
}
