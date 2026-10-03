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
        this.style=style;this.closing=closing;this.color=color;this.area.set(area);
        host.getOverlay().add(this);setBounds(0,0,host.getWidth(),host.getHeight());
        animator=ValueAnimator.ofFloat(0,1);animator.setDuration(closing?160:320);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();invalidateSelf();});
        animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){host.getOverlay().remove(LifecycleLight.this);}});
        animator.start();
    }
    void cancel(){if(animator!=null){animator.cancel();animator=null;}host.getOverlay().remove(this);}
    @Override public void draw(Canvas canvas){
        float t=closing?1-progress:progress;
        float alpha=(float)Math.sin(Math.PI*progress)*.22f;
        int tint=(color&0xffffff)|(Math.round(alpha*255)<<24);
        int save=canvas.save();canvas.clipRect(area);
        if(style==1){
            paint.setShader(new RadialGradient(area.centerX(),area.centerY(),Math.max(1,area.width()*(.2f+t*.65f)),tint,color&0xffffff,Shader.TileMode.CLAMP));
        }else if(style==2){
            float x=area.left+area.width()*t,w=area.width()*.3f;
            paint.setShader(new LinearGradient(x-w,0,x+w,0,new int[]{color&0xffffff,tint,color&0xffffff},null,Shader.TileMode.CLAMP));
        }else{
            paint.setShader(new LinearGradient(0,area.bottom,0,area.top,new int[]{tint,color&0xffffff},null,Shader.TileMode.CLAMP));
        }
        canvas.drawRect(area,paint);paint.setShader(null);canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
