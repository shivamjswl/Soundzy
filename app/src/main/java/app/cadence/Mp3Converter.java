package app.cadence;

import android.media.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

/** Decodes with Android's codecs and converts to MP3 locally, without an upload service. */
public final class Mp3Converter {
    private Mp3Converter(){}
    public interface Progress { void update(long positionUs,long durationUs)throws IOException; }
    public static void validate(File file)throws IOException{
        MediaExtractor extractor=new MediaExtractor();
        try{extractor.setDataSource(file.getAbsolutePath());boolean found=false;for(int i=0;i<extractor.getTrackCount();i++){String type=extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);if(type!=null&&type.startsWith("audio/")){found=true;break;}}if(!found)throw new IOException("The downloaded file contains no playable audio.");}
        finally{extractor.release();}
    }
    public static void convert(File source,File destination,Progress progress)throws IOException{
        MediaExtractor extractor=new MediaExtractor();MediaCodec decoder=null;PcmMp3Encoder encoder=null;
        File temporary=new File(destination+".part");boolean converted=false;
        try(FileOutputStream out=new FileOutputStream(temporary)){
            extractor.setDataSource(source.getAbsolutePath());int track=-1;MediaFormat format=null;
            for(int i=0;i<extractor.getTrackCount();i++){MediaFormat f=extractor.getTrackFormat(i);String mime=f.getString(MediaFormat.KEY_MIME);if(mime!=null&&mime.startsWith("audio/")){track=i;format=f;break;}}
            if(track<0||format==null)throw new IOException("This file has no audio to convert.");
            extractor.selectTrack(track);long duration=format.containsKey(MediaFormat.KEY_DURATION)?format.getLong(MediaFormat.KEY_DURATION):0;
            String mime=format.getString(MediaFormat.KEY_MIME);format.setInteger(MediaFormat.KEY_PCM_ENCODING,AudioFormat.ENCODING_PCM_16BIT);
            decoder=MediaCodec.createDecoderByType(mime);decoder.configure(format,null,null,0);decoder.start();
            boolean inputEnded=false,outputEnded=false;long position=0;byte[] samples=new byte[32768];long lastOutput=System.nanoTime();int pcmEncoding=AudioFormat.ENCODING_PCM_16BIT;
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
            while(!outputEnded){
                progress.update(position,duration);
                if(!inputEnded){int index=decoder.dequeueInputBuffer(10000);if(index>=0){ByteBuffer buffer=decoder.getInputBuffer(index);int count=extractor.readSampleData(buffer,0);if(count<0){decoder.queueInputBuffer(index,0,0,0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);inputEnded=true;}else{decoder.queueInputBuffer(index,0,count,extractor.getSampleTime(),0);extractor.advance();}}}
                int index=decoder.dequeueOutputBuffer(info,10000);
                if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){MediaFormat actual=decoder.getOutputFormat();pcmEncoding=actual.containsKey(MediaFormat.KEY_PCM_ENCODING)?actual.getInteger(MediaFormat.KEY_PCM_ENCODING):AudioFormat.ENCODING_PCM_16BIT;}
                else if(index>=0){
                    lastOutput=System.nanoTime();
                    try{
                        if(info.size>0&&(info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0){
                            MediaFormat actual=decoder.getOutputFormat();
                            if(pcmEncoding!=AudioFormat.ENCODING_PCM_16BIT&&pcmEncoding!=AudioFormat.ENCODING_PCM_FLOAT)throw new IOException("The device returned an unsupported PCM format.");
                            if(encoder==null)encoder=new PcmMp3Encoder(actual.getInteger(MediaFormat.KEY_SAMPLE_RATE),actual.getInteger(MediaFormat.KEY_CHANNEL_COUNT),out);
                            ByteBuffer pcm=decoder.getOutputBuffer(index);if(pcm==null)throw new IOException("The decoder returned no audio samples.");pcm.position(info.offset);pcm.limit(info.offset+info.size);if(samples.length<info.size)samples=new byte[info.size];pcm.get(samples,0,info.size);encoder.encode(samples,info.size,pcmEncoding==AudioFormat.ENCODING_PCM_FLOAT);
                            position=Math.max(0,info.presentationTimeUs);progress.update(position,duration);
                        }
                        outputEnded=(info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0;
                    }finally{decoder.releaseOutputBuffer(index,false);}
                }
                if(System.nanoTime()-lastOutput>java.util.concurrent.TimeUnit.SECONDS.toNanos(30))throw new IOException("The device's audio decoder stopped responding.");
            }
            if(encoder==null)throw new IOException("The decoder produced no audio samples.");encoder.finish();out.getFD().sync();converted=true;
        }catch(RuntimeException e){throw new IOException("MP3 conversion failed on this device. The original audio has been kept when available.",e);}
        finally{if(encoder!=null)encoder.close();if(decoder!=null){try{decoder.stop();}catch(RuntimeException ignored){}decoder.release();}extractor.release();if(!converted&&temporary.exists()&&!temporary.delete())android.util.Log.w("Soundzy","Could not remove an interrupted MP3 conversion");}
        try{Files.move(temporary.toPath(),destination.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temporary.toPath());}
    }
}
