package lastfmhud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import lastfmhud.style.Frame;
import lastfmhud.style.HudStyle;
import lastfmhud.style.HudStyles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Draws the HUD with the selected style; lets you drag it while chat is open. */
public final class HudRenderer {
    private static final int DEFAULT_ACCENT = 0xFFD51007;
    private static Identifier texture;
    private static int shownSerial = -1;
    private static int counter;
    private static int accent = DEFAULT_ACCENT;

    // on-screen bounds in GUI units (updated every frame we draw); width 0 = not drawn
    private static float bx, by, bw, bh;
    private static boolean dragging;
    private static double grabX, grabY;

    private HudRenderer() {}

    private static final class ArtTexture extends NativeImageBackedTexture {
        ArtTexture(NativeImage image) {
            super(() -> "LastFmHud art", image);
            this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        }
    }

    /** Average colour of the cover, pushed towards a vivid tone so it reads well on a dark card. */
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
        float lift = Math.min(2.2f, 230f / max);
        float avg = (c[0] + c[1] + c[2]) / 3f;
        int out = 0xFF000000;
        for (int i = 0; i < 3; i++) {
            float v = avg + (c[i] - avg) * 1.4f;
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

    /** Mouse position in GUI units. */
    private static double[] mouseGui(MinecraftClient mc) {
        var w = mc.getWindow();
        double x = mc.mouse.getX() * w.getScaledWidth() / Math.max(1, w.getWidth());
        double y = mc.mouse.getY() * w.getScaledHeight() / Math.max(1, w.getHeight());
        return new double[] {x, y};
    }

    public static void render(DrawContext ctx) {
        Config c = Config.get();
        bw = 0;
        if (!c.enabled) { dragging = false; return; }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.textRenderer == null) return;
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        if (!chatOpen) dragging = false;

        LastFm.Now n = LastFm.now();
        if (n == null && !chatOpen) return; // nothing to show; in chat we show a placeholder so it can be moved
        syncTexture(mc);

        HudStyle style = HudStyles.get(c.style);
        Frame frame;
        if (n != null) {
            long elapsed = System.currentTimeMillis() - n.startedMs();
            String[] lyr = c.lyrics ? Lyrics.around(elapsed + c.lyricsOffsetMs) : null;
            frame = new Frame(n.title(), n.artist(), elapsed, n.durationMs(), texture, accent, lyr);
        } else {
            frame = new Frame("Last.fm HUD", "Drag me to move", 0, 0, null, DEFAULT_ACCENT, null);
        }

        float scale = c.scale;
        bw = style.width() * scale;
        bh = style.height(frame.lyrics() != null) * scale;
        var win = mc.getWindow();

        if (dragging) {
            double[] m = mouseGui(mc);
            c.x = (int) Math.max(0, Math.min(win.getScaledWidth() - bw, m[0] - grabX));
            c.y = (int) Math.max(0, Math.min(win.getScaledHeight() - bh, m[1] - grabY));
        }
        bx = c.x;
        by = c.y;

        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) c.x, (float) c.y);
        m.scale(scale, scale);
        try {
            style.draw(ctx, mc, frame);
        } finally {
            m.popMatrix();
        }
        if (chatOpen && dragging) { // thin outline while moving
            ctx.fill((int) bx - 1, (int) by - 1, (int) (bx + bw) + 1, (int) by, 0xAAFFFFFF);
            ctx.fill((int) bx - 1, (int) (by + bh), (int) (bx + bw) + 1, (int) (by + bh) + 1, 0xAAFFFFFF);
            ctx.fill((int) bx - 1, (int) by, (int) bx, (int) (by + bh), 0xAAFFFFFF);
            ctx.fill((int) (bx + bw), (int) by, (int) (bx + bw) + 1, (int) (by + bh), 0xAAFFFFFF);
        }
    }

    /**
     * Left mouse button press (action 1) / release (action 0). Returns true when the click was used for dragging and
     * should not reach the chat screen.
     */
    public static boolean mouseButton(int action) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (action == 1) {
            if (!(mc.currentScreen instanceof ChatScreen) || bw <= 0) return false;
            double[] m = mouseGui(mc);
            if (m[0] >= bx && m[0] <= bx + bw && m[1] >= by && m[1] <= by + bh) {
                dragging = true;
                grabX = m[0] - bx;
                grabY = m[1] - by;
                return true;
            }
            return false;
        }
        if (action == 0 && dragging) {
            dragging = false;
            Config.save();
            return true;
        }
        return false;
    }
}
