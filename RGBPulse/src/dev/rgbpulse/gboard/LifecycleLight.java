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
        animator=ValueAnimator.ofFloat(0,1);animator.setDuration(closing?260:520);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener(a->{progress=(Float)a.getAnimatedValue();invalidateSelf();});
        animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){host.getOverlay().remove(LifecycleLight.this);}});
        animator.start();
    }
    void cancel(){if(animator!=null){animator.cancel();animator=null;}host.getOverlay().remove(this);}
    @Override public void draw(Canvas canvas){
        float t=closing?1-progress:progress;
        // Closing starts visibly instead of spending its few available frames fading in.
        float envelope=closing?1-progress:(float)Math.sin(Math.PI*progress);
        int tint=(color&0xffffff)|(Math.round(envelope*.65f*255)<<24);
        int transparent=color&0xffffff;
        int save=canvas.save();canvas.clipRect(area);
        paint.setStyle(Paint.Style.FILL);
        if(style==1){
            float radius=Math.max(1,(float)Math.hypot(area.width(),area.height())*(.05f+t*.65f));
            paint.setShader(new RadialGradient(area.centerX(),area.centerY(),radius,
                new int[]{transparent,tint,transparent},new float[]{.50f,.82f,1},Shader.TileMode.CLAMP));
            canvas.drawRect(area,paint);paint.setShader(null);
            paint.setColor((color&0xffffff)|(Math.round(envelope*.8f*255)<<24));paint.setStrokeWidth(2*host.getResources().getDisplayMetrics().density);paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(area.centerX(),area.centerY(),radius*.82f,paint);
        }else if(style==2){
            float gap=area.width()*.5f*t;
            float left=area.centerX()-gap,right=area.centerX()+gap,w=area.width()*.25f;
            paint.setShader(new LinearGradient(left-w,0,left+w,0,new int[]{transparent,tint,transparent},null,Shader.TileMode.CLAMP));
            canvas.drawRect(area,paint);
            paint.setShader(new LinearGradient(right-w,0,right+w,0,new int[]{transparent,tint,transparent},null,Shader.TileMode.CLAMP));
            canvas.drawRect(area,paint);
        }else{
            float y=area.bottom-area.height()*t,tail=area.height()*.5f;
            paint.setShader(new LinearGradient(0,y-tail,0,y+tail,new int[]{transparent,tint,transparent},null,Shader.TileMode.CLAMP));
            canvas.drawRect(area,paint);paint.setShader(null);paint.setColor(tint);
            canvas.drawRect(area.left,y,area.right,y+3*host.getResources().getDisplayMetrics().density,paint);
        }
        paint.setShader(null);paint.setStyle(Paint.Style.FILL);canvas.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
