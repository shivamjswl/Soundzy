package app.cadence;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import okhttp3.*;

/** Downloads bounded ranges and validates every offset before publishing a complete file. */
public final class ResumableDownload {
    public interface Source { AudioSource get(boolean refresh)throws IOException; }
    public interface Progress { void update(long bytes,long total)throws IOException; }
    public interface Calls { void current(Call call); }
    private static final long CHUNK=2*1024*1024;
    private static final Pattern RANGE=Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)");
    private final OkHttpClient client;
    public ResumableDownload(OkHttpClient client){this.client=client.newBuilder().callTimeout(0,TimeUnit.MILLISECONDS).build();}
    public OkHttpClient client(){return client;}
    public static File partial(File target){return new File(target+".download");}
    public AudioSource save(Source source,File target,Progress progress,Calls calls)throws IOException {
        File part=partial(target),meta=new File(part+".meta");AudioSource audio=source.get(false);
        String old="";try{old=new String(Files.readAllBytes(meta.toPath()),java.nio.charset.StandardCharsets.UTF_8);}catch(IOException ignored){}
        if(audio.length()<=0||!old.equals(audio.fingerprint())){Files.deleteIfExists(part.toPath());}
        Files.write(meta.toPath(),audio.fingerprint().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long total=audio.length();int failures=0;boolean refresh=false;
        try(RandomAccessFile file=new RandomAccessFile(part,"rw")){
            if(total>0&&file.length()>total)file.setLength(0);
            while(total<=0||file.length()<total){
                progress.update(file.length(),total);
                if(refresh){
                    AudioSource next=source.get(true);refresh=false;
                    if(!next.fingerprint().equals(audio.fingerprint())){file.setLength(0);total=next.length();}
                    audio=next;Files.write(meta.toPath(),audio.fingerprint().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                long start=file.length(),end=total>0?Math.min(total-1,start+CHUNK-1):start+CHUNK-1;
                Request.Builder request=new Request.Builder().url(audio.url()).header("User-Agent",YouTubeRepository.USER_AGENT)
                    .header("Referer","https://www.youtube.com/").header("Accept-Encoding","identity").header("Range","bytes="+start+"-"+end);
                Call call=client.newCall(request.build());calls.current(call);
                try(Response response=call.execute()){
                    if(response.code()==403||response.code()==410){refresh=true;throw new IOException("The audio link expired. Requesting a fresh link.");}
                    if(response.code()==429)throw new IOException("YouTube is limiting downloads. Please retry later.");
                    if(!response.isSuccessful()||response.body()==null)throw new IOException("Audio download returned HTTP "+response.code());
                    String type=response.header("Content-Type","");if(type.startsWith("text/")||type.contains("json"))throw new IOException("The server did not return audio.");
                    long expected;
                    if(response.code()==206){
                        Matcher range=RANGE.matcher(response.header("Content-Range",""));
                        if(!range.matches())throw new IOException("The server returned an invalid audio range.");
                        long actualStart=parse(range.group(1)),actualEnd=parse(range.group(2)),actualTotal=parse(range.group(3));
                        if(actualStart!=start||actualEnd<actualStart||actualEnd>=actualTotal)throw new IOException("The server returned a different audio range.");
                        if(total>0&&total!=actualTotal){file.setLength(0);total=0;throw new IOException("The audio size changed. Restarting safely.");}
                        total=actualTotal;expected=actualEnd-actualStart+1;
                        if(response.body().contentLength()>=0&&response.body().contentLength()!=expected)throw new IOException("Audio response length did not match its range.");
                    }else if(response.code()==200){
                        // Servers that ignore Range must be restarted, never appended to an existing prefix.
                        if(start>0){file.setLength(0);start=0;}
                        long length=response.body().contentLength();
                        if(total>0&&length>0&&total!=length)throw new IOException("The audio response was incomplete.");
                        if(length>0)total=length;expected=length;
                    }else throw new IOException("Unexpected audio response: HTTP "+response.code());
                    file.seek(start);long received=0;
                    try(InputStream input=response.body().byteStream()){
                        byte[] buffer=new byte[64*1024];int n;
                        while((n=input.read(buffer))!=-1){
                            if(expected>=0&&received+n>expected)throw new IOException("The server sent more audio bytes than expected.");
                            file.write(buffer,0,n);received+=n;progress.update(file.length(),total);
                        }
                    }
                    if(received==0||expected>=0&&received!=expected)throw new IOException("The connection ended early. Resuming download.");
                    if(total<=0)total=file.length();failures=0;
                }catch(IOException failure){
                    progress.update(file.length(),total); // Cancellation is checked before retrying.
                    if(++failures>=5)throw new IOException("Download paused after repeated connection failures. Tap Retry to continue. "+failure.getMessage(),failure);
                    if(failures>=2)refresh=true;
                    try{Thread.sleep(Math.min(4000,250L<<failures));}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException("Download cancelled.",interrupted);}
                }finally{calls.current(null);}
            }
            if(file.length()==0||total>0&&file.length()!=total)throw new IOException("The audio file is incomplete.");
            file.getFD().sync();
        }
        Files.move(part.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);Files.deleteIfExists(meta.toPath());
        return audio;
    }
    private static long parse(String n)throws IOException{try{return Long.parseLong(n);}catch(NumberFormatException e){throw new IOException("Invalid audio length",e);}}
}
