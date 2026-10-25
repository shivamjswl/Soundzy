package app.cadence;

import android.app.Application;

public final class CadenceApp extends Application {
    public Library library;
    public YouTubeRepository youtube;
    public PlaylistSource playlists;
    public MusicCatalog catalog;
    public AudioEqualizer equalizer;
    public LyricsSource lyrics;
    @Override public void onCreate(){
        super.onCreate();library=new Library(this);library.recoverDownloads();
        java.io.File[] parts=new java.io.File(getFilesDir(),"audio").listFiles((dir,name)->name.endsWith(".part"));
        if(parts!=null)for(java.io.File part:parts){if(!part.delete())android.util.Log.w("Cadence","Could not clear an interrupted download fragment");}
        youtube=new YouTubeRepository();playlists=youtube;catalog=youtube;equalizer=new AudioEqualizer(this);lyrics=new LyricsRepository(new java.io.File(getFilesDir(),"lyrics"));
    }
    public static CadenceApp of(android.content.Context c){return (CadenceApp)c.getApplicationContext();}
}
