package dev.rgbpulse.gboard;

import android.graphics.*;

/** One clearly visible pool of light behind the tap. New taps replace it, never stack. */
final class QuietBackground {
    private final Paint paint=new Paint(3);
    private final float dp;
    private long start=-1;
    private float x,y,radius;
    QuietBackground(float density){dp=density;}
    void tap(RectF key,long now){x=key.centerX();y=key.centerY();radius=QuietPolicy.radius(key.width(),key.height(),dp);start=now;}
    void clear(){start=-1;}
    boolean active(long now){return start>=0&&now-start<QuietPolicy.DURATION;}
    private static int towardWhite(int rgb,float t){
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
        r=Math.round(r+(255-r)*t);g=Math.round(g+(255-g)*t);b=Math.round(b+(255-b)*t);
        return (r<<16)|(g<<8)|b;
    }
    void draw(Canvas canvas,RectF clip,Config cfg,long now){
        if(!active(now))return;
        float p=(now-start)/(float)QuietPolicy.DURATION;
        float alpha=QuietPolicy.alpha(now-start,cfg.quietStrength);
        int color=Color.HSVToColor(new float[]{cfg.hue1,cfg.sat/100f,1});
        int base=color&0xffffff;
        // Layered light: a hot core over the tint, plus a wide soft halo, so the
        // pool reads as light rather than a flat sticker.
        int core=towardWhite(base,.55f)|(Math.round(alpha*255)<<24);
        int mid=base|(Math.round(alpha*.8f*255)<<24);
        int halo=base|(Math.round(alpha*.35f*255)<<24);
        int edge=base&0xffffff;
        // Radius stays fixed: this is a gentle light response, not an expanding wave.
        // The pool sinks a few dp while it fades, so the background visibly moves.
        float sink=6*dp*p;
        float r=Math.max(1,radius)*(1f+.05f*(float)Math.sin(p*6.28318f));
        int save=canvas.save();canvas.clipRect(clip);
        paint.setShader(new RadialGradient(x,y+sink,r*1.7f,new int[]{halo,edge},new float[]{0f,1f},Shader.TileMode.CLAMP));
        canvas.drawRect(x-r*1.7f,y-r*1.7f+sink,x+r*1.7f,y+r*1.7f+sink,paint);
        paint.setShader(new RadialGradient(x,y+sink,r,new int[]{core,mid,edge},new float[]{0f,.45f,1f},Shader.TileMode.CLAMP));
        canvas.drawRect(x-r,y-r+sink,x+r,y+r+sink,paint);
        canvas.restoreToCount(save);paint.setShader(null);
    }
}
