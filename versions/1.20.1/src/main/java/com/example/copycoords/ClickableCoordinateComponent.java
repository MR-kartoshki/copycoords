package com.example.copycoords;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent;

public class ClickableCoordinateComponent {

    public static MutableComponent createClickableCoordinate(String coordString, int x, int y, int z, String dimensionId) {
        MutableComponent coord = Component.literal(coordString);

        ClickEvent click = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coordString);
        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy"));

        Style style = Style.EMPTY
                .withUnderlined(true)
                .withClickEvent(click)
                .withHoverEvent(hover);

        return coord.withStyle(style);
    }

    public static MutableComponent createClickableCoordinateMessage(String prefix, String coordString, int x, int y, int z, String dimensionId) {
        MutableComponent message = Component.literal(prefix);
        MutableComponent clickable = createClickableCoordinate(coordString, x, y, z, dimensionId);
        return message.append(clickable);
    }
}