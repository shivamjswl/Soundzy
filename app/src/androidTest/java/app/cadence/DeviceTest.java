package app.cadence;

import android.content.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.media3.common.*;
import androidx.media3.session.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

@RunWith(AndroidJUnit4.class)
public final class DeviceTest {
    private CadenceApp app;
    private String playlist;
    private final Track one=new Track("testtone001","Test tone one","Cadence tests","",2);
    private final Track two=new Track("testtone002","Test tone two","Cadence tests","",2);
    @Before public void setup(){app=CadenceApp.of(InstrumentationRegistry.getInstrumentation().getTargetContext());playlist=app.library.createPlaylist("Test "+System.nanoTime());}
    @After public void cleanup(){app.library.deletePlaylist(playlist);app.library.removeDownload(one.id());app.library.removeDownload(two.id());}
    @Test public void playlistOrderAndDuplicateMembershipSurviveDatabaseReopen(){
        app.library.add(playlist,one);app.library.add(playlist,two);app.library.add(playlist,one);
        assertEquals(2,app.library.playlistTracks(playlist).size());app.library.move(playlist,two.id(),-1);
        Library reopened=new Library(app);
        assertEquals(two.id(),reopened.playlistTracks(playlist).get(0).id());reopened.close();
        app.library.remove(playlist,one.id());assertEquals(1,app.library.playlistTracks(playlist).size());
    }
    @Test public void offlineFilesMustExistAndPlaylistDeletionKeepsDownloads()throws Exception{
        File file=fixture(one.id());app.library.download(one,"ready",file.getAbsolutePath(),"");app.library.add(playlist,one);
        app.library.deletePlaylist(playlist);assertEquals(file.getAbsolutePath(),app.library.offline(one.id()));
        assertTrue(file.delete());assertNull(app.library.offline(one.id()));
    }
    @Test public void interruptedDownloadsAreRetryable(){
        app.library.download(one,"downloading",null,"");app.library.recoverDownloads();
        var row=app.library.downloads().stream().filter(d->d.track().id().equals(one.id())).findFirst().orElseThrow();assertEquals("failed",row.status());assertTrue(row.error().contains("interrupted"));assertNull(app.library.offline(one.id()));
    }
    @Test public void mainScreenLaunchesAndRestoresAfterRecreation(){
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(activity->assertNotNull(activity.getWindow().getDecorView()));scenario.recreate();scenario.onActivity(activity->assertFalse(activity.isFinishing()));
        }
    }
    @Test public void offlinePlaybackContinuesWithoutAnActivity()throws Exception{
        File file=fixture(one.id());app.library.download(one,"ready",file.getAbsolutePath(),"");
        ActivityScenario<MainActivity> foreground=ActivityScenario.launch(MainActivity.class);
        AtomicReference<com.google.common.util.concurrent.ListenableFuture<MediaController>> future=new AtomicReference<>();
        var instrumentation=InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(()->future.set(new MediaController.Builder(app,new SessionToken(app,new ComponentName(app,PlaybackService.class))).buildAsync()));
        MediaController controller=future.get().get(45,TimeUnit.SECONDS);CountDownLatch ready=new CountDownLatch(1);AtomicReference<PlaybackException> failure=new AtomicReference<>();
        instrumentation.runOnMainSync(()->{
            controller.addListener(new Player.Listener(){@Override public void onIsPlayingChanged(boolean playing){if(playing)ready.countDown();}@Override public void onPlayerError(PlaybackException e){failure.set(e);ready.countDown();}});
            controller.setRepeatMode(Player.REPEAT_MODE_ONE);controller.setMediaItem(one.mediaItem(file.getAbsolutePath()));controller.prepare();controller.play();
        });
        try{
            assertTrue("Player did not become ready",ready.await(60,TimeUnit.SECONDS));assertNull("Offline audio failed: "+failure.get(),failure.get());
            instrumentation.runOnMainSync(()->assertTrue(controller.isPlaying()));
            foreground.close();
            instrumentation.runOnMainSync(()->assertTrue("Playback stopped when activity closed",controller.isPlaying()));
        }finally{foreground.close();instrumentation.runOnMainSync(()->{controller.stop();controller.clearMediaItems();controller.release();});}
    }
    private File fixture(String id)throws Exception{
        File dir=new File(app.getFilesDir(),"audio");assertTrue(dir.isDirectory()||dir.mkdirs());File f=new File(dir,id+".audio");
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("tone.wav");FileOutputStream out=new FileOutputStream(f)){in.transferTo(out);}return f;
    }
}
