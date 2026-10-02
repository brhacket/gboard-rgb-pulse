package dev.rgbpulse.gboard;

import android.graphics.*;

/** Shared, allocation-free renderer: low-opacity wash and hairline, confined to the key. */
final class RipplePaint {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF inset=new RectF();
    void draw(Canvas canvas,RectF bounds,float dp,float amount,float strength,float radius){
        float alpha=Math.max(0,Math.min(1,amount*strength));
        if(alpha<=.001f||bounds.width()<4*dp||bounds.height()<4*dp)return;
        inset.set(bounds);inset.inset(2*dp,2*dp);
        int save=canvas.save();canvas.clipRect(bounds);
        // Border only: never wash over/dim the native key face or its text.
        paint.setColor(0xfff3f5f4);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3*dp);paint.setAlpha(Math.round(24*alpha));
        canvas.drawRoundRect(inset,radius,radius,paint);
        paint.setStrokeWidth(.85f*dp);paint.setAlpha(Math.round(150*alpha));
        canvas.drawRoundRect(inset,radius,radius,paint);canvas.restoreToCount(save);
    }
}
