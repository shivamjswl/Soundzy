package app.cadence;
import org.junit.Test;
import static org.junit.Assert.*;
public final class VideoIdsTest {
    private static final String ID="dQw4w9WgXcQ";
    @Test public void acceptsVideoId(){assertEquals(ID,VideoIds.parse(ID));}
    @Test public void acceptsSupportedYouTubeLinks(){
        for(String u:new String[]{"https://www.youtube.com/watch?v="+ID+"&t=42","https://youtu.be/"+ID+"?si=abc","https://music.youtube.com/watch?v="+ID,"https://m.youtube.com/shorts/"+ID,"https://www.youtube.com/embed/"+ID})assertEquals(u,ID,VideoIds.parse(u));
    }
    @Test public void rejectsUntrustedHosts(){for(String h:new String[]{"youtube.com.evil.test","notyoutube.com","evil.test"})assertNull(VideoIds.parse("https://"+h+"/watch?v="+ID));}
    @Test public void rejectsPathsAndMalformedIds(){for(String v:new String[]{"", "../../file", "file:///tmp/song", "https://youtu.be/", "https://youtube.com/watch?v=short", "javascript:alert(1)","https://youtube.com/playlist?list=123"})assertNull(VideoIds.parse(v));}
    @Test public void canonicalizesLink(){assertEquals("https://www.youtube.com/watch?v="+ID,VideoIds.url(ID));}
    @Test(expected=IllegalArgumentException.class) public void rejectsUnsafeFilename(){VideoIds.url("../../private");}
}
