package lastfmhud.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "full": big card, large cover, "NOW PLAYING" label, segmented bar, elapsed / -remaining, two lyric lines. */
public final class FullStyle implements HudStyle {
    private static final int W = 204, H = 82, GAP = 2, LYRICS_H = 32;

    @Override public String id() { return "full"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + GAP + LYRICS_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int green = Draw.spotify(f);
        Draw.roundRect(ctx, 0, 0, W, H, 9, Draw.bg(0xF0181818));

        int tx = 12;
        if (f.texture() != null) {
            Draw.cover(ctx, f.texture(), 8, 8, 66);
            tx = 84;
        }
        int maxW = W - tx - 10;
        Draw.text(ctx, font, "NOW PLAYING", tx, 10, maxW, green);
        Draw.text(ctx, font, f.title(), tx, 23, maxW, 0xFFFFFFFF);
        Draw.text(ctx, font, f.artist(), tx, 35, maxW, 0xFFB3B3B3);

        int barY = 56;
        int n = Math.max(1, maxW / 4);
        long pos = f.durationMs() > 0 ? Math.min(f.durationMs(), Math.max(0, f.elapsedMs())) : 0;
        int filled = f.durationMs() > 0 ? (int) (n * pos / f.durationMs()) : 0;
        for (int i = 0; i < n; i++) {
            ctx.fill(tx + i * 4, barY, tx + i * 4 + 2, barY + 4, i < filled ? green : 0x44FFFFFF);
        }
        if (f.durationMs() > 0) {
            String end = Draw.endTime(pos, f.durationMs());
            ctx.drawText(font, Draw.time(pos), tx, 65, 0xFFA7A7A7, false);
            ctx.drawText(font, end, tx + maxW - font.getWidth(end), 65, 0xFFA7A7A7, false);
        }

        if (f.lyrics() != null) {
            int y0 = H + GAP;
            Draw.roundRect(ctx, 0, y0, W, LYRICS_H, 9, Draw.bg(0xF0181818));
            String cur = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            String next = f.lyrics()[1].isEmpty() ? "" : f.lyrics()[2];
            Draw.text(ctx, font, cur, 10, y0 + 6, W - 20, 0xFFFFFFFF);
            Draw.text(ctx, font, next, 10, y0 + 18, W - 20, 0xFF7A7A7A);
        }
    }
}
