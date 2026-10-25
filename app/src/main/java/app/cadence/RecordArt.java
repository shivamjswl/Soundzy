package app.cadence;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public final class RecordArt extends View {
    private final Paint p=new Paint(3);
    private RadialGradient glow;
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);glow=new RadialGradient(w*.57f,h*.5f,Math.max(1,Math.min(w,h)*.66f),new int[]{0x555e9b30,0x00101310},null,Shader.TileMode.CLAMP);}
    public RecordArt(Context c){super(c);setContentDescription("MuseDock vinyl record artwork");}
    @Override protected void onDraw(Canvas c){
        float w=getWidth(),h=getHeight(),r=Math.min(w,h)*.44f,x=w*.57f,y=h*.5f;
        p.setShader(glow);c.drawRect(0,0,w,h,p);p.setShader(null);
        p.setColor(0xff111910);c.drawCircle(x,y,r,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);
        for(int i=0;i<16;i++){p.setColor(i%3==0?0xff33422b:0xff222e20);c.drawCircle(x,y,r*(.35f+i*.039f),p);}
        p.setStyle(Paint.Style.FILL);p.setColor(0xffb8f36d);c.drawCircle(x,y,r*.31f,p);
        p.setColor(0xff15200d);c.drawCircle(x,y,r*.065f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(r*.1f);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(0x22ffffff);c.drawArc(x-r*.8f,y-r*.8f,x+r*.8f,y+r*.8f,210,45,false,p);p.setStyle(Paint.Style.FILL);
    }
}
