package dev.rgbpulse.gboard;
import android.graphics.*;
import android.view.*;
import java.util.*;
/** Delegates to StudioTiles for solid redesign. Keeps old name for compatibility with hooks. */
final class CutoutTiles {
 final StudioTiles studio=new StudioTiles();
 boolean hint(CapHooks.Entry e,String text,Paint p){return studio.hint(e,text,p);}
 void bind(List<View> keys){studio.bind(keys);}
 boolean draw(CapHooks.Entry e,String text,Paint p,float x,float y,Canvas c){return studio.draw(e,text,p,x,y,c);}
 void clear(){studio.clear();}
 static void fitHole(Path hole,RectF tile,boolean single){StudioTiles.fitHole(hole,tile,single);}
 static void fitHintHole(Path hole,RectF tile,boolean single){StudioTiles.fitHintHole(hole,tile,single);}
 static Path shape(RectF tile,Path hole){return StudioTiles.shape(tile,hole);}
 static void paint(Canvas c,RectF tile,Path shape,float dp,int alpha){StudioTiles.paint(c,tile,shape,dp,alpha);}
 static void paintHint(Canvas c,RectF tile,String hint,Config cfg){StudioTiles.paintHint(c,tile,hint,cfg);}
}
