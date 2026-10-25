package app.cadence;

import org.junit.*;
import static org.junit.Assert.*;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public final class LyricsRepositoryTest {
    private MockWebServer server; private File cache; private LyricsRepository repository;
    private final Track track=new Track("testtone001","Test song (Official Audio)","Test artist - Topic","",120);
    private static final String MATCH="{\"trackName\":\"Test song\",\"artistName\":\"Test artist\",\"duration\":120,\"instrumental\":false,\"plainLyrics\":\"Original test line\",\"syncedLyrics\":\"[00:01.00]Original test line\"}";
    @Before public void setup()throws Exception{server=new MockWebServer();server.start();cache=Files.createTempDirectory("musedock-lyrics").toFile();repository=new LyricsRepository(cache,new OkHttpClient.Builder().callTimeout(2,TimeUnit.SECONDS).build(),server.url("/api/get"));}
    @After public void cleanup()throws Exception{server.shutdown();try(var files=Files.walk(cache.toPath())){files.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(IOException ignored){}});}}
    @Test public void exactLookupNormalizesMetadataAndCachesForOfflineUse()throws Exception{
        server.enqueue(new MockResponse().setBody(MATCH));assertEquals(1,repository.find(track,false).lines().size());RecordedRequest request=server.takeRequest();assertEquals("Test song",request.getRequestUrl().queryParameter("track_name"));assertEquals("Test artist",request.getRequestUrl().queryParameter("artist_name"));assertEquals("120",request.getRequestUrl().queryParameter("duration"));assertTrue(request.getHeader("User-Agent").startsWith("MuseDock/"));
        LyricsRepository offline=new LyricsRepository(cache,new OkHttpClient(),server.url("/unreachable"));assertEquals("Original test line",offline.find(track,false).lines().get(0).text());assertEquals(1,server.getRequestCount());
    }
    @Test public void refreshBypassesCache()throws Exception{server.enqueue(new MockResponse().setBody(MATCH));repository.find(track,false);server.enqueue(new MockResponse().setResponseCode(404));assertTrue(repository.find(track,true).empty());assertEquals(2,server.getRequestCount());}
    @Test public void rejectsDifferentRecordingOrArtist()throws Exception{
        server.enqueue(new MockResponse().setBody(MATCH.replace("Test artist","Other artist")));assertTrue(repository.find(track,false).empty());
        server.enqueue(new MockResponse().setBody(MATCH.replace("\"duration\":120","\"duration\":300")));assertTrue(repository.find(track,false).empty());assertEquals(0,cache.list().length);
    }
    @Test public void missingProviderEntryIsNotNetworkFailure()throws Exception{server.enqueue(new MockResponse().setResponseCode(404));assertTrue(repository.find(track,false).empty());}
    @Test public void providerFailureAndOversizedResponsesAreReported()throws Exception{
        server.enqueue(new MockResponse().setResponseCode(503));try{repository.find(track,false);fail();}catch(IOException expected){}
        server.enqueue(new MockResponse().setBody("x".repeat(1024*1024+1)));try{repository.find(track,false);fail();}catch(IOException expected){}
    }
    @Test public void corruptCacheIsRecoveredFromProvider()throws Exception{server.enqueue(new MockResponse().setBody(MATCH));repository.find(track,false);File cached=cache.listFiles()[0];Files.write(cached.toPath(),"broken".getBytes());server.enqueue(new MockResponse().setBody(MATCH));assertFalse(repository.find(track,false).empty());assertEquals(2,server.getRequestCount());}
}
