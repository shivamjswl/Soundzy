package app.cadence;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Recognize the downloaded container so exported files have useful names and MIME types. */
public final class AudioFiles {
    private AudioFiles(){}
    public static String extension(File file)throws IOException{
        byte[] head=new byte[12];int size;try(InputStream in=new FileInputStream(file)){size=in.read(head);}
        if(size>=12&&new String(head,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(head,8,4,StandardCharsets.US_ASCII).equals("WAVE"))return "wav";
        if(size>=8&&new String(head,4,4,StandardCharsets.US_ASCII).equals("ftyp"))return "m4a";
        if(size>=4&&(head[0]&255)==0x1a&&(head[1]&255)==0x45&&(head[2]&255)==0xdf&&(head[3]&255)==0xa3)return "webm";
        if(size>=4&&new String(head,0,4,StandardCharsets.US_ASCII).equals("OggS"))return "ogg";
        if(size>=3&&new String(head,0,3,StandardCharsets.US_ASCII).equals("ID3"))return "mp3";
        if(size>=2&&(head[0]&255)==255&&(head[1]&0xe0)==0xe0)return (head[1]&6)!=0?"mp3":"aac";
        return "audio";
    }
    public static String mime(String extension){return switch(extension){case "mp3"->"audio/mpeg";case "m4a"->"audio/mp4";case "webm"->"audio/webm";case "wav"->"audio/wav";case "ogg"->"audio/ogg";case "aac"->"audio/aac";default->"application/octet-stream";};}
}
