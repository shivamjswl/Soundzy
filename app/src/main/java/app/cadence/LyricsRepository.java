package app.cadence;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import com.grack.nanojson.*;

/** Exact-metadata lookups only; no arbitrary first search result presented as matching lyrics. */
public final class LyricsRepository implements LyricsSource {
    private static final int LIMIT=1024*1024;
    private final OkHttpClient client;
    private final HttpUrl endpoint;
    private final File directory;
    public LyricsRepository(File directory){this(directory,new OkHttpClient.Builder().connectTimeout(12,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(25,TimeUnit.SECONDS).build(),HttpUrl.get("https://lrclib.net/api/get"));}
    LyricsRepository(File directory,OkHttpClient client,HttpUrl endpoint){this.directory=directory;this.client=client;this.endpoint=endpoint;}
    static String title(String value){return value.replaceAll("(?i)\\s*[\\[(](?:official\\s+(?:music\\s+)?(?:video|audio)|lyrics?|lyric\\s+video|audio)[\\])]"," ").trim();}
    static String artist(String value){return value.replaceFirst("(?i)\\s*-\\s*Topic$","").trim();}
    @Override public synchronized Lyrics find(Track track,boolean refresh)throws Exception {
        String title=title(track.title()),artist=artist(track.artist());
        if(title.isBlank()||artist.isBlank())throw new IOException("Song title and artist are needed to match lyrics.");
        String key=hex(MessageDigest.getInstance("SHA-256").digest((track.id()+"\n"+title+"\n"+artist+"\n"+track.duration()).getBytes(StandardCharsets.UTF_8)));
        File cached=new File(directory,key+".json");
        if(!refresh&&cached.isFile()&&cached.length()<=LIMIT){try{Lyrics value=decode(Files.readAllBytes(cached.toPath()),title,artist,track.duration());if(!value.empty())return value;}catch(Exception ignored){/* Try the provider after a corrupt cache. */}}
        HttpUrl.Builder url=endpoint.newBuilder().addQueryParameter("track_name",title).addQueryParameter("artist_name",artist);
        if(track.duration()>0)url.addQueryParameter("duration",Long.toString(track.duration()));
        byte[] data;
        try(Response response=client.newCall(new Request.Builder().url(url.build()).header("User-Agent","MuseDock/0.6.0 (https://github.com/shivamjswl/Soundzy)").build()).execute()){
            if(response.code()==404)return new Lyrics("",List.of(),false);
            if(!response.isSuccessful()||response.body()==null)throw new IOException("Lyrics provider is unavailable. Try again later.");
            if(response.body().contentLength()>LIMIT)throw new IOException("The lyrics response is too large.");
            try(InputStream input=response.body().byteStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(out.size()+count>LIMIT)throw new IOException("The lyrics response is too large.");out.write(buffer,0,count);}data=out.toByteArray();
            }
        }
        Lyrics result=decode(data,title,artist,track.duration());
        if(!result.empty()){
            try{if(directory.isDirectory()||directory.mkdirs()){File temp=new File(directory,key+".part");try{Files.write(temp.toPath(),data);Files.move(temp.toPath(),cached.toPath(),StandardCopyOption.REPLACE_EXISTING);trim();}finally{Files.deleteIfExists(temp.toPath());}}}catch(IOException ignored){/* Lyrics can still display if device storage is full. */}
        }
        return result;
    }
    private Lyrics decode(byte[] data,String title,String artist,long duration)throws Exception {
        JsonObject value=JsonParser.object().from(new String(data,StandardCharsets.UTF_8));
        if(!normal(title).equals(normal(value.getString("trackName","")))||!normal(artist).equals(normal(value.getString("artistName",""))))return new Lyrics("",List.of(),false);
        if(duration>0&&value.has("duration")&&Math.abs(value.getDouble("duration")-duration)>10)return new Lyrics("",List.of(),false);
        return Lyrics.parse(value.getString("plainLyrics",""),value.getString("syncedLyrics",""),value.getBoolean("instrumental",false));
    }
    private static String normal(String value){return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]","");}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte value:bytes)s.append(String.format(Locale.ROOT,"%02x",value&255));return s.toString();}
    private void trim(){File[] files=directory.listFiles((dir,name)->name.endsWith(".json"));if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));long bytes=0;for(File f:files)bytes+=f.length();int left=files.length;for(File f:files){if(left<=100&&bytes<=20L*1024*1024)break;long length=f.length();if(f.delete()){left--;bytes-=length;}}}
}
