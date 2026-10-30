package lastfmhud.style;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Style "vinyl": round record (cover as the label) with a centre hole and an orbiting shine, plus title and artist. */
public final class VinylStyle implements HudStyle {
    private static final int W = 170, H = 64, GAP = 2, LYRICS_H = 22;

    @Override public String id() { return "vinyl"; }

    @Override public int width() { return W; }

    @Override public int height(boolean withLyrics) { return withLyrics ? H + GAP + LYRICS_H : H; }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, Frame f) {
        TextRenderer font = mc.textRenderer;
        int green = Draw.spotify(f);
        Draw.roundRect(ctx, 0, 0, W, H, 10, Draw.bg(0xF0181818));

        int cx = 32, cy = 32;
        Draw.roundRect(ctx, cx - 28, cy - 28, 56, 56, 28, 0xFF0C0C0C); // record
        Draw.roundRect(ctx, cx - 23, cy - 23, 46, 46, 23, 0xFF151515); // groove ring
        if (f.texture() != null) {
            String old = lastfmhud.Config.get().coverShape;
            lastfmhud.Config.get().coverShape = "circle"; // the label of a record is always round
            Draw.cover(ctx, f.texture(), cx - 17, cy - 17, 34);
            lastfmhud.Config.get().coverShape = old;
        } else {
            Draw.roundRect(ctx, cx - 17, cy - 17, 34, 34, 17, green);
        }
        Draw.roundRect(ctx, cx - 3, cy - 3, 6, 6, 3, 0xFF101010); // spindle hole
        double a = System.currentTimeMillis() / 700.0;
        int sx = cx + (int) Math.round(Math.cos(a) * 25), sy = cy + (int) Math.round(Math.sin(a) * 25);
        ctx.fill(sx - 1, sy - 1, sx + 1, sy + 1, 0x66FFFFFF);

        int tx = 66, maxW = W - tx - 8;
        Draw.text(ctx, font, f.title(), tx, 10, maxW, 0xFFFFFFFF);
        Draw.text(ctx, font, f.artist(), tx, 22, maxW, 0xFFB3B3B3);
        Draw.roundRect(ctx, tx, 41, maxW, 3, 1, 0x55FFFFFF);
        if (f.durationMs() > 0) {
            long pos = Math.min(f.durationMs(), Math.max(0, f.elapsedMs()));
            Draw.roundRect(ctx, tx, 41, Math.max(2, (int) (maxW * pos / f.durationMs())), 3, 1, green);
            String end = Draw.endTime(pos, f.durationMs());
            ctx.drawText(font, Draw.time(pos), tx, 48, 0xFFA7A7A7, false);
            ctx.drawText(font, end, tx + maxW - font.getWidth(end), 48, 0xFFA7A7A7, false);
        }

        if (f.lyrics() != null) {
            int y0 = H + GAP;
            Draw.roundRect(ctx, 0, y0, W, LYRICS_H, 10, Draw.bg(0xF0181818));
            String line = f.lyrics()[1].isEmpty() ? f.lyrics()[2] : f.lyrics()[1];
            Draw.text(ctx, font, line, 8, y0 + 7, W - 16, 0xFFFFFFFF);
        }
    }
}
