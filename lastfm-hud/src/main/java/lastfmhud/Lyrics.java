package lastfmhud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Synced lyrics from lrclib.net (free, no key). Loaded in the background when the track changes. */
public final class Lyrics {
    private record Line(long ms, String text) {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LastFmHud lyrics");
        t.setDaemon(true);
        return t;
    });
    private static final Pattern STAMP = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)]\\s*(.*)$");

    private static volatile List<Line> lines = List.of();
    private static volatile String wantedKey = "";

    private Lyrics() {}

    public static void clear() {
        wantedKey = "";
        lines = List.of();
    }

    public static void load(String artist, String title, long durationMs, String key) {
        if (key.equals(wantedKey)) return;
        wantedKey = key;
        lines = List.of();
        EXECUTOR.execute(() -> {
            List<Line> found = fetch(artist, title, durationMs);
            if (key.equals(wantedKey)) lines = found;
        });
    }

    /** {previous, current, next} around the given elapsed time, or null if there are no synced lyrics. */
    public static String[] around(long elapsedMs) {
        List<Line> l = lines;
        if (l.isEmpty()) return null;
        int idx = -1;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).ms() <= elapsedMs) idx = i; else break;
        }
        String prev = idx - 1 >= 0 ? l.get(idx - 1).text() : "";
        String cur = idx >= 0 ? l.get(idx).text() : "";
        String next = idx + 1 < l.size() ? l.get(idx + 1).text() : "";
        return new String[] {prev, cur, next};
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static List<Line> fetch(String artist, String title, long durationMs) {
        try {
            String base = "https://lrclib.net/api/get?artist_name=" + enc(artist) + "&track_name=" + enc(title);
            String body = null;
            if (durationMs > 0) body = get(base + "&duration=" + Math.round(durationMs / 1000.0));
            if (body == null) body = get(base);
            if (body == null) return List.of();
            JsonObject o = JsonParser.parseString(body).getAsJsonObject();
            if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return List.of();
            return parse(o.get("syncedLyrics").getAsString());
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String get(String url) throws Exception {
        HttpResponse<InputStream> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
            .header("User-Agent", "LastFmHud/1.2 (Fabric mod)").GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = r.body()) {
            byte[] b = in.readNBytes(600_001);
            if (r.statusCode() != 200 || b.length > 600_000) return null;
            return new String(b, StandardCharsets.UTF_8);
        }
    }

    static List<Line> parse(String synced) {
        List<Line> out = new ArrayList<>();
        for (String raw : synced.split("\\r?\\n")) {
            Matcher m = STAMP.matcher(raw.trim());
            if (!m.matches()) continue;
            long ms = (long) ((Long.parseLong(m.group(1)) * 60 + Double.parseDouble(m.group(2))) * 1000);
            String text = m.group(3).replaceAll("[\\p{Cntrl}]", " ").trim();
            out.add(new Line(ms, text.isEmpty() ? "♪" : text));
        }
        return out;
    }
}
