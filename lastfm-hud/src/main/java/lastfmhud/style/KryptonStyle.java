package lastfmhud.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Style "krypton": wide dark bar, cover on the left, "now playing" label, title + artist, round badge top right and a
 * segmented progress bar with elapsed / remaining time. A one-line lyric strip hangs under it when lyrics exist.
 */
public final class KryptonStyle implements HudStyle {
    private static final int W = 184, H = 56, STRIP_GAP = 2, STRIP_H = 16;

    @Override public String id() { return "krypton"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + STRIP_GAP + STRIP_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int accent = f.accent();

        Draw.roundRect(ctx, 0, 0, W, H, 8, 0xCC101014);

        int tx = 10;
        if (f.texture() != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, f.texture(), 7, 7, 0.0F, 0.0F, 32, 32, 64, 64, 64, 64, 0xFFFFFFFF);
            tx = 46;
        }
        int maxW = W - tx - 24;
        ctx.drawText(font, Text.literal("now playing"), tx, 6, 0xFF8C8C96, false);
        ctx.drawText(font, Text.literal(font.trimToWidth(f.title(), maxW)), tx, 16, 0xFFFFFFFF, false);
        ctx.drawText(font, Text.literal(font.trimToWidth(f.artist(), maxW)), tx, 26, 0xFFA4A4AE, false);

        badge(ctx, W - 19, 6, accent);

        // bottom row: elapsed | segmented bar | remaining
        String left = Draw.time(f.elapsedMs());
        String right = f.durationMs() > 0 ? "-" + Draw.time(Math.max(0, f.durationMs() - f.elapsedMs())) : "--:--";
        int rowY = 43;
        ctx.drawText(font, Text.literal(left), 7, rowY, 0xFFFFFFFF, false);
        ctx.drawText(font, Text.literal(right), W - 7 - font.getWidth(right), rowY, 0xFFFFFFFF, false);
        int barX = 7 + font.getWidth(left) + 6;
        int barEnd = W - 7 - font.getWidth(right) - 6;
        int pitch = 3;
        int n = Math.max(1, (barEnd - barX) / pitch);
        double progress = f.durationMs() > 0 ? Math.min(1.0, Math.max(0.0, f.elapsedMs() / (double) f.durationMs())) : 0.0;
        int filled = (int) Math.round(n * progress);
        int mid = rowY + 4;
        for (int i = 0; i < n; i++) {
            int x = barX + i * pitch;
            if (i < filled) {
                int h = i < 8 ? 2 + i / 2 : 6; // short bars at the start, full height after
                ctx.fill(x, mid - h / 2, x + 2, mid - h / 2 + h, accent);
            } else {
                ctx.fill(x, mid - 1, x + 2, mid + 1, 0x55FFFFFF);
            }
        }

        if (f.lyrics() != null) {
            int y0 = H + STRIP_GAP;
            Draw.roundRect(ctx, 0, y0, W, STRIP_H, 6, 0xCC101014);
            String line = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            ctx.drawText(font, Text.literal(font.trimToWidth(line, W - 16)), 8, y0 + 4, accent, false);
        }
    }

    /** Round badge with three equaliser bars. */
    private static void badge(DrawContext ctx, int x, int y, int accent) {
        Draw.roundRect(ctx, x, y, 11, 11, 5, accent);
        ctx.fill(x + 3, y + 5, x + 4, y + 8, 0xFF101014);
        ctx.fill(x + 5, y + 3, x + 6, y + 8, 0xFF101014);
        ctx.fill(x + 7, y + 4, x + 8, y + 8, 0xFF101014);
    }
}
