package dev.rgbpulse.gboard;

import android.graphics.*;

/** One small, low-contrast pool behind the tap. New taps replace it, never stack. */
final class QuietBackground {
    private final Paint paint=new Paint(3);
    private final float dp;
    private long start=-1;
    private float x,y,radius;
    QuietBackground(float density){dp=density;}
    void tap(RectF key,long now){x=key.centerX();y=key.centerY();radius=QuietPolicy.radius(key.width(),key.height(),dp);start=now;}
    void clear(){start=-1;}
    boolean active(long now){return start>=0&&now-start<QuietPolicy.DURATION;}
    void draw(Canvas canvas,RectF clip,Config cfg,long now){
        if(!active(now))return;
        float p=(now-start)/(float)QuietPolicy.DURATION;
        float alpha=QuietPolicy.alpha(now-start,cfg.quietStrength);
        int color=Color.HSVToColor(new float[]{cfg.hue1,cfg.sat/100f,1});
        int tint=(color&0xffffff)|(Math.round(alpha*255)<<24);
        // Radius stays fixed: this is a gentle light response, not an expanding wave.
        paint.setShader(new RadialGradient(x,y+3*dp*p,Math.max(1,radius),tint,color&0xffffff,Shader.TileMode.CLAMP));
        int save=canvas.save();canvas.clipRect(clip);canvas.drawRect(x-radius,y-radius,x+radius,y+radius+3*dp,paint);canvas.restoreToCount(save);paint.setShader(null);
    }
}
