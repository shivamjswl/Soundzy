package app.cadence;

public interface LyricsSource {
    Lyrics find(Track track, boolean refresh) throws Exception;
}
