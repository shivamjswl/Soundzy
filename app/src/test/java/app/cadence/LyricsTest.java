package app.cadence;

import org.junit.Test;
import static org.junit.Assert.*;

public final class LyricsTest {
    @Test public void synchronizedLinesRespectFractionsRepeatedTagsAndOrdering(){
        Lyrics value=Lyrics.parse("","[ar:Test artist]\n[00:02.25]Second\n[00:01.5][00:03.125]First\n[01:70.0]Invalid",false);
        assertEquals(3,value.lines().size());assertEquals(1500,value.lines().get(0).milliseconds());assertEquals(2250,value.lines().get(1).milliseconds());assertEquals(3125,value.lines().get(2).milliseconds());
        assertEquals(-1,value.active(1499));assertEquals(0,value.active(1500));assertEquals(1,value.active(2250));assertEquals(2,value.active(9000));
    }
    @Test public void positiveOffsetAdvancesAndNegativeOffsetDelays(){
        assertEquals(500,Lyrics.parse("","[offset:+500]\n[00:01.00]Test",false).lines().get(0).milliseconds());
        assertEquals(1500,Lyrics.parse("","[offset:-500]\n[00:01.00]Test",false).lines().get(0).milliseconds());
    }
    @Test public void handlesPlainInstrumentalAndEmptyResults(){assertFalse(Lyrics.parse("Original test line",null,false).empty());assertFalse(Lyrics.parse(null,null,true).empty());assertTrue(Lyrics.parse(null,"[ar:Metadata]",false).empty());}
    @Test public void boundsLineCount(){StringBuilder source=new StringBuilder();for(int i=0;i<6000;i++)source.append("[00:01]Test\n");assertEquals(5000,Lyrics.parse("",source.toString(),false).lines().size());}
}
