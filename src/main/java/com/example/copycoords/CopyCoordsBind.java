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

    // legacy string used by 1.21.0–1.21.8
    private static final String LEGACY_CATEGORY_KEY = "key.categories.copycoords";

    // constant identifier components used for modern category (1.21.9+)
    private static final String CATEGORY_ID_NAMESPACE = "copycoords";
    private static final String CATEGORY_ID_PATH = "keybinds";

    // modern category caching/detection
    private static volatile Object MODERN_CATEGORY;
    private static final boolean IS_MODERN;

    // ensure register() runs once
    private static boolean REGISTERED = false;

    static {
        boolean modern = false;
        try {
            Class.forName("net.minecraft.client.KeyMapping$Category");
            modern = true;
        } catch (ClassNotFoundException ignored) {
            // old releases don't have the nested Category class
        }
        IS_MODERN = modern;
    }

    public static void register() {
        if (REGISTERED) {
            return;
        }
        REGISTERED = true;
        // no logging here; failures below will be printed if they occur

        try {
            KeyMapping km = createKeyMapping("key.copycoords.copy", GLFW.GLFW_KEY_C);
            if (km != null) copyKeyBinding = KeyBindingHelper.registerKeyBinding(km);
        } catch (Throwable t) {
            System.err.println("CopyCoords: failed to register 'copy' keybind: " + t.getMessage());
        }

        try {
            KeyMapping km = createKeyMapping("key.copycoords.copy_converted", GLFW.GLFW_KEY_V);
            if (km != null) copyConvertedKeyBinding = KeyBindingHelper.registerKeyBinding(km);
        } catch (Throwable t) {
            System.err.println("CopyCoords: failed to register 'copy_converted' keybind: " + t.getMessage());
        }

        try {
            KeyMapping km = createKeyMapping("key.copycoords.copy_with_dimension", GLFW.GLFW_KEY_B);
            if (km != null) copyWithDimensionKeyBinding = KeyBindingHelper.registerKeyBinding(km);
        } catch (Throwable t) {
            System.err.println("CopyCoords: failed to register 'copy_with_dimension' keybind: " + t.getMessage());
        }

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
     * Lazily construct (and cache) the shared category object. the
     * only important detail is that the resulting resource location
     * uses `copycoords:keybinds` – this matches the translation key
     * constant above and keeps all of our bindings together.
     */
    private static Object getModernCategory() {
        if (!IS_MODERN) {
            throw new IllegalStateException("modern category requested on legacy runtime");
        }
        if (MODERN_CATEGORY == null) {
            synchronized (CopyCoordsBind.class) {
                if (MODERN_CATEGORY == null) {
                    try {
                        Class<?> identifierClass = Class.forName("net.minecraft.resources.Identifier");
                        java.lang.reflect.Method factory = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class);
                        Object id = factory.invoke(null, CATEGORY_ID_NAMESPACE, CATEGORY_ID_PATH);
                        Class<?> categoryClass = Class.forName("net.minecraft.client.KeyMapping$Category");
                        java.lang.reflect.Method register = categoryClass.getMethod("register", identifierClass);
                        MODERN_CATEGORY = register.invoke(null, id);
                    } catch (Throwable t) {
                        throw new RuntimeException("Failed to create modern category", t);
                    }
                }
            }
        }
        return MODERN_CATEGORY;
    }

    private static KeyMapping createKeyMapping(String translationKey, int keyCode) {
        if (IS_MODERN) {
            Object cat = getModernCategory();
            try {
                java.lang.reflect.Constructor<KeyMapping> ctor = KeyMapping.class.getConstructor(String.class, InputConstants.Type.class, int.class, cat.getClass());
                return ctor.newInstance(translationKey, InputConstants.Type.KEYSYM, keyCode, cat);
            } catch (Throwable t) {
                throw new RuntimeException("Modern runtime lacks expected KeyMapping constructor", t);
            }
        } else {
            try {
                java.lang.reflect.Constructor<KeyMapping> ctor = KeyMapping.class.getConstructor(String.class, InputConstants.Type.class, int.class, String.class);
                return ctor.newInstance(translationKey, InputConstants.Type.KEYSYM, keyCode, LEGACY_CATEGORY_KEY);
            } catch (Throwable ignored) {}
            try {
                java.lang.reflect.Constructor<KeyMapping> ctor = KeyMapping.class.getConstructor(String.class, int.class, String.class);
                return ctor.newInstance(translationKey, keyCode, LEGACY_CATEGORY_KEY);
            } catch (Throwable ignored) {}
            throw new RuntimeException("Legacy runtime lacks expected KeyMapping constructors");
        }
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
            ClipboardUtils.copyToClipboard(coordString);
            net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal("Copied coordinates to clipboard: ");
            net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, x, y, z, dimensionId);
            minecraft.gui.getChat().addMessage(message.append(clickableCoord));
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
            ClipboardUtils.copyToClipboard(coordString);
            net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal("Copied converted coordinates to clipboard: ");
            net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, convertedX, y, convertedZ, convertedDimensionId);
            minecraft.gui.getChat().addMessage(message.append(clickableCoord));
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
            ClipboardUtils.copyToClipboard(coordString);
            net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal("Copied coordinates with dimension to clipboard: ");
            net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, x, y, z, dimensionId);
            minecraft.gui.getChat().addMessage(message.append(clickableCoord));
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
