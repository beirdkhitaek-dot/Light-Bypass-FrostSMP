package lastfmhud.style;

import lastfmhud.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Style "spotify": Spotify-like now-playing card. Charcoal rounded card, cover on the left, white title, grey artist,
 * animated equaliser bars, slim progress bar with times and (when lyrics exist) a lyrics strip underneath.
 * Spotify green by default; the "My color" setting overrides it. No logo is drawn.
 */
public final class SpotifyStyle implements HudStyle {
    private static final int W = 180, H = 58, GAP = 2, LYRICS_H = 22;
    private static final int GREEN = 0xFF1DB954;

    @Override public String id() { return "spotify"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + GAP + LYRICS_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int green = Config.get().customAccent ? f.accent() : GREEN;

        Draw.roundRect(ctx, 0, 0, W, H, 8, 0xF0181818);

        int tx = 10;
        if (f.texture() != null) {
            Draw.roundRect(ctx, 7, 7, 44, 44, 4, 0xFF000000);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, f.texture(), 7, 7, 0.0F, 0.0F, 44, 44, 64, 64, 64, 64, 0xFFFFFFFF);
            tx = 58;
        }
        int maxW = W - tx - 24;
        ctx.drawText(font, Text.literal(font.trimToWidth(f.title(), maxW)), tx, 9, 0xFFFFFFFF, false);
        ctx.drawText(font, Text.literal(font.trimToWidth(f.artist(), maxW)), tx, 20, 0xFFB3B3B3, false);

        // animated equaliser (top right)
        long t = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            int h = 3 + (int) Math.round(4 * (0.5 + 0.5 * Math.sin(t / 170.0 + i * 1.9)));
            int bx = W - 17 + i * 4;
            ctx.fill(bx, 15 - h, bx + 2, 15, green);
        }

        // progress
        int barX = tx, barW = W - tx - 8, barY = 38;
        Draw.roundRect(ctx, barX, barY, barW, 3, 1, 0x55FFFFFF);
        if (f.durationMs() > 0) {
            long pos = Math.min(f.durationMs(), Math.max(0, f.elapsedMs()));
            int fill = Math.max(2, (int) (barW * pos / f.durationMs()));
            Draw.roundRect(ctx, barX, barY, fill, 3, 1, green);
            Draw.roundRect(ctx, barX + fill - 2, barY - 1, 5, 5, 2, 0xFFFFFFFF);
            String total = Draw.time(f.durationMs());
            ctx.drawText(font, Text.literal(Draw.time(pos)), barX, 44, 0xFFA7A7A7, false);
            ctx.drawText(font, Text.literal(total), barX + barW - font.getWidth(total), 44, 0xFFA7A7A7, false);
        }

        if (f.lyrics() != null) {
            int y0 = H + GAP;
            Draw.roundRect(ctx, 0, y0, W, LYRICS_H, 8, 0xF0181818);
            String line = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            ctx.drawText(font, Text.literal(font.trimToWidth(line, W - 16)), 8, y0 + 7, 0xFFFFFFFF, false);
            ctx.fill(8, y0 + LYRICS_H - 4, 8 + 14, y0 + LYRICS_H - 3, green);
        }
    }
}
