package dev.rgbpulse.gboard;

import android.graphics.*;

/** Shared, allocation-free renderer: state-color border with a soft halo, confined to the key. */
final class RipplePaint {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF inset=new RectF();
    void draw(Canvas canvas,RectF bounds,float dp,float amount,float strength,float radius){
        draw(canvas,bounds,dp,amount,strength,radius,0xfff3f5f4,0xff49454f,.85f);
    }
    void draw(Canvas canvas,RectF bounds,float dp,float amount,float strength,float radius,int active,int inactive,float width){
        float level=Math.max(0,Math.min(1,amount));
        float alpha=Math.max(0,Math.min(1,strength))*(.18f+.82f*level);
        if(alpha<=.001f||bounds.width()<4*dp||bounds.height()<4*dp)return;
        inset.set(bounds);inset.inset(Math.max(2,width*.5f+1)*dp,Math.max(2,width*.5f+1)*dp);
        int save=canvas.save();canvas.clipRect(bounds);
        // Border only: never wash over/dim the native key face or its text.
        paint.setColor(LegendTint.color(0xffffffff,level,inactive,active));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth((width+2)*dp);paint.setAlpha(Math.round(24*strength*level));
        canvas.drawRoundRect(inset,radius,radius,paint);
        paint.setStrokeWidth(width*dp);paint.setAlpha(Math.round(150*alpha));
        canvas.drawRoundRect(inset,radius,radius,paint);canvas.restoreToCount(save);
    }
}
