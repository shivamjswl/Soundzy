package app.cadence;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class VideoIds {
    private VideoIds() {}
    public static boolean valid(String id) { return id != null && id.matches("[A-Za-z0-9_-]{11}"); }
    public static String url(String id) {
        if (!valid(id)) throw new IllegalArgumentException("Invalid YouTube video ID");
        return "https://www.youtube.com/watch?v=" + id;
    }
    public static String parse(String value) {
        if (valid(value)) return value;
        try {
            URI u = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(u.getScheme()) && !"http".equalsIgnoreCase(u.getScheme())) return null;
            String host = u.getHost();
            if (host == null) return null;
            host = host.toLowerCase(java.util.Locale.ROOT);
            String id = null;
            if (host.equals("youtu.be")) id = u.getPath().substring(1).split("/")[0];
            else if (Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com").contains(host)) {
                if (u.getPath().startsWith("/shorts/") || u.getPath().startsWith("/embed/")) id = u.getPath().split("/")[2];
                else if (u.getPath().equals("/watch") && u.getRawQuery() != null) {
                    for (String p : u.getRawQuery().split("&")) {
                        String[] kv = p.split("=", 2);
                        if (kv.length == 2 && kv[0].equals("v")) id = URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                    }
                }
            }
            return valid(id) ? id : null;
        } catch (java.io.UnsupportedEncodingException | RuntimeException e) { return null; }
    }
}
