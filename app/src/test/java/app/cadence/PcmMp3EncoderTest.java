package app.cadence;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

public final class PcmMp3EncoderTest {
    @Test public void encodesRealPcmIntoAnMp3File()throws Exception{
        int rate=44100;ByteArrayOutputStream mp3=new ByteArrayOutputStream();
        try(PcmMp3Encoder encoder=new PcmMp3Encoder(rate,2,mp3)){
            int offset=0;while(offset<rate*2){int count=Math.min(2048,rate*2-offset);ByteBuffer pcm=ByteBuffer.allocate(count*4).order(ByteOrder.LITTLE_ENDIAN);for(int i=0;i<count;i++){short sample=(short)(6000*Math.sin(2*Math.PI*440*(offset+i)/rate));pcm.putShort(sample).putShort(sample);}encoder.encode(pcm.array(),pcm.array().length,false);offset+=count;}encoder.finish();
        }
        assertTrue(mp3.size()>10000);byte[] result=mp3.toByteArray();assertEquals(0xff,result[0]&0xff);assertEquals(0xe0,result[1]&0xe0);
        File validation=new File(System.getProperty("musedock.validation.dir"));assertTrue(validation.isDirectory()||validation.mkdirs());Files.write(new File(validation,"encoder-test.mp3").toPath(),result);
    }
    @Test public void supportsFloatPcmAndResamples48Khz()throws Exception{
        ByteArrayOutputStream mp3=new ByteArrayOutputStream();ByteBuffer pcm=ByteBuffer.allocate(4800*4).order(ByteOrder.LITTLE_ENDIAN);for(int i=0;i<4800;i++)pcm.putFloat((float)(.2*Math.sin(2*Math.PI*440*i/48000)));
        try(PcmMp3Encoder encoder=new PcmMp3Encoder(48000,1,mp3)){encoder.encode(pcm.array(),pcm.array().length,true);encoder.finish();}assertTrue(mp3.size()>500);
    }
    @Test(expected=IOException.class)public void rejectsIncompletePcmFrames()throws Exception{try(PcmMp3Encoder encoder=new PcmMp3Encoder(44100,2,new ByteArrayOutputStream())){encoder.encode(new byte[]{1,2,3},3,false);}}
}
