package app.cadence;

import okhttp3.*;
import okhttp3.mockwebserver.*;
import okhttp3.mockwebserver.Dispatcher;
import okio.Buffer;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public final class ResumableDownloadTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private MockWebServer server;private byte[] bytes;
    @Before public void setup()throws Exception{server=new MockWebServer();server.start();bytes=new byte[5*1024*1024+17];new java.util.Random(42).nextBytes(bytes);}
    @After public void close()throws Exception{server.shutdown();}
    private AudioSource source(String path){return new AudioSource(server.url(path).toString(),"audio/webm","251",bytes.length);}
    private MockResponse range(RecordedRequest request){
        String raw=request.getHeader("Range");String[] parts=raw.substring(6).split("-");int from=Integer.parseInt(parts[0]),to=Math.min(bytes.length-1,Integer.parseInt(parts[1]));
        return new MockResponse().setResponseCode(206).setHeader("Content-Type","audio/webm").setHeader("Content-Range","bytes "+from+"-"+to+"/"+bytes.length).setBody(new Buffer().write(bytes,from,to-from+1));
    }
    private ResumableDownload downloader(){return new ResumableDownload(new OkHttpClient.Builder().callTimeout(70,TimeUnit.SECONDS).readTimeout(2,TimeUnit.SECONDS).build());}
    private File target(){return new File(temp.getRoot(),"song.source");}
    private void assertComplete(File file)throws Exception{assertEquals(bytes.length,file.length());assertEquals(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes)),java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()))));assertFalse(ResumableDownload.partial(file).exists());}
    @Test public void downloadsMultipleRangesWithoutGlobalTimeout()throws Exception{
        server.setDispatcher(new Dispatcher(){@Override public MockResponse dispatch(RecordedRequest r){return range(r);}});
        ResumableDownload d=downloader();assertEquals(0,d.client().callTimeoutMillis());File file=target();d.save(refresh->source("/audio"),file,(n,t)->{},call->{});assertComplete(file);assertEquals(3,server.getRequestCount());
    }
    @Test public void disconnectedTransferContinuesFromWrittenOffset()throws Exception{
        AtomicInteger requests=new AtomicInteger();AtomicReference<String> resumed=new AtomicReference<>();
        server.setDispatcher(new Dispatcher(){@Override public MockResponse dispatch(RecordedRequest r){MockResponse response=range(r);if(requests.getAndIncrement()==0)return response.setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY);if(resumed.get()==null)resumed.set(r.getHeader("Range"));return response;}});
        File file=target();downloader().save(refresh->source("/audio"),file,(n,t)->{},call->{});assertComplete(file);assertNotNull(resumed.get());assertFalse(resumed.get().startsWith("bytes=0-"));
    }
    @Test public void retryReusesPersistedPartialFile()throws Exception{
        File file=target();int prefix=10000;Files.write(ResumableDownload.partial(file).toPath(),java.util.Arrays.copyOf(bytes,prefix));Files.write(Path.of(ResumableDownload.partial(file)+".meta"),source("/audio").fingerprint().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        server.setDispatcher(new Dispatcher(){@Override public MockResponse dispatch(RecordedRequest r){return range(r);}});
        downloader().save(refresh->source("/audio"),file,(n,t)->{},call->{});assertComplete(file);assertTrue(server.takeRequest().getHeader("Range").startsWith("bytes=10000-"));
    }
    @Test public void ignoredRangeRestartsRatherThanDuplicatingPrefix()throws Exception{
        File file=target();Files.write(ResumableDownload.partial(file).toPath(),java.util.Arrays.copyOf(bytes,1000));Files.write(Path.of(ResumableDownload.partial(file)+".meta"),source("/audio").fingerprint().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        server.enqueue(new MockResponse().setHeader("Content-Type","audio/webm").setBody(new Buffer().write(bytes)));
        downloader().save(refresh->source("/audio"),file,(n,t)->{},call->{});assertComplete(file);
    }
    @Test public void expiredAudioLinkIsRefreshedAutomatically()throws Exception{
        AtomicInteger refreshes=new AtomicInteger();server.setDispatcher(new Dispatcher(){@Override public MockResponse dispatch(RecordedRequest r){return r.getPath().equals("/expired")?new MockResponse().setResponseCode(403):range(r);}});
        File file=target();downloader().save(refresh->{if(refresh)refreshes.incrementAndGet();return source(refreshes.get()>0?"/audio":"/expired");},file,(n,t)->{},call->{});assertComplete(file);assertEquals(1,refreshes.get());
    }
    @Test public void cancellationKeepsAResumablePrefixButNeverPublishesIt()throws Exception{
        server.setDispatcher(new Dispatcher(){@Override public MockResponse dispatch(RecordedRequest r){return range(r);}});File file=target();
        try{downloader().save(refresh->source("/audio"),file,(n,t)->{if(n>0)throw new IOException("Cancelled");},call->{});fail();}catch(IOException expected){}
        assertFalse(file.exists());assertTrue(ResumableDownload.partial(file).length()>0);
    }
    @Test public void changedSourceIdentityDiscardsOldPrefix()throws Exception{
        File file=target();Files.write(ResumableDownload.partial(file).toPath(),new byte[]{1,2,3});Files.write(Path.of(ResumableDownload.partial(file)+".meta"),"a different stream".getBytes(java.nio.charset.StandardCharsets.UTF_8));server.setDispatcher(new Dispatcher(){@Override public MockResponse dispatch(RecordedRequest r){return range(r);}});
        downloader().save(refresh->source("/audio"),file,(n,t)->{},call->{});assertComplete(file);assertTrue(server.takeRequest().getHeader("Range").startsWith("bytes=0-"));
    }
}
