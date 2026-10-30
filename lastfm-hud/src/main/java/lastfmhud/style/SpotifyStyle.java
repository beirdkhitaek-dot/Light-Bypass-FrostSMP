package lastfmhud.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "spotify": charcoal card, cover left, white title, grey artist, animated bars, slim progress bar. No logo. */
public final class SpotifyStyle implements HudStyle {
    private static final int W = 180, H = 58, GAP = 2, LYRICS_H = 22;

    @Override public String id() { return "spotify"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + GAP + LYRICS_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int green = Draw.spotify(f);

        Draw.roundRect(ctx, 0, 0, W, H, 8, Draw.bg(0xF0181818));

        int tx = 10;
        if (f.texture() != null) {
            Draw.cover(ctx, f.texture(), 7, 7, 44);
            tx = 58;
        }
        int maxW = W - tx - 24;
        Draw.text(ctx, font, f.title(), tx, 9, maxW, 0xFFFFFFFF);
        Draw.text(ctx, font, f.artist(), tx, 20, maxW, 0xFFB3B3B3);

        long t = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            int h = 3 + (int) Math.round(4 * (0.5 + 0.5 * Math.sin(t / 170.0 + i * 1.9)));
            int bx = W - 17 + i * 4;
            ctx.fill(bx, 15 - h, bx + 2, 15, green);
        }

        int barX = tx, barW = W - tx - 8, barY = 38;
        Draw.roundRect(ctx, barX, barY, barW, 3, 1, 0x55FFFFFF);
        if (f.durationMs() > 0) {
            long pos = Math.min(f.durationMs(), Math.max(0, f.elapsedMs()));
            int fill = Math.max(2, (int) (barW * pos / f.durationMs()));
            Draw.roundRect(ctx, barX, barY, fill, 3, 1, green);
            Draw.roundRect(ctx, barX + fill - 2, barY - 1, 5, 5, 2, 0xFFFFFFFF);
            String end = Draw.endTime(pos, f.durationMs());
            ctx.drawText(font, Draw.time(pos), barX, 44, 0xFFA7A7A7, false);
            ctx.drawText(font, end, barX + barW - font.getWidth(end), 44, 0xFFA7A7A7, false);
        }

        if (f.lyrics() != null) {
            int y0 = H + GAP;
            Draw.roundRect(ctx, 0, y0, W, LYRICS_H, 8, Draw.bg(0xF0181818));
            String line = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            Draw.text(ctx, font, line, 8, y0 + 7, W - 16, 0xFFFFFFFF);
            ctx.fill(8, y0 + LYRICS_H - 4, 8 + 14, y0 + LYRICS_H - 3, green);
        }
    }
}
