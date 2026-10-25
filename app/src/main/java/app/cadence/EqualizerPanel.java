package app.cadence;

import android.app.*;
import android.content.res.ColorStateList;
import android.os.*;
import android.view.*;
import android.widget.*;

public final class EqualizerPanel {
    private final Activity activity; private final AudioEqualizer equalizer;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Dialog dialog; private TextView status, preset; private final SeekBar[] sliders=new SeekBar[5]; private final TextView[] gains=new TextView[5];
    public EqualizerPanel(Activity activity,AudioEqualizer equalizer){this.activity=activity;this.equalizer=equalizer;}
    public void show(){
        LinearLayout page=new LinearLayout(activity);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(24),dp(16),dp(24),dp(16));page.setBackgroundColor(0xff1c211b);
        Switch enabled=new Switch(activity);enabled.setText("Equalizer enabled");enabled.setTextColor(0xfff4f6ef);enabled.setChecked(equalizer.enabled());enabled.setOnCheckedChangeListener((v,on)->{equalizer.setEnabled(on);updateStatus();});page.addView(enabled,new LinearLayout.LayoutParams(-1,dp(56)));
        status=text(13);page.addView(status);preset=text(16);page.addView(preset,new LinearLayout.LayoutParams(-1,dp(46)));
        HorizontalScrollView rail=new HorizontalScrollView(activity);LinearLayout buttons=new LinearLayout(activity);
        for(int i=0;i<AudioEqualizer.PRESETS.length;i++){final int index=i;Button button=new Button(activity);button.setText(AudioEqualizer.PRESETS[i]);button.setOnClickListener(v->{equalizer.preset(index);refresh();});buttons.addView(button);}rail.addView(buttons);page.addView(rail);
        for(int i=0;i<5;i++){
            final int index=i;TextView label=text(14);int hz=AudioEqualizer.FREQUENCIES[i];label.setText(hz>=1000?String.format(java.util.Locale.ROOT,"%.1f kHz",hz/1000f):hz+" Hz");page.addView(label);
            LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);SeekBar slider=new SeekBar(activity);slider.setMax(2400);slider.setProgressTintList(ColorStateList.valueOf(0xffb8f36d));slider.setThumbTintList(ColorStateList.valueOf(0xffb8f36d));slider.setContentDescription("Equalizer "+hz+" Hz");sliders[i]=slider;row.addView(slider,new LinearLayout.LayoutParams(0,dp(46),1));gains[i]=text(13);row.addView(gains[i],new LinearLayout.LayoutParams(dp(66),-2));page.addView(row);
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int progress,boolean user){int value=progress-1200;gains[index].setText(String.format(java.util.Locale.ROOT,"%+.1f dB",value/100f));if(user){equalizer.setBand(index,value);updateStatus();}}});
        }
        TextView note=text(12);note.setText("Applies to streaming and offline audio. Available bands and gain limits depend on your device. Settings are kept when you close the app.");page.addView(note);
        ScrollView scroll=new ScrollView(activity);scroll.addView(page);dialog=new AlertDialog.Builder(activity).setTitle("MuseDock equalizer").setView(scroll).setPositiveButton("Done",null).create();dialog.setOnDismissListener(d->handler.removeCallbacksAndMessages(null));dialog.show();refresh();handler.post(new Runnable(){public void run(){if(!dialog.isShowing())return;updateStatus();handler.postDelayed(this,700);}});
    }
    public void dismiss(){if(dialog!=null)dialog.dismiss();}
    private void refresh(){int[] values=equalizer.levels();for(int i=0;i<5;i++){sliders[i].setProgress(values[i]+1200);gains[i].setText(String.format(java.util.Locale.ROOT,"%+.1f dB",values[i]/100f));}updateStatus();}
    private void updateStatus(){status.setText(equalizer.status());preset.setText("Curve: "+equalizer.preset());}
    private TextView text(int size){TextView v=new TextView(activity);v.setTextSize(size);v.setTextColor(0xfff4f6ef);return v;}
    private int dp(int size){return Math.round(size*activity.getResources().getDisplayMetrics().density);}
}
