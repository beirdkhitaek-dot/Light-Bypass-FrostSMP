package lastfmhud.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "compact": just "♪ title - artist" with a shadow, no background. The lightest option. */
public final class CompactStyle implements HudStyle {
    private static final int W = 200, H = 11, LYRICS_H = 11;

    @Override public String id() { return "compact"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + LYRICS_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        Draw.text(ctx, font, "♪ " + f.title() + " - " + f.artist(), 0, 1, W, 0xFFFFFFFF, true);
        if (f.lyrics() != null) {
            String line = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            Draw.text(ctx, font, line, 0, H + 1, W, Draw.spotify(f), true);
        }
    }
}
