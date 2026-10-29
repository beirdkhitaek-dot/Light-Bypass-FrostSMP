package lastfmhud.style;

import net.minecraft.util.Identifier;

/**
 * Everything a style needs for one frame.
 *
 * @param title      song title
 * @param artist     artist
 * @param elapsedMs  estimated playback position
 * @param durationMs track length, 0 if unknown
 * @param texture    cover art texture or null
 * @param accent     ARGB accent colour taken from the cover
 * @param lyrics     {previous, current, next} or null when there are none
 */
public record Frame(String title, String artist, long elapsedMs, long durationMs, Identifier texture, int accent, String[] lyrics) {}
