package app.cadence;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.core.view.*;
import androidx.media3.common.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** A live lyrics surface tied to the current timeline item, not a stale song snapshot. */
public final class LyricsPanel {
    private final Activity activity;
    private final Supplier<Player> player;
    private final LyricsSource source;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final List<TextView> rows=new ArrayList<>();
    private Dialog dialog; private LinearLayout content; private ScrollView scroll;
    private TextView heading, detail, follow; private Lyrics lyrics;
    private String key=""; private int generation,active=-2; private boolean following=true,closed;
    private Future<?> job;
    public LyricsPanel(Activity activity,Supplier<Player> player,LyricsSource source){this.activity=activity;this.player=player;this.source=source;}
    public void show(){
        dialog=new Dialog(activity);LinearLayout page=new LinearLayout(activity);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(22),dp(12),dp(22),dp(18));page.setBackgroundColor(0xff30412b);
        LinearLayout top=new LinearLayout(activity);top.setGravity(Gravity.CENTER_VERTICAL);Button close=new Button(activity);close.setText("Close");close.setContentDescription("Close lyrics");close.setOnClickListener(v->dismiss());top.addView(close);
        follow=text("Following song",13,0xffb8f36d);follow.setPadding(dp(18),dp(14),dp(18),dp(14));follow.setOnClickListener(v->{following=!following;follow.setText(following?"Following song":"Follow song");active=-2;updatePosition();});top.addView(follow);page.addView(top);
        heading=text("Lyrics",24,0xfff4f6ef);heading.setTypeface(null,Typeface.BOLD);page.addView(heading);detail=text("",13,0xffc1cdbb);page.addView(detail);
        scroll=new ScrollView(activity){@Override public boolean onInterceptTouchEvent(MotionEvent event){if(event.getAction()==MotionEvent.ACTION_DOWN){following=false;follow.setText("Follow song");}return super.onInterceptTouchEvent(event);}};scroll.setFillViewport(true);scroll.setSmoothScrollingEnabled(true);content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,dp(28),0,dp(200));scroll.addView(content);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView credit=text("Lyrics by LRCLIB · Tap to visit",12,0xffc1cdbb);credit.setPadding(0,dp(8),0,dp(8));credit.setOnClickListener(v->{try{activity.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://lrclib.net")));}catch(ActivityNotFoundException ignored){}});page.addView(credit);
        page.addView(text("Title, artist and duration are sent to LRCLIB for matching. Loaded lyrics are cached for offline use.",11,0xffc1cdbb));
        dialog.setContentView(page);dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);WindowCompat.setDecorFitsSystemWindows(dialog.getWindow(),false);
        ViewCompat.setOnApplyWindowInsetsListener(page,(v,insets)->{androidx.core.graphics.Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());v.setPadding(dp(22)+bars.left,dp(12)+bars.top,dp(22)+bars.right,dp(18)+bars.bottom);return insets;});
        dialog.setOnDismissListener(d->{closed=true;generation++;handler.removeCallbacksAndMessages(null);if(job!=null)job.cancel(true);worker.shutdownNow();});dialog.show();dialog.getWindow().setLayout(-1,-1);page.requestApplyInsets();
        handler.post(new Runnable(){public void run(){if(closed)return;updateTrack(false);updatePosition();handler.postDelayed(this,350);}});
    }
    public void dismiss(){if(dialog!=null)dialog.dismiss();}
    private Track current(){Player p=player.get();return p==null||p.getCurrentMediaItem()==null?null:Track.from(p.getCurrentMediaItem());}
    private void updateTrack(boolean refresh){
        Track track=current();if(track==null){if(!key.equals("none")){key="none";generation++;lyrics=null;rows.clear();content.removeAllViews();content.addView(text("Play a song to see its lyrics.",25,0xfff4f6ef));}return;}
        String nextKey=track.id()+"\n"+track.title()+"\n"+track.artist();if(!refresh&&key.equals(nextKey))return;
        key=nextKey;heading.setText(track.title());detail.setText(track.artist()+" · Finding lyrics…");lyrics=null;rows.clear();active=-2;content.removeAllViews();content.addView(text("Finding your words…",26,0xfff4f6ef));following=true;follow.setText("Following song");scroll.scrollTo(0,0);
        int version=++generation;if(job!=null)job.cancel(true);
        job=worker.submit(()->{
            try{Lyrics result=source.find(track,refresh);handler.post(()->{if(closed||version!=generation||!matches(track))return;lyrics=result;draw();});}
            catch(Exception error){handler.post(()->{if(closed||version!=generation||!matches(track))return;content.removeAllViews();detail.setText(track.artist());content.addView(text("Couldn't load lyrics. Check your connection and try again.",23,0xfff4f6ef));retry();});}
        });
    }
    private boolean matches(Track track){Track now=current();return now!=null&&track.id().equals(now.id())&&track.title().equals(now.title())&&track.artist().equals(now.artist());}
    private void draw(){
        content.removeAllViews();rows.clear();Track track=current();detail.setText((track==null?"":track.artist()+" · ")+(lyrics.instrumental()?"Instrumental":lyrics.lines().isEmpty()?"Plain lyrics · Timing unavailable":"Synced lyrics · Tap a line to seek"));
        if(lyrics.instrumental())content.addView(text("Just the music.\nThis track is marked instrumental.",28,0xfff4f6ef));
        else if(lyrics.empty()){content.addView(text("Lyrics aren't available for this recording yet.",28,0xfff4f6ef));content.addView(text("A different upload or version may not have a matching lyric entry.",14,0xffc1cdbb));retry();}
        else if(lyrics.lines().isEmpty())content.addView(text(lyrics.plain(),28,0xfff4f6ef));
        else for(Lyrics.Line line:lyrics.lines()){
            TextView view=text(line.text().isEmpty()?"♪":line.text(),28,0xff9cae92);view.setTypeface(null,Typeface.BOLD);view.setPadding(0,dp(10),0,dp(10));view.setContentDescription("Lyric at "+line.milliseconds()+" milliseconds: "+line.text());view.setOnClickListener(v->{Player p=player.get();Track now=current();if(now==null||!(now.id()+"\n"+now.title()+"\n"+now.artist()).equals(key)){updateTrack(false);return;}if(p!=null&&p.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)){p.seekTo(line.milliseconds());following=true;follow.setText("Following song");active=-2;updatePosition();}});content.addView(view);rows.add(view);
        }
        active=-2;updatePosition();
    }
    private void retry(){Button button=new Button(activity);button.setText("Retry lyrics");button.setOnClickListener(v->updateTrack(true));content.addView(button);}
    private void updatePosition(){
        Player p=player.get();if(p==null||lyrics==null||rows.isEmpty())return;
        int next=lyrics.active(p.getCurrentPosition());if(next==active)return;
        int old=active;active=next;if(old==-2)for(TextView row:rows)row.setTextColor(0xff9cae92);if(old>=0&&old<rows.size())rows.get(old).setTextColor(0xff9cae92);
        if(next>=0&&next<rows.size()){TextView selected=rows.get(next);selected.setTextColor(0xfff4f6ef);if(following)scroll.post(()->{if(!closed&&following)scroll.smoothScrollTo(0,Math.max(0,selected.getTop()-scroll.getHeight()/3));});}
    }
    private TextView text(String value,int size,int color){TextView v=new TextView(activity);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(dp(4),1);return v;}
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}
