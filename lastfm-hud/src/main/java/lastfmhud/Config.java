package lastfmhud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** config/lastfmhud.json */
public final class Config {
    public boolean enabled = true;
    public String apiKey = "";
    public String user = "";
    public int x = 6;
    public int y = 40;
    public float scale = 1.0f;

    private static Config instance = new Config();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("lastfmhud.json");
    }

    public static Config get() {
        return instance;
    }

    public static synchronized void load() {
        try {
            if (Files.exists(file()) && Files.size(file()) < 65536) {
                Config read = new Gson().fromJson(Files.readString(file()), Config.class);
                if (read != null) instance = read;
            }
        } catch (Exception ignored) {
            instance = new Config();
        }
        if (instance.apiKey == null) instance.apiKey = "";
        if (instance.user == null) instance.user = "";
        if (instance.scale < 0.5f || instance.scale > 4f) instance.scale = 1.0f;
        save();
    }

    public static synchronized void save() {
        try {
            Files.writeString(file(), new GsonBuilder().setPrettyPrinting().create().toJson(instance));
        } catch (IOException ignored) {
        }
    }
}
