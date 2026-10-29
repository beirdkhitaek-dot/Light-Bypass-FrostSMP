package lastfmhud.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** A selectable look for the HUD. Drawn at (0,0) in GUI units; the renderer handles position and scale. */
public interface HudStyle {
    /** Command / config id, lowercase (for example "glass"). */
    String id();

    int width();

    int height(boolean withLyrics);

    void draw(DrawContext ctx, MinecraftClient mc, Frame frame);
}
