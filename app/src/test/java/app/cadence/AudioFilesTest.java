package app.cadence;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
public final class AudioFilesTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private File file(byte[] bytes)throws Exception{File f=temp.newFile();Files.write(f.toPath(),bytes);return f;}
    @Test public void recognizesM4aAndWebm()throws Exception{
        assertEquals("m4a",AudioFiles.extension(file(new byte[]{0,0,0,24,'f','t','y','p','M','4','A',' '})));
        assertEquals("webm",AudioFiles.extension(file(new byte[]{0x1a,0x45,(byte)0xdf,(byte)0xa3,0,0})));
    }
    @Test public void recognizesMp3AndAacWithoutMislabeling()throws Exception{assertEquals("mp3",AudioFiles.extension(file(new byte[]{(byte)0xff,(byte)0xfb,0,0})));assertEquals("aac",AudioFiles.extension(file(new byte[]{(byte)0xff,(byte)0xf1,0,0})));}
    @Test public void recognizesWaveAndFallsBackForUnknownData()throws Exception{assertEquals("wav",AudioFiles.extension(file("RIFF0000WAVE".getBytes(StandardCharsets.US_ASCII))));assertEquals("audio",AudioFiles.extension(file(new byte[]{1})));assertEquals("audio/mpeg",AudioFiles.mime("mp3"));}
}
