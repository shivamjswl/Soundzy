package app.cadence;

import android.net.Uri;
import androidx.media3.common.*;
import androidx.media3.datasource.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class PlaybackService extends MediaSessionService {
    private ExoPlayer player;
    private MediaSession session;
    @Override public void onCreate(){
        super.onCreate();
        CadenceApp app=CadenceApp.of(this);
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory()
            .setUserAgent(YouTubeRepository.USER_AGENT).setConnectTimeoutMs(20000).setReadTimeoutMs(30000)
            .setDefaultRequestProperties(java.util.Map.of("Referer","https://www.youtube.com/"));
        ResolvingDataSource.Factory resolving=new ResolvingDataSource.Factory(new DefaultDataSource.Factory(this,http),spec->{
            String id=VideoIds.parse(spec.uri.toString());
            if(id==null)return spec;
            String local=app.library.offline(id);
            return spec.withUri(local==null?Uri.parse(app.youtube.resolve(id)):Uri.fromFile(new java.io.File(local)));
        });
        player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(resolving)).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(),true);
        player.setHandleAudioBecomingNoisy(true);
        player.setWakeMode(C.WAKE_MODE_LOCAL);
        player.addListener(new Player.Listener(){
            @Override public void onAudioSessionIdChanged(int id){app.equalizer.attach(id);}
            @Override public void onMediaItemTransition(MediaItem item,int reason){
                if(item!=null){Track t=app.library.track(item.mediaId);if(t!=null)app.library.played(t);}
            }
            @Override public void onPlayerError(PlaybackException error){
                MediaItem current=player.getCurrentMediaItem();if(current!=null)app.youtube.invalidate(current.mediaId);
            }
        });
        session=new MediaSession.Builder(this,player).setCallback(new MediaSession.Callback(){
            @Override public MediaSession.ConnectionResult onConnect(MediaSession s,MediaSession.ControllerInfo controller){
                if(!controller.getPackageName().equals(getPackageName())&&!s.isMediaNotificationController(controller)&&!controller.isTrusted())return MediaSession.ConnectionResult.reject();
                return MediaSession.Callback.super.onConnect(s,controller);
            }
        }).build();
        session.setSessionActivity(android.app.PendingIntent.getActivity(this,0,new android.content.Intent(this,MainActivity.class),android.app.PendingIntent.FLAG_IMMUTABLE));
    }
    @Override public MediaSession onGetSession(MediaSession.ControllerInfo controller){return session;}
    @Override public void onTaskRemoved(android.content.Intent intent){
        if(!player.getPlayWhenReady()||player.getMediaItemCount()==0)stopSelf();
    }
    @Override public void onDestroy(){if(session!=null)session.release();if(player!=null)player.release();CadenceApp.of(this).equalizer.release();super.onDestroy();}
}
