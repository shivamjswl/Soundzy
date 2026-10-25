package app.cadence;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MusicLanguageTest {
    @Test public void languageEnrichmentDoesNotRewriteVideoOrPlaylistLinks(){
        String video="https://music.youtube.com/watch?v=dQw4w9WgXcQ",playlist="https://music.youtube.com/playlist?list=PL_fixture";
        assertEquals(video,MusicLanguage.PUNJABI.query(video));assertEquals(playlist,MusicLanguage.ENGLISH.query(playlist));assertEquals("dQw4w9WgXcQ",MusicLanguage.PUNJABI.query("dQw4w9WgXcQ"));
    }
    @Test public void enrichmentPreservesQueriesAndDoesNotDuplicateLanguage(){assertEquals("Punjabi acoustic songs",MusicLanguage.PUNJABI.query("acoustic songs"));assertEquals("Best PUNJABI songs",MusicLanguage.PUNJABI.query("Best PUNJABI songs"));assertEquals("jazz albums",MusicLanguage.ANY.query("  jazz albums  "));assertEquals("English jazz albums",MusicLanguage.ENGLISH.query("jazz albums"));}
}
