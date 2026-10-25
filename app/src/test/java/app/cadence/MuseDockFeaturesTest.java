package app.cadence;

import android.app.*;
import android.content.ComponentName;
import android.view.*;
import android.widget.*;
import androidx.media3.exoplayer.ExoPlayer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.schabi.newpipe.extractor.*;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk={29,37},application=CadenceApp.class,qualifiers="w411dp-h914dp-420dpi")
@LooperMode(LooperMode.Mode.PAUSED)
public final class MuseDockFeaturesTest {
    private CadenceApp app;
    private MusicCatalog.Mode requested;
    private final Track song=new Track("testtone001","Original test song","Test artist","",120);
    @Before public void setup(){
        app=(CadenceApp)RuntimeEnvironment.getApplication();Shadows.shadowOf(app).grantPermissions(app.getPackageName()+".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");Shadows.shadowOf(app).declareComponentUnbindable(new ComponentName(app,PlaybackService.class));
        app.playlists=new PlaylistSource(){public List<Suggestion> suggest(String q){return List.of();}public Tracks playlist(String url,Page next){return new Tracks("Original test album",List.of(song),null);}};
        app.catalog=(query,next,mode)->{requested=mode;return mode==MusicCatalog.Mode.ALBUMS?new MusicCatalog.Result(List.of(),next==null?List.of(album("PL_first","Original test album")):List.of(album("PL_first","Original test album"),album("PL_second","Second test album")),next==null?new Page("next"):null,query):new MusicCatalog.Result(List.of(song),List.of(),null,query);};
    }
    private PlaylistSource.Suggestion album(String id,String name){return new PlaylistSource.Suggestion("https://www.youtube.com/playlist?list="+id,name,"Test artist","",1);}
    @After public void cleanup(){app.equalizer.release();app.library.close();}
    @Test public void musicFiltersReachTheMusicExtractor()throws Exception{
        for(MusicCatalog.Mode mode:List.of(MusicCatalog.Mode.SONGS,MusicCatalog.Mode.ALBUMS)){
            var query=ServiceList.YouTube.getSearchQHFactory().fromQuery("Test artist",List.of(mode.filter),"");assertTrue(query.getUrl().startsWith("https://music.youtube.com/"));assertTrue(ServiceList.YouTube.getSearchExtractor(query).getClass().getSimpleName().contains("Music"));
        }
        var regular=ServiceList.YouTube.getSearchQHFactory().fromQuery("Test",List.of(MusicCatalog.Mode.VIDEOS.filter),"");assertTrue(regular.getUrl().startsWith("https://www.youtube.com/"));
    }
    @Test public void albumBrowsingPagesDeduplicatesOpensAndSavesTracks()throws Exception{
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            View root=controller.get().getWindow().getDecorView();assertNotNull(find(root,"MuseDock"));click(root,"Browse albums");await(root,"Original test album");assertEquals(MusicCatalog.Mode.ALBUMS,requested);
            click(root,"Load more albums");await(root,"Second test album");assertEquals(1,count(root,"Original test album"));click(root,"Original test album");await(root,"Original test song");click(root,"Save 1 songs to Library");AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();idle();Library.Playlist saved=app.library.playlists().stream().filter(p->p.name().equals("Original test album")).findFirst().orElseThrow();assertEquals(List.of(song),app.library.playlistTracks(saved.id()));
        }
    }
    @Test public void songsDefaultAndVideoFallbackUseSeparateFilters()throws Exception{
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            View root=controller.get().getWindow().getDecorView();click(root,"Search");EditText input=input(root);input.setText("Test artist");input.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);await(root,"Original test song");assertEquals(MusicCatalog.Mode.SONGS,requested);click(root,"YouTube videos");await(root,"Original test song");assertEquals(MusicCatalog.Mode.VIDEOS,requested);
        }
    }
    @Test public void equalizerIsDiscoverableAndPresetControlsPersist(){
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            View root=controller.get().getWindow().getDecorView();View info=description(root,"App info");info.performClick();AlertDialog about=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();about.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();idle();AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View eq=dialog.getWindow().getDecorView();assertNotNull(find(eq,"Equalizer enabled"));click(eq,"Bass boost");assertEquals("Bass boost",app.equalizer.preset());find(eq,"Equalizer enabled").performClick();idle();assertTrue(app.equalizer.enabled());dialog.dismiss();
        }
    }
    @Test public void lyricsFollowSeekAndTrackChangesWithoutStaleText()throws Exception{
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            ExoPlayer player=new ExoPlayer.Builder(app).build();LyricsPanel panel=null;
            try{
                player.setMediaItem(song.mediaItem(null));LyricsSource source=(track,refresh)->Lyrics.parse("","[00:01]Original line for "+track.title()+"\n[00:05]Another original test line",false);panel=new LyricsPanel(controller.get(),()->player,source);panel.show();Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();View page=dialog.getWindow().getDecorView();await(page,"Original line for Original test song");assertNotNull(find(page,"Test artist · Synced lyrics · Tap a line to seek"));click(page,"Another original test line");assertEquals(0,player.getCurrentPosition());prepareSeekable(player,song);click(page,"Another original test line");assertEquals(5000,player.getCurrentPosition());
                prepareSeekable(player,new Track("testtone002","Second recording","Other artist","",100));Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(400,TimeUnit.MILLISECONDS);await(page,"Original line for Second recording");assertNull(find(page,"Original line for Original test song"));
            }finally{if(panel!=null)panel.dismiss();player.release();}
        }
    }
    @Test public void lyricsFailureCanBeRetriedAndPlainTextIsLabeled()throws Exception{
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            ExoPlayer player=new ExoPlayer.Builder(app).build();LyricsPanel panel=null;int[] requests={0};
            try{player.setMediaItem(song.mediaItem(null));panel=new LyricsPanel(controller.get(),()->player,(track,refresh)->{if(requests[0]++==0)throw new IOException("Test failure");return Lyrics.parse("Original plain test lyrics",null,false);});panel.show();View page=org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView();await(page,"Retry lyrics");click(page,"Retry lyrics");await(page,"Original plain test lyrics");assertNotNull(find(page,"Test artist · Plain lyrics · Timing unavailable"));}
            finally{if(panel!=null)panel.dismiss();player.release();}
        }
    }
    @Test public void lateLyricsFromPreviousTrackCannotReplaceCurrentTrack()throws Exception{
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            ExoPlayer player=new ExoPlayer.Builder(app).build();LyricsPanel panel=null;java.util.concurrent.CountDownLatch started=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
            try{player.setMediaItem(song.mediaItem(null));panel=new LyricsPanel(controller.get(),()->player,(track,refresh)->{if(track.id().equals(song.id())){started.countDown();release.await(8,TimeUnit.SECONDS);return Lyrics.parse("Old recording's original test lyric",null,false);}return Lyrics.parse("Current recording's original test lyric",null,false);});panel.show();View page=org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView();idle();assertTrue(started.await(3,TimeUnit.SECONDS));player.setMediaItem(new Track("testtone002","Changed track","Other artist","",100).mediaItem(null));release.countDown();Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(400,TimeUnit.MILLISECONDS);await(page,"Current recording's original test lyric");assertNull(find(page,"Old recording's original test lyric"));}
            finally{release.countDown();if(panel!=null)panel.dismiss();player.release();}
        }
    }
    @Test @Config(sdk=37) @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void newPanelsRenderWithOriginalTestLyrics()throws Exception{
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            EqualizerPanel eq=new EqualizerPanel(controller.get(),app.equalizer);eq.show();idle();capture(org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView(),"musedock-equalizer-framework.png");eq.dismiss();
            ExoPlayer player=new ExoPlayer.Builder(app).build();LyricsPanel lyrics=null;
            try{player.setMediaItem(song.mediaItem(null),1500);lyrics=new LyricsPanel(controller.get(),()->player,(track,refresh)->Lyrics.parse("","[00:00]Find a rhythm of your own\n[00:05]Let the quiet turn to sound\n[00:10]Take a breath and sing along\n[00:15]This is where your music lives",false));lyrics.show();View page=org.robolectric.shadows.ShadowDialog.getLatestDialog().getWindow().getDecorView();await(page,"Find a rhythm of your own");capture(page,"musedock-lyrics-framework.png");}finally{if(lyrics!=null)lyrics.dismiss();player.release();}
        }
    }
    private void capture(View root,String name)throws Exception{
        root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2400,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,2400);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(1080,2400,android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(bitmap));File dir=new File(System.getProperty("cadence.validation.dir"));assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
    }
    private void prepareSeekable(ExoPlayer player,Track track)throws Exception{
        androidx.media3.exoplayer.source.SilenceMediaSource source=new androidx.media3.exoplayer.source.SilenceMediaSource(track.duration()*1000000);source.updateMediaItem(track.mediaItem(null));player.setMediaSource(source);player.prepare();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(!player.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)&&System.nanoTime()<deadline){idle();Thread.sleep(10);}assertTrue("Prepared silence timeline should be seekable",player.isCommandAvailable(androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM));
    }
    private void idle(){Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();}
    private void await(View root,String value)throws Exception{long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);do{idle();if(find(root,value)!=null)return;Thread.sleep(10);}while(System.nanoTime()<deadline);fail("Missing UI text: "+value);}
    private TextView find(View root,String value){if(root instanceof TextView t&&t.getText().toString().equals(value))return t;if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){TextView v=find(group.getChildAt(i),value);if(v!=null)return v;}return null;}
    private int count(View root,String value){int count=root instanceof TextView t&&t.getText().toString().equals(value)?1:0;if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)count+=count(group.getChildAt(i),value);return count;}
    private EditText input(View root){if(root instanceof EditText e)return e;if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){EditText v=input(group.getChildAt(i));if(v!=null)return v;}return null;}
    private View description(View root,String value){if(value.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View v=description(group.getChildAt(i),value);if(v!=null)return v;}return null;}
    private void click(View root,String value){View v=find(root,value);assertNotNull(value,v);while(!v.isClickable()&&v.getParent() instanceof View parent)v=parent;assertTrue(v.performClick());idle();}
}
