package app.cadence;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.io.File;
import java.util.*;

public final class Library extends SQLiteOpenHelper {
    public record Playlist(String id, String name, int count) {}
    public record Download(Track track, String status, String path, String error) {}
    private final File audioDirectory;
    public Library(Context context) { super(context, "cadence.db", null, 1); audioDirectory=new File(context.getFilesDir(),"audio"); }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tracks(id TEXT PRIMARY KEY,title TEXT NOT NULL,artist TEXT NOT NULL,artwork TEXT NOT NULL,duration INTEGER NOT NULL,played INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE playlists(id TEXT PRIMARY KEY,name TEXT NOT NULL)");
        db.execSQL("CREATE TABLE members(playlist TEXT NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,track TEXT NOT NULL REFERENCES tracks(id),position INTEGER NOT NULL,PRIMARY KEY(playlist,track))");
        db.execSQL("CREATE TABLE downloads(track TEXT PRIMARY KEY REFERENCES tracks(id),status TEXT NOT NULL,path TEXT,error TEXT NOT NULL DEFAULT '')");
        db.execSQL("INSERT INTO playlists VALUES('liked','Liked songs')");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("Unsupported schema upgrade"); }
    public synchronized void remember(Track t) {
        ContentValues v = new ContentValues(); v.put("id",t.id()); v.put("title",t.title()); v.put("artist",t.artist()); v.put("artwork",t.artwork()); v.put("duration",t.duration());
        SQLiteDatabase db = getWritableDatabase();
        if (db.update("tracks",v,"id=?",new String[]{t.id()}) == 0) db.insertOrThrow("tracks",null,v);
    }
    public synchronized Track track(String id) {
        try (Cursor c=getReadableDatabase().rawQuery("SELECT id,title,artist,artwork,duration FROM tracks WHERE id=?",new String[]{id})) { return c.moveToFirst()?read(c):null; }
    }
    private Track read(Cursor c) { return new Track(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4)); }
    public synchronized List<Track> recent() { return tracks("SELECT id,title,artist,artwork,duration FROM tracks WHERE played>0 ORDER BY played DESC LIMIT 30",new String[]{}); }
    public synchronized void played(Track t) { remember(t); ContentValues v=new ContentValues();v.put("played",System.currentTimeMillis());getWritableDatabase().update("tracks",v,"id=?",new String[]{t.id()}); }
    public synchronized List<Track> tracks(String sql,String[] args) {
        List<Track> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery(sql,args)){ while(c.moveToNext())out.add(read(c)); }return out;
    }
    public synchronized List<Playlist> playlists() {
        List<Playlist> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT p.id,p.name,COUNT(m.track) FROM playlists p LEFT JOIN members m ON p.id=m.playlist GROUP BY p.id ORDER BY CASE WHEN p.id='liked' THEN 0 ELSE 1 END,p.name COLLATE NOCASE",null)) {
            while(c.moveToNext())out.add(new Playlist(c.getString(0),c.getString(1),c.getInt(2)));
        } return out;
    }
    public synchronized String createPlaylist(String name) {
        if(name.trim().isEmpty() || name.trim().length()>80)throw new IllegalArgumentException("Use a name between 1 and 80 characters");
        String id=UUID.randomUUID().toString();ContentValues v=new ContentValues();v.put("id",id);v.put("name",name.trim());getWritableDatabase().insertOrThrow("playlists",null,v);return id;
    }
    public synchronized String importPlaylist(String name,List<Track> tracks) {
        if(tracks.isEmpty())throw new IllegalArgumentException("Load some songs before saving this playlist.");
        String title=name==null||name.trim().isEmpty()?"YouTube playlist":name.trim();
        if(title.length()>80)title=title.substring(0,80);
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            String id=createPlaylist(title);
            for(Track track:tracks)add(id,track);
            db.setTransactionSuccessful();return id;
        }finally{db.endTransaction();}
    }
    public synchronized String importHistory(List<HistoryImport.Entry> entries){
        if(entries.isEmpty())throw new IllegalArgumentException("Choose at least one song.");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            List<Track> tracks=new ArrayList<>();
            for(HistoryImport.Entry entry:entries){Track existing=track(entry.track().id());tracks.add(existing==null?entry.track():existing);}
            String id=importPlaylist("YouTube listening history",tracks);
            for(HistoryImport.Entry entry:entries)if(entry.playedAt()>0)db.execSQL("UPDATE tracks SET played=MAX(played,?) WHERE id=?",new Object[]{entry.playedAt(),entry.track().id()});
            db.setTransactionSuccessful();return id;
        }finally{db.endTransaction();}
    }
    public synchronized void rename(String id,String name) {
        if(id.equals("liked"))return;
        if(name.trim().isEmpty()||name.trim().length()>80)throw new IllegalArgumentException("Use a name between 1 and 80 characters");
        ContentValues v=new ContentValues();v.put("name",name.trim());getWritableDatabase().update("playlists",v,"id=?",new String[]{id});
    }
    public synchronized void deletePlaylist(String id) { if(!id.equals("liked"))getWritableDatabase().delete("playlists","id=?",new String[]{id}); }
    public synchronized List<Track> playlistTracks(String id) { return tracks("SELECT t.id,t.title,t.artist,t.artwork,t.duration FROM members m JOIN tracks t ON t.id=m.track WHERE m.playlist=? ORDER BY m.position,m.rowid",new String[]{id}); }
    public synchronized boolean contains(String playlist,String track) { try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM members WHERE playlist=? AND track=?",new String[]{playlist,track})){return c.moveToFirst();} }
    public synchronized void add(String playlist,Track t) {
        remember(t); SQLiteDatabase db=getWritableDatabase();
        db.execSQL("INSERT OR IGNORE INTO members(playlist,track,position) SELECT ?,?,COALESCE(MAX(position),-1)+1 FROM members WHERE playlist=?",new Object[]{playlist,t.id(),playlist});
    }
    public synchronized void remove(String playlist,String track){ getWritableDatabase().delete("members","playlist=? AND track=?",new String[]{playlist,track}); }
    public synchronized void move(String playlist,String id,int offset) {
        List<Track> list=playlistTracks(playlist);int from=-1;for(int i=0;i<list.size();i++)if(list.get(i).id().equals(id))from=i;
        if(from<0||from+offset<0||from+offset>=list.size())return;
        Collections.swap(list,from,from+offset);SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{
            for(int i=0;i<list.size();i++)db.execSQL("UPDATE members SET position=? WHERE playlist=? AND track=?",new Object[]{i,playlist,list.get(i).id()});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public synchronized void download(Track t,String status,String path,String error){
        remember(t); ContentValues v=new ContentValues();v.put("track",t.id());v.put("status",status);v.put("path",path);v.put("error",error==null?"":error);
        getWritableDatabase().insertWithOnConflict("downloads",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public synchronized List<Download> downloads(){
        List<Download> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT t.id,t.title,t.artist,t.artwork,t.duration,d.status,d.path,d.error FROM downloads d JOIN tracks t ON t.id=d.track ORDER BY d.rowid DESC",null)){
            while(c.moveToNext())out.add(new Download(read(c),c.getString(5),c.getString(6),c.getString(7)));
        }return out;
    }
    public synchronized String offline(String id){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT path FROM downloads WHERE track=? AND status IN ('ready','converting')",new String[]{id})){
            if(c.moveToFirst()&&c.getString(0)!=null){File f=new File(c.getString(0));if(f.isFile()&&f.length()>0)return f.getAbsolutePath();}
        }return null;
    }
    public synchronized boolean removeDownload(String id){
        String path=offline(id); if(path!=null){File f=new File(path);if(f.exists()&&!f.delete())return false;}
        if(VideoIds.valid(id))for(String suffix:new String[]{".source",".source.download",".source.download.meta",".mp3.part",".audio",".m4a",".webm",".mp3",".ogg",".wav",".aac"}){
            File fragment=new File(audioDirectory,id+suffix);if(fragment.exists()&&!fragment.delete())return false;
        }
        getWritableDatabase().delete("downloads","track=?",new String[]{id});return true;
    }
    public synchronized void recoverDownloads(){getWritableDatabase().execSQL("UPDATE downloads SET status=CASE WHEN path IS NULL THEN 'failed' ELSE 'ready' END,error='MP3 conversion interrupted. Original audio is kept when available.' WHERE status='converting'");getWritableDatabase().execSQL("UPDATE downloads SET status='failed',error='Download interrupted. Tap Retry to resume with a fresh connection.' WHERE status IN ('queued','downloading')");}
}
