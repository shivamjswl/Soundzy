package app.cadence;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.session.*;
import androidx.media3.session.MediaController;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final int BG=0xff101310,SURFACE=0xff1c211b,RAISED=0xff272e25,FG=0xfff4f6ef,MUTED=0xffa5b09f,GREEN=0xffb8f36d,INK=0xff15200d;
    private CadenceApp app;
    private LinearLayout root,body,mini,nav;
    private ScrollView scroll;
    private MediaController player;
    private ListenableFuture<MediaController> connection;
    private final ExecutorService network=Executors.newSingleThreadExecutor();
    private final ExecutorService files=Executors.newSingleThreadExecutor();
    private final ExecutorService images=Executors.newFixedThreadPool(3);
    private final android.util.LruCache<String,Bitmap> imageCache=new android.util.LruCache<String,Bitmap>(12*1024*1024){@Override protected int sizeOf(String k,Bitmap v){return v.getByteCount();}};
    private final Handler handler=new Handler(Looper.getMainLooper());
    private String tab="home",playlistId=null,playlistName="",query="";
    private final ArrayList<Track> results=new ArrayList<>();
    private org.schabi.newpipe.extractor.Page nextPage;
    private boolean searching=false;
    private int searchVersion=0;
    private String searchError="";
    private String resultQuery="";
    private LinearLayout suggestionsContainer;
    private final ArrayList<PlaylistSource.Suggestion> suggestions=new ArrayList<>();
    private boolean suggestionsLoading=false;
    private String suggestionsError="",suggestionsQuery="",suggestionFilter="For you";
    private int suggestionsVersion=0,remoteVersion=0;
    private Future<?> suggestionsJob,remoteJob;
    private PlaylistSource.Suggestion remotePlaylist;
    private final ArrayList<Track> remoteTracks=new ArrayList<>();
    private org.schabi.newpipe.extractor.Page remoteNext;
    private boolean remoteLoading=false;
    private String remoteError="",remoteBack="home";
    private Dialog nowDialog;
    private QueuePanel queuePanel;
    private GoogleAccount googleAccount;
    private boolean closing=false;
    private String pendingExport;
    private static final int PICK_HISTORY=81,EXPORT_AUDIO=82;
    private TextView nowTitle,nowArtist,nowStatus,elapsed,total,miniTitle,miniArtist;
    private ImageView nowArt;
    private FrameLayout nowArtFrame;
    private SeekBar seek;
    private Icons playIcon,likeIcon,shuffleIcon,repeatIcon,miniPlay;
    private boolean dragging=false;
    private final BroadcastReceiver downloadsChanged=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(tab.equals("offline"))render();}};
    private final Player.Listener listener=new Player.Listener(){
        @Override public void onEvents(Player p,Player.Events events){updateMini();updateNow();}
        @Override public void onPlayerError(PlaybackException error){toast("Playback failed. Check your connection, then tap Play to retry.");}
    };
    private final Runnable ticker=new Runnable(){public void run(){updateNow();handler.postDelayed(this,500);}};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);app=CadenceApp.of(this);
        if(state!=null){tab=state.getString("tab","home");query=state.getString("query","");playlistId=state.getString("playlist");playlistName=state.getString("playlistName","");pendingExport=state.getString("pendingExport");}
        if(getIntent().getBooleanExtra("offline",false))tab="offline";
        getWindow().getDecorView().setSystemUiVisibility(0);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        render();
        connection=new MediaController.Builder(this,new SessionToken(this,new ComponentName(this,PlaybackService.class))).buildAsync();
        connection.addListener(()->{if(closing||isDestroyed())return;try{player=connection.get();player.addListener(listener);updateMini();}catch(Exception e){toast("Could not connect to the audio player. Restart Cadence to retry.");}},getMainExecutor());
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::goBack);
        registerReceiver(downloadsChanged,new IntentFilter(OfflineService.CHANGED),Context.RECEIVER_NOT_EXPORTED);
        handler.post(ticker);
        handler.post(()->loadSuggestions(recommendationQuery()));
    }
    @Override protected void onSaveInstanceState(Bundle s){super.onSaveInstanceState(s);s.putString("tab",tab);s.putString("query",query);s.putString("playlist",playlistId);s.putString("playlistName",playlistName);s.putString("pendingExport",pendingExport);}
    @Override protected void onDestroy(){
        closing=true;if(googleAccount!=null)googleAccount.cancel();if(queuePanel!=null)queuePanel.dismiss();
        handler.removeCallbacksAndMessages(null);unregisterReceiver(downloadsChanged);network.shutdownNow();files.shutdownNow();images.shutdownNow();
        if(player!=null)player.removeListener(listener);if(connection!=null)MediaController.releaseFuture(connection);if(nowDialog!=null)nowDialog.dismiss();super.onDestroy();
    }
    private void goBack(){
        if(tab.equals("youtube-playlist")){tab=remoteBack;render();}
        else if(playlistId!=null){playlistId=null;render();}
        else if(!tab.equals("home")){tab="home";render();}
        else finish();
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable border(int color,int radius){GradientDrawable d=bg(color,radius);d.setStroke(dp(1),0xff323b2e);return d;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");if(bold)t.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.BOLD));return t;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=text(s,size,color,bold);t.setMaxLines(1);t.setEllipsize(TextUtils.TruncateAt.END);return t;}
    private void space(LinearLayout l,int n){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
    private void add(LinearLayout parent,View v,int height){parent.addView(v,new LinearLayout.LayoutParams(-1,height<0?height:dp(height)));}
    private void inset(LinearLayout l,int n){l.setPadding(dp(n),dp(n),dp(n),dp(n));}
    private Icons icon(String name,int color,Runnable action){Icons i=new Icons(this,name,color);i.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));if(action!=null){i.setOnClickListener(v->action.run());i.setBackground(selectable());}return i;}
    private android.graphics.drawable.Drawable selectable(){android.util.TypedValue out=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,out,true);return getDrawable(out.resourceId);}
    private TextView button(String title,boolean primary,Runnable action){TextView b=label(title,14,primary?INK:FG,true);b.setGravity(Gravity.CENTER);b.setMinHeight(dp(48));b.setPadding(dp(18),dp(10),dp(18),dp(10));b.setBackground(primary?bg(GREEN,26):border(SURFACE,26));b.setOnClickListener(v->action.run());return b;}
    private void section(String title,String action,Runnable click){LinearLayout r=row();r.addView(text(title,21,FG,true),new LinearLayout.LayoutParams(0,-2,1));if(action!=null)r.addView(button(action,false,click));add(body,r,56);}
    private void navigate(String target){tab=target;playlistId=null;hideKeyboard();render();}
    private void render(){
        root=column();root.setBackgroundColor(BG);setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets sys=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(sys.left,sys.top,sys.right,sys.bottom);return insets;});root.requestApplyInsets();
        LinearLayout header=row();header.setPadding(dp(16),dp(6),dp(12),dp(4));
        Icons mark=icon("wave",GREEN,null);header.addView(mark);TextView brand=text("Soundzy",25,FG,true);brand.setLetterSpacing(-.04f);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));Icons queue=icon("queue",MUTED,this::showQueue);queue.setContentDescription("View playback queue");header.addView(queue);Icons info=icon("settings",MUTED,this::showAbout);info.setContentDescription("App info");header.addView(info);add(root,header,64);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        body=column();body.setPadding(dp(22),dp(12),dp(22),dp(28));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        switch(tab){case "search"->searchScreen();case "library"->{if(playlistId==null)libraryScreen();else playlistScreen();}case "offline"->offlineScreen();case "youtube-playlist"->remotePlaylistScreen();case "account"->accountScreen();default->homeScreen();}
        mini=column();mini.setPadding(dp(10),0,dp(10),dp(4));add(root,mini,-2);updateMini();
        nav=row();nav.setPadding(dp(8),dp(4),dp(8),dp(6));nav.setBackgroundColor(BG);
        String[][] tabs={{"home","home","Listen"},{"search","search","Search"},{"library","library","Library"},{"offline","download","Offline"}};
        for(String[] t:tabs){LinearLayout item=column();item.setGravity(Gravity.CENTER);boolean active=t[0].equals(tab)||(t[0].equals("home")&&tab.equals("youtube-playlist"));Icons i=icon(t[1],active?GREEN:MUTED,null);item.addView(i,new LinearLayout.LayoutParams(dp(32),dp(30)));TextView name=label(t[2],11,active?GREEN:MUTED,active);name.setGravity(Gravity.CENTER);item.addView(name);item.setContentDescription(t[2]);item.setOnClickListener(v->navigate(t[0]));nav.addView(item,new LinearLayout.LayoutParams(0,dp(56),1));}
        add(root,nav,-2);
    }
    private void homeScreen(){
        TextView eyebrow=text("A LITTLE LESS NOISE. A LOT MORE MUSIC.",10,GREEN,true);eyebrow.setLetterSpacing(.12f);add(body,eyebrow,-2);space(body,12);
        TextView headline=text("Find your\nfrequency.",44,FG,true);headline.setLineSpacing(0,.93f);headline.setLetterSpacing(-.055f);add(body,headline,-2);space(body,22);
        FrameLayout hero=new FrameLayout(this);GradientDrawable gradient=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff293c23,0xff1b291c,0xff192017});gradient.setCornerRadius(dp(24));hero.setBackground(gradient);
        RecordArt art=new RecordArt(this);FrameLayout.LayoutParams ap=new FrameLayout.LayoutParams(dp(215),dp(215),Gravity.END|Gravity.CENTER_VERTICAL);ap.setMarginEnd(-dp(28));hero.addView(art,ap);
        LinearLayout copy=column();copy.setPadding(dp(22),dp(23),dp(12),dp(20));TextView tag=text("YOUR DAILY SOUNDTRACK",10,GREEN,true);tag.setLetterSpacing(.12f);copy.addView(tag);space(copy,12);copy.addView(text("Good music.\nYour rules.",26,FG,true));space(copy,8);copy.addView(text("Find a song. Make it yours.",12,0xffc3d0bc,false));space(copy,15);TextView explore=button("Find your next favorite  →",true,()->navigate("search"));copy.addView(explore,new LinearLayout.LayoutParams(-2,dp(46)));hero.addView(copy,new FrameLayout.LayoutParams(-1,-1));add(body,hero,234);space(body,22);
        suggestionsContainer=column();add(body,suggestionsContainer,-2);drawSuggestions();space(body,18);
        section("Made for your mood",null,null);add(body,text("Pick a starting point. Discover something good.",13,MUTED,false),-2);space(body,14);
        String[][] moods={{"Late-night focus","Slow down. Tune in.","lofi beats music", "#39483B"},{"On repeat","The songs you come back to","indie pop music", "#4A3948"},{"Golden hour","A little warmth for your day","soul acoustic music", "#54432C"},{"Turn it up","Find your second wind","workout electronic music", "#293F50"}};
        for(int j=0;j<4;j+=2){LinearLayout r=row();for(int k=j;k<j+2;k++){String[] m=moods[k];LinearLayout card=column();inset(card,16);card.setBackground(bg(android.graphics.Color.parseColor(m[3]),18));Icons music=icon("music",GREEN,null);card.addView(music);space(card,14);card.addView(text(m[0],17,FG,true));space(card,4);card.addView(text(m[1],11,0xffc0c7bd,false));card.setOnClickListener(v->startSearch(m[2],false));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(164),1);if(k==j)cp.rightMargin=dp(12);r.addView(card,cp);}add(body,r,-2);space(body,12);}
        space(body,10);section("Recently played",null,null);List<Track> recent=app.library.recent();
        if(recent.isEmpty())empty("Your soundtrack starts here","Search for a favorite song. Your listening history will appear here.","search","Explore music",()->navigate("search"));
        else for(Track t:recent)trackRow(body,t,recent,false);
    }
    private String recommendationQuery(){
        List<Track> recent=app.library.recent();
        return recent.isEmpty()||recent.get(0).artist().isBlank()?"popular music playlists":recent.get(0).artist()+" music playlists";
    }
    private void drawSuggestions(){
        if(suggestionsContainer==null||!tab.equals("home"))return;
        suggestionsContainer.removeAllViews();
        LinearLayout title=row();title.addView(text("YouTube playlists",21,FG,true),new LinearLayout.LayoutParams(0,-2,1));
        Icons refresh=icon("repeat",GREEN,()->loadSuggestions(suggestionsQuery.isBlank()?recommendationQuery():suggestionsQuery));refresh.setContentDescription("Refresh YouTube playlists");title.addView(refresh);suggestionsContainer.addView(title);
        suggestionsContainer.addView(text(suggestionFilter.equals("For you")&&!app.library.recent().isEmpty()?"Inspired by your recent listening.":"Public playlists for your next listening session.",13,MUTED,false));space(suggestionsContainer,12);
        HorizontalScrollView filters=new HorizontalScrollView(this);filters.setHorizontalScrollBarEnabled(false);LinearLayout chips=row();
        for(String name:List.of("For you","Hindi","Chill","Workout")){
            TextView chip=button(name,name.equals(suggestionFilter),()->{suggestionFilter=name;loadSuggestions(switch(name){case "Hindi"->"Hindi Bollywood music playlists";case "Chill"->"chill lofi music playlists";case "Workout"->"workout music playlists";default->recommendationQuery();});});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(48));lp.setMarginEnd(dp(8));chips.addView(chip,lp);
        }
        filters.addView(chips);suggestionsContainer.addView(filters);space(suggestionsContainer,14);
        if(suggestionsLoading){LinearLayout loading=row();ProgressBar progress=new ProgressBar(this);loading.addView(progress,new LinearLayout.LayoutParams(dp(22),dp(22)));loading.addView(text("  Finding playlists on YouTube…",13,MUTED,false));suggestionsContainer.addView(loading);return;}
        if(!suggestionsError.isEmpty()){
            suggestionsContainer.addView(text(suggestionsError,13,MUTED,false));space(suggestionsContainer,10);suggestionsContainer.addView(button("Retry playlists",false,()->loadSuggestions(suggestionsQuery)),new LinearLayout.LayoutParams(-2,dp(48)));return;
        }
        if(suggestions.isEmpty()){suggestionsContainer.addView(text("Choose a mood to find public YouTube playlists.",13,MUTED,false));return;}
        HorizontalScrollView rail=new HorizontalScrollView(this);rail.setHorizontalScrollBarEnabled(false);LinearLayout cards=row();
        for(PlaylistSource.Suggestion playlist:suggestions){
            LinearLayout card=column();card.setBackground(bg(SURFACE,18));inset(card,12);
            Track cover=new Track("artwork0001",playlist.title(),playlist.author(),playlist.artwork(),0);
            card.addView(artwork(cover,142),new LinearLayout.LayoutParams(-1,dp(116)));space(card,12);
            TextView name=text(playlist.title(),15,FG,true);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);card.addView(name,new LinearLayout.LayoutParams(-1,dp(44)));
            card.addView(label(playlist.author(),11,MUTED,false));space(card,5);card.addView(label(playlist.count()>0?playlist.count()+" tracks · YouTube":"Playlist · YouTube",11,GREEN,false));
            card.setContentDescription("Open YouTube playlist "+playlist.title());card.setOnClickListener(v->openRemotePlaylist(playlist));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(176),dp(238));lp.setMarginEnd(dp(12));cards.addView(card,lp);
        }
        rail.addView(cards);suggestionsContainer.addView(rail);
    }
    private void loadSuggestions(String query){
        if(closing||isDestroyed())return;
        String q=query.isBlank()?recommendationQuery():query;
        if(suggestionsLoading&&q.equals(suggestionsQuery))return;
        suggestionsQuery=q;suggestionsLoading=true;suggestionsError="";suggestions.clear();int version=++suggestionsVersion;
        if(suggestionsJob!=null)suggestionsJob.cancel(false);drawSuggestions();
        suggestionsJob=network.submit(()->{
            try{
                List<PlaylistSource.Suggestion> found=app.playlists.suggest(q);
                runOnUiThread(()->{if(closing||isDestroyed()||version!=suggestionsVersion)return;suggestionsLoading=false;suggestions.addAll(found);if(found.isEmpty())suggestionsError="No public playlists found. Try another mood.";drawSuggestions();});
            }catch(Exception e){runOnUiThread(()->{if(closing||isDestroyed()||version!=suggestionsVersion)return;suggestionsLoading=false;suggestionsError="Couldn't load YouTube playlists. Check your connection and retry.";drawSuggestions();});}
        });
    }
    private void openRemotePlaylist(PlaylistSource.Suggestion playlist){
        if(!tab.equals("youtube-playlist"))remoteBack=tab;
        remotePlaylist=playlist;remoteTracks.clear();remoteNext=null;tab="youtube-playlist";hideKeyboard();loadRemoteTracks(false);
    }
    private void loadRemoteTracks(boolean more){
        if(remotePlaylist==null||more&&remoteLoading)return;
        String url=remotePlaylist.url();org.schabi.newpipe.extractor.Page page=more?remoteNext:null;
        remoteLoading=true;remoteError="";int version=++remoteVersion;if(remoteJob!=null)remoteJob.cancel(false);render();
        remoteJob=network.submit(()->{
            try{
                PlaylistSource.Tracks fetched=app.playlists.playlist(url,page);
                runOnUiThread(()->{if(closing||isDestroyed()||version!=remoteVersion)return;
                    if(!fetched.title().isBlank())remotePlaylist=new PlaylistSource.Suggestion(remotePlaylist.url(),fetched.title(),remotePlaylist.author(),remotePlaylist.artwork(),remotePlaylist.count());
                    LinkedHashMap<String,Track> unique=new LinkedHashMap<>();for(Track t:remoteTracks)unique.put(t.id(),t);for(Track t:fetched.tracks())unique.putIfAbsent(t.id(),t);remoteTracks.clear();remoteTracks.addAll(unique.values());
                    remoteNext=fetched.next();remoteLoading=false;if(remoteTracks.isEmpty()&&remoteNext==null)remoteError="This playlist has no available songs. It may be private or unavailable in your region.";
                    if(tab.equals("youtube-playlist"))render();
                });
            }catch(Exception e){runOnUiThread(()->{if(closing||isDestroyed()||version!=remoteVersion)return;remoteLoading=false;remoteError="Couldn't load this playlist. Check your connection or try another public playlist.";if(tab.equals("youtube-playlist"))render();});}
        });
    }
    private void remotePlaylistScreen(){
        if(remotePlaylist==null){add(body,text("Choose a playlist from Listen.",16,FG,false),-2);return;}
        LinearLayout top=row();Icons back=icon("back",FG,this::goBack);back.setContentDescription("Back from YouTube playlist");top.addView(back);top.addView(text("FROM YOUTUBE",11,GREEN,true));add(body,top,48);space(body,12);
        add(body,text(remotePlaylist.title(),30,FG,true),-2);space(body,8);add(body,text(remotePlaylist.author()+" · Public playlist",13,MUTED,false),-2);space(body,18);
        if(!remoteTracks.isEmpty()){
            List<Track> queue=new ArrayList<>(remoteTracks);LinearLayout actions=row();actions.addView(button("▶  Play",true,()->play(queue,0,false)),new LinearLayout.LayoutParams(0,dp(48),1));TextView shuffle=button("Shuffle",false,()->play(queue,0,true));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);lp.setMarginStart(dp(10));actions.addView(shuffle,lp);add(body,actions,-2);space(body,10);
            TextView save=button("Save "+queue.size()+" songs to Library",false,()->saveRemotePlaylist(queue));save.setEnabled(!remoteLoading);add(body,save,48);space(body,10);
            add(body,text(remoteNext!=null?"Showing "+queue.size()+" songs. Load more before saving to include more tracks.":queue.size()+" available songs. Saving a playlist doesn't download audio.",12,MUTED,false),-2);space(body,16);
            for(Track track:queue)trackRow(body,track,queue,false);
        }
        if(remoteLoading){ProgressBar progress=new ProgressBar(this);add(body,progress,44);add(body,text("Loading songs from YouTube…",13,MUTED,false),-2);}
        else if(!remoteError.isEmpty()){add(body,text(remoteError,13,MUTED,false),-2);space(body,12);add(body,button("Retry playlist",false,()->loadRemoteTracks(!remoteTracks.isEmpty()&&remoteNext!=null)),48);}
        else if(remoteNext!=null){space(body,14);add(body,button("Load more playlist songs",false,()->loadRemoteTracks(true)),48);}
    }
    private void saveRemotePlaylist(List<Track> tracks){
        String title=remotePlaylist.title();
        new AlertDialog.Builder(this).setTitle("Save to your library?").setMessage("Save the "+tracks.size()+" loaded songs as a local playlist. This saves song links, not offline audio.")
            .setPositiveButton("Save playlist",(d,which)->{
                try{playlistId=app.library.importPlaylist(title,tracks);playlistName=title.length()>80?title.substring(0,80):title;tab="library";render();toast("Playlist saved to Library");}
                catch(RuntimeException e){toast("Couldn't save this playlist. Please try again.");}
            }).setNegativeButton("Cancel",null).show();
    }
    private void searchScreen(){
        add(body,text("Discover your\nnext favorite.",34,FG,true),-2);space(body,8);add(body,text("Search YouTube or paste a video or playlist link.",14,MUTED,false),-2);space(body,20);
        LinearLayout bar=row();bar.setBackground(border(SURFACE,16));bar.addView(icon("search",MUTED,null));EditText input=new EditText(this);input.setSingleLine(true);input.setTextColor(FG);input.setHintTextColor(MUTED);input.setTextSize(15);input.setHint("Songs, artists, or a YouTube link");input.setText(query);input.setBackgroundColor(android.graphics.Color.TRANSPARENT);input.setPadding(0,dp(12),0,dp(12));input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);bar.addView(input,new LinearLayout.LayoutParams(0,dp(56),1));Icons go=icon("next",GREEN,()->startSearch(input.getText().toString(),false));go.setContentDescription("Search YouTube");bar.addView(go);add(body,bar,-2);
        input.setOnEditorActionListener((v,id,event)->{if(id==EditorInfo.IME_ACTION_SEARCH){startSearch(input.getText().toString(),false);return true;}return false;});
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){query=s.toString();}public void afterTextChanged(Editable e){}});
        space(body,20);
        if(searching){LinearLayout loading=row();ProgressBar p=new ProgressBar(this);loading.addView(p,new LinearLayout.LayoutParams(dp(24),dp(24)));TextView status=text("  Finding your sound…",14,MUTED,false);loading.addView(status);add(body,loading,48);}
        if(!searchError.isEmpty())empty("Couldn't reach the music",searchError,"search","Try again",()->startSearch(query,false));
        if(!results.isEmpty()){
            section("Search results",null,null);for(Track t:results)trackRow(body,t,results,false);
            if(nextPage!=null&&!searching){space(body,18);add(body,button("Load more songs",false,()->startSearch(resultQuery,true)),48);}
        }else if(!searching&&searchError.isEmpty()){
            if(query.isEmpty()){
                section("Start with something you love",null,null);
                for(String suggestion:List.of("Indie discoveries","Jazz for a quiet evening","Hindi acoustic songs","Electronic essentials")){
                    LinearLayout r=row();r.addView(icon("search",MUTED,null));r.addView(text(suggestion,15,FG,false),new LinearLayout.LayoutParams(0,-2,1));r.addView(icon("next",MUTED,null));r.setOnClickListener(v->startSearch(suggestion,false));add(body,r,62);
                }
                space(body,18);add(body,text("YouTube is the source. Availability can vary by region and video.",12,MUTED,false),-2);
            }else empty("No songs here yet","Run a search above. Try a song title, artist, or a YouTube video link.","music",null,null);
        }
    }
    private void startSearch(String value,boolean more){
        String q=value.trim();if(q.isEmpty()){navigate("search");return;}
        String playlistLink=PlaylistLinks.canonical(q);
        if(!more&&playlistLink!=null&&VideoIds.parse(q)==null){openRemotePlaylist(new PlaylistSource.Suggestion(playlistLink,"YouTube playlist","YouTube","",-1));return;}
        if(searching && more)return;
        tab="search";playlistId=null;query=q;hideKeyboard();int version=++searchVersion;
        org.schabi.newpipe.extractor.Page page=more?nextPage:null;
        if(!more){results.clear();nextPage=null;}searching=true;searchError="";render();
        network.execute(()->{
            try{
                var response=app.youtube.search(q,page);
                runOnUiThread(()->{if(closing||isDestroyed()||version!=searchVersion)return;
                    LinkedHashMap<String,Track> all=new LinkedHashMap<>();for(Track t:results)all.put(t.id(),t);for(Track t:response.tracks())all.put(t.id(),t);results.clear();results.addAll(all.values());
                    nextPage=response.next();resultQuery=response.query();searching=false;if(results.isEmpty())searchError="No playable results were found. Try a different song or artist.";if(tab.equals("search"))render();});
            }catch(Exception e){runOnUiThread(()->{if(closing||isDestroyed()||version!=searchVersion)return;searching=false;searchError="Check your internet connection and try again. YouTube may be limiting requests, or the extractor may need an update.";if(tab.equals("search"))render();});}
        });
    }
    private void libraryScreen(){
        add(body,text("A little collection\nof you.",34,FG,true),-2);space(body,8);add(body,text("Your playlists. Your kind of music.",14,MUTED,false),-2);space(body,22);
        add(body,button("+  Create playlist",true,()->editPlaylist(null,"")),50);space(body,10);add(body,button("Import YouTube history",false,()->navigate("account")),48);space(body,18);
        for(Library.Playlist p:app.library.playlists()){
            LinearLayout card=row();card.setBackground(bg(SURFACE,18));card.setPadding(dp(12),dp(12),dp(8),dp(12));
            FrameLayout cover=new FrameLayout(this);cover.setBackground(bg(p.id().equals("liked")?0xff3b4a29:0xff343549,14));Icons i=icon(p.id().equals("liked")?"liked":"music",p.id().equals("liked")?GREEN:0xffc9b8ef,null);cover.addView(i,new FrameLayout.LayoutParams(-1,-1));card.addView(cover,new LinearLayout.LayoutParams(dp(68),dp(68)));
            LinearLayout copy=column();copy.setPadding(dp(16),0,0,0);copy.addView(label(p.name(),18,FG,true));space(copy,5);copy.addView(text(p.count()+" songs · On this device",12,MUTED,false));card.addView(copy,new LinearLayout.LayoutParams(0,-2,1));card.addView(icon("next",MUTED,null));card.setOnClickListener(v->{playlistId=p.id();playlistName=p.name();render();});add(body,card,100);space(body,12);
        }
        space(body,12);add(body,text("Playlists are saved locally and work without an account. Download tracks to listen without a connection.",12,MUTED,false),-2);
    }
    private void playlistScreen(){
        LinearLayout tools=row();tools.addView(icon("back",FG,()->{playlistId=null;render();}));tools.addView(text("YOUR COLLECTION",11,MUTED,true),new LinearLayout.LayoutParams(0,-2,1));if(!playlistId.equals("liked"))tools.addView(icon("more",FG,this::playlistOptions));add(body,tools,48);
        space(body,18);Icons art=icon(playlistId.equals("liked")?"liked":"music",GREEN,null);art.setBackground(bg(0xff2c3c25,24));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(dp(130),dp(130));ap.gravity=Gravity.CENTER_HORIZONTAL;body.addView(art,ap);space(body,22);
        add(body,text(playlistName,32,FG,true),-2);List<Track> songs=app.library.playlistTracks(playlistId);space(body,6);add(body,text(songs.size()+" songs · Made by you",13,MUTED,false),-2);space(body,20);
        if(!downloadedTracks().isEmpty()){add(body,button("Add downloaded songs",false,()->selectDownloadedSongs(playlistId,null)),48);space(body,12);}
        if(!songs.isEmpty()){
            LinearLayout controls=row();controls.addView(button("▶  Play",true,()->play(songs,0,false)),new LinearLayout.LayoutParams(0,dp(50),1));TextView shuffle=button("Shuffle",false,()->play(songs,0,true));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(50),1);bp.leftMargin=dp(10);controls.addView(shuffle,bp);add(body,controls,-2);space(body,18);for(Track t:songs)trackRow(body,t,songs,true);
        }else empty("Make room for your favorites","Find a song, tap the three dots, then choose Add to playlist.","music","Find songs",()->navigate("search"));
    }
    private void offlineScreen(){
        add(body,text("Take the music\nwith you.",34,FG,true),-2);space(body,8);add(body,text("Saved on your phone. Ready for airplane mode.",14,MUTED,false),-2);space(body,20);
        List<Library.Download> downloads=app.library.downloads();List<Track> available=new ArrayList<>();long bytes=0;
        for(var d:downloads){String path=app.library.offline(d.track().id());if(path!=null){available.add(d.track());bytes+=new java.io.File(path).length();}}
        LinearLayout summary=row();summary.setBackground(border(SURFACE,18));inset(summary,16);summary.addView(icon("download",GREEN,null));LinearLayout copy=column();copy.addView(text(available.size()+" songs offline",18,FG,true));copy.addView(text(String.format(Locale.ROOT,"%.1f MB on this device",bytes/1048576.0),12,MUTED,false));summary.addView(copy,new LinearLayout.LayoutParams(0,-2,1));add(body,summary,88);space(body,18);
        if(!available.isEmpty()){
            add(body,button("Play offline collection",true,()->play(available,0,false)),48);space(body,10);
            add(body,button("Create playlist from downloads",false,this::createOfflinePlaylist),48);space(body,10);
            add(body,text("Choose downloaded songs for your own playlist. Find it later in Library.",12,MUTED,false),-2);space(body,16);
        }
        if(downloads.isEmpty())empty("Your music, even without Wi-Fi","Find a song, open its menu, and choose Save offline. Downloads stay in the app until you remove them.","download","Find songs",()->navigate("search"));
        for(var d:downloads){
            if(app.library.offline(d.track().id())!=null&&!d.status().equals("converting")){trackRow(body,d.track(),available,false);if(!d.error().isBlank())add(body,text(d.error(),12,MUTED,false),-2);}
            else{
                LinearLayout card=column();card.setBackground(bg(SURFACE,16));inset(card,14);card.addView(label(d.track().title(),16,FG,true));space(card,5);
                boolean busy=d.status().equals("queued")||d.status().equals("downloading")||d.status().equals("converting");
                card.addView(text(busy?(d.status().equals("converting")?"Converting to MP3 on this device":d.status().equals("queued")?"Queued for download":"Downloading · See notification for progress"):(d.error().isEmpty()?"File missing. Download again to restore offline playback.":d.error()),12,MUTED,false));space(card,12);
                LinearLayout actions=row();actions.addView(button(busy?"Cancel":"Retry",!busy,()->{if(busy)OfflineService.cancel(this,d.track().id());else download(d.track());}));if(!busy){TextView remove=button("Remove",false,()->{app.library.removeDownload(d.track().id());render();});LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-2,dp(48));rp.leftMargin=dp(8);actions.addView(remove,rp);}card.addView(actions);add(body,card,-2);space(body,10);
            }
        }
    }
    private void trackRow(LinearLayout parent,Track t,List<Track> queue,boolean inPlaylist){
        LinearLayout r=row();r.setPadding(0,dp(7),0,dp(7));
        FrameLayout art=artwork(t,54);r.addView(art,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout copy=column();copy.setPadding(dp(13),0,dp(3),0);copy.addView(label(t.title(),15,FG,true));space(copy,5);copy.addView(label((app.library.offline(t.id())!=null?"↓  ":"")+t.artist()+ (t.duration()>0?" · "+time(t.duration()*1000):""),12,MUTED,false));r.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        Icons add=icon("add",GREEN,()->choosePlaylist(t));add.setContentDescription("Add "+t.title()+" to playlist");r.addView(add);Icons more=icon("more",MUTED,()->songMenu(t,inPlaylist));more.setContentDescription("Options for "+t.title());r.addView(more);r.setOnClickListener(v->{int index=0;for(int i=0;i<queue.size();i++)if(queue.get(i).id().equals(t.id())){index=i;break;}play(queue,index,false);});r.setContentDescription("Play "+t.title()+" by "+t.artist());parent.addView(r,new LinearLayout.LayoutParams(-1,dp(76)));
    }
    private void empty(String title,String subtitle,String symbol,String action,Runnable click){
        LinearLayout box=column();inset(box,22);box.setBackground(bg(SURFACE,20));Icons i=icon(symbol,GREEN,null);box.addView(i);space(box,12);box.addView(text(title,20,FG,true));space(box,8);TextView copy=text(subtitle,13,MUTED,false);copy.setLineSpacing(dp(3),1);box.addView(copy);if(action!=null){space(box,18);box.addView(button(action,true,click),new LinearLayout.LayoutParams(-2,dp(48)));}add(body,box,-2);
    }
    private FrameLayout artwork(Track t,int size){
        FrameLayout f=new FrameLayout(this);f.setBackground(bg(RAISED,12));f.setClipToOutline(true);f.addView(new RecordArt(this),new FrameLayout.LayoutParams(-1,-1));
        if(t!=null&&!t.artwork().isEmpty()){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setContentDescription("Artwork for "+t.title());f.addView(image,new FrameLayout.LayoutParams(-1,-1));loadImage(image,t.artwork());}return f;
    }
    private void loadImage(ImageView image,String url){
        if(closing||images.isShutdown())return;
        image.setTag(url);Bitmap cached=imageCache.get(url);if(cached!=null){image.setImageBitmap(cached);return;}
        if(!url.startsWith("https://"))return;
        images.execute(()->{try(okhttp3.Response r=app.youtube.client.newCall(new okhttp3.Request.Builder().url(url).build()).execute()){
            if(!r.isSuccessful()||r.body()==null)return;java.io.InputStream in=r.body().byteStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[8192];int n,count=0;while((n=in.read(chunk))!=-1){count+=n;if(count>3*1024*1024)return;out.write(chunk,0,n);}
            byte[] data=out.toByteArray();BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,opts);if(opts.outWidth<1||opts.outHeight<1)return;opts.inSampleSize=Math.max(1,Math.max(opts.outWidth,opts.outHeight)/720);opts.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,opts);if(bitmap==null)return;imageCache.put(url,bitmap);runOnUiThread(()->{if(!closing&&!isDestroyed()&&url.equals(image.getTag()))image.setImageBitmap(bitmap);});
        }catch(Exception ignored){/* Keep generated artwork if a thumbnail is unavailable. */}});
    }
    private void play(List<Track> tracks,int index,boolean shuffle){
        if(player==null){toast("Audio player is connecting. Please try again.");return;}if(tracks.isEmpty())return;
        List<Track> copy=new ArrayList<>(tracks);List<MediaItem> items=new ArrayList<>();for(Track t:copy){app.library.remember(t);items.add(t.mediaItem(app.library.offline(t.id())));}
        player.setShuffleModeEnabled(shuffle);player.setMediaItems(items,index,0);player.prepare();player.play();updateMini();showNowPlaying();
    }
    private void togglePlay(){if(player==null)return;if(player.isPlaying())player.pause();else{if(player.getPlaybackState()==Player.STATE_IDLE)player.prepare();if(player.getPlaybackState()==Player.STATE_ENDED)player.seekToDefaultPosition();player.play();}}
    private void updateMini(){
        if(mini==null)return;mini.removeAllViews();if(player==null||player.getCurrentMediaItem()==null)return;
        Track t=Track.from(player.getCurrentMediaItem());LinearLayout r=row();r.setBackground(border(0xff2b3828,14));r.setPadding(dp(8),dp(8),dp(4),dp(8));r.addView(artwork(t,44),new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout copy=column();copy.setPadding(dp(12),0,0,0);miniTitle=label(t.title(),13,FG,true);miniArtist=label(t.artist(),11,0xffc1cdbb,false);copy.addView(miniTitle);space(copy,3);copy.addView(miniArtist);r.addView(copy,new LinearLayout.LayoutParams(0,-2,1));miniPlay=icon(player.isPlaying()?"pause":"play",GREEN,this::togglePlay);miniPlay.setContentDescription(player.isPlaying()?"Pause":"Play");r.addView(miniPlay);r.addView(icon("next",FG,()->{if(player.hasNextMediaItem())player.seekToNextMediaItem();else toast("You're at the end of the queue.");}));r.setOnClickListener(v->showNowPlaying());mini.addView(r,new LinearLayout.LayoutParams(-1,dp(68)));
    }
    private void showNowPlaying(){
        if(player==null||player.getCurrentMediaItem()==null)return;if(nowDialog!=null&&nowDialog.isShowing())return;
        nowDialog=new Dialog(this);LinearLayout page=column();page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xff30412b,0xff151c14,BG}));page.setPadding(dp(26),dp(18),dp(26),dp(24));
        ScrollView container=new ScrollView(this);container.setFillViewport(true);container.addView(page);nowDialog.setContentView(container);nowDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);nowDialog.getWindow().setLayout(-1,-1);container.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets s=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(0,s.top,0,s.bottom);return i;});
        LinearLayout top=row();top.addView(icon("down",FG,()->nowDialog.dismiss()));TextView title=label("IN YOUR FREQUENCY",11,FG,true);title.setGravity(Gravity.CENTER);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));top.addView(icon("queue",FG,this::showQueue));page.addView(top);space(page,28);
        int artSize=Math.min(360,(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-64);nowArtFrame=new FrameLayout(this);nowArtFrame.setBackground(bg(RAISED,24));nowArtFrame.setClipToOutline(true);nowArtFrame.addView(new RecordArt(this),new FrameLayout.LayoutParams(-1,-1));nowArt=new ImageView(this);nowArt.setScaleType(ImageView.ScaleType.CENTER_CROP);nowArtFrame.addView(nowArt,new FrameLayout.LayoutParams(-1,-1));LinearLayout.LayoutParams artParams=new LinearLayout.LayoutParams(dp(artSize),dp(artSize));artParams.gravity=Gravity.CENTER_HORIZONTAL;page.addView(nowArtFrame,artParams);space(page,30);
        LinearLayout name=row();LinearLayout names=column();nowTitle=label("",25,FG,true);nowArtist=label("",15,0xffc0cbb9,false);names.addView(nowTitle);space(names,6);names.addView(nowArtist);name.addView(names,new LinearLayout.LayoutParams(0,-2,1));likeIcon=icon("heart",GREEN,()->{Track t=Track.from(player.getCurrentMediaItem());boolean liked=app.library.contains("liked",t.id());if(liked)app.library.remove("liked",t.id());else app.library.add("liked",t);updateNow();});likeIcon.setContentDescription("Toggle liked song");name.addView(likeIcon);page.addView(name);space(page,18);
        seek=new SeekBar(this);seek.setMax(1000);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(GREEN));seek.setContentDescription("Playback position");page.addView(seek,new LinearLayout.LayoutParams(-1,dp(44)));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int v,boolean user){if(user&&player.getDuration()>0)elapsed.setText(time(player.getDuration()*v/1000));}public void onStartTrackingTouch(SeekBar s){dragging=true;}public void onStopTrackingTouch(SeekBar s){if(player.getDuration()>0)player.seekTo(player.getDuration()*s.getProgress()/1000);dragging=false;}});
        LinearLayout times=row();elapsed=text("0:00",12,MUTED,false);total=text("0:00",12,MUTED,false);times.addView(elapsed,new LinearLayout.LayoutParams(0,-2,1));times.addView(total);page.addView(times);space(page,16);
        LinearLayout controls=row();controls.setGravity(Gravity.CENTER);shuffleIcon=icon("shuffle",MUTED,()->player.setShuffleModeEnabled(!player.getShuffleModeEnabled()));controls.addView(shuffleIcon,new LinearLayout.LayoutParams(0,dp(56),1));Icons prev=icon("previous",FG,()->{if(player.getCurrentPosition()>3000)player.seekTo(0);else if(player.hasPreviousMediaItem())player.seekToPreviousMediaItem();else player.seekTo(0);});controls.addView(prev,new LinearLayout.LayoutParams(0,dp(60),1));playIcon=icon("play",INK,this::togglePlay);playIcon.setBackground(bg(GREEN,50));controls.addView(playIcon,new LinearLayout.LayoutParams(dp(76),dp(76)));Icons next=icon("next",FG,()->{if(player.hasNextMediaItem())player.seekToNextMediaItem();else toast("You're at the end of the queue.");});controls.addView(next,new LinearLayout.LayoutParams(0,dp(60),1));repeatIcon=icon("repeat",MUTED,()->player.setRepeatMode((player.getRepeatMode()+1)%3));repeatIcon.setContentDescription("Change repeat mode");controls.addView(repeatIcon,new LinearLayout.LayoutParams(0,dp(56),1));page.addView(controls);space(page,18);
        nowStatus=text("",12,MUTED,false);nowStatus.setGravity(Gravity.CENTER);page.addView(nowStatus);space(page,14);LinearLayout actions=row();actions.setGravity(Gravity.CENTER);Icons save=icon("download",FG,()->download(Track.from(player.getCurrentMediaItem())));save.setContentDescription("Save current song offline");actions.addView(save);TextView source=text("YouTube audio",12,MUTED,false);source.setPadding(dp(12),0,dp(12),0);actions.addView(source);actions.addView(icon("more",FG,()->songMenu(Track.from(player.getCurrentMediaItem()),false)));page.addView(actions);
        nowDialog.setOnDismissListener(d->{dragging=false;if(!closing&&(tab.equals("library")||tab.equals("home")))render();});nowArt.setTag(null);nowDialog.show();nowDialog.getWindow().setLayout(-1,-1);updateNow();
    }
    private void updateNow(){
        if(nowDialog==null||!nowDialog.isShowing()||player==null)return;
        if(player.getCurrentMediaItem()==null){nowDialog.dismiss();return;}
        Track t=Track.from(player.getCurrentMediaItem());nowTitle.setText(t.title());nowArtist.setText(t.artist());
        if(!t.id().equals(nowArtFrame.getTag())){nowArtFrame.setTag(t.id());nowArt.setImageDrawable(null);nowArt.setTag("");if(!t.artwork().isEmpty())loadImage(nowArt,t.artwork());}
        nowArt.setContentDescription("Artwork for "+t.title());
        playIcon.set(player.isPlaying()?"pause":"play",INK);playIcon.setContentDescription(player.isPlaying()?"Pause":"Play");likeIcon.set(app.library.contains("liked",t.id())?"liked":"heart",GREEN);shuffleIcon.set("shuffle",player.getShuffleModeEnabled()?GREEN:MUTED);repeatIcon.set("repeat",player.getRepeatMode()==Player.REPEAT_MODE_OFF?MUTED:GREEN);
        long duration=player.getDuration();if(!dragging){elapsed.setText(time(player.getCurrentPosition()));seek.setProgress(duration>0?(int)(player.getCurrentPosition()*1000/duration):0);}total.setText(duration>0?time(duration):"–:––");seek.setEnabled(duration>0);
        String state=player.getPlayerError()!=null?"Can't play this track · Tap Play to retry":player.getPlaybackState()==Player.STATE_BUFFERING?"Finding the best audio stream…":app.library.offline(t.id())!=null?"Playing from your offline library":"Streaming from YouTube";
        if(player.getRepeatMode()==Player.REPEAT_MODE_ONE)state+=" · Repeat one";else if(player.getRepeatMode()==Player.REPEAT_MODE_ALL)state+=" · Repeat all";nowStatus.setText(state);
    }
    private void showQueue(){
        if(player==null){toast("The player is connecting. Please try again.");return;}
        if(queuePanel!=null)queuePanel.dismiss();queuePanel=new QueuePanel(this,player,this::artwork);queuePanel.show();
    }
    private void songMenu(Track t,boolean inPlaylist){
        boolean liked=app.library.contains("liked",t.id()),offline=app.library.offline(t.id())!=null;
        ArrayList<String> options=new ArrayList<>(List.of(liked?"Remove from liked songs":"Like song","Add to playlist",offline?"Remove offline copy":"Save offline","Add to queue","Open on YouTube"));
        if(inPlaylist){options.add("Remove from this playlist");options.add("Move earlier");options.add("Move later");}
        if(offline){options.add("Export offline audio");if(!app.library.offline(t.id()).endsWith(".mp3"))options.add("Convert offline copy to MP3");}
        new AlertDialog.Builder(this).setTitle(t.title()).setItems(options.toArray(new String[0]),(d,action)->{
            if(options.get(action).equals("Export offline audio")){exportAudio(t);return;}
            if(options.get(action).equals("Convert offline copy to MP3")){startDownload(t,true);return;}
            switch(action){
                case 0->{if(liked)app.library.remove("liked",t.id());else app.library.add("liked",t);toast(liked?"Removed from liked songs":"Added to liked songs");updateNow();if(tab.equals("library"))render();}
                case 1->choosePlaylist(t);
                case 2->{if(offline)new AlertDialog.Builder(this).setTitle("Remove offline copy?").setMessage("This frees device storage. The song stays in your playlists.").setPositiveButton("Remove",(dd,w)->{if(app.library.removeDownload(t.id())){toast("Offline copy removed");render();}else toast("Could not remove the file. Please try again.");}).setNegativeButton("Keep",null).show();else download(t);}
                case 3->{if(player!=null){app.library.remember(t);player.addMediaItem(t.mediaItem(app.library.offline(t.id())));toast("Added to queue");}}
                case 4->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(t.url())));}catch(ActivityNotFoundException e){toast("No browser is installed.");}}
                case 5->{app.library.remove(playlistId,t.id());render();}
                case 6->{app.library.move(playlistId,t.id(),-1);render();}
                case 7->{app.library.move(playlistId,t.id(),1);render();}
            }
        }).setNegativeButton("Close",null).show();
    }
    private void choosePlaylist(Track t){
        List<Library.Playlist> lists=app.library.playlists();ArrayList<String> names=new ArrayList<>();names.add("+ Create a new playlist");for(var p:lists)names.add(p.name());
        new AlertDialog.Builder(this).setTitle("Add to playlist").setItems(names.toArray(new String[0]),(d,index)->{if(index==0)createAndAdd(t);else{var p=lists.get(index-1);app.library.add(p.id(),t);toast("Added to "+p.name());}}).show();
    }
    private void openOwnPlaylist(String id){
        Library.Playlist saved=app.library.playlists().stream().filter(item->item.id().equals(id)).findFirst().orElseThrow();
        playlistId=id;playlistName=saved.name();tab="library";render();
    }
    private void createAndAdd(Track track){
        nameDialog("New playlist","",name->{String id=app.library.importPlaylist(name,List.of(track));openOwnPlaylist(id);toast("Playlist created · Song added");});
    }
    private void editPlaylist(String id,String old){
        nameDialog(id==null?"New playlist":"Rename playlist",old,name->{
            if(id==null){openOwnPlaylist(app.library.createPlaylist(name));toast("Your playlist is ready");}
            else{app.library.rename(id,name);openOwnPlaylist(id);}
        });
    }
    private interface NameAction{void accept(String name);}
    private void nameDialog(String title,String old,NameAction action){
        LinearLayout content=column();inset(content,22);content.addView(text("Give your playlist a name.",14,MUTED,false));space(content,10);
        EditText input=new EditText(this);input.setSingleLine(true);input.setText(old);input.setHint("Playlist name");input.setTextColor(FG);input.setHintTextColor(MUTED);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});content.addView(input,new LinearLayout.LayoutParams(-1,dp(56)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(content).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.show();
        Runnable save=()->{
            String name=input.getText().toString().trim();
            if(name.isEmpty()){input.setError("Enter a playlist name");input.requestFocus();return;}
            try{action.accept(name);hideKeyboard();dialog.dismiss();}
            catch(IllegalArgumentException error){input.setError(error.getMessage());}
            catch(android.database.SQLException error){input.setError("Couldn't save this playlist. Please try again.");}
        };
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->save.run());
        input.setOnEditorActionListener((v,event,key)->{if(event==EditorInfo.IME_ACTION_DONE){save.run();return true;}return false;});
        input.requestFocus();input.setSelection(input.length());
    }
    private List<Track> downloadedTracks(){
        List<Track> tracks=new ArrayList<>();for(Library.Download download:app.library.downloads())if(app.library.offline(download.track().id())!=null&&!download.status().equals("converting"))tracks.add(download.track());return tracks;
    }
    private void createOfflinePlaylist(){
        if(downloadedTracks().isEmpty()){toast("Save songs offline first, or create an empty playlist in Library.");return;}
        nameDialog("New offline playlist","",name->selectDownloadedSongs(null,name));
    }
    private void selectDownloadedSongs(String target,String newName){
        List<Track> tracks=downloadedTracks();if(tracks.isEmpty()){toast("No downloaded songs are available yet.");return;}
        String[] names=tracks.stream().map(track->track.title()+" · "+track.artist()).toArray(String[]::new);boolean[] checked=new boolean[tracks.size()];
        AlertDialog picker=new AlertDialog.Builder(this).setTitle("Choose downloaded songs").setMultiChoiceItems(names,checked,(d,index,value)->checked[index]=value)
            .setPositiveButton(target==null?"Create playlist":"Add songs",null).setNeutralButton("Select all",null).setNegativeButton("Cancel",null).create();picker.show();
        picker.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{boolean all=true;for(boolean item:checked)all&=item;for(int i=0;i<checked.length;i++){checked[i]=!all;picker.getListView().setItemChecked(i,!all);}picker.getButton(AlertDialog.BUTTON_NEUTRAL).setText(all?"Select all":"Clear selection");});
        picker.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            List<Track> selected=new ArrayList<>();for(int i=0;i<checked.length;i++)if(checked[i])selected.add(tracks.get(i));if(selected.isEmpty()){toast("Choose at least one downloaded song");return;}
            try{String id=target;if(id==null)id=app.library.importPlaylist(newName,selected);else for(Track track:selected)app.library.add(id,track);picker.dismiss();openOwnPlaylist(id);toast("Downloaded songs added to your playlist");}
            catch(android.database.SQLException error){toast("Couldn't save these songs. Your downloads are kept.");}
        });
    }
    private void playlistOptions(){new AlertDialog.Builder(this).setTitle(playlistName).setItems(new String[]{"Rename playlist","Delete playlist"},(d,index)->{if(index==0)editPlaylist(playlistId,playlistName);else new AlertDialog.Builder(this).setTitle("Delete this playlist?").setMessage("Downloaded audio will stay in your offline library.").setPositiveButton("Delete",(dd,w)->{app.library.deletePlaylist(playlistId);playlistId=null;render();}).setNegativeButton("Keep",null).show();}).show();}
    private void download(Track track){
        if(app.library.offline(track.id())!=null){toast("Already available offline. Use its menu to export or convert to MP3.");return;}
        for(var d:app.library.downloads())if(d.track().id().equals(track.id())&&List.of("queued","downloading","converting").contains(d.status())){toast("This song is already being saved");return;}
        LinearLayout choices=column();inset(choices,22);choices.addView(text("Save audio you have permission to download. Mobile data may be used.",13,MUTED,false));space(choices,12);
        RadioGroup formats=new RadioGroup(this);RadioButton original=new RadioButton(this);original.setId(View.generateViewId());original.setText("Original audio · Faster, smaller");original.setTextColor(FG);formats.addView(original);
        RadioButton mp3=new RadioButton(this);mp3.setId(View.generateViewId());mp3.setText("MP3 · Convert on this device");mp3.setTextColor(FG);formats.addView(mp3);formats.check(original.getId());choices.addView(formats);space(choices,10);choices.addView(text("MP3 conversion takes extra time and storage. Interrupted transfers can continue when you retry.",12,MUTED,false));
        new AlertDialog.Builder(this).setTitle("Save offline").setView(choices).setPositiveButton("Save offline",(d,w)->startDownload(track,formats.getCheckedRadioButtonId()==mp3.getId())).setNegativeButton("Cancel",null).show();
    }
    private void startDownload(Track track,boolean mp3){
        if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
        try{OfflineService.enqueue(this,track,mp3);toast(mp3?"Saving and converting to MP3 · Check Offline":"Download queued · Check Offline");}catch(RuntimeException e){toast("Couldn't start saving. Keep Soundzy open and retry.");}
    }
    private void exportAudio(Track track){
        String path=app.library.offline(track.id());if(path==null){toast("Download this song first.");return;}pendingExport=path;
        String extension;try{extension=AudioFiles.extension(new java.io.File(path));}catch(java.io.IOException e){toast("Couldn’t read this audio file. Download it again.");return;}String type=AudioFiles.mime(extension);
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(type).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,track.title().replaceAll("[\\\\/:*?\"<>|]","_")+"."+extension);
        try{startActivityForResult(intent,EXPORT_AUDIO);}catch(ActivityNotFoundException e){toast("No file picker is available on this device.");}
    }
    private void accountScreen(){
        add(body,text("Your Soundzy.",34,FG,true),-2);space(body,8);add(body,text("Bring your favorites with you.",14,MUTED,false),-2);space(body,22);
        android.content.SharedPreferences profile=getSharedPreferences("soundzy.account",MODE_PRIVATE);String name=profile.getString("name","");
        LinearLayout account=column();account.setBackground(bg(SURFACE,20));inset(account,20);account.addView(text(name.isBlank()?"Google account":name,22,FG,true));space(account,8);
        account.addView(text(name.isBlank()?"Optional sign-in for your Google profile. Your music stays on this device.":profile.getString("email",""),13,MUTED,false));space(account,16);
        if(name.isBlank()){
            TextView signIn=button("Sign in with Google",true,this::signInGoogle);boolean ready=!getString(R.string.google_web_client_id).isBlank();signIn.setEnabled(ready);signIn.setAlpha(ready?1f:.5f);account.addView(signIn,new LinearLayout.LayoutParams(-1,dp(48)));
            if(!ready){space(account,10);account.addView(text("Google sign-in isn't available in this build yet. You can import history without signing in.",12,MUTED,false));}
        }else account.addView(button("Sign out",false,()->{
            Runnable clear=()->{if(closing)return;profile.edit().clear().apply();if(tab.equals("account"))render();};
            try{if(googleAccount==null)googleAccount=new GoogleAccount(this);googleAccount.signOut(clear);}catch(RuntimeException e){clear.run();}
        }),new LinearLayout.LayoutParams(-1,dp(48)));
        add(body,account,-2);space(body,22);section("YouTube listening history",null,null);
        add(body,text("Google doesn't let apps read YouTube watch history through sign-in. Import a Google Takeout export instead. Choose which entries are songs before adding them.",14,MUTED,false),-2);space(body,16);
        add(body,button("1. Open Google Takeout",false,()->openBrowser("https://takeout.google.com/")),48);space(body,10);
        add(body,text("In Takeout, select YouTube and YouTube Music → history. Export watch history as JSON or HTML, then unzip the downloaded export.",12,MUTED,false),-2);space(body,14);
        add(body,button("2. Choose history file",true,()->{
            Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/html","text/plain","application/octet-stream"});
            try{startActivityForResult(picker,PICK_HISTORY);}catch(ActivityNotFoundException e){toast("No file picker is available on this device.");}
        }),48);space(body,16);add(body,text("Your export is read locally and is never uploaded. History can include videos as well as music. Signing out keeps your playlists and offline files.",12,MUTED,false),-2);
    }
    private void signInGoogle(){
        if(googleAccount==null)googleAccount=new GoogleAccount(this);
        googleAccount.signIn(new GoogleAccount.Callback(){public void signedIn(String name,String email){if(closing)return;getSharedPreferences("soundzy.account",MODE_PRIVATE).edit().putString("name",name).putString("email",email).apply();if(tab.equals("account"))render();}public void failed(String message){if(!closing)toast(message);}});
    }
    private void openBrowser(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException e){toast("No browser is installed.");}}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(intent.getBooleanExtra("offline",false))navigate("offline");}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();
        if(request==PICK_HISTORY){toast("Reading history on this device…");files.submit(()->{
            try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null)throw new java.io.IOException("Couldn't open the selected history file.");HistoryImport.Result history=HistoryImport.read(in);runOnUiThread(()->{if(!closing&&!isDestroyed())chooseHistory(history);});
            }catch(Exception e){runOnUiThread(()->{if(!closing)toast(e.getMessage()==null?"Couldn't read this history export.":e.getMessage());});}
        });}else if(request==EXPORT_AUDIO&&pendingExport!=null){String source=pendingExport;pendingExport=null;files.submit(()->{
            try(java.io.InputStream in=new java.io.FileInputStream(source);java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                if(out==null)throw new java.io.IOException("Couldn't open the selected destination.");in.transferTo(out);runOnUiThread(()->{if(!closing)toast("Audio exported");});
            }catch(Exception e){runOnUiThread(()->{if(!closing)toast("Export failed. Choose another destination and retry.");});}
        });}
    }
    private void chooseHistory(HistoryImport.Result history){
        if(history.entries().isEmpty()){toast("No YouTube video links were found. Choose a watch-history JSON or HTML export.");return;}
        String[] labels=history.entries().stream().map(e->e.track().title()+" · "+e.track().artist()).toArray(String[]::new);boolean[] selected=new boolean[labels.length];
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Choose songs from history"+(history.limited()?" · First 5,000 entries":""))
            .setMultiChoiceItems(labels,selected,(d,which,checked)->selected[which]=checked).setPositiveButton("Import selected",null).setNeutralButton("Select all",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{boolean all=true;for(boolean item:selected)all&=item;for(int i=0;i<selected.length;i++){selected[i]=!all;dialog.getListView().setItemChecked(i,!all);}dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText(all?"Select all":"Clear selection");});
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                ArrayList<HistoryImport.Entry> chosen=new ArrayList<>();for(int i=0;i<selected.length;i++)if(selected[i])chosen.add(history.entries().get(i));if(chosen.isEmpty()){toast("Choose at least one song to import.");return;}
                dialog.dismiss();toast("Adding selected songs…");files.submit(()->{try{String id=app.library.importHistory(chosen);runOnUiThread(()->{if(closing)return;playlistId=id;playlistName="YouTube listening history";tab="library";render();toast("History imported · "+chosen.size()+" songs");});}catch(RuntimeException e){runOnUiThread(()->{if(!closing)toast("Couldn't import these entries. Your library was kept.");});}});
            });
        });dialog.show();
    }
    private void showAbout(){
        new AlertDialog.Builder(this).setTitle("Soundzy · "+getString(R.string.app_version)).setMessage(getString(R.string.developed_by)+"\n\nYour music, in your frequency.\n\nAndroid 14+ · No account required\n\nSearch and stream YouTube audio and discover public playlists with the open-source NewPipe Extractor. For you suggestions use the most recently played artist when available; other filters use the selected mood. Optional Google sign-in saves only your name and email locally; it does not grant YouTube history access. Takeout exports are processed locally. Offline audio and optional MP3 conversion stay on this device. There are no in-app ads. YouTube availability and extraction can change; some videos may not play.\n\nPlaylists and downloads stay on this device. Uninstalling clears them. Audio files are kept in app-private storage. Search requests and playback connect directly to YouTube/Google; thumbnail requests connect to their image servers. No analytics or developer server.\n\nOnly download content you have permission to save. This app is not affiliated with YouTube or Spotify.\n\nOpen-source: GPL-3.0-or-later. NewPipe Extractor and nanojson: GPL-3.0-or-later and Apache-2.0 respectively; AndroidX Media3: Apache-2.0; OkHttp: Apache-2.0. Full notices are included with the source.").setNeutralButton("Account & history",(d,w)->navigate("account")).setPositiveButton("Got it",null).show();
    }
    private void hideKeyboard(){View focus=getCurrentFocus();if(focus!=null)getSystemService(InputMethodManager.class).hideSoftInputFromWindow(focus.getWindowToken(),0);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private String time(long ms){long seconds=Math.max(0,ms/1000);return seconds>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
}
