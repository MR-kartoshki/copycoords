package com.example.copycoords;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent;

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

        // build click/hover events and style without reflection
        ClickEvent click = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coordString);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy"));

        Style style = Style.EMPTY
                .withUnderlined(true)
                .withClickEvent(click)
                .withHoverEvent(hover);

        return coord.withStyle(style);
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