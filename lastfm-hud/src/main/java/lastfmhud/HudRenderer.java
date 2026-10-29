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
    private static final int RED = 0xFFD51007;
    private static Identifier texture;
    private static int shownSerial = -1;
    private static int counter;

    private HudRenderer() {}

    private static final class ArtTexture extends NativeImageBackedTexture {
        ArtTexture(NativeImage image) {
            super(() -> "LastFmHud art", image);
            this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        }
    }

    private static void syncTexture(MinecraftClient mc) {
        int serial = LastFm.artSerial();
        if (serial == shownSerial) return;
        shownSerial = serial;
        if (texture != null) {
            mc.getTextureManager().destroyTexture(texture);
            texture = null;
        }
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
        } catch (Throwable ignored) {
            texture = null;
        }
    }

    public static void render(DrawContext ctx) {
        Config c = Config.get();
        if (!c.enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.textRenderer == null) return;
        LastFm.Now n = LastFm.now();
        if (n == null) return;
        syncTexture(mc);

        int w = 150, h = 40;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) c.x, (float) c.y);
        m.scale(c.scale, c.scale);
        ctx.fill(0, 0, w, h, 0xB0101010);
        ctx.fill(0, 0, 2, h, RED);
        int tx = 8;
        if (texture != null) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texture, 6, 4, 0.0F, 0.0F, 32, 32, 64, 64, 64, 64, 0xFFFFFFFF);
            tx = 42;
        }
        TextRenderer f = mc.textRenderer;
        int maxW = w - tx - 5;
        ctx.drawText(f, Text.literal(f.trimToWidth(n.title(), maxW)), tx, 6, 0xFFFFFFFF, false);
        ctx.drawText(f, Text.literal(f.trimToWidth(n.artist(), maxW)), tx, 18, 0xFFB3B3B3, false);
        if (n.durationMs() > 0) {
            long pos = Math.min(n.durationMs(), Math.max(0, System.currentTimeMillis() - n.startedMs()));
            int barW = w - tx - 5;
            ctx.fill(tx, 32, tx + barW, 34, 0x40FFFFFF);
            ctx.fill(tx, 32, tx + (int) (barW * pos / n.durationMs()), 34, RED);
        }
        m.popMatrix();
    }
}
