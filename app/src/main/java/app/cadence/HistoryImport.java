package app.cadence;

import android.util.JsonReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** Reads only a user-selected Google Takeout history export; nothing is uploaded. */
public final class HistoryImport {
    public static final int MAX_ENTRIES=5000;
    private static final long MAX_BYTES=20L*1024*1024;
    public record Entry(Track track,long playedAt) {}
    public record Result(List<Entry> entries,boolean limited) {}
    private HistoryImport(){}
    public static Result read(InputStream input)throws IOException{
        BufferedInputStream buffered=new BufferedInputStream(new Bounded(input,MAX_BYTES));buffered.mark(4096);
        int ch;do{ch=buffered.read();}while(ch>=0&&Character.isWhitespace(ch));
        boolean bom=ch==0xef&&buffered.read()==0xbb&&buffered.read()==0xbf;
        if(bom)do{ch=buffered.read();}while(ch>=0&&Character.isWhitespace(ch));buffered.reset();if(bom){buffered.read();buffered.read();buffered.read();}
        if(ch=='[')return json(buffered);
        if(ch=='<')return html(buffered);
        throw new IOException("Choose watch-history.json or watch-history.html from Google Takeout. Unzip the export first.");
    }
    private static Result json(InputStream input)throws IOException{
        LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();boolean limited=false;
        try(JsonReader reader=new JsonReader(new InputStreamReader(input,StandardCharsets.UTF_8))){
            reader.beginArray();
            while(reader.hasNext()){
                String url="",title="",artist="",time="";
                reader.beginObject();
                while(reader.hasNext()){
                    String key=reader.nextName();
                    if(key.equals("titleUrl"))url=string(reader);
                    else if(key.equals("title"))title=string(reader);
                    else if(key.equals("time"))time=string(reader);
                    else if(key.equals("subtitles")&&reader.peek()==android.util.JsonToken.BEGIN_ARRAY){
                        reader.beginArray();while(reader.hasNext()){
                            if(reader.peek()!=android.util.JsonToken.BEGIN_OBJECT){reader.skipValue();continue;}
                            reader.beginObject();while(reader.hasNext()){String name=reader.nextName();if(name.equals("name")&&artist.isEmpty())artist=string(reader);else reader.skipValue();}reader.endObject();
                        }reader.endArray();
                    }else reader.skipValue();
                }reader.endObject();
                String id=VideoIds.parse(url);if(id==null)continue;
                Entry entry=entry(id,title,artist,parseTime(time));
                if(!entries.containsKey(id)&&entries.size()>=MAX_ENTRIES){limited=true;continue;}
                Entry old=entries.get(id);if(old==null||entry.playedAt()>old.playedAt())entries.put(id,entry);
            }reader.endArray();
        }catch(IllegalStateException|IllegalArgumentException e){throw new IOException("This file isn't a supported YouTube watch-history JSON export.",e);}
        return result(entries,limited);
    }
    private static String string(JsonReader reader)throws IOException{if(reader.peek()==android.util.JsonToken.STRING)return reader.nextString();reader.skipValue();return "";}
    private static Result html(InputStream input)throws IOException{
        LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();boolean limited=false;
        org.jsoup.nodes.Document document=Jsoup.parse(input,StandardCharsets.UTF_8.name(),"");
        for(Element link:document.select("a[href]")){
            String id=VideoIds.parse(link.attr("href"));if(id==null)continue;
            String title=link.text(),artist="";Element parent=link.parent();
            if(parent!=null)for(Element other:parent.select("a[href]")){String href=other.attr("href");if(href.startsWith("https://www.youtube.com/channel/")||href.startsWith("https://www.youtube.com/@")){artist=other.text();break;}}
            if(!entries.containsKey(id)&&entries.size()>=MAX_ENTRIES){limited=true;continue;}
            entries.putIfAbsent(id,entry(id,title,artist,0));
        }
        return result(entries,limited);
    }
    private static Entry entry(String id,String title,String artist,long played){
        String clean=title.replaceFirst("^Watched\\s+","").trim();if(clean.isEmpty())clean="Imported YouTube video";
        return new Entry(new Track(id,clean,artist.isBlank()?"YouTube history":artist,"https://i.ytimg.com/vi/"+id+"/hqdefault.jpg",0),played);
    }
    private static Result result(Map<String,Entry> entries,boolean limited){List<Entry> sorted=new ArrayList<>(entries.values());sorted.sort(Comparator.comparingLong(Entry::playedAt).reversed());return new Result(sorted,limited);}
    private static long parseTime(String text){try{return Math.max(0,Instant.parse(text).toEpochMilli());}catch(RuntimeException ignored){return 0;}}
    private static final class Bounded extends FilterInputStream {
        private long count;private final long max;
        Bounded(InputStream input,long max){super(input);this.max=max;}
        private void used(long n)throws IOException{if(n>0&&(count+=n)>max)throw new IOException("This history file exceeds 20 MB. Export a smaller date range from Google Takeout.");}
        @Override public int read()throws IOException{int value=super.read();used(value<0?0:1);return value;}
        @Override public int read(byte[] buffer,int offset,int length)throws IOException{int n=in.read(buffer,offset,length);used(n);return n;}
    }
}
