package lastfmhud.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Style 1: rounded glass card with cover art, accent progress bar and an optional lyrics panel. */
public final class GlassStyle implements HudStyle {
    private static final int W = 168, CARD_H = 56, LYRICS_GAP = 3, LYRICS_H = 40;

    @Override public String id() { return "glass"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? CARD_H + LYRICS_GAP + LYRICS_H : CARD_H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int accent = f.accent();
        int glow = accent & 0x00FFFFFF;

        // glass card: soft accent-tinted rim, dark translucent body, faint top highlight
        Draw.roundRect(ctx, 0, 0, W, CARD_H, 7, 0x40000000 | glow);
        Draw.roundRect(ctx, 1, 1, W - 2, CARD_H - 2, 6, 0xC4121216);
        Draw.roundRect(ctx, 2, 2, W - 4, 10, 5, 0x10FFFFFF);

        int tx = 10;
        if (f.texture() != null) {
            Draw.roundRect(ctx, 7, 7, 42, 42, 4, 0xFF000000 | glow);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, f.texture(), 8, 8, 0.0F, 0.0F, 40, 40, 64, 64, 64, 64, 0xFFFFFFFF);
            tx = 56;
        }
        int maxW = W - tx - 8;
        ctx.drawText(font, Text.literal(font.trimToWidth(f.title(), maxW)), tx, 9, 0xFFFFFFFF, false);
        ctx.drawText(font, Text.literal(font.trimToWidth(f.artist(), maxW)), tx, 21, 0xFFB5B5BD, false);

        int barW = W - tx - 8;
        Draw.roundRect(ctx, tx, 36, barW, 3, 1, 0x40FFFFFF);
        if (f.durationMs() > 0) {
            long pos = Math.min(f.durationMs(), Math.max(0, f.elapsedMs()));
            int fill = Math.max(2, (int) (barW * pos / f.durationMs()));
            Draw.roundRect(ctx, tx, 36, fill, 3, 1, accent);
            String total = Draw.time(f.durationMs());
            ctx.drawText(font, Text.literal(Draw.time(pos)), tx, 43, 0xFF9A9AA3, false);
            ctx.drawText(font, Text.literal(total), tx + barW - font.getWidth(total), 43, 0xFF9A9AA3, false);
        } else {
            Draw.roundRect(ctx, tx, 36, Math.min(barW, 26), 3, 1, accent); // unknown length: short accent stub
        }

        if (f.lyrics() != null) drawLyrics(ctx, font, f, glow);
    }

    private void drawLyrics(DrawContext ctx, TextRenderer font, Frame f, int glow) {
        int y0 = CARD_H + LYRICS_GAP;
        Draw.roundRect(ctx, 0, y0, W, LYRICS_H, 7, 0x40000000 | glow);
        Draw.roundRect(ctx, 1, y0 + 1, W - 2, LYRICS_H - 2, 6, 0xC4121216);
        int maxW = W - 16;
        String[] l = f.lyrics();
        ctx.drawText(font, Text.literal(font.trimToWidth(l[0], maxW)), 8, y0 + 6, 0xFF7E7E88, false);
        ctx.drawText(font, Text.literal(font.trimToWidth(l[1], maxW)), 8, y0 + 16, f.accent(), false);
        ctx.drawText(font, Text.literal(font.trimToWidth(l[2], maxW)), 8, y0 + 27, 0xFF7E7E88, false);
    }
}
