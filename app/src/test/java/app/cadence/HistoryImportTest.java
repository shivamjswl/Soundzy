package app.cadence;

import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.io.*;
import java.nio.charset.StandardCharsets;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=36,application=android.app.Application.class)
public final class HistoryImportTest {
    private HistoryImport.Result read(String text)throws IOException{return HistoryImport.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));}
    @Test public void jsonImportsValidVideosAndKeepsMostRecentDuplicate()throws Exception{
        String text="""
            [
              {"title":"Watched First song","titleUrl":"https://www.youtube.com/watch?v=testtone001","subtitles":[{"name":"Artist one"}],"time":"2025-01-01T12:00:00Z"},
              {"title":"Watched Updated song","titleUrl":"https://youtu.be/testtone001","subtitles":[{"name":"Artist one"}],"time":"2026-01-01T12:00:00Z"},
              {"title":"Watched Second song","titleUrl":"https://www.youtube.com/watch?v=testtone002","time":"2025-02-01T12:00:00Z"},
              {"title":"Do not import","titleUrl":"https://evil.test/watch?v=testtone003"}
            ]
            """;
        var result=read(text);assertEquals(2,result.entries().size());assertEquals("Updated song",result.entries().get(0).track().title());assertEquals("Artist one",result.entries().get(0).track().artist());assertEquals("testtone002",result.entries().get(1).track().id());assertFalse(result.limited());
    }
    @Test public void htmlReadsLinksWithoutTreatingMarkupAsCode()throws Exception{
        var result=read("<html><div>Watched <a href='https://www.youtube.com/watch?v=testtone001'>Song &amp; title</a><br><a href='https://www.youtube.com/channel/UC_test'>Artist</a></div><a href='https://evil.test/watch?v=testtone002'>Skip</a></html>");
        assertEquals(1,result.entries().size());assertEquals("Song & title",result.entries().get(0).track().title());assertEquals("Artist",result.entries().get(0).track().artist());assertEquals(0,result.entries().get(0).playedAt());
    }
    @Test public void supportsUtf8BomAndMissingOptionalMetadata()throws Exception{
        var result=read("\ufeff[{\"title\":null,\"titleUrl\":\"https://youtu.be/testtone001\",\"subtitles\":null,\"time\":\"unknown\"}]");assertEquals(1,result.entries().size());assertEquals("Imported YouTube video",result.entries().get(0).track().title());
    }
    @Test public void capsLargeHistoriesAndReportsTheLimit()throws Exception{
        StringBuilder text=new StringBuilder("[");for(int i=0;i<5002;i++){if(i>0)text.append(',');text.append("{\"title\":\"Song\",\"titleUrl\":\"https://youtu.be/").append(String.format(java.util.Locale.ROOT,"%011d",i)).append("\"}");}text.append(']');
        var result=read(text.toString());assertEquals(5000,result.entries().size());assertTrue(result.limited());
    }
    @Test(expected=IOException.class)public void rejectsUnsupportedFileFormats()throws Exception{read("PK a zipped export");}
    @Test(expected=IOException.class)public void rejectsMalformedJson()throws Exception{read("[{\"titleUrl\":");}
}
