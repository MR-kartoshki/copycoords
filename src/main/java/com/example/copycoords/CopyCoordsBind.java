package com.example.copycoords;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class CopyCoordsBind {
    private static KeyMapping copyKeyBinding;

    public static void register() {
        KeyMapping.Category copyCategory = KeyMapping.Category.register(Identifier.tryParse("copycoords:copycoords"));
        copyKeyBinding = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.copycoords.copy", GLFW.GLFW_KEY_C, copyCategory)
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (copyKeyBinding.consumeClick()) {
                executeKeybindCopy();
            }
        });
    }

    private static void executeKeybindCopy() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        int x = minecraft.player.blockPosition().getX();
        int y = minecraft.player.blockPosition().getY();
        int z = minecraft.player.blockPosition().getZ();
        String coordString = x + " " + y + " " + z;

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
