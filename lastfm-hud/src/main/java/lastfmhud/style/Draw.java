package lastfmhud.style;

import lastfmhud.Config;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Small drawing helpers shared by the styles. */
public final class Draw {
    public static final int SPOTIFY_GREEN = 0xFF1DB954;

    private Draw() {}

    /** Filled rounded rectangle built from horizontal strips. */
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
        if (h - 2 * r > 0) ctx.fill(x, y + r, x + w, y + h - r, color);
    }

    public static String time(long ms) {
        long s = Math.max(0, ms / 1000);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    /** Right-hand time label: "-remaining" or total length, depending on the setting. */
    public static String endTime(long elapsed, long duration) {
        if (Config.get().remainingTime) return "-" + time(Math.max(0, duration - elapsed));
        return time(duration);
    }

    /** Applies the background-opacity setting to a translucent card colour. */
    public static int bg(int argb) {
        float o = Config.get().opacity / 100f;
        int a = Math.round(((argb >>> 24) & 255) * o);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Spotify green, or the user's own colour when "My color" is on. */
    public static int spotify(Frame f) {
        return Config.get().customAccent ? f.accent() : SPOTIFY_GREEN;
    }

    /** Text cut to the given width with an ellipsis, so it never leaves its box. */
    public static String fit(TextRenderer font, String s, int maxW) {
        if (s == null) return "";
        if (font.getWidth(s) <= maxW) return s;
        int dots = font.getWidth("...");
        if (maxW <= dots) return "";
        return font.trimToWidth(s, maxW - dots) + "...";
    }

    /** Text inside a box: scrolls back and forth when too long (if enabled), otherwise ends with "...". */
    public static void text(DrawContext ctx, TextRenderer font, String s, int x, int y, int maxW, int color, boolean shadow) {
        if (s == null || s.isEmpty() || maxW <= 0) return;
        int w = font.getWidth(s);
        if (w <= maxW) {
            ctx.drawText(font, s, x, y, color, shadow);
            return;
        }
        if (!Config.get().scrollText) {
            ctx.drawText(font, fit(font, s, maxW), x, y, color, shadow);
            return;
        }
        int over = w - maxW;
        long travel = over * 30L;
        long cycle = travel * 2 + 3000;
        long p = System.currentTimeMillis() % cycle;
        long off;
        if (p < 1500) off = 0;
        else if (p < 1500 + travel) off = (p - 1500) / 30;
        else if (p < 1500 + travel + 1500) off = over;
        else off = over - (p - 3000 - travel) / 30;
        off = Math.max(0, Math.min(over, off));
        ctx.enableScissor(x, y - 1, x + maxW, y + 10);
        ctx.drawText(font, s, x - (int) off, y, color, shadow);
        ctx.disableScissor();
    }

    public static void text(DrawContext ctx, TextRenderer font, String s, int x, int y, int maxW, int color) {
        text(ctx, font, s, x, y, maxW, color, false);
    }

    /** Cover art in the chosen shape (square / rounded / circle). */
    public static void cover(DrawContext ctx, Identifier tex, int x, int y, int size) {
        String shape = Config.get().coverShape;
        int r = "circle".equals(shape) ? size / 2 : "rounded".equals(shape) ? Math.max(2, size / 8) : 0;
        if (r == 0) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0F, 0.0F, size, size, 64, 64, 64, 64, 0xFFFFFFFF);
            return;
        }
        int rh = Math.max(1, Math.round(64f / size));
        for (int i = 0; i < size; i++) {
            double dy = i < r ? r - i - 0.5 : i >= size - r ? i - (size - r) + 0.5 : -1;
            int inset = dy >= 0 ? r - (int) Math.round(Math.sqrt(Math.max(0, (double) r * r - dy * dy))) : 0;
            int w = size - 2 * inset;
            if (w <= 0) continue;
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x + inset, y + i, inset * 64f / size, i * 64f / size,
                w, 1, Math.max(1, Math.round(w * 64f / size)), rh, 64, 64, 0xFFFFFFFF);
        }
    }
}
