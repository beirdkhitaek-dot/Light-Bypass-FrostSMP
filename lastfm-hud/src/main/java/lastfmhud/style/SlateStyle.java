package lastfmhud.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Style "slate": dark slate panel with a "NOW PLAYING" label, title, artist, a times row with a thin progress line, and
 * the lyrics (current line in the accent colour, next line dimmed) inside the same panel.
 */
public final class SlateStyle implements HudStyle {
    private static final int W = 176, TOP_H = 54, LYRICS_H = 28;

    @Override public String id() { return "slate"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? TOP_H + LYRICS_H : TOP_H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int accent = f.accent();
        int h = height(f.lyrics() != null);

        Draw.roundRect(ctx, 0, 0, W, h, 4, 0xD0202024);

        int tx = 8;
        if (f.texture() != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, f.texture(), 6, 6, 0.0F, 0.0F, 36, 36, 64, 64, 64, 64, 0xFFFFFFFF);
            tx = 48;
        }
        int maxW = W - tx - 8;
        ctx.drawText(font, Text.literal("NOW PLAYING"), tx, 5, 0xFF7E7E88, false);
        ctx.drawText(font, Text.literal(font.trimToWidth(f.title(), maxW)), tx, 15, 0xFFFFFFFF, false);
        ctx.drawText(font, Text.literal(font.trimToWidth(f.artist(), maxW)), tx, 25, 0xFFA0A0AA, false);

        int rowY = 36;
        String left = Draw.time(f.elapsedMs());
        String right = f.durationMs() > 0 ? "-" + Draw.time(Math.max(0, f.durationMs() - f.elapsedMs())) : "--:--";
        ctx.drawText(font, Text.literal(left), tx, rowY, 0xFF9A9AA3, false);
        ctx.drawText(font, Text.literal(right), W - 8 - font.getWidth(right), rowY, 0xFF9A9AA3, false);

        int barY = 47, barW = W - tx - 8;
        ctx.fill(tx, barY, tx + barW, barY + 2, 0x40FFFFFF);
        if (f.durationMs() > 0) {
            long pos = Math.min(f.durationMs(), Math.max(0, f.elapsedMs()));
            ctx.fill(tx, barY, tx + Math.max(1, (int) (barW * pos / f.durationMs())), barY + 2, accent);
        }

        if (f.lyrics() != null) {
            String[] l = f.lyrics();
            String cur = l[1].isEmpty() ? l[2] : l[1];
            String next = l[1].isEmpty() ? "" : l[2];
            int y0 = TOP_H + 2;
            ctx.fill(8, y0 - 2, W - 8, y0 - 1, 0x20FFFFFF);
            ctx.drawText(font, Text.literal(font.trimToWidth(cur, W - 16)), 8, y0 + 2, accent, false);
            ctx.drawText(font, Text.literal(font.trimToWidth(next, W - 16)), 8, y0 + 14, 0xFF6E6E78, false);
        }
    }
}
