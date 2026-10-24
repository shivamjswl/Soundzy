package app.cadence;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import okhttp3.Call;

public final class OfflineService extends Service {
    public static final String CHANGED="app.cadence.LIBRARY_CHANGED";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final java.util.Set<String> pending=ConcurrentHashMap.newKeySet(),cancelled=ConcurrentHashMap.newKeySet();
    private volatile String active;
    private volatile Call call;
    private volatile boolean stopping;
    private int latestStart;
    private long notificationAt;
    public static void enqueue(Context context,Track track){enqueue(context,track,false);}
    public static void enqueue(Context context,Track track,boolean mp3){
        CadenceApp.of(context).library.remember(track);
        context.startForegroundService(new Intent(context,OfflineService.class).setAction("download").putExtra("id",track.id()).putExtra("mp3",mp3));
    }
    public static void cancel(Context context,String id){context.startService(new Intent(context,OfflineService.class).setAction("cancel").putExtra("id",id));}
    @Override public void onCreate(){super.onCreate();getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("offline","Offline downloads",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        latestStart=startId;
        if(intent==null){stopSelf(startId);return START_NOT_STICKY;}
        String id=intent.getStringExtra("id");if(!VideoIds.valid(id)){stopSelf(startId);return START_NOT_STICKY;}
        Library db=CadenceApp.of(this).library;Track track=db.track(id);if(track==null){stopSelf(startId);return START_NOT_STICKY;}
        if("cancel".equals(intent.getAction())){cancelled.add(id);if(id.equals(active)&&call!=null)call.cancel();if(pending.isEmpty())stopSelf(startId);return START_NOT_STICKY;}
        boolean mp3=intent.getBooleanExtra("mp3",false);String existing=db.offline(id);
        if(pending.isEmpty())startForeground(42,notification("Preparing offline audio…",0,0,id,false));
        if(existing!=null&&(!mp3||existing.endsWith(".mp3"))){if(pending.isEmpty()){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}return START_NOT_STICKY;}
        if(!pending.add(id))return START_NOT_STICKY;
        cancelled.remove(id);db.download(track,existing==null?"queued":"converting",existing,"");changed();
        worker.execute(()->runDownload(track,mp3,existing));return START_NOT_STICKY;
    }
    private void checkCancelled(String id)throws IOException{if(stopping||cancelled.contains(id)||Thread.currentThread().isInterrupted())throw new IOException("Download cancelled.");}
    private void runDownload(Track track,boolean mp3,String existing){
        String id=track.id();active=id;Library db=CadenceApp.of(this).library;
        File original=existing==null?null:new File(existing);PowerManager.WakeLock wake=null;
        try{
            checkCancelled(id);wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Soundzy:OfflineAudio");wake.acquire(60*60*1000L);
            File dir=new File(getFilesDir(),"audio");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Could not create offline storage.");
            if(original==null){
                db.download(track,"downloading",null,"");changed();File source=new File(dir,id+".source");
                if(!source.isFile()){
                    ResumableDownload downloader=new ResumableDownload(CadenceApp.of(this).youtube.client);
                    downloader.save(refresh->CadenceApp.of(this).youtube.audio(id,refresh),source,(bytes,total)->{
                        checkCancelled(id);if(dir.getUsableSpace()<16*1024*1024L)throw new IOException("Device storage is full. Free space and tap Retry.");
                        report(track.title(),bytes,total,id,false);
                    },current->call=current);
                }
                try{Mp3Converter.validate(source);}catch(IOException invalid){Files.deleteIfExists(source.toPath());throw invalid;}
                original=source;
            }
            checkCancelled(id);
            if(mp3){
                db.download(track,"converting",original.getAbsolutePath(),"");changed();report("Converting to MP3 · "+track.title(),0,0,id,true);
                File destination=new File(dir,id+".mp3");
                Mp3Converter.convert(original,destination,(position,duration)->{checkCancelled(id);if(dir.getUsableSpace()<16*1024*1024L)throw new IOException("Not enough storage to finish MP3 conversion.");report("Converting to MP3 · "+track.title(),position,duration,id,true);});
                Mp3Converter.validate(destination);db.download(track,"ready",destination.getAbsolutePath(),"");
                if(!original.equals(destination))Files.deleteIfExists(original.toPath());
            }else{
                File destination=new File(dir,id+"."+AudioFiles.extension(original));Files.move(original.toPath(),destination.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);db.download(track,"ready",destination.getAbsolutePath(),"");
            }
        }catch(Exception failure){
            CadenceApp.of(this).youtube.invalidate(id);
            String message=cancelled.contains(id)?"Cancelled. Tap Retry to continue downloading.":failure.getMessage()==null?"Download failed. Tap Retry to continue.":failure.getMessage();
            if(original!=null&&original.isFile()&&original.length()>0)db.download(track,"ready",original.getAbsolutePath(),"MP3 conversion did not finish. Original audio is available offline. "+message);
            else db.download(track,"failed",null,message);
        }finally{
            if(wake!=null&&wake.isHeld())wake.release();call=null;active=null;pending.remove(id);cancelled.remove(id);changed();
            new Handler(getMainLooper()).post(()->{if(pending.isEmpty()){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(latestStart);}});
        }
    }
    private void report(String title,long value,long total,String id,boolean conversion){long now=System.currentTimeMillis();if(now-notificationAt>700){notificationAt=now;getSystemService(NotificationManager.class).notify(42,notification(title,value,total,id,conversion));}}
    private Notification notification(String title,long value,long total,String id,boolean conversion){
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class).putExtra("offline",true),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent cancel=PendingIntent.getService(this,1,new Intent(this,OfflineService.class).setAction("cancel").putExtra("id",id),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        int percent=total>0?(int)Math.min(100,value*100/total):0;
        return new Notification.Builder(this,"offline").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(total>0?percent+"% · "+(conversion?"Converting on this device":"Downloading audio"):conversion?"Converting on this device":"Saving audio on this device")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setProgress(100,percent,total<=0).addAction(new Notification.Action.Builder(null,"Cancel",cancel).build()).build();
    }
    private void changed(){sendBroadcast(new Intent(CHANGED).setPackage(getPackageName()));}
    @Override public void onTimeout(int startId,int fgsType){stopping=true;if(call!=null)call.cancel();stopSelf();}
    @Override public void onDestroy(){
        stopping=true;if(call!=null)call.cancel();worker.shutdownNow();Library db=CadenceApp.of(this).library;
        for(String id:pending)if(!id.equals(active)){Track track=db.track(id);if(track!=null){String file=db.offline(id);db.download(track,file==null?"failed":"ready",file,"Interrupted. Tap Retry to continue.");}}
        changed();super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
