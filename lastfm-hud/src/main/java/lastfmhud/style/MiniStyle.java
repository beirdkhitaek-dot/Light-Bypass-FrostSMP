package lastfmhud.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "mini": small pill with cover, one "title - artist" line and a thin progress line. Good for small screens. */
public final class MiniStyle implements HudStyle {
    private static final int W = 170, H = 26;

    @Override public String id() { return "mini"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int green = Draw.spotify(f);
        Draw.roundRect(ctx, 0, 0, W, H, 13, Draw.bg(0xF0181818));
        int tx = 11;
        if (f.texture() != null) {
            Draw.cover(ctx, f.texture(), 4, 4, 18);
            tx = 29;
        }
        int maxW = W - tx - 10;
        Draw.text(ctx, font, f.title() + " - " + f.artist(), tx, 5, maxW, 0xFFFFFFFF);
        Draw.roundRect(ctx, tx, 17, maxW, 2, 1, 0x55FFFFFF);
        if (f.durationMs() > 0) {
            long pos = Math.min(f.durationMs(), Math.max(0, f.elapsedMs()));
            Draw.roundRect(ctx, tx, 17, Math.max(2, (int) (maxW * pos / f.durationMs())), 2, 1, green);
        }
    }
}
