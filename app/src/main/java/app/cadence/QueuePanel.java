package app.cadence;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import java.util.*;

/** A live queue view keyed by timeline UIDs, so edits cannot target stale positions. */
public final class QueuePanel {
    public interface Artwork {View create(Track track,int size);}
    public record Item(Object uid,int index,Track track){}
    public record Snapshot(Item current,List<Item> next,List<Item> earlier){}
    private static final int BG=0xff111711,FG=0xfff4f6ef,MUTED=0xffa5b09f,GREEN=0xffb8f36d,INK=0xff15200d;
    private final Activity activity;private final Player player;private final Artwork artwork;
    private Dialog dialog;private LinearLayout root;
    private final Player.Listener listener=new Player.Listener(){@Override public void onEvents(Player p,Player.Events events){if(dialog!=null&&dialog.isShowing())render();}};
    public QueuePanel(Activity activity,Player player,Artwork artwork){this.activity=activity;this.player=player;this.artwork=artwork;}
    public static Snapshot snapshot(Player player){
        Timeline timeline=player.getCurrentTimeline();if(timeline.isEmpty())return new Snapshot(null,List.of(),List.of());
        int current=player.getCurrentMediaItemIndex();Set<Integer> visited=new HashSet<>();List<Item> next=new ArrayList<>(),earlier=new ArrayList<>();visited.add(current);
        int at=current;
        while((at=timeline.getNextWindowIndex(at,Player.REPEAT_MODE_OFF,player.getShuffleModeEnabled()))!=C.INDEX_UNSET&&visited.add(at))next.add(item(player,at));
        for(int i=0;i<timeline.getWindowCount();i++)if(!visited.contains(i))earlier.add(item(player,i));
        return new Snapshot(item(player,current),next,earlier);
    }
    private static Item item(Player player,int index){return new Item(player.getCurrentTimeline().getWindow(index,new Timeline.Window()).uid,index,Track.from(player.getMediaItemAt(index)));}
    public static int indexOf(Player player,Object uid){Timeline t=player.getCurrentTimeline();Timeline.Window w=new Timeline.Window();for(int i=0;i<t.getWindowCount();i++)if(Objects.equals(uid,t.getWindow(i,w).uid))return i;return C.INDEX_UNSET;}
    public void show(){
        dialog=new Dialog(activity);root=column();root.setPadding(dp(20),dp(14),dp(20),dp(12));root.setBackground(round(BG,26));dialog.setContentView(root);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setGravity(Gravity.BOTTOM);
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets nav=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(20),dp(14),dp(20),dp(12)+nav.bottom);return insets;});
        dialog.setOnDismissListener(d->player.removeListener(listener));player.addListener(listener);render();dialog.show();
        dialog.getWindow().setLayout(-1,(int)(activity.getResources().getDisplayMetrics().heightPixels*.86));root.requestApplyInsets();
    }
    public void dismiss(){if(dialog!=null)dialog.dismiss();}
    private void render(){
        root.removeAllViews();View handle=new View(activity);handle.setBackground(round(0xff56624f,3));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(36),dp(4));hp.gravity=Gravity.CENTER_HORIZONTAL;root.addView(handle,hp);space(root,14);
        LinearLayout top=row();top.addView(text("Your queue",27,FG,true),new LinearLayout.LayoutParams(0,-2,1));top.addView(icon("close",FG,"Close queue",this::dismiss));root.addView(top);space(root,4);
        Snapshot state=snapshot(player);root.addView(text(player.getMediaItemCount()+" tracks · Your listening session",12,MUTED,false));space(root,15);
        if(state.current()==null){root.addView(text("A little room for more music",21,FG,true));space(root,8);root.addView(text("Add songs from Search or a playlist to build your queue.",14,MUTED,false));return;}
        LinearLayout mode=row();TextView shuffle=action(player.getShuffleModeEnabled()?"Shuffle on":"Shuffle off",()->player.setShuffleModeEnabled(!player.getShuffleModeEnabled()));mode.addView(shuffle);TextView clear=action("Clear queue",()->new AlertDialog.Builder(activity).setTitle("Clear your queue?").setMessage("Playback will stop. Your playlists stay in Library.").setPositiveButton("Clear",(d,w)->{player.stop();player.clearMediaItems();render();}).setNegativeButton("Keep",null).show());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(44));cp.setMarginStart(dp(10));mode.addView(clear,cp);root.addView(mode);space(root,16);
        ScrollView scroll=new ScrollView(activity);scroll.setVerticalScrollBarEnabled(false);LinearLayout list=column();scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        heading(list,"NOW PLAYING");trackRow(list,state.current(),true);space(list,18);heading(list,"UP NEXT  ·  "+state.next().size());
        if(state.next().isEmpty()){TextView empty=text("You're all caught up. Add another favorite.",14,MUTED,false);empty.setPadding(0,dp(12),0,dp(20));list.addView(empty);}
        else for(Item item:state.next())trackRow(list,item,false);
        if(!state.earlier().isEmpty()){space(list,16);heading(list,"EARLIER IN YOUR QUEUE");for(Item item:state.earlier())trackRow(list,item,false);}
        space(root,8);root.addView(text("Tap a song to play · Open its menu to reorder",11,MUTED,false));
    }
    private void trackRow(LinearLayout parent,Item item,boolean current){
        Track track=item.track();LinearLayout card=row();card.setPadding(dp(10),dp(9),dp(2),dp(9));card.setBackground(round(current?0xff2c3e25:0xff1b2419,16));
        card.addView(artwork.create(track,48),new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout copy=column();copy.setPadding(dp(12),0,dp(6),0);
        TextView title=text(track.title(),15,current?GREEN:FG,true);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);copy.addView(title);space(copy,4);TextView artist=text(track.artist(),12,MUTED,false);artist.setMaxLines(1);artist.setEllipsize(TextUtils.TruncateAt.END);copy.addView(artist);card.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        if(current)card.addView(icon(player.isPlaying()?"pause":"play",GREEN,player.isPlaying()?"Pause current song":"Play current song",()->{if(player.isPlaying())player.pause();else{if(player.getPlaybackState()==Player.STATE_IDLE)player.prepare();player.play();}}));
        Icons more=icon("more",MUTED,"Queue options for "+track.title(),null);more.setOnClickListener(v->options(more,item));card.addView(more);
        card.setContentDescription("Play queued song "+track.title());card.setOnClickListener(v->{int index=indexOf(player,item.uid());if(index!=C.INDEX_UNSET){player.seekTo(index,0);if(player.getPlaybackState()==Player.STATE_IDLE)player.prepare();player.play();}});
        parent.addView(card,new LinearLayout.LayoutParams(-1,-2));space(parent,8);
    }
    private void options(View anchor,Item item){
        PopupMenu menu=new PopupMenu(activity,anchor);menu.getMenu().add(0,1,0,"Move earlier");menu.getMenu().add(0,2,1,"Move later");menu.getMenu().add(0,3,2,"Remove from queue");
        menu.setOnMenuItemClickListener(action->{int index=indexOf(player,item.uid());if(index==C.INDEX_UNSET)return true;
            if(action.getItemId()==3)player.removeMediaItem(index);
            else{int to=index+(action.getItemId()==1?-1:1);if(to>=0&&to<player.getMediaItemCount()){if(player.getShuffleModeEnabled()){player.setShuffleModeEnabled(false);Toast.makeText(activity,"Shuffle off · Using your chosen order",Toast.LENGTH_SHORT).show();}player.moveMediaItem(index,to);}}
            render();return true;});menu.show();
    }
    private void heading(LinearLayout parent,String label){TextView t=text(label,11,GREEN,true);t.setLetterSpacing(.12f);parent.addView(t);space(parent,10);}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(activity);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(activity);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String text,int size,int color,boolean bold){TextView t=new TextView(activity);t.setText(text);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Icons icon(String kind,int color,String label,Runnable action){Icons i=new Icons(activity,kind,color);i.setContentDescription(label);i.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));if(action!=null)i.setOnClickListener(v->action.run());return i;}
    private TextView action(String label,Runnable action){TextView t=text(label,12,FG,true);t.setPadding(dp(16),dp(12),dp(16),dp(12));t.setGravity(Gravity.CENTER);t.setBackground(round(0xff2a3526,22));t.setOnClickListener(v->action.run());return t;}
    private void space(LinearLayout parent,int n){parent.addView(new View(activity),new LinearLayout.LayoutParams(1,dp(n)));}
}
