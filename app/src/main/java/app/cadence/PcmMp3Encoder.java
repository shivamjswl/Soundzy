package app.cadence;

import java.io.*;
import de.sciss.jump3r.mp3.*;
import de.sciss.jump3r.mpg.MPGLib;
import de.sciss.jump3r.mpg.Common;

/** Small Android-compatible adapter around jump3r's LAME core; no Java Sound dependency. */
public final class PcmMp3Encoder implements AutoCloseable {
    private final Lame lame=new Lame();
    private final LameGlobalFlags flags;
    private final OutputStream output;
    private final int channels;
    private final int[] left=new int[2048],right=new int[2048];
    private final byte[] encoded=new byte[16384];
    private boolean finished;
    public PcmMp3Encoder(int sampleRate,int channels,OutputStream output)throws IOException{
        if(channels<1||channels>2||sampleRate<8000||sampleRate>192000)throw new IOException("Unsupported audio format for MP3 conversion.");
        this.channels=channels;this.output=output;
        GainAnalysis gain=new GainAnalysis();BitStream bits=new BitStream();Presets presets=new Presets();QuantizePVT qp=new QuantizePVT();Quantize quantize=new Quantize();VBRTag vbr=new VBRTag();Version version=new Version();ID3Tag tags=new ID3Tag();Reservoir reservoir=new Reservoir();Takehiro takehiro=new Takehiro();
        MPGLib mpg=new MPGLib();de.sciss.jump3r.mpg.Interface decoder=new de.sciss.jump3r.mpg.Interface();Common common=new Common();
        lame.setModules(gain,bits,presets,qp,quantize,vbr,version,tags,mpg);bits.setModules(gain,mpg,version,vbr);tags.setModules(bits,version);presets.setModules(lame);
        quantize.setModules(bits,reservoir,qp,takehiro);qp.setModules(takehiro,reservoir,lame.enc.psy);reservoir.setModules(bits);takehiro.setModules(qp);vbr.setModules(lame,bits,version);mpg.setModules(decoder,common);decoder.setModules(vbr,common);
        flags=lame.lame_init();flags.num_channels=channels;flags.in_samplerate=sampleRate;flags.out_samplerate=44100;flags.brate=192;flags.quality=5;flags.mode=channels==1?MPEGMode.MONO:MPEGMode.JOINT_STEREO;flags.bWriteVbrTag=false;flags.findReplayGain=false;
        tags.id3tag_init(flags);flags.write_id3tag_automatic=false;
        if(lame.lame_init_params(flags)<0){lame.lame_close(flags);throw new IOException("MP3 encoder could not initialize.");}
    }
    public void encode(byte[] pcm,int length,boolean floatPcm)throws IOException{
        if(finished)throw new IOException("MP3 conversion has finished.");
        int width=floatPcm?4:2,frame=width*channels;
        if(length%frame!=0)throw new IOException("The decoder returned incomplete PCM frames.");
        java.nio.ByteBuffer buffer=java.nio.ByteBuffer.wrap(pcm,0,length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        while(buffer.hasRemaining()){
            int count=Math.min(2048,buffer.remaining()/frame);
            for(int i=0;i<count;i++){
                left[i]=sample(buffer,floatPcm)<<16;
                right[i]=channels==2?sample(buffer,floatPcm)<<16:left[i];
            }
            write(lame.lame_encode_buffer_int(flags,left,right,count,encoded,0,encoded.length));
        }
    }
    private int sample(java.nio.ByteBuffer buffer,boolean floating){if(!floating)return buffer.getShort();float f=buffer.getFloat();return Math.round(Math.max(-1f,Math.min(1f,Float.isFinite(f)?f:0))*32767);}
    private void write(int count)throws IOException{if(count<0)throw new IOException("MP3 encoding failed ("+count+").");if(count>0)output.write(encoded,0,count);}
    public void finish()throws IOException{if(!finished){write(lame.lame_encode_flush(flags,encoded,0,encoded.length));finished=true;}}
    @Override public void close(){lame.lame_close(flags);}
}
