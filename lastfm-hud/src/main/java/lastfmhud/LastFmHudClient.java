package lastfmhud;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lastfmhud.style.HudStyles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

public final class LastFmHudClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Config.load();
        LastFm.start();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> styleNode = ClientCommandManager.literal("style");
            for (String id : HudStyles.ids()) {
                styleNode.then(ClientCommandManager.literal(id).executes(ctx -> {
                    Config.get().style = id;
                    Config.save();
                    return say(ctx, "Style set to " + id);
                }));
            }
            styleNode.executes(ctx -> say(ctx, "Styles: " + String.join(", ", HudStyles.ids()) + " (now: " + Config.get().style + ")"));

            dispatcher.register(ClientCommandManager.literal("lastfmhud")
                .then(ClientCommandManager.literal("user").then(ClientCommandManager.argument("name", StringArgumentType.word())
                    .executes(ctx -> {
                        Config.get().user = StringArgumentType.getString(ctx, "name");
                        Config.save();
                        return say(ctx, "Last.fm user set to " + Config.get().user);
                    })))
                .then(ClientCommandManager.literal("key").then(ClientCommandManager.argument("apikey", StringArgumentType.word())
                    .executes(ctx -> {
                        Config.get().apiKey = StringArgumentType.getString(ctx, "apikey");
                        Config.save();
                        return say(ctx, "API key saved.");
                    })))
                .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                    Config.get().enabled = !Config.get().enabled;
                    Config.save();
                    return say(ctx, Config.get().enabled ? "HUD on" : "HUD off");
                }))
                .then(styleNode)
                .then(ClientCommandManager.literal("lyrics").executes(ctx -> {
                    Config.get().lyrics = !Config.get().lyrics;
                    Config.save();
                    return say(ctx, Config.get().lyrics ? "Lyrics on" : "Lyrics off");
                }))
                .then(ClientCommandManager.literal("syncoffset")
                    .then(ClientCommandManager.argument("ms", IntegerArgumentType.integer(-10000, 10000)).executes(ctx -> {
                        Config.get().lyricsOffsetMs = IntegerArgumentType.getInteger(ctx, "ms");
                        Config.save();
                        return say(ctx, "Lyrics offset set to " + Config.get().lyricsOffsetMs + " ms (positive = lyrics earlier)");
                    })))
                .then(ClientCommandManager.literal("pos")
                    .then(ClientCommandManager.argument("x", IntegerArgumentType.integer(0, 4000))
                        .then(ClientCommandManager.argument("y", IntegerArgumentType.integer(0, 4000)).executes(ctx -> {
                            Config.get().x = IntegerArgumentType.getInteger(ctx, "x");
                            Config.get().y = IntegerArgumentType.getInteger(ctx, "y");
                            Config.save();
                            return say(ctx, "Position set to " + Config.get().x + ", " + Config.get().y);
                        }))))
                .then(ClientCommandManager.literal("scale")
                    .then(ClientCommandManager.argument("size", FloatArgumentType.floatArg(0.5f, 4.0f)).executes(ctx -> {
                        Config.get().scale = FloatArgumentType.getFloat(ctx, "size");
                        Config.save();
                        return say(ctx, "Scale set to " + Config.get().scale);
                    })))
                .then(ClientCommandManager.literal("status").executes(ctx -> {
                    Config c = Config.get();
                    return say(ctx, (c.enabled ? "on" : "off") + ", style=" + c.style + ", lyrics=" + (c.lyrics ? "on" : "off")
                        + ", user=" + (c.user.isBlank() ? "(not set)" : c.user)
                        + ", key=" + (c.apiKey.isBlank() ? "(not set)" : "set") + ", " + LastFm.status());
                })));
        });
    }

    private static int say(CommandContext<FabricClientCommandSource> ctx, String message) {
        ctx.getSource().sendFeedback(Text.literal("[Last.fm HUD] " + message));
        return 1;
    }
}
