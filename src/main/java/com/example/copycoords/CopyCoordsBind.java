package com.example.copycoords;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Registers and handles keybindings for the CopyCoords mod.
 * Allows players to copy coordinates with a hotkey (default: C).
 */
public class CopyCoordsBind {
    private static KeyMapping copyKeyBinding;

    public static void register() {
        // Create a KeyMapping with a default key of C and null category (uses default)
        copyKeyBinding = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.copycoords.copy", GLFW.GLFW_KEY_C, null)
        );

        // Listen for key presses on the client tick.
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (copyKeyBinding.consumeClick()) {
                executeKeybindCopy();
            }
        });
    }

    /**
     * Executes the copy coordinates action when the keybind is pressed.
     */
    private static void executeKeybindCopy() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        int x = minecraft.player.blockPosition().getX();
        int y = minecraft.player.blockPosition().getY();
        int z = minecraft.player.blockPosition().getZ();
        String coordString = x + " " + y + " " + z;

        // Copy to clipboard
        try {
            Process process = Runtime.getRuntime().exec("cmd.exe /c echo " + coordString + " | clip.exe");
            process.waitFor();
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.copied", coordString));
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.failed", errorMsg));
        }
    }
}
