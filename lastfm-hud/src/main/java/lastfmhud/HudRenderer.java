package lastfmhud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class HudRenderer {
    private static final int DEFAULT_ACCENT = 0xFFD51007;
    private static Identifier texture;
    private static int shownSerial = -1;
    private static int counter;
    private static int accent = DEFAULT_ACCENT;

    private HudRenderer() {}

    private static final class ArtTexture extends NativeImageBackedTexture {
        ArtTexture(NativeImage image) {
            super(() -> "LastFmHud art", image);
            this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        }
    }

    /** Average colour of the cover, pushed towards a vivid tone so it reads well on the dark card. */
    private static int accentFrom(int[] px) {
        long r = 0, g = 0, b = 0;
        for (int p : px) {
            r += (p >> 16) & 0xFF;
            g += (p >> 8) & 0xFF;
            b += p & 0xFF;
        }
        int n = px.length;
        float[] c = {r / (float) n, g / (float) n, b / (float) n};
        float max = Math.max(c[0], Math.max(c[1], c[2]));
        if (max < 1) return DEFAULT_ACCENT;
        float lift = Math.min(2.2f, 230f / max); // brighten dull covers
        float avg = (c[0] + c[1] + c[2]) / 3f;
        int out = 0xFF000000;
        for (int i = 0; i < 3; i++) {
            float v = avg + (c[i] - avg) * 1.4f; // add saturation
            int ch = Math.max(40, Math.min(255, Math.round(v * lift)));
            out |= ch << (16 - 8 * i);
        }
        return out;
    }

    private static void syncTexture(MinecraftClient mc) {
        int serial = LastFm.artSerial();
        if (serial == shownSerial) return;
        shownSerial = serial;
        if (texture != null) {
            mc.getTextureManager().destroyTexture(texture);
            texture = null;
        }
        accent = DEFAULT_ACCENT;
        int[] px = LastFm.art();
        if (px == null || px.length != 64 * 64) return;
        try {
            NativeImage image = new NativeImage(64, 64, false);
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) image.setColorArgb(x, y, px[y * 64 + x]);
            }
            Identifier id = Identifier.of("lastfmhud", "art/" + (counter++));
            mc.getTextureManager().registerTexture(id, new ArtTexture(image));
            texture = id;
            accent = accentFrom(px);
        } catch (Throwable ignored) {
            texture = null;
        }
    }

    /** Filled rounded rectangle built from horizontal strips. */
    private static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
        if (h - 2 * r > 0) ctx.fill(x, y + r, x + w, y + h - r, color);
    }

    private static String time(long ms) {
        long s = Math.max(0, ms / 1000);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    public static void render(DrawContext ctx) {
        Config c = Config.get();
        if (!c.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.textRenderer == null) return;
        LastFm.Now n = LastFm.now();
        if (n == null) return;
        syncTexture(mc);

        int w = 168, h = 56;
        int glow = (accent & 0x00FFFFFF);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) c.x, (float) c.y);
        m.scale(c.scale, c.scale);

        // glass card: soft accent-tinted rim, dark translucent body, faint top highlight
        roundRect(ctx, 0, 0, w, h, 7, 0x40000000 | glow);
        roundRect(ctx, 1, 1, w - 2, h - 2, 6, 0xC4121216);
        roundRect(ctx, 2, 2, w - 4, 10, 5, 0x10FFFFFF);

        int tx = 10;
        if (texture != null) {
            roundRect(ctx, 7, 7, 42, 42, 4, 0xFF000000 | glow);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 8, 8, 0.0F, 0.0F, 40, 40, 64, 64, 64, 64, 0xFFFFFFFF);
            tx = 56;
        }
        TextRenderer f = mc.textRenderer;
        int maxW = w - tx - 8;
        ctx.drawText(f, Text.literal(f.trimToWidth(n.title(), maxW)), tx, 9, 0xFFFFFFFF, false);
        ctx.drawText(f, Text.literal(f.trimToWidth(n.artist(), maxW)), tx, 21, 0xFFB5B5BD, false);

        int barW = w - tx - 8;
        roundRect(ctx, tx, 36, barW, 3, 1, 0x40FFFFFF);
        if (n.durationMs() > 0) {
            long pos = Math.min(n.durationMs(), Math.max(0, System.currentTimeMillis() - n.startedMs()));
            int fill = Math.max(2, (int) (barW * pos / n.durationMs()));
            roundRect(ctx, tx, 36, fill, 3, 1, accent);
            String total = time(n.durationMs());
            ctx.drawText(f, Text.literal(time(pos)), tx, 43, 0xFF9A9AA3, false);
            ctx.drawText(f, Text.literal(total), tx + barW - f.getWidth(total), 43, 0xFF9A9AA3, false);
        } else {
            roundRect(ctx, tx, 36, Math.min(barW, 26), 3, 1, accent); // unknown length: short accent stub
        }
        m.popMatrix();
    }
}
