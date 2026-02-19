package com.example.copycoords;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

// Utility class to create clickable coordinate components in chat messages
public class ClickableCoordinateComponent {
    
    /**
     * Creates a clickable coordinate component
     * @param coordString The coordinate string to display
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param dimensionId The dimension ID
     * @return A Component with click action to re-copy coordinates
     */
    public static MutableComponent createClickableCoordinate(String coordString, int x, int y, int z, String dimensionId) {
        MutableComponent coord = Component.literal(coordString);
        
        // Create click and hover events using reflection (they're abstract classes)
        ClickEvent clickEvent = buildClickEvent(coordString);
        HoverEvent hoverEvent = buildHoverEvent(Component.literal("Click to copy coordinates"));
        
        // Apply both events to the style
        Style style = Style.EMPTY;
        if (clickEvent != null) {
            style = style.withClickEvent(clickEvent);
        }
        if (hoverEvent != null) {
            style = style.withHoverEvent(hoverEvent);
        }
        
        return coord.withStyle(style);
    }

    /**
     * Creates a COPY_TO_CLIPBOARD click event using reflection
     */
    private static ClickEvent buildClickEvent(String coordString) {
        // Try factory method first
        try {
            Method copyToClipboard = ClickEvent.class.getDeclaredMethod("copyToClipboard", String.class);
            return (ClickEvent) copyToClipboard.invoke(null, coordString);
        } catch (Exception ignored) {
        }

        // Try CopyToClipboard inner class
        try {
            Class<?> copyToClipboardClass = Class.forName("net.minecraft.network.chat.ClickEvent$CopyToClipboard");
            return (ClickEvent) copyToClipboardClass.getConstructor(String.class).newInstance(coordString);
        } catch (Exception ignored) {
        }

        // Try direct constructor as fallback
        try {
            Constructor<ClickEvent> ctor = ClickEvent.class.getConstructor(ClickEvent.Action.class, String.class);
            return ctor.newInstance(ClickEvent.Action.COPY_TO_CLIPBOARD, coordString);
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Creates a SHOW_TEXT hover event using reflection
     */
    private static HoverEvent buildHoverEvent(Component text) {
        // Try factory method first
        try {
            Method showText = HoverEvent.class.getDeclaredMethod("showText", Component.class);
            return (HoverEvent) showText.invoke(null, text);
        } catch (Exception ignored) {
        }

        // Try ShowText inner class
        try {
            Class<?> showTextClass = Class.forName("net.minecraft.network.chat.HoverEvent$ShowText");
            return (HoverEvent) showTextClass.getConstructor(Component.class).newInstance(text);
        } catch (Exception ignored) {
        }

        // Try direct constructor as fallback
        try {
            Constructor<HoverEvent> ctor = HoverEvent.class.getConstructor(HoverEvent.Action.class, Component.class);
            return ctor.newInstance(HoverEvent.Action.SHOW_TEXT, text);
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Creates a message with clickable coordinates
     * @param prefix The prefix text (e.g., "Your current coordinates are: ")
     * @param coordString The coordinate string
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param dimensionId The dimension ID
     * @return A Component combining prefix and clickable coordinates
     */
    public static MutableComponent createClickableCoordinateMessage(String prefix, String coordString, int x, int y, int z, String dimensionId) {
        MutableComponent message = Component.literal(prefix);
        MutableComponent clickable = createClickableCoordinate(coordString, x, y, z, dimensionId);
        return message.append(clickable);
    }
}
