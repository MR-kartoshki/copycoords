package com.example.copycoords;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

/**
 * Cross‑version keybind helper.
 *
 * The old 1.21.11 implementation hard‑coded a category
 * Identifier `copycoords:copycoords`. the new reflection‑based
 * code attempted to create the same value in one place and
 * `copycoords:keybinds` elsewhere – the result was that the
 * very first keybind created would end up in a different
 * category than the others and the translation key
 * (`key.category.copycoords.keybinds`) never matched.
 *
 * The only real bug in the previous iteration was the
 * inconsistent resource‑location string. the fixed version
 * below always uses `copycoords:keybinds` when constructing
 * the category.
 */
public class CopyCoordsBind {
    private static KeyMapping copyKeyBinding;
    private static KeyMapping copyConvertedKeyBinding;
    private static KeyMapping copyWithDimensionKeyBinding;

    // translation key that appears in options screen
    private static final String CATEGORY_KEY = "key.category.copycoords.keybinds";

    // resource‑location components used when creating the category
    private static final String CATEGORY_NAMESPACE = "copycoords";
    private static final String CATEGORY_PATH = "keybinds";

    // cached category object (may be a KeyMapping.Category instance,
    // depending on runtime)
    private static Object cachedCategory = null;

    public static void register() {
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
                while (copyKeyBinding.wasPressed()) {
                    executeKeybindCopy(client);
                }
            }
            if (copyConvertedKeyBinding != null) {
                while (copyConvertedKeyBinding.wasPressed()) {
                    executeKeybindCopyConverted(client);
                }
            }
            if (copyWithDimensionKeyBinding != null) {
                while (copyWithDimensionKeyBinding.wasPressed()) {
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
    private static Object getOrCreateCategory() {
        if (cachedCategory != null) {
            return cachedCategory;
        }

        try {
            Class<?> keyClass = Class.forName("net.minecraft.client.KeyMapping");
            for (Class<?> nested : keyClass.getDeclaredClasses()) {
                if ("Category".equals(nested.getSimpleName())) {
                    // try the modern factory method first
                    try {
                        Class<?> identifierClass = Class.forName("net.minecraft.util.Identifier");
                        java.lang.reflect.Method createMethod = null;
                        try {
                            createMethod = nested.getMethod("create", identifierClass);
                        } catch (NoSuchMethodException ignored) {
                            try {
                                createMethod = nested.getMethod("of", identifierClass);
                            } catch (NoSuchMethodException ex) {
                                createMethod = null;
                            }
                        }

                        if (createMethod != null) {
                            Object id = identifierClass.getConstructor(String.class, String.class)
                                    .newInstance(CATEGORY_NAMESPACE, CATEGORY_PATH);
                            cachedCategory = createMethod.invoke(null, id);
                            return cachedCategory;
                        }
                    } catch (Throwable ignored) {
                        // fall through to constructor path
                    }

                    // fallback to instantiating the Category via a constructor
                    try {
                        java.lang.reflect.Constructor<?> ctor = nested.getDeclaredConstructors()[0];
                        Class<?>[] paramTypes = ctor.getParameterTypes();
                        if (paramTypes.length == 1) {
                            Class<?> rlType = paramTypes[0];
                            Object rl = null;
                            try {
                                rl = rlType.getMethod("fromNamespaceAndPath", String.class, String.class)
                                        .invoke(null, CATEGORY_NAMESPACE, CATEGORY_PATH);
                            } catch (NoSuchMethodException e1) {
                                try {
                                    rl = rlType.getMethod("parse", String.class)
                                            .invoke(null, CATEGORY_NAMESPACE + ":" + CATEGORY_PATH);
                                } catch (NoSuchMethodException e2) {
                                    try {
                                        rl = rlType.getConstructor(String.class, String.class)
                                                .newInstance(CATEGORY_NAMESPACE, CATEGORY_PATH);
                                    } catch (NoSuchMethodException e3) {
                                        rl = null;
                                    }
                                }
                            }
                            if (rl != null) {
                                cachedCategory = ctor.newInstance(rl);
                                return cachedCategory;
                            }
                        }
                    } catch (Throwable ignored) {
                        // nothing we can do here, continue searching
                    }
                }
            }
        } catch (Throwable ignored) {
            // ignore and try last‑ditch method below
        }

        // last resort: construct the category directly with reflection
        try {
            Class<?> categoryClass = Class.forName("net.minecraft.client.KeyMapping$Category");
            java.lang.reflect.Constructor<?>[] constructors = categoryClass.getConstructors();
            if (constructors.length == 0) {
                throw new RuntimeException("No public constructors found for KeyMapping.Category");
            }

            java.lang.reflect.Constructor<?> categoryConstructor = constructors[0];
            Class<?>[] paramTypes = categoryConstructor.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new RuntimeException("Expected Category constructor to have 1 parameter, found: " + paramTypes.length);
            }

            Class<?> resourceLocationClass = paramTypes[0];
            Object resourceLocation = null;
            try {
                resourceLocation = resourceLocationClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                        .invoke(null, CATEGORY_NAMESPACE, CATEGORY_PATH);
            } catch (NoSuchMethodException e1) {
                try {
                    resourceLocation = resourceLocationClass.getMethod("parse", String.class)
                            .invoke(null, CATEGORY_NAMESPACE + ":" + CATEGORY_PATH);
                } catch (NoSuchMethodException e2) {
                    try {
                        resourceLocation = resourceLocationClass.getConstructor(String.class, String.class)
                                .newInstance(CATEGORY_NAMESPACE, CATEGORY_PATH);
                    } catch (NoSuchMethodException e3) {
                        throw new RuntimeException("Could not create ResourceLocation with any known method", e1);
                    }
                }
            }

            cachedCategory = categoryConstructor.newInstance(resourceLocation);
            return cachedCategory;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create category: " + e.getMessage(), e);
        }
    }

    private static KeyMapping createKeyMapping(String translationKey, int keyCode) {
        // look for a nested Category class each time; the existence of the
        // class determines which constructor signatures we try
        Class<?> categoryClass = null;
        for (Class<?> c : KeyMapping.class.getDeclaredClasses()) {
            if ("Category".equals(c.getSimpleName())) {
                categoryClass = c;
                break;
            }
        }

        if (categoryClass != null) {
            try {
                java.lang.reflect.Constructor<KeyMapping> ctor =
                        KeyMapping.class.getConstructor(String.class, InputConstants.Type.class, int.class, categoryClass);
                Object category = getOrCreateCategory();
                return ctor.newInstance(translationKey, InputConstants.Type.KEYSYM, keyCode, category);
            } catch (Throwable ignored) {}

            try {
                java.lang.reflect.Constructor<KeyMapping> ctor =
                        KeyMapping.class.getConstructor(String.class, int.class, categoryClass);
                Object category = getOrCreateCategory();
                return ctor.newInstance(translationKey, keyCode, category);
            } catch (Throwable ignored) {}
        }

        try {
            java.lang.reflect.Constructor<KeyMapping> ctor =
                    KeyMapping.class.getConstructor(String.class, InputConstants.Type.class, int.class, String.class);
            return ctor.newInstance(translationKey, InputConstants.Type.KEYSYM, keyCode, CATEGORY_KEY);
        } catch (Throwable ignored) {}

        try {
            java.lang.reflect.Constructor<KeyMapping> ctor =
                    KeyMapping.class.getConstructor(String.class, int.class, String.class);
            return ctor.newInstance(translationKey, keyCode, CATEGORY_KEY);
        } catch (Throwable ignored) {}

        try {
            java.lang.reflect.Constructor<KeyMapping> ctor =
                    KeyMapping.class.getConstructor(String.class, InputConstants.Type.class, int.class);
            return ctor.newInstance(translationKey, InputConstants.Type.KEYSYM, keyCode);
        } catch (Throwable ignored) {}

        try {
            java.lang.reflect.Constructor<KeyMapping> ctor =
                    KeyMapping.class.getConstructor(String.class, int.class);
            return ctor.newInstance(translationKey, keyCode);
        } catch (Throwable ignored) {}

        throw new RuntimeException("No suitable KeyMapping constructor found for this runtime");
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
