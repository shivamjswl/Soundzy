package app.cadence;

import android.content.Context;
import android.graphics.*;
import android.view.View;

public final class Icons extends View {
    private String kind; private int color; private final Paint p=new Paint(3);
    private final Path shape=new Path();
    private static final float[] WAVE_HEIGHTS={6,14,21,10};
    public Icons(Context c){this(c,"music",0xfff4f6ef);}
    public Icons(Context c,String kind,int color){super(c);this.kind=kind;this.color=color;setContentDescription(kind);}
    public void set(String kind,int color){this.kind=kind;this.color=color;invalidate();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.save();float size=Math.min(getWidth(),getHeight())*.52f;
        canvas.translate((getWidth()-size)/2,(getHeight()-size)/2);canvas.scale(size/24,size/24);
        p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        switch(kind){
            case "search" -> {canvas.drawCircle(10,10,6.5f,p);line(canvas,15,15,21,21);}
            case "home" -> {path(canvas,3,10,12,3,21,10,21,21,15,21,15,14,9,14,9,21,3,21,3,10);}
            case "library" -> {canvas.drawRoundRect(3,4,7,21,1,1,p);canvas.drawRoundRect(10,4,14,21,1,1,p);line(canvas,17,4,21,21);}
            case "download" -> {line(canvas,12,3,12,15);path(canvas,7,10,12,15,17,10);path(canvas,3,16,3,21,21,21,21,16);}
            case "play" -> {p.setStyle(Paint.Style.FILL);path(canvas,7,3,21,12,7,21,7,3);}
            case "pause" -> {p.setStyle(Paint.Style.FILL);canvas.drawRoundRect(6,3,10,21,1,1,p);canvas.drawRoundRect(15,3,19,21,1,1,p);}
            case "next" -> {p.setStyle(Paint.Style.FILL);path(canvas,5,4,17,12,5,20,5,4);canvas.drawRect(18,4,21,20,p);}
            case "previous" -> {p.setStyle(Paint.Style.FILL);path(canvas,19,4,7,12,19,20,19,4);canvas.drawRect(3,4,6,20,p);}
            case "heart", "liked" -> {if(kind.equals("liked"))p.setStyle(Paint.Style.FILL);Path h=shape;h.reset();h.moveTo(12,21);h.cubicTo(9,18,2,13,2,8);h.cubicTo(2,1,10,1,12,6);h.cubicTo(14,1,22,1,22,8);h.cubicTo(22,13,15,18,12,21);canvas.drawPath(h,p);}
            case "more" -> {p.setStyle(Paint.Style.FILL);for(int x=5;x<=19;x+=7)canvas.drawCircle(x,12,1.5f,p);}
            case "chevron" -> {path(canvas,9,4,17,12,9,20);}
            case "back" -> {path(canvas,15,4,7,12,15,20);}
            case "down" -> {path(canvas,5,8,12,15,19,8);}
            case "add" -> {line(canvas,12,4,12,20);line(canvas,4,12,20,12);}
            case "close" -> {line(canvas,5,5,19,19);line(canvas,19,5,5,19);}
            case "check" -> {path(canvas,4,12,10,18,21,5);}
            case "shuffle" -> {path(canvas,3,5,7,5,17,19,22,19);path(canvas,18,15,22,19,18,23);path(canvas,3,19,7,19,11,13);path(canvas,13,10,17,5,22,5);path(canvas,18,1,22,5,18,9);}
            case "repeat" -> {path(canvas,4,8,4,5,20,5,20,10);path(canvas,16,6,20,10,23,6);path(canvas,20,16,20,19,4,19,4,14);path(canvas,1,18,4,14,8,18);}
            case "queue" -> {line(canvas,3,5,21,5);line(canvas,3,11,21,11);line(canvas,3,17,12,17);p.setStyle(Paint.Style.FILL);path(canvas,17,14,23,18,17,22,17,14);}
            case "music" -> {path(canvas,9,17,9,5,21,3,21,15);canvas.drawOval(3,16,9,21,p);canvas.drawOval(15,14,21,19,p);}
            case "settings" -> {canvas.drawCircle(12,12,8,p);canvas.drawCircle(12,8,2.5f,p);canvas.drawArc(6,13,18,23,200,140,false,p);}
            case "wave" -> {for(int i=0;i<4;i++){float height=WAVE_HEIGHTS[i];line(canvas,4+i*5,12-height/2,4+i*5,12+height/2);}}
            default -> {canvas.drawCircle(12,12,9,p);line(canvas,12,7,12,13);canvas.drawCircle(12,17,.5f,p);}
        }
        canvas.restore();
    }
    private void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,p);}
    private void path(Canvas c,float...coords){Path path=shape;path.reset();path.moveTo(coords[0],coords[1]);for(int i=2;i<coords.length;i+=2)path.lineTo(coords[i],coords[i+1]);c.drawPath(path,p);}
}
