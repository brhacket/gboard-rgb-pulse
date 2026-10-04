package dev.rgbpulse.gboard;

import android.animation.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

/** A bounded light-only overlay. Never moves keys, intercepts input or delays hide. */
final class LifecycleLight extends Drawable {
    private final ViewGroup host;
    private final Paint paint=new Paint(3);
    private final RectF area=new RectF();
    private ValueAnimator animator;
    private float progress;
    private int style,color;private boolean closing;
    LifecycleLight(ViewGroup host){this.host=host;}
    void play(int style,boolean closing,int color,RectF area){
        cancel();if(style==0||area.isEmpty()||!host.isShown())return;
        this.style=style;this.closing=closing;this.color=color;this.area.set(area);progress=0;
        host.getOverlay().add(this);setBounds(0,0,host.getWidth(),host.getHeight());
        animator=ValueAnimator.ofFloat(0,1);animator.setDuration(closing?300:620);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();invalidateSelf();});
        animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){host.getOverlay().remove(LifecycleLight.this);}});
        animator.start();
    }
    void cancel(){if(animator!=null){animator.cancel();animator=null;}host.getOverlay().remove(this);}
    private static int towardWhite(int rgb,float t){
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
        r=Math.round(r+(255-r)*t);g=Math.round(g+(255-g)*t);b=Math.round(b+(255-b)*t);
        return (r<<16)|(g<<8)|b;
    }
    private int withAlpha(int rgb,float a){return (rgb&0xffffff)|(Math.round(Math.max(0,Math.min(1,a))*255)<<24);}
    @Override public void draw(Canvas canvas){
        float t=closing?1-progress:progress;
        float te=1-(1-t)*(1-t)*(1-t); // eased sweep
        // Closing starts visibly instead of spending its few available frames fading in.
        float envelope=closing?1-progress:(float)Math.sin(Math.PI*progress);
        int base=color&0xffffff;
        int hot=towardWhite(base,.75f);
        float dp=host.getResources().getDisplayMetrics().density;
        int save=canvas.save();canvas.clipRect(area);
        paint.setStyle(Paint.Style.FILL);
        if(style==1){
            // Ignition: core flash, hot expanding crest with chromatic fringe, wide halo.
            float radius=Math.max(1,(float)Math.hypot(area.width(),area.height())*(.05f+te*.72f));
            paint.setShader(new RadialGradient(area.centerX(),area.centerY(),Math.max(1,radius*.55f),
                new int[]{withAlpha(hot,envelope*.65f*(1-te)),withAlpha(base,envelope*.30f),withAlpha(base,0)},null,Shader.TileMode.CLAMP));
            canvas.drawRect(area,paint);
            paint.setShader(new RadialGradient(area.centerX(),area.centerY(),radius*1.18f,
                new int[]{withAlpha(base,0),withAlpha(base,envelope*.45f),withAlpha(base,0)},new float[]{.55f,.84f,1},Shader.TileMode.CLAMP));
            canvas.drawRect(area,paint);
            paint.setShader(null);paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.6f*dp);paint.setColor(withAlpha(hot,envelope*.9f));
            canvas.drawCircle(area.centerX(),area.centerY(),radius*.86f,paint);
            paint.setStrokeWidth(1.4f*dp);paint.setColor(withAlpha(base,envelope*.4f));
            canvas.drawCircle(area.centerX(),area.centerY(),radius*.86f+4*dp,paint);
            canvas.drawCircle(area.centerX(),area.centerY(),Math.max(1,radius*.86f-4*dp),paint);
        }else if(style==2){
            // Stage curtains: light panels part with hot leading edges and fringe lines,
            // over a seam flash at the first moment.
            float gap=area.width()*.52f*te;
            float left=area.centerX()-gap,right=area.centerX()+gap,w=area.width()*.3f;
            paint.setShader(new LinearGradient(area.left,0,left,0,new int[]{withAlpha(base,0),withAlpha(base,envelope*.38f)},null,Shader.TileMode.CLAMP));
            canvas.drawRect(area.left,0,left,area.bottom,paint);
            paint.setShader(new LinearGradient(area.right,0,right,0,new int[]{withAlpha(base,0),withAlpha(base,envelope*.38f)},null,Shader.TileMode.CLAMP));
            canvas.drawRect(right,0,area.right,area.bottom,paint);
            paint.setShader(null);
            paint.setShader(new LinearGradient(left-w,0,left+w,0,new int[]{withAlpha(base,0),withAlpha(hot,envelope*.85f),withAlpha(base,0)},null,Shader.TileMode.CLAMP));
            canvas.drawRect(left-w,0,left+w,area.bottom,paint);
            paint.setShader(new LinearGradient(right-w,0,right+w,0,new int[]{withAlpha(base,0),withAlpha(hot,envelope*.85f),withAlpha(base,0)},null,Shader.TileMode.CLAMP));
            canvas.drawRect(right-w,0,right+w,area.bottom,paint);
            paint.setShader(null);
            int seam=withAlpha(hot,envelope*.5f*(1-te));
            paint.setShader(new LinearGradient(area.centerX()-w*.4f,0,area.centerX()+w*.4f,0,new int[]{withAlpha(base,0),seam,withAlpha(base,0)},null,Shader.TileMode.CLAMP));
            canvas.drawRect(area.centerX()-w*.4f,0,area.centerX()+w*.4f,area.bottom,paint);
        }else{
            // Horizon rise: a hot crest line with fringe, sky glow above and a shorter
            // reflection below.
            float y=area.bottom-area.height()*te,tail=area.height()*.5f;
            paint.setShader(new LinearGradient(0,y-tail,0,y,new int[]{withAlpha(base,0),withAlpha(base,envelope*.5f)},null,Shader.TileMode.CLAMP));
            canvas.drawRect(area.left,y-tail,area.right,y,paint);
            paint.setShader(new LinearGradient(0,y,0,y+tail*.5f,new int[]{withAlpha(base,envelope*.3f),withAlpha(base,0)},null,Shader.TileMode.CLAMP));
            canvas.drawRect(area.left,y,area.right,y+tail*.5f,paint);
            paint.setShader(null);
            paint.setColor(withAlpha(hot,envelope*.9f));canvas.drawRect(area.left,y-1.5f*dp,area.right,y+1.5f*dp,paint);
            paint.setColor(withAlpha(base,envelope*.4f));
            canvas.drawRect(area.left,y-5*dp,area.right,y-3*dp,paint);
            canvas.drawRect(area.left,y+3*dp,area.right,y+5*dp,paint);
        }
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
