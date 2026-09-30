package dev.rgbpulse.gboard;

import android.graphics.Canvas;
import android.graphics.RectF;

/** The same hard boundary as the working v4/v5 path; no off-screen bitmap. */
final class EffectSurface {
    void draw(Canvas canvas,Fx fx,RectF play,long now) {
        if(play.isEmpty())return;
        int save=canvas.save();
        try{canvas.clipRect(play);fx.drawFields(canvas,play,now);}
        finally{canvas.restoreToCount(save);}
    }
    void release(){ }
}
