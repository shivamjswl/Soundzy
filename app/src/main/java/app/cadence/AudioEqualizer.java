package app.cadence;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.Equalizer;

/** Owns an effect on this player's audio session, never the global output mix. */
public final class AudioEqualizer {
    public static final int[] FREQUENCIES = {60, 230, 910, 3600, 14000};
    public static final String[] PRESETS = {"Flat", "Bass boost", "Vocal", "Treble boost"};
    private static final int[][] CURVES = {{0,0,0,0,0}, {500,350,0,-100,-100}, {-150,0,350,300,0}, {-100,-100,0,300,500}};
    interface Effect {
        int bands(); int frequency(int band); int minimum(); int maximum();
        void level(int band, int value); void enabled(boolean value); void release();
    }
    interface Factory { Effect create(int session); }
    private final SharedPreferences prefs;
    private final Factory factory;
    private Effect effect;
    private int session;
    private String status = "Start a song to connect the equalizer to its audio output.";
    public AudioEqualizer(Context context) { this(context.getSharedPreferences("musedock.equalizer",Context.MODE_PRIVATE), AudioEqualizer::androidEffect); }
    AudioEqualizer(SharedPreferences prefs, Factory factory) { this.prefs=prefs; this.factory=factory; }
    private static Effect androidEffect(int session) {
        Equalizer eq=new Equalizer(0,session);
        return new Effect() {
            public int bands(){return eq.getNumberOfBands();}
            public int frequency(int band){return eq.getCenterFreq((short)band)/1000;}
            public int minimum(){return eq.getBandLevelRange()[0];}
            public int maximum(){return eq.getBandLevelRange()[1];}
            public void level(int band,int value){if(!eq.hasControl())throw new IllegalStateException("Equalizer has no control");eq.setBandLevel((short)band,(short)value);}
            public void enabled(boolean value){if(!eq.hasControl()||eq.setEnabled(value)!=android.media.audiofx.AudioEffect.SUCCESS)throw new IllegalStateException("Equalizer has no control");}
            public void release(){eq.release();}
        };
    }
    public synchronized void attach(int id) {
        if(id==session&&effect!=null)return;
        release(); if(id<=0)return;
        try { effect=factory.create(id);session=id;apply(); }
        catch(RuntimeException error){unavailable();}
    }
    public synchronized void release() {
        if(effect!=null){try{effect.release();}catch(RuntimeException ignored){} effect=null;}
        session=0;status="Start a song to connect the equalizer to its audio output.";
    }
    private void unavailable(){release();status="Equalizer unavailable on this audio output. Your settings are saved for another output or session.";}
    public synchronized boolean available(){return effect!=null;}
    public synchronized String status(){return status;}
    public boolean enabled(){return prefs.getBoolean("enabled",false);}
    public int[] levels(){int[] values=new int[5];for(int i=0;i<5;i++)values[i]=Math.max(-1200,Math.min(1200,prefs.getInt("band"+i,0)));return values;}
    public synchronized void setEnabled(boolean value){prefs.edit().putBoolean("enabled",value).apply();apply();}
    public synchronized void setBand(int band,int value){if(band<0||band>=5)throw new IllegalArgumentException("Invalid equalizer band");prefs.edit().putInt("band"+band,Math.max(-1200,Math.min(1200,value))).putString("preset","Custom").apply();apply();}
    public synchronized void preset(int index){if(index<0||index>=CURVES.length)throw new IllegalArgumentException("Invalid preset");SharedPreferences.Editor edit=prefs.edit().putString("preset",PRESETS[index]);for(int i=0;i<5;i++)edit.putInt("band"+i,CURVES[index][i]);edit.apply();apply();}
    public String preset(){return prefs.getString("preset","Flat");}
    /** Interpolate on logarithmic frequency so devices with different band counts share the curve. */
    static int gain(int hz,int[] values){
        if(hz<=FREQUENCIES[0])return values[0];
        for(int i=1;i<5;i++)if(hz<=FREQUENCIES[i]){double fraction=Math.log((double)hz/FREQUENCIES[i-1])/Math.log((double)FREQUENCIES[i]/FREQUENCIES[i-1]);return (int)Math.round(values[i-1]+fraction*(values[i]-values[i-1]));}
        return values[4];
    }
    private void apply(){
        if(effect==null)return;
        try { int[] values=levels();for(int b=0;b<effect.bands();b++)effect.level(b,Math.max(effect.minimum(),Math.min(effect.maximum(),gain(effect.frequency(b),values))));effect.enabled(enabled());status=enabled()?"Active on this player's audio output":"Off · Original sound"; }
        catch(RuntimeException error){unavailable();}
    }
}
