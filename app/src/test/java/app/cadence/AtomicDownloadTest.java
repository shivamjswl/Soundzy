package app.cadence;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.Files;
import static org.junit.Assert.*;
public final class AtomicDownloadTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final byte[] data="audio-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private File target(){return new File(temp.getRoot(),"song.audio");}
    @Test public void completeDownloadBecomesPlayable()throws Exception{
        File f=target();AtomicDownload.save(new ByteArrayInputStream(data),f,data.length,(n,t)->{});assertArrayEquals(data,Files.readAllBytes(f.toPath()));assertFalse(new File(f+".part").exists());
    }
    @Test public void unknownLengthStillRequiresBytes()throws Exception{File f=target();AtomicDownload.save(new ByteArrayInputStream(data),f,-1,(n,t)->{});assertEquals(data.length,f.length());}
    @Test public void truncatedDownloadNeverPublishesFile()throws Exception{assertRejected(new ByteArrayInputStream(data),data.length+10,(n,t)->{});}
    @Test public void emptyDownloadNeverPublishesFile()throws Exception{assertRejected(new ByteArrayInputStream(new byte[0]),-1,(n,t)->{});}
    @Test public void cancellationCleansPartialFile()throws Exception{assertRejected(new ByteArrayInputStream(data),data.length,(n,t)->{throw new IOException("Cancelled");});}
    @Test public void networkFailurePreservesExistingOfflineCopy()throws Exception{
        File f=target();Files.write(f.toPath(),data);
        try{AtomicDownload.save(new InputStream(){public int read()throws IOException{throw new IOException("Network lost");}},f,-1,(n,t)->{});fail();}catch(IOException expected){}
        assertArrayEquals(data,Files.readAllBytes(f.toPath()));assertFalse(new File(f+".part").exists());
    }
    private void assertRejected(InputStream in,long total,AtomicDownload.Progress p)throws Exception{
        File f=target();try{AtomicDownload.save(in,f,total,p);fail("Expected failure");}catch(IOException expected){}assertFalse(f.exists());assertFalse(new File(f+".part").exists());
    }
}
