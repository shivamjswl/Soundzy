package app.cadence;

import java.util.*;
import java.util.regex.*;

/** Line-synchronized LRC, including repeated timestamps and signed offset metadata. */
public record Lyrics(String plain, List<Line> lines, boolean instrumental) {
    public record Line(long milliseconds, String text) {}
    private static final Pattern STAMP=Pattern.compile("\\[(\\d{1,3}):([0-5]\\d)(?:[.:](\\d{1,3}))?\\]");
    private static final Pattern OFFSET=Pattern.compile("(?im)^\\[offset:([+-]?\\d{1,8})\\]\\s*$");
    public Lyrics { plain=plain==null?"":plain;lines=List.copyOf(lines); }
    public static Lyrics parse(String plain,String synced,boolean instrumental){
        List<Line> lines=new ArrayList<>();String source=synced==null?"":synced;long offset=0;
        Matcher meta=OFFSET.matcher(source);if(meta.find())offset=Long.parseLong(meta.group(1));
        for(String row:source.split("\\r?\\n")){
            Matcher stamps=STAMP.matcher(row);List<Long> times=new ArrayList<>();int end=0;
            while(stamps.find()){
                if(!row.substring(end,stamps.start()).isBlank())break;
                String part=stamps.group(3);long fraction=part==null?0:Long.parseLong((part+"000").substring(0,3));
                times.add(Math.max(0,Long.parseLong(stamps.group(1))*60000+Long.parseLong(stamps.group(2))*1000+fraction-offset));end=stamps.end();
            }
            if(times.isEmpty())continue;String text=row.substring(end).trim();
            for(long time:times){if(lines.size()>=5000)break;lines.add(new Line(time,text));}
            if(lines.size()>=5000)break;
        }
        lines.sort(Comparator.comparingLong(Line::milliseconds));return new Lyrics(plain,lines,instrumental);
    }
    public int active(long position){int low=0,high=lines.size()-1,found=-1;while(low<=high){int mid=(low+high)>>>1;if(lines.get(mid).milliseconds()<=position){found=mid;low=mid+1;}else high=mid-1;}return found;}
    public boolean empty(){return !instrumental&&lines.isEmpty()&&plain.isBlank();}
}
