package lastfmhud.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.util.ArrayList;
import java.util.List;
import lastfmhud.Config;
import lastfmhud.style.HudStyles;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Flat dark settings screen with an HSV colour wheel. Opened with Right Shift (or by tapping the HUD in chat). */
public final class SettingsScreen extends Screen {
    private interface Act { void run(double x, double y); }
    private record Hit(int x, int y, int w, int h, Act press, Act drag) {}

    // flat dark theme
    private static final int DIM = 0xA0000000, PANEL = 0xFF16161A, BORDER = 0xFF2A2A31, FIELD = 0xFF1F1F25,
        HOVER = 0xFF2B2B33, TEXT = 0xFFF0F0F4, MUTED = 0xFF8A8A94;
    private static final int[] PRESETS = {0xE0457B, 0xD51007, 0xFF9A1F, 0x2ECC71, 0x2DB7F5, 0x9B6BFF};

    private static Identifier wheel;

    private final List<Hit> hits = new ArrayList<>();
    private Hit active;
    private TextFieldWidget userField, keyField;
    private int px, py, pw, ph;
    private float hue, sat, val;

    public SettingsScreen() {
        super(Text.literal("Last.fm HUD"));
        int rgb = Config.get().accentColor & 0xFFFFFF;
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        val = Math.max(0.35f, max);
        sat = max == 0 ? 0 : d / max;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6;
        else if (max == g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        hue = (h / 6f + 1f) % 1f;
    }

    private int accent() { return 0xFF000000 | (Config.get().accentColor & 0xFFFFFF); }

    private static int hsv(float h, float s, float v) {
        float c = v * s, x = c * (1 - Math.abs((h * 6) % 2 - 1)), m = v - c, r, g, b;
        int seg = (int) (h * 6) % 6;
        switch (seg) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }

    private void applyWheelColor() {
        Config.get().accentColor = hsv(hue, sat, val);
    }

    private static final class WheelTexture extends NativeImageBackedTexture {
        WheelTexture(NativeImage image) {
            super(() -> "LastFmHud wheel", image);
            this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        }
    }

    private static void ensureWheel(MinecraftClient mc) {
        if (wheel != null) return;
        NativeImage img = new NativeImage(128, 128, false);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                double dx = x - 63.5, dy = y - 63.5, d = Math.sqrt(dx * dx + dy * dy);
                double a = Math.max(0, Math.min(1, 63.5 - d + 0.5));
                int argb = 0;
                if (a > 0) {
                    float h = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
                    argb = ((int) (a * 255) << 24) | hsv(h, (float) Math.min(1, d / 63.5), 1f);
                }
                img.setColorArgb(x, y, argb);
            }
        }
        Identifier id = Identifier.of("lastfmhud", "wheel");
        mc.getTextureManager().registerTexture(id, new WheelTexture(img));
        wheel = id;
    }

    @Override
    protected void init() {
        pw = Math.min(320, width - 8);
        ph = Math.min(236, height - 8);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        Config c = Config.get();
        int x0 = px + 14;
        userField = new TextFieldWidget(textRenderer, x0 + 3, py + 158, 144, 14, Text.literal("Last.fm user"));
        userField.setDrawsBackground(false);
        userField.setMaxLength(64);
        userField.setEditableColor(TEXT);
        userField.setPlaceholder(Text.literal("username").withColor(0x707078));
        userField.setText(c.user);
        userField.setChangedListener(s -> Config.get().user = s.trim());
        addDrawableChild(userField);
        keyField = new TextFieldWidget(textRenderer, x0 + 3, py + 192, 144, 14, Text.literal("API key"));
        keyField.setDrawsBackground(false);
        keyField.setMaxLength(64);
        keyField.setEditableColor(TEXT);
        keyField.setPlaceholder(Text.literal("last.fm api key").withColor(0x707078));
        keyField.setText(c.apiKey);
        keyField.setChangedListener(s -> Config.get().apiKey = s.trim());
        addDrawableChild(keyField);
    }

    @Override public boolean shouldPause() { return false; }

    @Override public void renderBackground(DrawContext c, int x, int y, float delta) {}

    private boolean inside(int mx, int my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private void button(DrawContext c, String label, int x, int y, int w, int h, boolean on, int mx, int my, Act press) {
        int bg = on ? accent() : (inside(mx, my, x, y, w, h) ? HOVER : FIELD);
        c.fill(x, y, x + w, y + h, bg);
        c.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + (h - 8) / 2 + 1, on ? 0xFF101014 : TEXT, false);
        hits.add(new Hit(x, y, w, h, press, null));
    }

    private void slider(DrawContext c, String label, String value, int x, int y, int w, double frac, int mx, int my, Act set) {
        c.drawText(textRenderer, label, x, y, MUTED, false);
        c.drawText(textRenderer, value, x + w - textRenderer.getWidth(value), y, TEXT, false);
        int by = y + 12;
        c.fill(x, by, x + w, by + 4, FIELD);
        c.fill(x, by, x + (int) (w * Math.max(0, Math.min(1, frac))), by + 4, accent());
        int kx = x + (int) (w * Math.max(0, Math.min(1, frac)));
        c.fill(kx - 2, by - 3, kx + 2, by + 7, TEXT);
        hits.add(new Hit(x - 3, by - 5, w + 6, 14, set, set));
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        hits.clear();
        ensureWheel(MinecraftClient.getInstance());
        Config cfg = Config.get();
        int acc = accent();

        c.fill(0, 0, width, height, DIM);
        c.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        c.fill(px, py, px + pw, py + ph, PANEL);

        c.drawText(textRenderer, "Last.fm HUD", px + 14, py + 10, acc, false);
        c.drawText(textRenderer, "Right Shift to close", px + pw - 14 - textRenderer.getWidth("Right Shift to close"), py + 10, MUTED, false);
        c.fill(px + 14, py + 22, px + pw - 14, py + 23, BORDER);

        int x0 = px + 14;
        // toggles
        button(c, "HUD", x0, py + 30, 46, 16, cfg.enabled, mx, my, (x, y) -> cfg.enabled = !cfg.enabled);
        button(c, "Lyrics", x0 + 50, py + 30, 46, 16, cfg.lyrics, mx, my, (x, y) -> cfg.lyrics = !cfg.lyrics);
        button(c, "My color", x0 + 100, py + 30, 50, 16, cfg.customAccent, mx, my, (x, y) -> cfg.customAccent = !cfg.customAccent);

        // style
        c.drawText(textRenderer, "Style", x0, py + 54, MUTED, false);
        List<String> ids = HudStyles.ids();
        int bw = (150 - 4 * (ids.size() - 1)) / ids.size();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            String label = Character.toUpperCase(id.charAt(0)) + id.substring(1);
            button(c, label, x0 + i * (bw + 4), py + 65, bw, 16, id.equalsIgnoreCase(cfg.style), mx, my, (x, y) -> cfg.style = id);
        }

        // sliders
        slider(c, "Size", String.format("%.2fx", cfg.scale), x0, py + 90, 150, (cfg.scale - 0.5) / 2.5, mx, my,
            (x, y) -> cfg.scale = (float) (Math.round((0.5 + 2.5 * Math.max(0, Math.min(1, (x - x0) / 150.0))) * 20) / 20.0));
        slider(c, "Lyrics sync", cfg.lyricsOffsetMs + " ms", x0, py + 116, 150, (cfg.lyricsOffsetMs + 5000) / 10000.0, mx, my,
            (x, y) -> cfg.lyricsOffsetMs = (int) (Math.round((-5000 + 10000 * Math.max(0, Math.min(1, (x - x0) / 150.0))) / 50.0) * 50));

        // text fields
        c.drawText(textRenderer, "Last.fm user", x0, py + 145, MUTED, false);
        c.fill(x0, py + 155, x0 + 150, py + 173, userField.isFocused() ? acc : FIELD);
        c.fill(x0 + 1, py + 156, x0 + 149, py + 172, FIELD);
        c.drawText(textRenderer, "API key", x0, py + 179, MUTED, false);
        c.fill(x0, py + 189, x0 + 150, py + 207, keyField.isFocused() ? acc : FIELD);
        c.fill(x0 + 1, py + 190, x0 + 149, py + 206, FIELD);

        // colour wheel
        int wx = px + pw - 14 - 100, wy = py + 30;
        c.drawText(textRenderer, "Colour", wx, py + 28 - 0, MUTED, false);
        wy = py + 40;
        int tint = 0xFF000000 | (Math.round(val * 255) * 0x010101);
        c.drawTexture(RenderPipelines.GUI_TEXTURED, wheel, wx, wy, 0.0F, 0.0F, 100, 100, 128, 128, 128, 128, tint);
        double ang = hue * 2 * Math.PI;
        int mxk = wx + 50 + (int) Math.round(Math.cos(ang) * sat * 50), myk = wy + 50 + (int) Math.round(Math.sin(ang) * sat * 50);
        c.fill(mxk - 3, myk - 3, mxk + 3, myk + 3, 0xFFFFFFFF);
        c.fill(mxk - 2, myk - 2, mxk + 2, myk + 2, 0xFF000000 | hsv(hue, sat, val));
        final int fwx = wx, fwy = wy;
        Act wheelAct = (x, y) -> {
            double dx = x - (fwx + 50), dy = y - (fwy + 50);
            sat = (float) Math.min(1, Math.sqrt(dx * dx + dy * dy) / 50.0);
            hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1);
            applyWheelColor();
            cfg.customAccent = true;
        };
        hits.add(new Hit(wx, wy, 100, 100, wheelAct, wheelAct));

        // brightness
        int by = py + 148;
        c.drawText(textRenderer, "Brightness", wx, by - 10, MUTED, false);
        c.fill(wx, by + 2, wx + 100, by + 6, FIELD);
        c.fill(wx, by + 2, wx + (int) (100 * (val - 0.35f) / 0.65f), by + 6, acc);
        int kx = wx + (int) (100 * (val - 0.35f) / 0.65f);
        c.fill(kx - 2, by - 1, kx + 2, by + 9, TEXT);
        Act bright = (x, y) -> {
            val = (float) (0.35 + 0.65 * Math.max(0, Math.min(1, (x - fwx) / 100.0)));
            applyWheelColor();
            cfg.customAccent = true;
        };
        hits.add(new Hit(wx - 3, by - 4, 106, 16, bright, bright));

        // preview + presets
        c.fill(wx, py + 166, wx + 100, py + 180, acc);
        String hex = String.format("#%06X", cfg.accentColor & 0xFFFFFF);
        c.drawText(textRenderer, hex, wx + (100 - textRenderer.getWidth(hex)) / 2, py + 170, 0xFF101014, false);
        for (int i = 0; i < PRESETS.length; i++) {
            int col = PRESETS[i], sx = wx + i * 17;
            c.fill(sx, py + 186, sx + 15, py + 201, 0xFF000000 | col);
            hits.add(new Hit(sx, py + 186, 15, 15, (x, y) -> {
                cfg.accentColor = col;
                cfg.customAccent = true;
                SettingsScreen.this.syncWheelFromColor();
            }, null));
        }

        // done
        button(c, "Done", px + pw - 14 - 44, py + ph - 24, 44, 16, false, mx, my, (x, y) -> close());

        super.render(c, mx, my, delta);
    }

    private void syncWheelFromColor() {
        int rgb = Config.get().accentColor & 0xFFFFFF;
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), d = max - Math.min(r, Math.min(g, b));
        val = Math.max(0.35f, max);
        sat = max == 0 ? 0 : d / max;
        float h = d == 0 ? 0 : max == r ? ((g - b) / d) % 6 : max == g ? (b - r) / d + 2 : (r - g) / d + 4;
        hue = (h / 6f + 1f) % 1f;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0) {
            double x = click.x(), y = click.y();
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (inside((int) x, (int) y, h.x(), h.y(), h.w(), h.h())) {
                    setFocused(null);
                    userField.setFocused(false);
                    keyField.setFocused(false);
                    h.press().run(x, y);
                    active = h.drag() != null ? h : null;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (active != null && active.drag() != null) {
            active.drag().run(click.x(), click.y());
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (active != null) {
            active = null;
            Config.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput key) {
        boolean typing = (userField != null && userField.isFocused()) || (keyField != null && keyField.isFocused());
        if (key.key() == 256 || (key.key() == 344 && !typing)) { // Esc or Right Shift
            close();
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public void removed() {
        Config.save();
        super.removed();
    }
}
