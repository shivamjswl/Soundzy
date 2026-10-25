package app.cadence;

import android.app.*;
import android.view.*;
import android.widget.*;
import androidx.media3.exoplayer.ExoPlayer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk=36,application=android.app.Application.class,qualifiers="w411dp-h914dp-420dpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class QueuePanelTest {
    private final Track first=new Track("testtone001","Moonlit drive","MuseDock test artist","",190),second=new Track("testtone002","A little closer","MuseDock test artist","",220),third=new Track("testtone003","Golden hour","MuseDock test artist","",180);
    private ExoPlayer player;
    @Before public void setup(){player=new ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build();player.setMediaItems(List.of(first.mediaItem(null),second.mediaItem(null),third.mediaItem(null)));}
    @After public void release(){player.release();}
    @Test public void separatesCurrentUpcomingAndEarlierTracks(){
        player.seekTo(1,0);var state=QueuePanel.snapshot(player);assertEquals(second,state.current().track());assertEquals(List.of(third),state.next().stream().map(QueuePanel.Item::track).toList());assertEquals(List.of(first),state.earlier().stream().map(QueuePanel.Item::track).toList());
    }
    @Test public void stableIdentitySurvivesRemovalAndReordering(){
        Object uid=QueuePanel.snapshot(player).next().get(1).uid();player.removeMediaItem(0);assertEquals(1,QueuePanel.indexOf(player,uid));player.moveMediaItem(1,0);assertEquals(0,QueuePanel.indexOf(player,uid));player.removeMediaItem(0);assertEquals(-1,QueuePanel.indexOf(player,uid));
    }
    @Test public void shuffleSnapshotContainsEachTrackOnce(){
        player.setShuffleModeEnabled(true);var state=QueuePanel.snapshot(player);Set<Object> ids=new HashSet<>();ids.add(state.current().uid());state.next().forEach(i->assertTrue(ids.add(i.uid())));state.earlier().forEach(i->assertTrue(ids.add(i.uid())));assertEquals(3,ids.size());
    }
    @Test public void emptyQueueHasNoCurrentTrack(){player.clearMediaItems();assertNull(QueuePanel.snapshot(player).current());assertTrue(QueuePanel.snapshot(player).next().isEmpty());}
    @Test public void rendersRichQueueWithArtworkAndSections()throws Exception{
        try(var controller=Robolectric.buildActivity(Activity.class)){
            Activity activity=controller.get();activity.setTheme(R.style.AppTheme);controller.setup();
            QueuePanel panel=new QueuePanel(activity,player,(track,size)->new RecordArt(activity));panel.show();Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertNotNull(dialog);View root=dialog.getWindow().getDecorView();
            assertTrue(hasText(root,"Your queue"));assertTrue(hasText(root,"NOW PLAYING"));assertTrue(hasText(root,"UP NEXT  ·  2"));assertTrue(hasText(root,"Moonlit drive"));
            root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2100,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,2100);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(1080,2100,android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(bitmap));
            File file=new File(System.getProperty("cadence.validation.dir"),"queue-framework.png");try(FileOutputStream out=new FileOutputStream(file)){assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
            panel.dismiss();
        }
    }
    private boolean hasText(View view,String text){if(view instanceof TextView t&&t.getText().toString().equals(text))return true;if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)if(hasText(group.getChildAt(i),text))return true;return false;}
}
