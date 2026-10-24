package app.cadence;

import java.util.List;
import org.schabi.newpipe.extractor.Page;

/** Public playlists, independently browsable from the audio stream resolver. */
public interface PlaylistSource {
    record Suggestion(String url,String title,String author,String artwork,long count) {}
    record Tracks(String title,List<Track> tracks,Page next) {}
    List<Suggestion> suggest(String query) throws Exception;
    Tracks playlist(String url,Page next) throws Exception;
}
