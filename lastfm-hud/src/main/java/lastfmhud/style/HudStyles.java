package lastfmhud.style;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry of selectable styles. To add one: implement HudStyle and register it here. */
public final class HudStyles {
    private static final Map<String, HudStyle> ALL = new LinkedHashMap<>();

    static {
        register(new GlassStyle());
        register(new KryptonStyle());
        register(new SlateStyle());
        register(new SpotifyStyle());
    }

    private HudStyles() {}

    private static void register(HudStyle style) {
        ALL.put(style.id(), style);
    }

    public static HudStyle get(String id) {
        HudStyle s = id == null ? null : ALL.get(id.toLowerCase());
        return s != null ? s : ALL.values().iterator().next();
    }

    public static List<String> ids() {
        return List.copyOf(ALL.keySet());
    }
}
