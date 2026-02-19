package com.example.copycoords;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;

// Handles keybinding registration and execution for copying coordinates via keyboard shortcut
public class CopyCoordsBind {
    private static KeyMapping copyKeyBinding;
    private static KeyMapping copyConvertedKeyBinding;
    private static KeyMapping copyWithDimensionKeyBinding;
    
    // Single category key for all keybinds
    private static final String CATEGORY_KEY = "key.category.copycoords.keybinds";
    
    // Detect if KeyMapping.Category exists (1.21.9+)
    private static final boolean HAS_KEY_MAPPING_CATEGORY = detectKeyMappingCategory();
    
    // Cached category object (for versions that use Category) - created once and reused
    private static Object cachedCategory = null;
    
    private static boolean detectKeyMappingCategory() {
        try {
            Class.forName("net.minecraft.client.KeyMapping$Category");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("null")
    public static void register() {
        copyKeyBinding = KeyBindingHelper.registerKeyBinding(
            createKeyMapping("key.copycoords.copy", GLFW.GLFW_KEY_C));
        
        copyConvertedKeyBinding = KeyBindingHelper.registerKeyBinding(
            createKeyMapping("key.copycoords.copy_converted", GLFW.GLFW_KEY_V));
        
        copyWithDimensionKeyBinding = KeyBindingHelper.registerKeyBinding(
            createKeyMapping("key.copycoords.copy_with_dimension", GLFW.GLFW_KEY_B));
        
        // Listen for key presses each client tick
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
     * Get or create the shared category for all CopyCoords keybinds.
     * For versions with KeyMapping.Category (1.21.9+): Creates a Category object once and caches it.
     * This ensures all keybinds share the same category instance, so they appear in one UI group.
     */
    private static Object getOrCreateCategory() {
        if (cachedCategory != null) {
            return cachedCategory;
        }
        
        try {
            Class<?> categoryClass = Class.forName("net.minecraft.client.KeyMapping$Category");
            
            // Inspect what constructor Category has
            java.lang.reflect.Constructor<?>[] constructors = categoryClass.getConstructors();
            if (constructors.length == 0) {
                throw new RuntimeException("No public constructors found for KeyMapping.Category");
            }
            
            // Get the first constructor and its parameter type
            java.lang.reflect.Constructor<?> categoryConstructor = constructors[0];
            Class<?>[] paramTypes = categoryConstructor.getParameterTypes();
            if (paramTypes.length != 1) {
                throw new RuntimeException("Expected Category constructor to have 1 parameter, found: " + paramTypes.length);
            }
            
            // Get the actual class of the parameter (ResourceLocation)
            Class<?> resourceLocationClass = paramTypes[0];
            
            // Create ResourceLocation instance using the actual runtime class
            Object resourceLocation = null;
            
            // Try fromNamespaceAndPath factory method first (modern Mojang API)
            try {
                resourceLocation = resourceLocationClass
                    .getMethod("fromNamespaceAndPath", String.class, String.class)
                    .invoke(null, "copycoords", "keybinds");
            } catch (NoSuchMethodException e1) {
                // Try parse method
                try {
                    resourceLocation = resourceLocationClass
                        .getMethod("parse", String.class)
                        .invoke(null, "copycoords:keybinds");
                } catch (NoSuchMethodException e2) {
                    // Try two-argument constructor
                    try {
                        resourceLocation = resourceLocationClass
                            .getConstructor(String.class, String.class)
                            .newInstance("copycoords", "keybinds");
                    } catch (NoSuchMethodException e3) {
                        throw new RuntimeException("Could not create ResourceLocation with any known method", e1);
                    }
                }
            }
            
            // Create category with ResourceLocation and cache it
            cachedCategory = categoryConstructor.newInstance(resourceLocation);
            return cachedCategory;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create category: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create a KeyMapping with the appropriate category for this Minecraft version.
     * 1.21.9+: Uses the shared KeyMapping.Category object
     * 1.21.0-1.21.8: Uses String category key
     */
    private static KeyMapping createKeyMapping(String translationKey, int keyCode) {
        if (HAS_KEY_MAPPING_CATEGORY) {
            // 1.21.9+: Use the shared category object
            try {
                Class<?> categoryClass = Class.forName("net.minecraft.client.KeyMapping$Category");
                Object category = getOrCreateCategory();
                
                // Create KeyMapping with the shared category
                java.lang.reflect.Constructor<KeyMapping> ctor = 
                    KeyMapping.class.getConstructor(String.class, InputConstants.Type.class, int.class, categoryClass);
                return ctor.newInstance(translationKey, InputConstants.Type.KEYSYM, keyCode, category);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create KeyMapping with Category: " + e.getMessage(), e);
            }
        } else {
            // 1.21.0-1.21.8: Use String category key constructor
            try {
                java.lang.reflect.Constructor<KeyMapping> ctor = 
                    KeyMapping.class.getConstructor(String.class, InputConstants.Type.class, int.class, String.class);
                return ctor.newInstance(translationKey, InputConstants.Type.KEYSYM, keyCode, CATEGORY_KEY);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create KeyMapping with String category: " + e.getMessage(), e);
            }
        }
    }

    // Execute coordinate copying when keybind is pressed
    @SuppressWarnings("null")
    private static void executeKeybindCopy(Minecraft minecraft) {
        // Ensure player exists before accessing position
        net.minecraft.world.entity.player.Player player = minecraft.player;
        if (player == null) {
            return;
        }

        // Get player's current block coordinates
        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();
        
        String dimensionId = CopyCoords.getDimensionId(player);
        String coordString = CopyCoords.formatCoordinates(x, y, z, dimensionId);

        try {
            // Copy coordinates to clipboard using cross-platform utility
            ClipboardUtils.copyToClipboard(coordString);
            // Notify player of successful copy with clickable component
            net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal("Copied coordinates to clipboard: ");
            net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, x, y, z, dimensionId);
            minecraft.gui.getChat().addMessage(message.append(clickableCoord));
            CopyCoords.addHistoryEntry(x, y, z, dimensionId);
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

    // Execute coordinate copying with dimension conversion when keybind is pressed
    @SuppressWarnings("null")
    private static void executeKeybindCopyConverted(Minecraft minecraft) {
        // Ensure player exists before accessing position
        net.minecraft.world.entity.player.Player player = minecraft.player;
        if (player == null) {
            return;
        }

        // Get player's current block coordinates
        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();
        
        // Determine goal dimension based on current dimension
        String goal;
        if (player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            goal = "nether";
        } else if (player.level().dimension().equals(net.minecraft.world.level.Level.NETHER)) {
            goal = "overworld";
        } else {
            // Cannot convert coordinates in End or other dimensions
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.command.unsupported_dimension"));
            return;
        }

        // Convert coordinates
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
            // Copy converted coordinates to clipboard
            ClipboardUtils.copyToClipboard(coordString);
            // Notify player of successful copy with clickable component
            net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal("Copied converted coordinates to clipboard: ");
            net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, convertedX, y, convertedZ, convertedDimensionId);
            minecraft.gui.getChat().addMessage(message.append(clickableCoord));
            CopyCoords.addHistoryEntry(convertedX, y, convertedZ, convertedDimensionId);
        } catch (Exception e) {
            // Handle clipboard copy errors
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
            }
            minecraft.gui.getChat().addMessage(net.minecraft.network.chat.Component.translatable("message.copycoords.keybind.failed", errorMsg));
        }
    }

    // Execute coordinate copying with dimension name when keybind is pressed
    @SuppressWarnings("null")
    private static void executeKeybindCopyWithDimension(Minecraft minecraft) {
        // Ensure player exists before accessing position
        net.minecraft.world.entity.player.Player player = minecraft.player;
        if (player == null) {
            return;
        }

        // Get player's current block coordinates
        int x = player.blockPosition().getX();
        int y = player.blockPosition().getY();
        int z = player.blockPosition().getZ();
        
        String dimensionId = CopyCoords.getDimensionId(player);
        
        // Format coordinates with dimension name (regardless of config)
        CoordinateFormat format = CoordinateFormat.fromId(CopyCoords.config.coordinateFormat);
        String coordString = format.format(x, y, z) + " (" + CopyCoords.getDimensionNameFromId(dimensionId) + ")";

        try {
            // Copy coordinates with dimension to clipboard
            ClipboardUtils.copyToClipboard(coordString);
            // Notify player of successful copy with clickable component
            net.minecraft.network.chat.MutableComponent message = net.minecraft.network.chat.Component.literal("Copied coordinates with dimension to clipboard: ");
            net.minecraft.network.chat.MutableComponent clickableCoord = ClickableCoordinateComponent.createClickableCoordinate(coordString, x, y, z, dimensionId);
            minecraft.gui.getChat().addMessage(message.append(clickableCoord));
            CopyCoords.addHistoryEntry(x, y, z, dimensionId);
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
